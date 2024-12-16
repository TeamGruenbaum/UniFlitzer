package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.NotFoundError
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.applicationservices.geography.GeographyService
import de.uniflitzer.backend.model.*
import de.uniflitzer.backend.model.errors.MissingActionError
import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.model.errors.RepeatedActionError
import de.uniflitzer.backend.repositories.DriveOffersRepository
import de.uniflitzer.backend.repositories.ImagesRepository
import de.uniflitzer.backend.repositories.UsersRepository
import de.uniflitzer.backend.repositories.errors.ImageDirectoryMissingError
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
import kotlin.jvm.optionals.getOrNull
import java.util.UUID as UUIDType

@RestController
@RequestMapping("v1/drive-offers")
@Validated
@Tag(name = "Drive Offers")
@SecurityRequirement(name = "Token Authentication")
private class DriveOffersCommunicator(
    @field:Autowired private val driveOffersRepository: DriveOffersRepository,
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val geographyService: GeographyService,
    @field:Autowired private val imagesRepository: ImagesRepository
) {
    @Operation(description = "Get all drive offers.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("")
    fun getDriveOffers(
        @RequestParam @Min(1) pageNumber: Int,
        @RequestParam @Min(1) @Max(50) perPage: Int,
        @RequestParam startLatitude:Double,
        @RequestParam startLongitude: Double,
        @RequestParam destinationLatitude:Double,
        @RequestParam destinationLongitude: Double,
        @RequestParam allowedAnimals: List<AnimalDP>? = null,
        @RequestParam isSmoking: Boolean? = null,
        @RequestParam allowedDrivingStyles: List<DrivingStyleDP>? = null,
        @RequestParam allowedGenders: List<GenderDP>? = null,
        @RequestParam sortingDirection: SortingDirection = SortingDirection.Ascending,
        userToken: UserToken
    ): ResponseEntity<PageDP<PartialDriveOfferDP>> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val allowedAnimals: List<Animal>? = allowedAnimals?.map { it.toAnimal() }
        val allowedDrivingStyles: List<DrivingStyle>? = allowedDrivingStyles?.map { it.toDrivingStyle() }
        val allowedGenders: List<Gender>? = allowedGenders?.map { it.toGender() }

        val startCoordinate: Coordinate = Coordinate(startLatitude, startLongitude)
        val destinationCoordinate: Coordinate = Coordinate(destinationLatitude, destinationLongitude)
        val tolerance: Meters = Meters(1000.0)
        val searchedDriveOffers: List<DriveOffer> =
            driveOffersRepository.findAll(
                allowedAnimals,
                isSmoking,
                allowedDrivingStyles,
                allowedGenders,
                actingUser.blockedUsers,
                Sort.by(
                    when (sortingDirection) {
                        SortingDirection.Ascending -> Sort.Direction.ASC
                        SortingDirection.Descending -> Sort.Direction.DESC
                    },
                    DriveOffer::plannedDeparture.name
                )
            )
            //TODO: CarpoolDriveOffers
            .filter { it.route.isCoordinateOnRoute(startCoordinate, tolerance) && it.route.isCoordinateOnRoute(destinationCoordinate, tolerance) }
            .filter { it.route.areCoordinatesInCorrectDirection(startCoordinate, destinationCoordinate) }

        return ResponseEntity.ok(
            PartialDriveOfferPageDP.fromList(
                searchedDriveOffers.map { PartialDriveOfferDP.fromDriveOffer(it, it.driver in actingUser.favoriteUsers) },
                pageNumber.toUInt(),
                perPage.toUInt()
            )
        )
    }

    @Operation(description = "Get details of a specific drive offer.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{id}")
    fun getDriveOffer(@PathVariable @UUID id: String, userToken: UserToken): ResponseEntity<DetailedDriveOfferDP> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))
        val searchedDriveOffer: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("The drive offer with the id $id could not be found."))

        return ResponseEntity.ok(
            DetailedDriveOfferDP.fromDriveOffer(searchedDriveOffer, searchedDriveOffer.driver in actingUser.favoriteUsers)
        )
    }

    @Operation(description = "Get the image of the car of a specific drive offer.")
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
    fun getImageOfCar(@PathVariable @UUID id: String, @RequestParam quality: QualityDP, userToken: UserToken): ResponseEntity<ByteArray> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val driveOffer: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("DriveOffer with id $id not found."))

        val car: Car = driveOffer.car
        if (car.image == null) throw NotFoundError(ErrorDP("Car has no image."))
        try {
            val image:ByteArray = imagesRepository.getById(car.image!!.id, if(quality == QualityDP.Preview) ImagesRepository.Quality.Preview else ImagesRepository.Quality.Full).getOrNull() ?: throw NotFoundError(ErrorDP("Image not found."))
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image)
        } catch (error: ImageDirectoryMissingError) {
            throw NotFoundError(ErrorDP(error.message!!))
        }
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
                    geographyService.createRoute(
                        geographyService.createPosition(driveOfferCreation.route.start.toCoordinate()),
                        geographyService.createPosition(driveOfferCreation.route.destination.toCoordinate())
                    ),
                    driveOfferCreation.plannedDepartureTime?.let { ZonedDateTime.parse(it) }
                )
            is CarpoolDriveOfferCreationDP -> TODO()
        }
        selectedCarForDriverOffer.image?.let { actingUserCarImage -> newDriveOffer.car.image = imagesRepository.copy(actingUserCarImage) }
        driveOffersRepository.save(newDriveOffer)

        return ResponseEntity.status(HttpStatus.CREATED).body(IdDP(newDriveOffer.id.toString()))
    }

    @Operation(description = "Delete a drive offer.")
    @CommonApiResponses @CreatedApiResponse
    @DeleteMapping("{id}/")
    fun deleteDriveOffer(@PathVariable @UUID id: String, @RequestBody @Valid userToken: UserToken): ResponseEntity<IdDP> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))
        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("The drive offer with the id $id could not be found."))
        if (driveOfferInEditing.driver.id.toString() != userToken.id) throw ForbiddenError(ErrorDP("The user with the id $id is not the driver of the drive offer with the id $id."))

        driveOffersRepository.delete(driveOfferInEditing)
        driveOffersRepository.flush()

        return ResponseEntity.noContent().build()
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
    fun requestSeat(@PathVariable @UUID id: String, @RequestBody @Valid userStopCreation: UserStopCreationDP, userToken: UserToken):ResponseEntity<Void> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))
        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("The drive offer with the id $id could not be found."))
        if(driveOfferInEditing.driver.id == actingUser.id) throw ForbiddenError(ErrorDP("The requesting user cannot be the driver of the same drive offer."))
        if(actingUser in driveOfferInEditing.driver.blockedUsers) throw ForbiddenError(ErrorDP("The requesting user ${actingUser.id} is blocked by the driver and cannot request a seat in this drive offer."))

        when (driveOfferInEditing) {
            is PublicDriveOffer -> {
                try {
                    driveOfferInEditing.addRequestFromUser(
                        actingUser,
                        geographyService.createPosition(userStopCreation.start.toCoordinate()),
                        geographyService.createPosition(userStopCreation.destination.toCoordinate())

                    )
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