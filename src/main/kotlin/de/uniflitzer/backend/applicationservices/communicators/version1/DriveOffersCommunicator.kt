package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.*
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.applicationservices.geography.GeographyService
import de.uniflitzer.backend.model.Car
import de.uniflitzer.backend.model.CarpoolDriveOffer
import de.uniflitzer.backend.model.Coordinate
import de.uniflitzer.backend.model.DriveOffer
import de.uniflitzer.backend.model.PublicDriveOffer
import de.uniflitzer.backend.model.errors.*
import de.uniflitzer.backend.model.Route
import de.uniflitzer.backend.model.Seats
import de.uniflitzer.backend.model.User
import de.uniflitzer.backend.repositories.DriveOffersRepository
import de.uniflitzer.backend.repositories.UsersRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.ZonedDateTime
import java.util.UUID as UUIDType
import kotlin.jvm.optionals.getOrNull

@RestController
@RequestMapping("v1/drive-offers")
@Validated
@Tag(name = "Drive Offers")
@SecurityRequirement(name = "Token Authentication")
private class DriveOffersCommunicator(
    @field:Autowired private val driveOffersRepository: DriveOffersRepository,
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val geographyService: GeographyService
) {
    @Operation(description = "Get all drive offers.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("")
    fun getDriveOffers(@RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int, @RequestParam latitude:Double, @RequestParam longitude: Double, @RequestParam sortingDirection: SortingDirection = SortingDirection.Ascending, userToken: UserToken): ResponseEntity<PageDP<PartialDriveOfferDP>> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val destinationCoordinate: Coordinate = Coordinate(latitude, longitude)
        val searchedDriveOffers: List<DriveOffer> = driveOffersRepository.findAll(
            Sort.by(
                when (sortingDirection) {
                    SortingDirection.Ascending -> Sort.Direction.ASC
                    SortingDirection.Descending -> Sort.Direction.DESC
                },
                DriveOffer::plannedDeparture.name
            )
        )
        .filter { it.route.destination.coordinate distanceTo destinationCoordinate <= 1000.0 }

        return ResponseEntity.ok(
            PageDP(
                maximumPage = (searchedDriveOffers.size / perPage) + 1,
                content = searchedDriveOffers
                    .subList((pageNumber - 1) * perPage, pageNumber * perPage)
                    .map { PartialDriveOfferDP.fromDriveOffer(it) }
            )
        )
    }

    @Operation(description = "Get details of a specific drive offer.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{id}")
    fun getDriveOffer(@PathVariable @UUID id: String, userToken: UserToken): ResponseEntity<DetailedDriveOfferDP> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))
        val searchedDriveOffer: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("The drive offer with the id $id could not be found."))

        return ResponseEntity.ok(
            DetailedDriveOfferDP.fromDriveOffer(searchedDriveOffer)
        )
    }

    @Operation(description = "Get the image of a specific car of a specific drive offer.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content =  [Content(mediaType = MediaType.IMAGE_JPEG_VALUE)]
            )
        ]
    )
    @CommonApiResponses @NotFoundApiResponse
    @GetMapping("{id}/car/image")
    fun getImageOfCar(@PathVariable @UUID id: String, @RequestParam quality: QualityDP): ResponseEntity<ByteArray> {
        TODO()
    }

    @Operation(description = "Create a new drive offer.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping
    fun createDriveOffer(@RequestBody @Valid driveOfferCreation: DriveOfferCreationDP, userToken: UserToken): ResponseEntity<IdDP> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))
        val selectedCarForDriverOffer: Car = actingUser.cars.getOrNull(driveOfferCreation.carIndex) ?: throw NotFoundError(ErrorDP("The car with the index ${driveOfferCreation.carIndex} could not be found."))

        val newDriveOffer: DriveOffer = when (driveOfferCreation) {
            is PublicDriveOfferCreationDP ->
                PublicDriveOffer(
                    actingUser,
                    selectedCarForDriverOffer,
                    Seats(driveOfferCreation.freeSeats.toUInt()),
                    Route(
                        geographyService.createPosition(driveOfferCreation.route.start.toCoordinate()),
                        geographyService.createPosition(driveOfferCreation.route.destination.toCoordinate())
                    ),
                    driveOfferCreation.plannedDepartureTime?.let { ZonedDateTime.parse(it) }
                )
            is CarpoolDriveOfferCreationDP -> TODO()
        }
        driveOffersRepository.save(newDriveOffer)

        return ResponseEntity.status(HttpStatus.CREATED).body(IdDP(newDriveOffer.id.toString()))
    }

    @Operation(description = "Update a specific drive offer. Only the planned departure time can be updated.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PatchMapping("{id}")
    fun updateDriveOffer(@PathVariable @UUID id: String, @RequestBody @Valid driveOfferUpdate: DriverOfferUpdateDP, userToken: UserToken):ResponseEntity<Void> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))
        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("The drive offer with the id $id could not be found."))
        if (driveOfferInEditing.driver.id.toString() != userToken.id) throw ForbiddenError(ErrorDP("The user with the id $id is not the driver of the drive offer with the id $id."))

        driveOfferInEditing.plannedDeparture = ZonedDateTime.parse(driveOfferUpdate.plannedDepartureTime)
        driveOffersRepository.save(driveOfferInEditing)

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Request the ride for a specific drive offer.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{id}/requests")
    fun requestSeat(@PathVariable @UUID id: String, @RequestBody @Valid coordinate: CoordinateDP, userToken: UserToken):ResponseEntity<Void> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))
        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("The drive offer with the id $id could not be found."))
        if(driveOfferInEditing.driver.id == actingUser.id) throw ForbiddenError(ErrorDP("The requesting user cannot be the driver of the same drive offer."))

        when (driveOfferInEditing) {
            is PublicDriveOffer -> {
                try {
                    driveOfferInEditing.addRequestFromUser(actingUser, geographyService.createPosition(coordinate.toCoordinate()))
                } catch (_: NotAvailableError) {
                    throw ForbiddenError(ErrorDP("No free seats left in the drive offer with the id $id."))
                } catch (_: RepeatedActionError) {
                    throw ForbiddenError(ErrorDP("The user with the id ${actingUser.id} has already requested a seat in the drive offer with the id $id."))
                }

            }
            is CarpoolDriveOffer -> TODO()
        }
        driveOffersRepository.save(driveOfferInEditing)

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Accept a requesting user for a specific drive offer.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveOfferId}/requesting-users/{requestingUserId}/acceptances")
    fun acceptRequestingUser(@PathVariable @UUID driveOfferId: String, @PathVariable @UUID requestingUserId: String, userToken: UserToken):ResponseEntity<Void> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))
        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError(ErrorDP("The drive offer with the id $driveOfferId could not be found."))
        if (driveOfferInEditing.driver.id != actingUser.id) throw ForbiddenError(ErrorDP("A user who is not the driver cannot accept requests"))
        if(!usersRepository.existsById(UUIDType.fromString(requestingUserId))) throw NotFoundError(ErrorDP("The requesting user with the id $requestingUserId could not be found."))

        when (driveOfferInEditing) {
            is PublicDriveOffer -> {
                try {
                    driveOfferInEditing.acceptRequestFromUser(UUIDType.fromString(requestingUserId))
                } catch (_: MissingActionError) {
                    throw NotFoundError(ErrorDP("The requesting user with the id $requestingUserId could not be found in the drive offer with the id $driveOfferId."))
                } catch (_: NotAvailableError) {
                    throw ForbiddenError(ErrorDP("No free seats left in the drive offer with the id $driveOfferId."))
                }
            }
            is CarpoolDriveOffer -> TODO()
        }
        driveOffersRepository.save(driveOfferInEditing)

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Reject a requesting user for a specific drive offer")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveOfferId}/requesting-users/{requestingUserId}/rejections")
    fun rejectRequestingUser(@PathVariable @UUID driveOfferId: String, @PathVariable @UUID requestingUserId: String, userToken: UserToken):ResponseEntity<Void> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))
        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError(ErrorDP("The drive offer with the id $driveOfferId could not be found."))
        if (driveOfferInEditing.driver.id != actingUser.id) throw ForbiddenError(ErrorDP("A user who is not the driver cannot accept requests"))
        if(!usersRepository.existsById(UUIDType.fromString(requestingUserId))) throw NotFoundError(ErrorDP("The requesting user with the id $requestingUserId could not be found."))

        when (driveOfferInEditing) {
            is PublicDriveOffer -> {
                try {
                    driveOfferInEditing.rejectRequestFromUser(UUIDType.fromString(requestingUserId))
                } catch (_: MissingActionError) {
                    throw NotFoundError(ErrorDP("The requesting user with the id $requestingUserId could not be found in the drive offer with the id $driveOfferId."))
                }
            }
            is CarpoolDriveOffer -> TODO()
        }
        driveOffersRepository.save(driveOfferInEditing)

        return ResponseEntity.noContent().build()
    }
}