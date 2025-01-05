package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.BadRequestError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.InternalServerError
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
private class DriveOffersCommunicator(
    @field:Autowired private val driveOffersRepository: DriveOffersRepository,
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val geographyService: GeographyService,
    @field:Autowired private val imagesRepository: ImagesRepository,
    @field:Autowired private val carpoolsRepository: CarpoolsRepository,
    @field:Autowired private val localizationService: LocalizationService
) {
    @Operation(description = "Get all drive offers.")
    @CommonApiResponses @OkApiResponse
    @GetMapping
    fun getDriveOffers(
        @RequestParam @Min(1) pageNumber: Int,
        @RequestParam @Min(1) @Max(50) perPage: Int,
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
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
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
                actingUser.carpools,
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
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        val searchedDriveOffer: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError("The drive offer with the id $driveOfferId could not be found.")

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
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        val selectedCarForDriverOffer: Car = actingUser.cars.getOrNull(driveOfferCreation.carIndex) ?: throw NotFoundError("The car with the index ${driveOfferCreation.carIndex} could not be found.")

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
                    geographyService.createRoute(
                        geographyService.createPosition(driveOfferCreation.route.start.toCoordinate()),
                        geographyService.createPosition(driveOfferCreation.route.destination.toCoordinate())
                    ),
                    driveOfferCreation.scheduleTime?.toScheduleTime()
                )
            }
            is CarpoolDriveOfferCreationDP -> {
                val targetedCarpool: Carpool = carpoolsRepository.findById(UUIDType.fromString(driveOfferCreation.carpoolId)).getOrNull() ?: throw NotFoundError("The carpool with the id ${driveOfferCreation.carpoolId} could not be found.")

                CarpoolDriveOffer(
                    actingUser,
                    Car(
                        selectedCarForDriverOffer.brand,
                        selectedCarForDriverOffer.model,
                        selectedCarForDriverOffer.color,
                        selectedCarForDriverOffer.licencePlate
                    ),
                    Seats(driveOfferCreation.freeSeats.toUInt()),
                    geographyService.createRoute(
                        geographyService.createPosition(driveOfferCreation.route.start.toCoordinate()),
                        geographyService.createPosition(driveOfferCreation.route.destination.toCoordinate())
                    ),
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
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        val driveOfferToDelete: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError("The drive offer with the id $driveOfferId could not be found.")
        if (driveOfferToDelete.driver.id != UUIDType.fromString(userToken.id)) throw ForbiddenError("The user with the id ${userToken.id} is not the driver of the drive offer with the id $driveOfferId.")

        driveOfferToDelete.throwAllUsersOut()
        driveOffersRepository.delete(driveOfferToDelete)
        driveOffersRepository.flush()

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Update a specific drive offer. Only the schedule time can be updated.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PatchMapping("{driveOfferId}")
    fun updateDriveOffer(@PathVariable @UUID driveOfferId: String, @RequestBody @Valid driveOfferUpdate: DriverOfferUpdateDP, userToken: UserToken): ResponseEntity<Void> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError("The drive offer with the id $driveOfferId could not be found.")
        if (driveOfferInEditing.driver.id != UUIDType.fromString(userToken.id)) throw ForbiddenError("The user with the id ${userToken.id} is not the driver of the drive offer with the id $driveOfferId.")

        driveOfferInEditing.scheduleTime = driveOfferUpdate.scheduleTime.toScheduleTime()
        driveOffersRepository.save(driveOfferInEditing)

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Request the ride for a specific drive offer.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveOfferId}/requests")
    fun requestSeat(@PathVariable @UUID driveOfferId: String, @RequestBody @Valid userStopCreation: UserStopCreationDP, userToken: UserToken): ResponseEntity<Void> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError("The drive offer with the id $driveOfferId could not be found.")

        when (driveOfferInEditing) {
            is PublicDriveOffer -> {
                if(actingUser in driveOfferInEditing.driver.blockedUsers) throw ForbiddenError("The requesting user ${actingUser.id} is blocked by the driver and cannot request a seat in this drive offer.")

                try {
                    actingUser.joinDriveOfferAsRequestingUser(driveOfferInEditing)
                    driveOfferInEditing.addRequestFromUser(
                        actingUser,
                        geographyService.createPosition(userStopCreation.start.toCoordinate()),
                        geographyService.createPosition(userStopCreation.destination.toCoordinate())
                    )
                } catch (_: RepeatedActionError) {
                    throw ForbiddenError("The user with the id ${actingUser.id} has already requested a seat in the drive offer with the id $driveOfferId.")
                } catch (_: MissingActionError) {
                    throw ForbiddenError("The Driver with id ${actingUser.id} of this drive offer with id $driveOfferId cannot be a passenger at the same time.")
                } catch (_: ConflictingActionError) {
                    throw ForbiddenError("The Driver with id ${actingUser.id} of this drive offer with id $driveOfferId cannot be a passenger at the same time.")
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
                    throw ForbiddenError("No free seats left in the drive offer with the id $driveOfferId.")
                } catch (_: RepeatedActionError) {
                    throw ForbiddenError("The user is already a passenger of the drive offer.")
                } catch (_: ConflictingActionError) {
                    throw ForbiddenError("The Driver with id ${actingUser.id} of this drive offer with id $driveOfferId cannot be a passenger at the same time.")
                }
            }
        }
        usersRepository.save(actingUser)
        driveOffersRepository.save(driveOfferInEditing)

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Accept a requesting user for a specific drive offer.")
    @CommonApiResponses @UnprocessableContentApiResponse @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveOfferId}/requesting-users/{requestingUserId}/acceptances")
    fun acceptRequestingUser(@PathVariable @UUID driveOfferId: String, @PathVariable @UUID requestingUserId: String, userToken: UserToken): ResponseEntity<Void> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError("The drive offer with the id $driveOfferId could not be found.")
        if (driveOfferInEditing.driver.id != actingUser.id) throw ForbiddenError("A user who is not the driver cannot accept requests")
        if(!usersRepository.existsById(UUIDType.fromString(requestingUserId))) throw NotFoundError("The requesting user with the id $requestingUserId could not be found.")

        when (driveOfferInEditing) {
            is PublicDriveOffer -> {
                try {
                    actingUser.joinDriveOfferAsPassenger(driveOfferInEditing)
                    driveOfferInEditing.acceptRequestFromUser(UUIDType.fromString(requestingUserId))
                } catch (_: MissingActionError) {
                    throw NotFoundError("The requesting user with the id $requestingUserId could not be found in the drive offer with the id $driveOfferId.")
                } catch (_: NotAvailableError) {
                    throw ForbiddenError("No free seats left in the drive offer with the id $driveOfferId.")
                } catch (_: RepeatedActionError) {
                    throw ForbiddenError("The user with the id $requestingUserId is already a passenger of the drive offer with the id $driveOfferId.")
                } catch (_: ConflictingActionError) {
                    throw ForbiddenError("The Driver with id $requestingUserId of this drive offer with id $driveOfferId cannot be a passenger at the same time.")
                }
            }
            is CarpoolDriveOffer -> throw UnprocessableContentError("The drive offer with the ID $driveOfferId is a carpool drive offer, so requests are automatically accepted.")
        }
        usersRepository.save(actingUser)
        driveOffersRepository.save(driveOfferInEditing)

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Reject a requesting user for a specific drive offer.")
    @CommonApiResponses @UnprocessableContentApiResponse @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveOfferId}/requesting-users/{requestingUserId}/rejections")
    fun rejectRequestingUser(@PathVariable @UUID driveOfferId: String, @PathVariable @UUID requestingUserId: String, userToken: UserToken):ResponseEntity<Void> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        val driveOfferInEditing: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError("The drive offer with the id $driveOfferId could not be found.")
        if (driveOfferInEditing.driver.id != actingUser.id) throw ForbiddenError("A user who is not the driver cannot accept requests")
        if(!usersRepository.existsById(UUIDType.fromString(requestingUserId))) throw NotFoundError("The requesting user with the id $requestingUserId could not be found.")

        when (driveOfferInEditing) {
            is PublicDriveOffer -> {
                try {
                    actingUser.leaveDriveOfferAsRequestingUser(driveOfferInEditing)
                    driveOfferInEditing.rejectRequestFromUser(UUIDType.fromString(requestingUserId))
                } catch (_: MissingActionError) {
                    throw NotFoundError("The requesting user with the id $requestingUserId could not be found in the drive offer with the id $driveOfferId.")
                }
            }
            is CarpoolDriveOffer -> throw UnprocessableContentError("The drive offer with the ID $driveOfferId is a carpool drive offer, so requests are automatically accepted.")
        }
        usersRepository.save(actingUser)
        driveOffersRepository.save(driveOfferInEditing)

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Get the complete route for a specific drive offer including its passengers and a specific requesting user.")
    @CommonApiResponses @UnprocessableContentApiResponse @OkApiResponse @NotFoundApiResponse
    @GetMapping("{driveOfferId}/requesting-users/{requestingUserId}/complete-route")
    fun getCompleteRouteWithRequestingUser(@PathVariable @UUID driveOfferId: String, @PathVariable @UUID requestingUserId: String, userToken: UserToken): ResponseEntity<CompleteRouteDP> {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        val driveOffer: DriveOffer = driveOffersRepository.findById(UUIDType.fromString(driveOfferId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("driveOffer.notFound", driveOfferId))
        if (driveOffer.driver.id != user.id) throw ForbiddenError(localizationService.getMessage("driveOffer.user.notDriverOf", user.id, driveOfferId))

        if(!usersRepository.existsById(UUIDType.fromString(requestingUserId))) throw NotFoundError(localizationService.getMessage("user.notFound", requestingUserId))
        when (driveOffer) {
            is PublicDriveOffer -> {
                if(driveOffer.requestingUsers.none { it.user.id.toString() == requestingUserId }) throw NotFoundError(localizationService.getMessage("driveOffer.user.notRequested", requestingUserId, driveOfferId))
                val completeRoute: CompleteRoute = geographyService.createCompleteRouteBasedOnUserStops(
                        driveOffer.route.start,
                        driveOffer.passengers + driveOffer.requestingUsers.filter { it.user.id.toString() == requestingUserId },
                        driveOffer.route.destination
                    )
                return ResponseEntity.ok(CompleteRouteDP.fromCompleteRoute(completeRoute))
            }
            is CarpoolDriveOffer -> throw UnprocessableContentError(localizationService.getMessage("driveOffer.carpool.requests.automaticallyAccepted", driveOfferId))
            else -> { throw IllegalStateException("Drive offer is neither a public drive offer nor a carpool drive offer.") }
        }
    }
}