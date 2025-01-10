package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.*
import de.uniflitzer.backend.applicationservices.communicators.version1.localization.LocalizationService
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
class DriveRequestsCommunicator(
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val driveRequestsRepository: DriveRequestsRepository,
    @field:Autowired private val driveOffersRepository: DriveOffersRepository,
    @field:Autowired private val carpoolsRepository: CarpoolsRepository,
    @field:Autowired private val geographyService: GeographyService,
    @field:Autowired private val imagesRepository: ImagesRepository,
    @field:Autowired private val localizationService: LocalizationService
)
{
    @Operation(description = "Create a new drive request.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("")
    fun createDriveRequest(@RequestBody @Valid driveRequestCreation: DriveRequestCreationDP, userToken: UserToken): ResponseEntity<IdDP>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

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
                    carpoolsRepository.findById(UUIDType.fromString(driveRequestCreation.carpoolId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("carpool.notFound", driveRequestCreation.carpoolId))
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
        @RequestParam @Min(1) @Max(200) perPage: Int,
        @RequestParam role: RoleDP? = null,
        @RequestParam currentLatitude: Double? = null,
        @RequestParam currentLongitude: Double? = null,
        @RequestParam sortingDirection: SortingDirectionDP = SortingDirectionDP.Ascending,
        @RequestParam scheduleTimeType: ScheduleTimeTypeDP = ScheduleTimeTypeDP.Departure,
        @RequestParam scheduleTime: ZonedDateTime? = null,
        userToken: UserToken): ResponseEntity<PageDP<PartialDriveRequestDP>>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

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
            .filter { it.requestingUser != user }

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
            if(currentLatitude == null || currentLongitude == null) throw BadRequestError(listOf(localizationService.getMessage("requestParam.latitudeAndLongitude.notExists")))

            val currentCoordinate: Coordinate = Coordinate(currentLatitude, currentLongitude)
            driveRequests = driveRequests.filter {
                when(role) {
                    RoleDP.Driver -> (it.route.start.coordinate distanceTo currentCoordinate).value <= 1000.0
                    RoleDP.Passenger -> it.route.isCoordinateOnRoute(currentCoordinate, Meters(1000.0))
                }
            }
        }
        else if(currentLatitude != null || currentLongitude != null) throw BadRequestError(listOf(localizationService.getMessage("requestParam.role.notExists")))

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
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(driveRequestId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveRequest.notFound", driveRequestId))

        val detailedDriveRequestDP: DetailedDriveRequestDP = when(driveRequest)
        {
            is CarpoolDriveRequest -> DetailedCarpoolDriveRequestDP(
                driveRequest.id.toString(),
                driveRequest.requestingUser in user.favoriteUsers,
                PartialUserDP.fromUser(driveRequest.requestingUser),
                DetailedRouteDP.fromRoute(driveRequest.route),
                driveRequest.scheduleTime?.let { ScheduleTimeDP.fromScheduleTime(it) },
                PartialCarpoolDP.fromCarpool(driveRequest.carpool)
            )
            is PublicDriveRequest -> DetailedPublicDriveRequestDP(
                driveRequest.id.toString(),
                driveRequest.requestingUser in user.favoriteUsers,
                PartialUserDP.fromUser(driveRequest.requestingUser),
                DetailedRouteDP.fromRoute(driveRequest.route),
                driveRequest.scheduleTime?.let { ScheduleTimeDP.fromScheduleTime(it) },
                driveRequest.driveOffers.map { PartialPublicDriveOfferDP.fromPublicDriveOffer(it, it.driver in user.favoriteUsers) }
            )
            else -> { throw IllegalStateException("Drive request is neither a carpool drive request nor a public drive request.") }
        }

        return ResponseEntity.ok(detailedDriveRequestDP)
    }

    @Operation(description = "Delete a specific drive request.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{driveRequestId}")
    fun deleteDriveRequest(@PathVariable @UUID driveRequestId:String, userToken: UserToken): ResponseEntity<Void>
    {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(driveRequestId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveRequest.notFound", driveRequestId))
        if(driveRequest.requestingUser.id != UUIDType.fromString(userToken.id)) throw ForbiddenError(localizationService.getMessage("driveRequest.user.notRequestingUserOf", userToken.id, driveRequestId))

        driveRequestsRepository.delete(driveRequest)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Create a new drive offer for a specific drive request. The drive request is either deleted if it's a CarpoolDriveRequest or its drive offers list is updated if it's a PublicDriveRequest.")
    @CommonApiResponses @UnprocessableContentApiResponse @CreatedApiResponse @NotFoundApiResponse
    @PostMapping("{driveRequestId}/drive-offers")
    fun createDriveOfferForDriveRequest(@PathVariable @UUID driveRequestId:String, @RequestBody @Valid driveOfferCreation: DriveOfferCreationDP, userToken: UserToken): ResponseEntity<IdDP>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(driveRequestId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveRequest.notFound", driveRequestId))
        if(user in driveRequest.requestingUser.blockedUsers) throw ForbiddenError(localizationService.getMessage("driveRequest.user.blockedByRequestingUser", user.id, driveRequestId))

        val driveOfferRoute: Route = geographyService.createRoute(geographyService.createPosition(driveOfferCreation.route.start.toCoordinate()), geographyService.createPosition(driveOfferCreation.route.destination.toCoordinate()))
        //driveOfferCreation.scheduleTime?.toScheduleTime() TODO: Uncomment
        //        ?.let { if(it.type == ScheduleTimeType.Arrival) it.time.minus(driveOfferRoute.duration) else it.time }
        //        ?.let { if(it.isBefore(ZonedDateTime.now().plusHours(1))) throw BadRequestError(listOf("Departure time can not be in the past or less than an hour in the future.")) }

        val driveOffer: DriveOffer
        when(driveRequest)
        {
            is CarpoolDriveRequest -> {
                when (driveOfferCreation)
                {
                    is CarpoolDriveOfferCreationDP ->
                    {
                        val originalCar:Car = try{ user.getCarByIndex(driveOfferCreation.carIndex.toUInt()) } catch(error:NotAvailableError){ throw NotFoundError(localizationService.getMessage("user.car.index.notExists", driveOfferCreation.carIndex, user.id)) }
                        driveOffer = CarpoolDriveOffer(
                            user,
                            Car(originalCar.brand, originalCar.model, originalCar.color, originalCar.licencePlate),
                            Seats(driveOfferCreation.freeSeats.toUInt()),
                            driveOfferRoute,
                            driveOfferCreation.scheduleTime?.toScheduleTime(),
                            carpoolsRepository.findById(UUIDType.fromString(driveOfferCreation.carpoolId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("carpool.notFound", driveOfferCreation.carpoolId))
                        )
                        originalCar.image?.let { driveOffer.car.image = imagesRepository.copy(it) }

                        driveOffersRepository.saveAndFlush(driveOffer)
                        try{ driveOffer.addPassenger(UserStop(
                            driveRequest.requestingUser,
                            driveOfferRoute.start,
                            driveOfferRoute.destination
                        )) }
                        catch(conflictingActionError: ConflictingActionError) { throw ConflictError(localizationService.getMessage("driveOffer.user.alreadyDriverOf", user.id, driveOffer.id)) }
                        catch(notAvailableError: NotAvailableError) { throw BadRequestError(listOf(localizationService.getMessage("driveOffer.seats.taken", driveOffer.id))) }
                        catch(repeatedActionError: RepeatedActionError) { throw BadRequestError(listOf(localizationService.getMessage("driveOffer.user.alreadyPassengerOf", user.id, driveOffer.id)) )}

                        driveOffersRepository.saveAndFlush(driveOffer)
                        driveRequestsRepository.delete(driveRequest)

                        try { user.joinDriveOfferAsPassenger(driveOffer)}
                        catch (repeatedActionError: RepeatedActionError) { throw BadRequestError(listOf(localizationService.getMessage("driveOffer.user.alreadyPassengerOf", user.id, driveOffer.id)) )}
                        usersRepository.save(user)
                    }
                    is PublicDriveOfferCreationDP -> { throw UnprocessableContentError(localizationService.getMessage("driveRequest.carpool.driveOffer.publicForbidden", driveRequestId)) }
                }
            }
            is PublicDriveRequest -> {
                when (driveOfferCreation)
                {
                    is CarpoolDriveOfferCreationDP -> { throw UnprocessableContentError(localizationService.getMessage("driveRequest.public.driveOffer.carpoolForbidden", driveRequestId)) }
                    is PublicDriveOfferCreationDP -> {
                        val originalCar:Car = try{ user.getCarByIndex(driveOfferCreation.carIndex.toUInt()) } catch(error:NotAvailableError){ throw NotFoundError(localizationService.getMessage("user.car.index.notExists", driveOfferCreation.carIndex, user.id)) }
                        driveOffer = PublicDriveOffer(
                            user,
                            Car(originalCar.brand, originalCar.model, originalCar.color, originalCar.licencePlate),
                            Seats(driveOfferCreation.freeSeats.toUInt()),
                            driveOfferRoute,
                            driveOfferCreation.scheduleTime?.toScheduleTime()
                        )
                        originalCar.image?.let { driveOffer.car.image = imagesRepository.copy(it) }

                        driveOffersRepository.saveAndFlush(driveOffer)
                        try { driveRequest.addDriveOffer(driveOffer) }
                        catch (repeatedActionError: RepeatedActionError) { throw BadRequestError(listOf(localizationService.getMessage("driveRequest.public.driveOffer.alreadyAdded", driveOffer.id, driveRequestId))) }

                        driveRequestsRepository.saveAndFlush(driveRequest)
                    }
                }
            }
            else -> { throw IllegalStateException("Drive request is neither a carpool drive request nor a public drive request.") }
        }

        return ResponseEntity.status(201).body(IdDP(driveOffer.id.toString()))
    }

    @Operation(description = "This endpoint is only allowed to use on a PublicRequestRequest. Reject a specific drive offer for a specific drive request. Neither the drive request nor the drive offer is deleted so other users can still see them.")
    @CommonApiResponses @UnprocessableContentApiResponse @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveRequestId}/drive-offers/{driveOfferId}/rejections")
    fun rejectDriveOffer(@PathVariable @UUID driveRequestId:String, @PathVariable @UUID driveOfferId:String, userToken: UserToken): ResponseEntity<Void>
    {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(driveRequestId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveRequest.notFound", driveRequestId))
        if(driveRequest.requestingUser.id != UUIDType.fromString(userToken.id)) throw ForbiddenError(localizationService.getMessage("driveRequest.user.notRequestingUserOf", userToken.id, driveRequestId))

        when(driveRequest)
        {
            is CarpoolDriveRequest -> { throw UnprocessableContentError(localizationService.getMessage("driveRequest.carpool.driveOffer.automaticallyAccepted", driveRequestId)) }
            is PublicDriveRequest ->
            {
                try { driveRequest.rejectDriveOffer(UUIDType.fromString(driveOfferId)) }
                catch (notAvailableError: NotAvailableError) { throw NotFoundError(localizationService.getMessage("driveOffer.notFound", driveOfferId)) }
                driveRequestsRepository.save(driveRequest)
            }
            else -> { throw IllegalStateException("Drive request is neither a carpool drive request nor a public drive request.") }
        }

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "This endpoint is only allowed to use on a PublicRequestRequest. Accept a specific drive offer for a specific drive request. The requesting user of the drive request is automatically accepted as a passenger and the drive request is deleted.")
    @CommonApiResponses @UnprocessableContentApiResponse @NoContentApiResponse @NotFoundApiResponse @ConflictApiResponse
    @PostMapping("{driveRequestId}/drive-offers/{driveOfferId}/acceptances")
    fun acceptDriveOffer(@PathVariable @UUID driveRequestId:String, @PathVariable @UUID driveOfferId:String, userToken: UserToken): ResponseEntity<Void>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(driveRequestId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveRequest.notFound", driveRequestId))
        if(driveRequest.requestingUser.id != UUIDType.fromString(userToken.id)) throw ForbiddenError(localizationService.getMessage("driveRequest.user.notRequestingUserOf", userToken.id, driveRequestId))

        when(driveRequest) {
            is CarpoolDriveRequest -> { throw UnprocessableContentError(localizationService.getMessage("driveRequest.carpool.driveOffer.automaticallyAccepted", driveRequestId)) }
            is PublicDriveRequest -> {
                val driveOffer: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveOffer.notFound", driveOfferId))
                try {
                    user.joinDriveOfferAsPassenger(driveOffer)
                    driveRequest.acceptDriveOffer(UUIDType.fromString(driveOfferId))
                }
                catch (notAvailableError: NotAvailableError) { throw NotFoundError(localizationService.getMessage("driveOffer.notFoundOrAllSeatsTaken", driveOfferId)) }
                catch (conflictingActionError: ConflictingActionError) { throw ConflictError(localizationService.getMessage("driveOffer.user.alreadyDriverOf", driveRequest.requestingUser.id, driveOfferId)) }
                catch (repeatedActionError: RepeatedActionError) { throw BadRequestError(listOf(localizationService.getMessage("driveOffer.user.alreadyPassengerOf", driveRequest.requestingUser.id, driveOfferId))) }

                usersRepository.save(user)
                driveRequestsRepository.saveAndFlush(driveRequest)
                driveRequestsRepository.delete(driveRequest)
            }
            else -> { throw IllegalStateException("Drive request is neither a carpool drive request nor a public drive request.") }
        }

        return ResponseEntity.noContent().build()
    }
}