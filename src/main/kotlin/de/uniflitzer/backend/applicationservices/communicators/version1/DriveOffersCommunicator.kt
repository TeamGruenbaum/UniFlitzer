package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.BadRequestError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.NotFoundError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.UnprocessableContentError
import de.uniflitzer.backend.applicationservices.communicators.version1.localization.LocalizationService
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.applicationservices.geography.GeographyService
import de.uniflitzer.backend.model.*
import de.uniflitzer.backend.model.errors.ConflictingActionError
import de.uniflitzer.backend.model.errors.MissingActionError
import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.model.errors.RepeatedActionError
import de.uniflitzer.backend.repositories.CarpoolsRepository
import de.uniflitzer.backend.repositories.DriveOffersRepository
import de.uniflitzer.backend.repositories.ImagesRepository
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.ZonedDateTime
import kotlin.jvm.optionals.getOrNull
import java.util.UUID as UUIDType

@RestController @RequestMapping("v1/drive-offers")
@Tag(name = "Drive Offers") @SecurityRequirement(name = "Token Authentication")
@Validated
@Transactional(rollbackFor = [Throwable::class])
class DriveOffersCommunicator(
    @field:Autowired private val driveOffersRepository: DriveOffersRepository,
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val geographyService: GeographyService,
    @field:Autowired private val imagesRepository: ImagesRepository,
    @field:Autowired private val carpoolsRepository: CarpoolsRepository,
    @field:Autowired private val localizationService: LocalizationService
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Operation(description = "Get all drive offers.")
    @CommonApiResponses @OkApiResponse
    @GetMapping
    fun getDriveOffers(
        @RequestParam @Min(1) pageNumber: Int,
        @RequestParam @Min(1) @Max(200) perPage: Int,
        @RequestParam startLatitude: Double,
        @RequestParam startLongitude: Double,
        @RequestParam destinationLatitude: Double,
        @RequestParam destinationLongitude: Double,
        @RequestParam allowedAnimals: List<AnimalDP>? = null,
        @RequestParam isSmoking: Boolean? = null,
        @RequestParam allowedDrivingStyles: List<DrivingStyleDP>? = null,
        @RequestParam allowedGenders: List<GenderDP>? = null,
        @RequestParam sortingDirection: SortingDirectionDP = SortingDirectionDP.Ascending,
        @RequestParam scheduleTimeType: ScheduleTimeTypeDP = ScheduleTimeTypeDP.Departure,
        @RequestParam scheduleTime: ZonedDateTime? = null,
        userToken: UserToken
    ): ResponseEntity<PageDP<PartialDriveOfferDP>> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        val startCoordinate: Coordinate = Coordinate(startLatitude, startLongitude)
        val destinationCoordinate: Coordinate = Coordinate(destinationLatitude, destinationLongitude)
        val tolerance: Meters = Meters(1000.0)

        val searchedDriveOffers: List<DriveOffer> =
            driveOffersRepository.findAll(
                allowedAnimals?.map { it.toAnimal() },
                isSmoking,
                allowedDrivingStyles?.map { it.toDrivingStyle() },
                allowedGenders?.map { it.toGender() },
                actingUser.blockedUsers,
                actingUser,
                Sort.by(
                    when (sortingDirection) {
                        SortingDirectionDP.Ascending -> Sort.Direction.ASC
                        SortingDirectionDP.Descending -> Sort.Direction.DESC
                    },
                    "scheduleTime.time"
                )
            )
            .filter {
                if(scheduleTime == null) return@filter true

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
                }
            }
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
    @GetMapping("{driveOfferId}")
    fun getDriveOffer(@PathVariable @UUID driveOfferId: String, userToken: UserToken): ResponseEntity<DetailedDriveOfferDP> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        val searchedDriveOffer: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveOffer.notFound", driveOfferId))

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
    @GetMapping("{driveOfferId}/car/image")
    fun getImageOfCar(@PathVariable @UUID driveOfferId: String, @RequestParam quality: QualityDP, userToken: UserToken): ResponseEntity<ByteArray> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        val driveOffer: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveOffer.notFound", driveOfferId))

        val car: Car = driveOffer.car
        if (car.image == null) throw NotFoundError(localizationService.getMessage("driveOffer.car.image.notExists", driveOfferId))

        val image:ByteArray = imagesRepository.getById(car.image!!.id, quality.toQuality()).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveOffer.car.image.notFound", driveOfferId))
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image)
    }

    @Operation(description = "Create a new drive offer.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping
    fun createDriveOffer(@RequestBody @Valid driveOfferCreation: DriveOfferCreationDP, userToken: UserToken): ResponseEntity<IdDP> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        val selectedCarForDriverOffer: Car = actingUser.cars.getOrNull(driveOfferCreation.carIndex) ?: throw NotFoundError(localizationService.getMessage("user.car.index.notExists", driveOfferCreation.carIndex, actingUser.id))
        val driveOfferRoute: Route = geographyService.createRoute(
            geographyService.createPosition(driveOfferCreation.route.start.toCoordinate()),
            geographyService.createPosition(driveOfferCreation.route.destination.toCoordinate())
        )
        //driveOfferCreation.scheduleTime?.toScheduleTime() TODO: Uncomment
        //    ?.let { if(it.type == ScheduleTimeType.Arrival) it.time.minus(driveOfferRoute.duration) else it.time }
        //    ?.let { if(it.isBefore(ZonedDateTime.now().plusHours(1))) throw BadRequestError(listOf("Departure time can not be in the past or less than an hour in the future.")) }

        val newDriveOffer: DriveOffer = when (driveOfferCreation) {
            is PublicDriveOfferCreationDP -> {
                PublicDriveOffer(
                    actingUser,
                    Car(
                        selectedCarForDriverOffer.brand,
                        selectedCarForDriverOffer.model,
                        selectedCarForDriverOffer.color,
                        selectedCarForDriverOffer.licencePlate
                    ),
                    Seats(driveOfferCreation.freeSeats.toUInt()),
                    driveOfferRoute,
                    driveOfferCreation.scheduleTime?.toScheduleTime()
                )
            }
            is CarpoolDriveOfferCreationDP -> {
                val targetedCarpool: Carpool = carpoolsRepository.findById(UUIDType.fromString(driveOfferCreation.carpoolId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("carpool.notFound", driveOfferCreation.carpoolId))

                CarpoolDriveOffer(
                    actingUser,
                    Car(
                        selectedCarForDriverOffer.brand,
                        selectedCarForDriverOffer.model,
                        selectedCarForDriverOffer.color,
                        selectedCarForDriverOffer.licencePlate
                    ),
                    Seats(driveOfferCreation.freeSeats.toUInt()),
                    driveOfferRoute,
                    driveOfferCreation.scheduleTime?.toScheduleTime(),
                    targetedCarpool
                )
            }
        }
        selectedCarForDriverOffer.image?.let { newDriveOffer.car.image = imagesRepository.copy(it) }
        driveOffersRepository.save(newDriveOffer)

        return ResponseEntity.status(HttpStatus.CREATED).body(IdDP(newDriveOffer.id.toString()))
    }

    @Operation(description = "Delete a drive offer.")
    @CommonApiResponses @CreatedApiResponse
    @DeleteMapping("{driveOfferId}")
    fun deleteDriveOffer(@PathVariable @UUID driveOfferId: String, userToken: UserToken): ResponseEntity<IdDP> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        val driveOfferToDelete: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveOffer.notFound", driveOfferId))
        if (driveOfferToDelete.driver.id != UUIDType.fromString(userToken.id)) throw ForbiddenError(localizationService.getMessage("driveOffer.user.notDriverOf", userToken.id, driveOfferId))

        driveOfferToDelete.removeAllRequestingUsersAndPassengers()
        driveOffersRepository.delete(driveOfferToDelete)
        driveOffersRepository.flush()

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Update a specific drive offer. Only the schedule time can be updated.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PatchMapping("{driveOfferId}")
    fun updateDriveOffer(@PathVariable @UUID driveOfferId: String, @RequestBody @Valid driveOfferUpdate: DriverOfferUpdateDP, userToken: UserToken): ResponseEntity<Void> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveOffer.notFound", driveOfferId))
        if(driveOfferInEditing.scheduleTime != null) throw BadRequestError(listOf(localizationService.getMessage("driveOffer.scheduleTime.alreadySet", driveOfferId)))
        if(driveOfferInEditing.driver.id != UUIDType.fromString(userToken.id)) throw ForbiddenError(localizationService.getMessage("driveOffer.user.notDriverOf", userToken.id, driveOfferId))
        //driveOfferUpdate.scheduleTime.toScheduleTime().time TODO: Uncomment
        //    .let { if(driveOfferUpdate.scheduleTime.type == ScheduleTimeTypeDP.Arrival) it.minus(driveOfferInEditing.route.duration) else it }
        //    .let { if(it.isBefore(ZonedDateTime.now().plusHours(1))) throw BadRequestError(listOf("Departure time can not be in the past or less than an hour in the future.")) }

        driveOfferInEditing.scheduleTime = driveOfferUpdate.scheduleTime.toScheduleTime()
        driveOffersRepository.save(driveOfferInEditing)

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Request the ride for a specific drive offer.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse @ConflictApiResponse
    @PostMapping("{driveOfferId}/requests")
    fun requestSeat(@PathVariable @UUID driveOfferId: String, @RequestBody @Valid userStopCreation: UserStopCreationDP, userToken: UserToken): ResponseEntity<Void> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveOffer.notFound", driveOfferId))

        when (driveOfferInEditing) {
            is PublicDriveOffer -> {
                if(actingUser in driveOfferInEditing.driver.blockedUsers) throw ForbiddenError(localizationService.getMessage("driveOffer.user.blockedByDriver", actingUser.id, driveOfferId))

                try {
                    actingUser.joinDriveOfferAsRequestingUser(driveOfferInEditing)
                    driveOfferInEditing.addRequestFromUser(
                        actingUser,
                        geographyService.createPosition(userStopCreation.start.toCoordinate()),
                        geographyService.createPosition(userStopCreation.destination.toCoordinate())
                    )
                } catch (_: RepeatedActionError) {
                    throw BadRequestError(listOf(localizationService.getMessage("driveOffer.user.alreadyRequested", actingUser.id, driveOfferId)))
                } catch (_: ConflictingActionError) {
                    throw ConflictError(localizationService.getMessage("driveOffer.user.alreadyDriverOf", actingUser.id, driveOfferId))
                }
            }
            is CarpoolDriveOffer -> {
                try {
                    actingUser.joinDriveOfferAsPassenger(driveOfferInEditing)
                    driveOfferInEditing.addPassenger(
                        UserStop(
                            actingUser,
                            geographyService.createPosition(userStopCreation.start.toCoordinate()),
                            geographyService.createPosition(userStopCreation.destination.toCoordinate())
                        )
                    )
                } catch (_: NotAvailableError) {
                    throw BadRequestError(listOf(localizationService.getMessage("driveOffer.seats.taken", driveOfferId)))
                } catch (_: RepeatedActionError) {
                    throw BadRequestError(listOf(localizationService.getMessage("driveOffer.user.alreadyPassengerOf", actingUser.id, driveOfferId)))
                } catch (_: ConflictingActionError) {
                    throw ConflictError(localizationService.getMessage("driveOffer.user.alreadyDriverOf", actingUser.id, driveOfferId))
                }
            }
        }
        usersRepository.save(actingUser)
        driveOffersRepository.save(driveOfferInEditing)

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Accept a requesting user for a specific drive offer.")
    @CommonApiResponses @UnprocessableContentApiResponse @NoContentApiResponse @NotFoundApiResponse @ConflictApiResponse
    @PostMapping("{driveOfferId}/requesting-users/{requestingUserId}/acceptances") //TODO Manchmal werden Nutzer ohne Anahme angenommen
    fun acceptRequestingUser(@PathVariable @UUID driveOfferId: String, @PathVariable @UUID requestingUserId: String, userToken: UserToken): ResponseEntity<Void> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        val requestingUser: User = usersRepository.findById(UUIDType.fromString(requestingUserId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("user.notExists", requestingUserId))
        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveOffer.notFound", driveOfferId))
        if (driveOfferInEditing.driver.id != actingUser.id) throw ForbiddenError(localizationService.getMessage("driveOffer.user.notDriverOf", actingUser.id, driveOfferId))

        when (driveOfferInEditing) {
            is PublicDriveOffer -> {
                try {
                    requestingUser.joinDriveOfferAsPassenger(driveOfferInEditing)
                    driveOfferInEditing.acceptRequestFromUser(requestingUser.id)
                    driveOfferInEditing.route = geographyService.createRoute(
                        driveOfferInEditing.route.start,
                        driveOfferInEditing.passengers.map { it.start.coordinate } + driveOfferInEditing.passengers.map { it.destination.coordinate },
                        driveOfferInEditing.route.destination
                    )
                } catch (_: MissingActionError) {
                    throw NotFoundError(localizationService.getMessage("driveOffer.user.notRequested", requestingUserId, driveOfferId))
                } catch (_: NotAvailableError) {
                    throw BadRequestError(listOf(localizationService.getMessage("driveOffer.seats.taken", driveOfferId)))
                } catch (_: RepeatedActionError) {
                    throw BadRequestError(listOf(localizationService.getMessage("driveOffer.user.alreadyPassengerOf", requestingUserId, driveOfferId)))
                } catch (_: ConflictingActionError) {
                    throw ConflictError(localizationService.getMessage("driveOffer.user.alreadyDriverOf", requestingUserId, driveOfferId))
                }
            }
            is CarpoolDriveOffer -> throw UnprocessableContentError(localizationService.getMessage("driveOffer.carpool.requests.automaticallyAccepted", driveOfferId))
        }
        usersRepository.save(actingUser)
        driveOffersRepository.save(driveOfferInEditing)

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Reject a requesting user for a specific drive offer.")
    @CommonApiResponses @UnprocessableContentApiResponse @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveOfferId}/requesting-users/{requestingUserId}/rejections")
    fun rejectRequestingUser(@PathVariable @UUID driveOfferId: String, @PathVariable @UUID requestingUserId: String, userToken: UserToken):ResponseEntity<Void> {
        val logId: UUIDType = UUIDType.randomUUID()

        logger.info("$logId: User with id ${userToken.id} made request to reject user with id $requestingUserId in drive offer with id $driveOfferId.")

        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: run {
            logger.warn("$logId: Rejecting user with id ${userToken.id} does not exist in resource server.")
            throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        }
        logger.trace("$logId: Rejecting user does exist.")

        val requestingUser: User = usersRepository.findById(UUIDType.fromString(requestingUserId)).getOrNull() ?: run {
            logger.warn("$logId: User to reject with id $requestingUserId does not exist.")
            throw NotFoundError(localizationService.getMessage("user.notExists", requestingUserId))
        }
        logger.trace("$logId: User to reject does exist.")

        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?:run {
            logger.warn("$logId: Drive offer with id $driveOfferId does not exist.")
            throw NotFoundError(localizationService.getMessage("driveOffer.notFound", driveOfferId))
        }
        logger.trace("$logId: Drive offer does exist.")

        if (driveOfferInEditing.driver.id != actingUser.id) {
            logger.error("Rejecting user with id ${userToken.id} is not the driver of the drive offer with id $driveOfferId.")
            throw ForbiddenError(localizationService.getMessage("driveOffer.user.notDriverOf", actingUser.id, driveOfferId))
        }

        when (driveOfferInEditing) {
            is PublicDriveOffer -> {
                try {
                    requestingUser.leaveDriveOfferAsRequestingUser(driveOfferInEditing)
                    driveOfferInEditing.rejectRequestFromUser(requestingUser.id)
                    logger.trace("$logId: Requesting user was removed from drive offer and drive offer was removed from account of user to reject.")
                } catch (_: MissingActionError) {
                    logger.warn("User to reject with id $requestingUserId has not requested a seat in drive offer with id $driveOfferId.")
                    throw NotFoundError(localizationService.getMessage("driveOffer.user.notRequested", requestingUserId, driveOfferId))
                }
            }
            is CarpoolDriveOffer -> {
                logger.warn("$logId: Drive offer with id $driveOfferId is a carpool drive offer, so requests are automatically accepted.")
                throw UnprocessableContentError(localizationService.getMessage("driveOffer.carpool.requests.automaticallyAccepted", driveOfferId))
            }
        }
        usersRepository.save(actingUser)
        driveOffersRepository.save(driveOfferInEditing)
        logger.trace("$logId: User to reject and drive offer were saved.")

        logger.info("$logId: Rejecting user with id ${userToken.id} successfully rejected user with id $requestingUserId in drive offer with id $driveOfferId.")
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Get the complete route for a specific drive offer including its passengers and a specific requesting user.")
    @CommonApiResponses @UnprocessableContentApiResponse @OkApiResponse @NotFoundApiResponse
    @GetMapping("{driveOfferId}/requesting-users/{requestingUserId}/complete-route")
    fun getCompleteRouteWithRequestingUser(@PathVariable @UUID driveOfferId: String, @PathVariable @UUID requestingUserId: String, userToken: UserToken): ResponseEntity<DetailedRouteDP> {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        val driveOffer: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveOffer.notFound", driveOfferId))
        if (driveOffer.driver.id != user.id) throw ForbiddenError(localizationService.getMessage("driveOffer.user.notDriverOf", user.id, driveOfferId))

        if(!usersRepository.existsById(UUIDType.fromString(requestingUserId))) throw NotFoundError(localizationService.getMessage("user.notFound", requestingUserId))
        when (driveOffer) {
            is PublicDriveOffer -> {
                if(driveOffer.requestingUsers.none { it.user.id.toString() == requestingUserId }) throw NotFoundError(localizationService.getMessage("driveOffer.user.notRequested", requestingUserId, driveOfferId))
                val userStops:List<UserStop> = driveOffer.passengers + driveOffer.requestingUsers.filter { it.user.id.toString() == requestingUserId }
                val route: Route = geographyService.createRoute(
                        driveOffer.route.start,
                        userStops.map { it.start.coordinate } + userStops.map { it.destination.coordinate },
                        driveOffer.route.destination
                    )
                return ResponseEntity.ok(DetailedRouteDP.fromRoute(route))
            }
            is CarpoolDriveOffer -> throw UnprocessableContentError(localizationService.getMessage("driveOffer.carpool.requests.automaticallyAccepted", driveOfferId))
            else -> { throw IllegalStateException("Drive offer is neither a public drive offer nor a carpool drive offer.") }
        }
    }
}