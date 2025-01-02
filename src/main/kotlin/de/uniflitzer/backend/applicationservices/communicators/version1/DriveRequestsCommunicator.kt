package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.*
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.applicationservices.geography.GeographyService
import de.uniflitzer.backend.model.*
import de.uniflitzer.backend.model.errors.ConflictingActionError
import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.model.errors.RepeatedActionError
import de.uniflitzer.backend.repositories.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.ZonedDateTime
import kotlin.jvm.optionals.getOrNull
import java.util.UUID as UUIDType

@RestController @RequestMapping("v1/drive-requests")
@Tag(name = "Drive Requests") @SecurityRequirement(name = "Token Authentication")
@Validated
@Transactional(rollbackFor = [Throwable::class])
private class DriveRequestsCommunicator(
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val driveRequestsRepository: DriveRequestsRepository,
    @field:Autowired private val driveOffersRepository: DriveOffersRepository,
    @field:Autowired private val carpoolsRepository: CarpoolsRepository,
    @field:Autowired private val geographyService: GeographyService,
    @field:Autowired private val imagesRepository: ImagesRepository
)
{
    @Operation(description = "Create a new drive request.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("")
    fun createDriveRequest(@RequestBody @Valid driveRequestCreation: DriveRequestCreationDP, userToken: UserToken): ResponseEntity<IdDP>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")

        val driveRequest: DriveRequest =
        when (driveRequestCreation)
        {
            is CarpoolDriveRequestCreationDP -> {
                CarpoolDriveRequest(
                    user,
                    geographyService.createRoute(
                        geographyService.createPosition(driveRequestCreation.route.start.toCoordinate()),
                        geographyService.createPosition(driveRequestCreation.route.destination.toCoordinate())
                    ),
                    driveRequestCreation.scheduleTime?.toScheduleTime(),
                    carpoolsRepository.findById(UUIDType.fromString(driveRequestCreation.carpoolId)).getOrNull() ?: throw NotFoundError("Carpool with id ${driveRequestCreation.carpoolId} not found.")
                )
            }
            is PublicDriveRequestCreationDP -> {
                PublicDriveRequest(
                    user,
                    geographyService.createRoute(
                        geographyService.createPosition(driveRequestCreation.route.start.toCoordinate()),
                        geographyService.createPosition(driveRequestCreation.route.destination.toCoordinate())
                    ),
                    driveRequestCreation.scheduleTime?.toScheduleTime()
                )
            }
        }
        driveRequestsRepository.saveAndFlush(driveRequest)
        return ResponseEntity.status(201).body(IdDP(driveRequest.id.toString()))
    }

    @Operation(description = "Get all drive requests.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("")
    fun getDriveRequests(
        @RequestParam @Min(1) pageNumber: Int,
        @RequestParam @Min(1) @Max(50) perPage: Int,
        @RequestParam role: RoleDP? = null,
        @RequestParam currentLatitude: Double? = null,
        @RequestParam currentLongitude: Double? = null,
        @RequestParam sortingDirection: SortingDirectionDP = SortingDirectionDP.Ascending,
        @RequestParam scheduleTimeType: ScheduleTimeTypeDP = ScheduleTimeTypeDP.Departure,
        @RequestParam scheduleTime: ZonedDateTime? = null,
        userToken: UserToken): ResponseEntity<PageDP<PartialDriveRequestDP>>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")

        var driveRequests:List<DriveRequest> = driveRequestsRepository.findAllDriveRequests(
                Sort.by(
                    when(sortingDirection) {
                        SortingDirectionDP.Ascending -> Sort.Direction.ASC
                        SortingDirectionDP.Descending -> Sort.Direction.DESC
                    },
                    "scheduleTime.time"
                )
            )
            .filter {
                when (it) {
                    is CarpoolDriveRequest -> user.carpools.contains(it.carpool)
                    is PublicDriveRequest -> true
                    else -> false
                }
            }
            .filter { it.requestingUser !in user.blockedUsers }

        if(scheduleTime != null)
        {
            driveRequests = driveRequests.filter {
                when(scheduleTimeType) {
                    ScheduleTimeTypeDP.Arrival -> {
                        when (it.scheduleTime?.type) {
                            ScheduleTimeType.Arrival -> it.scheduleTime?.time?.isBefore(scheduleTime) ?: true
                            ScheduleTimeType.Departure -> it.scheduleTime?.time?.plus(it.route.duration)?.isBefore(scheduleTime) ?: true
                            else -> true
                        }
                    }
                    ScheduleTimeTypeDP.Departure -> {
                        when (it.scheduleTime?.type) {
                            ScheduleTimeType.Arrival -> it.scheduleTime?.time?.minus(it.route.duration)?.isAfter(scheduleTime) ?: true
                            ScheduleTimeType.Departure -> it.scheduleTime?.time?.isAfter(scheduleTime) ?: true
                            else -> true
                        }
                    }
                    null -> true
                }
            }
        }

        if(role != null)
        {
            if(currentLatitude == null || currentLongitude == null) throw BadRequestError(listOf("Current latitude and longitude must be provided when filtering by role."))

            val currentCoordinate: Coordinate = Coordinate(currentLatitude, currentLongitude)
            driveRequests = driveRequests.filter {
                when(role) {
                    RoleDP.Driver -> (it.route.start.coordinate distanceTo currentCoordinate).value <= 1000.0
                    RoleDP.Passenger -> it.route.isCoordinateOnRoute(currentCoordinate, Meters(1000.0))
                }
            }
        }
        else if(currentLatitude != null || currentLongitude != null) throw BadRequestError(listOf("Role must be provided when filtering by current latitude and longitude."))

        return ResponseEntity.ok(
            PartialDriveRequestPageDP.fromList(
                driveRequests.map {
                    when (it) {
                        is CarpoolDriveRequest -> PartialCarpoolDriveRequestDP.fromCarpoolDriveRequest(it, it.requestingUser in user.favoriteUsers)
                        is PublicDriveRequest -> PartialPublicDriveRequestDP.fromPublicDriveRequest(it, it.requestingUser in user.favoriteUsers)
                        else -> throw Exception()
                    }
                },
                pageNumber.toUInt(),
                perPage.toUInt()
            )
        )
    }

    @Operation(description = "Get details of a specific drive request.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{driveRequestId}")
    fun getDriveRequest(@PathVariable @UUID driveRequestId:String, userToken: UserToken): ResponseEntity<DetailedDriveRequestDP>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(driveRequestId)).getOrNull() ?: throw NotFoundError("Drive request with id $driveRequestId not found.")

        val detailedDriveRequestDP: DetailedDriveRequestDP = when(driveRequest)
        {
            is CarpoolDriveRequest -> DetailedCarpoolDriveRequestDP(
                driveRequest.id.toString(),
                driveRequest.requestingUser in user.favoriteUsers,
                PartialUserDP.fromUser(driveRequest.requestingUser),
                PartialRouteDP.fromRoute(driveRequest.route),
                driveRequest.scheduleTime?.let { ScheduleTimeDP.fromScheduleTime(it) },
                PartialCarpoolDP.fromCarpool(driveRequest.carpool)
            )
            is PublicDriveRequest -> DetailedPublicDriveRequestDP(
                driveRequest.id.toString(),
                driveRequest.requestingUser in user.favoriteUsers,
                PartialUserDP.fromUser(driveRequest.requestingUser),
                PartialRouteDP.fromRoute(driveRequest.route),
                driveRequest.scheduleTime?.let { ScheduleTimeDP.fromScheduleTime(it) },
                driveRequest.driveOffers.map { PartialPublicDriveOfferDP.fromPublicDriveOffer(it, it.driver in user.favoriteUsers) }
            )
            else -> { throw InternalServerError("Drive request is neither a carpool drive request nor a public drive request.") }
        }

        return ResponseEntity.ok(detailedDriveRequestDP)
    }

    @Operation(description = "Delete a specific drive request.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{driveRequestId}")
    fun deleteDriveRequest(@PathVariable @UUID driveRequestId:String, userToken: UserToken): ResponseEntity<Void>
    {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(driveRequestId)).getOrNull() ?: throw NotFoundError("Drive request with id $driveRequestId not found.")
        if(driveRequest.requestingUser.id != UUIDType.fromString(userToken.id)) throw ForbiddenError("UserToken id does not match the requesting user id of the drive request.")

        driveRequestsRepository.delete(driveRequest)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Create a new drive offer for a specific drive request. The drive request is either deleted if it's a CarpoolDriveRequest or its drive offers list is updated if it's a PublicDriveRequest.")
    @CommonApiResponses @UnprocessableContentApiResponse @CreatedApiResponse @NotFoundApiResponse
    @PostMapping("{driveRequestId}/drive-offers")
    fun createDriveOfferForDriveRequest(@PathVariable @UUID driveRequestId:String, @RequestBody @Valid driveOfferCreation: DriveOfferCreationDP, userToken: UserToken): ResponseEntity<IdDP>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(driveRequestId)).getOrNull() ?: throw NotFoundError("Drive request with id $driveRequestId not found.")
        if(user in driveRequest.requestingUser.blockedUsers) throw ForbiddenError("User with id ${user.id} is blocked by the requesting user of the drive request.")

        val driveOffer: DriveOffer
        when(driveRequest)
        {
            is CarpoolDriveRequest -> {
                when (driveOfferCreation)
                {
                    is CarpoolDriveOfferCreationDP ->
                    {
                        val originalCar:Car = try{ user.getCarByIndex(driveOfferCreation.carIndex.toUInt()) } catch(error:NotAvailableError){ throw NotFoundError(error.message!!) }
                        driveOffer = CarpoolDriveOffer(
                            user,
                            Car(originalCar.brand, originalCar.model, originalCar.color, originalCar.licencePlate),
                            Seats(driveOfferCreation.freeSeats.toUInt()),
                            geographyService.createRoute(geographyService.createPosition(driveOfferCreation.route.start.toCoordinate()), geographyService.createPosition(driveOfferCreation.route.destination.toCoordinate())),
                            driveOfferCreation.scheduleTime?.toScheduleTime(),
                            carpoolsRepository.findById(UUIDType.fromString(driveOfferCreation.carpoolId)).getOrNull() ?: throw NotFoundError("Carpool with id ${driveOfferCreation.carpoolId} not found.")
                        )
                        originalCar.image?.let { driveOffer.car.image = imagesRepository.copy(it) }

                        driveOffersRepository.saveAndFlush(driveOffer)
                        driveRequestsRepository.delete(driveRequest)
                    }
                    is PublicDriveOfferCreationDP -> { throw UnprocessableContentError("Public drive offer creation is not allowed for carpool drive requests.") }
                }
            }
            is PublicDriveRequest -> {
                when (driveOfferCreation)
                {
                    is CarpoolDriveOfferCreationDP -> { throw UnprocessableContentError("Carpool drive offer creation is not allowed for public drive requests.") }
                    is PublicDriveOfferCreationDP -> {
                        val originalCar:Car = try{ user.getCarByIndex(driveOfferCreation.carIndex.toUInt()) } catch(error:NotAvailableError){ throw NotFoundError(error.message!!) }
                        driveOffer = PublicDriveOffer(
                            user,
                            Car(originalCar.brand, originalCar.model, originalCar.color, originalCar.licencePlate),
                            Seats(driveOfferCreation.freeSeats.toUInt()),
                            geographyService.createRoute(geographyService.createPosition(driveOfferCreation.route.start.toCoordinate()), geographyService.createPosition(driveOfferCreation.route.destination.toCoordinate())),
                            driveOfferCreation.scheduleTime?.toScheduleTime()
                        )
                        originalCar.image?.let { driveOffer.car.image = imagesRepository.copy(it) }

                        driveOffersRepository.saveAndFlush(driveOffer)
                        try { driveRequest.addDriveOffer(driveOffer) }
                        catch (repeatedActionError: RepeatedActionError) { throw ForbiddenError(repeatedActionError.message!!) }

                        driveRequestsRepository.saveAndFlush(driveRequest)
                    }
                }
            }
            else -> { throw InternalServerError("Drive request is neither a carpool drive request nor a public drive request.") }
        }

        return ResponseEntity.status(201).body(IdDP(driveOffer.id.toString()))
    }

    @Operation(description = "This endpoint is only allowed to use on a PublicRequestRequest. Reject a specific drive offer for a specific drive request. Neither the drive request nor the drive offer is deleted so other users can still see them.")
    @CommonApiResponses @UnprocessableContentApiResponse @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveRequestId}/drive-offers/{driveOfferId}/rejections")
    fun rejectDriveOffer(@PathVariable @UUID driveRequestId:String, @PathVariable @UUID driveOfferId:String, userToken: UserToken): ResponseEntity<Void>
    {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(driveRequestId)).getOrNull() ?: throw NotFoundError("Drive request with id $driveRequestId not found.")
        if(driveRequest.requestingUser.id != UUIDType.fromString(userToken.id)) throw ForbiddenError("UserToken id does not match the requesting user id of the drive request.")

        when(driveRequest)
        {
            is CarpoolDriveRequest -> { throw UnprocessableContentError("Drive offers for carpool drive requests are automatically accepted.") }
            is PublicDriveRequest ->
            {
                try { driveRequest.rejectDriveOffer(UUIDType.fromString(driveOfferId)) }
                catch (notAvailableError: NotAvailableError) { throw NotFoundError(notAvailableError.message!!) }
                driveRequestsRepository.save(driveRequest)
            }
            else -> { throw InternalServerError("Drive request is neither a carpool drive request nor a public drive request.") }
        }

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "This endpoint is only allowed to use on a PublicRequestRequest. Accept a specific drive offer for a specific drive request. The requesting user of the drive request is automatically accepted as a passenger and the drive request is deleted.")
    @CommonApiResponses @UnprocessableContentApiResponse @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveRequestId}/drive-offers/{driveOfferId}/acceptances")
    fun acceptDriveOffer(@PathVariable @UUID driveRequestId:String, @PathVariable @UUID driveOfferId:String, userToken: UserToken): ResponseEntity<Void>
    {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(driveRequestId)).getOrNull() ?: throw NotFoundError("Drive request with id $driveRequestId not found.")
        if(driveRequest.requestingUser.id != UUIDType.fromString(userToken.id)) throw ForbiddenError("UserToken id does not match the requesting user id of the drive request.")

        when(driveRequest) {
            is CarpoolDriveRequest -> { throw UnprocessableContentError("Drive offers for carpool drive requests are automatically accepted.") }
            is PublicDriveRequest -> {
                try { driveRequest.acceptDriveOffer(UUIDType.fromString(driveOfferId)) }
                catch (notAvailableError: NotAvailableError) { throw NotFoundError(notAvailableError.message!!) }
                catch (conflictingActionError: ConflictingActionError) { throw ForbiddenError(conflictingActionError.message!!) }
                catch (repeatedActionError: RepeatedActionError) { throw ForbiddenError(repeatedActionError.message!!) }
                driveRequestsRepository.saveAndFlush(driveRequest)
                driveRequestsRepository.delete(driveRequest)
            }
            else -> { throw InternalServerError("Drive request is neither a carpool drive request nor a public drive request.") }
        }

        return ResponseEntity.noContent().build()
    }
}