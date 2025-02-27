package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.CommonApiResponses
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.NoContentApiResponse
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.NotFoundApiResponse
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.OkApiResponse
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.BadRequestError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.NotFoundError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.StompError
import de.uniflitzer.backend.applicationservices.communicators.version1.localization.LocalizationService
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.applicationservices.geography.GoogleMapsPlatformGeographyService
import de.uniflitzer.backend.model.Car
import de.uniflitzer.backend.model.Coordinate
import de.uniflitzer.backend.model.Drive
import de.uniflitzer.backend.model.User
import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.model.errors.RepeatedActionError
import de.uniflitzer.backend.repositories.DrivesRepository
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.ZonedDateTime
import kotlin.jvm.optionals.getOrNull
import java.util.UUID as UUIDType

@RestController @RequestMapping("v1/drives")
@Tag(name = "Drives") @SecurityRequirement(name = "Token Authentication")
@MessageMapping("v1/drives")
@Validated
@Transactional(rollbackFor = [Throwable::class])
class DrivesCommunicator(
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val drivesRepository: DrivesRepository,
    @field:Autowired private val geographyService: GoogleMapsPlatformGeographyService,
    @field:Autowired private val imagesRepository: ImagesRepository,
    @field:Autowired private val messagingTemplate: SimpMessagingTemplate,
    @field:Autowired private val localizationService: LocalizationService
) {
    @Operation(description = "Get details of a specific drive.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{driveId}")
    fun getDrive(@PathVariable @UUID driveId:String, userToken: UserToken): ResponseEntity<DetailedDriveDP>
    {
        val user: User = usersRepository.findById(java.util.UUID.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val drive: Drive = drivesRepository.findById(java.util.UUID.fromString(driveId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("drive.notFound", driveId))
        if(drive.driver.id != user.id && drive.passengers.none { it.id == user.id }) throw ForbiddenError(localizationService.getMessage("drive.user.notDriverOrPassengerOf", user.id, driveId))

        return ResponseEntity.ok(DetailedDriveDP.fromDrive(drive))
    }

    @Operation(description = "Get the image of the car of a specific drive.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content =  [Content(mediaType = MediaType.IMAGE_JPEG_VALUE)]
            )
        ]
    )
    @CommonApiResponses @NotFoundApiResponse
    @GetMapping("{driveId}/car/image")
    fun getImageOfCar(@PathVariable @UUID driveId: String, @RequestParam quality: QualityDP, userToken: UserToken): ResponseEntity<ByteArray> {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val drive: Drive = drivesRepository.findById(UUIDType.fromString(driveId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("drive.notFound", driveId))
        if(drive.driver.id != user.id && drive.passengers.none { it.id == user.id }) throw ForbiddenError(localizationService.getMessage("drive.user.notDriverOrPassengerOf", user.id, driveId))

        val car: Car = drive.car
        if (car.image == null) throw NotFoundError(localizationService.getMessage("drive.car.image.notExists", driveId))

        val image:ByteArray = imagesRepository.getById(car.image!!.id, quality.toQuality()).getOrNull() ?: throw NotFoundError(localizationService.getMessage("drive.car.image.notFound", driveId))
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image)
    }

    @Operation(description = "Update the actual departure or the arrival of a specific drive. If the actual departure was updated once, it must not be updated again. The same applies to the arrival.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PatchMapping("{driveId}")
    fun updateDrive(@PathVariable @UUID driveId:String, @RequestBody @Valid driveUpdate: DriveUpdateDP, userToken: UserToken): ResponseEntity<Void>
    {
        val user: User = usersRepository.findById(java.util.UUID.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val drive: Drive = drivesRepository.findById(java.util.UUID.fromString(driveId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("drive.notFound", driveId))
        if(drive.driver.id != user.id) throw ForbiddenError(localizationService.getMessage("drive.user.notDriverOf", user.id, driveId))
        if(drive.isCancelled) throw ForbiddenError(localizationService.getMessage("drive.alreadyCancelled", driveId))

        driveUpdate.actualDeparture?.let {
            if(drive.actualDeparture != null) throw ForbiddenError(localizationService.getMessage("drive.actualDeparture.alreadySet", driveId))
            drive.actualDeparture = ZonedDateTime.parse(it)
        }
        driveUpdate.actualArrival?.let {
            if(drive.actualArrival != null) throw ForbiddenError(localizationService.getMessage("drive.actualArrival.alreadySet", driveId))
            drive.actualArrival = ZonedDateTime.parse(it)
        }

        drivesRepository.save(drive)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Confirm that your are waiting at a specific user stop of a specific drive.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveId}/complete-route/user-stops/{userId}/confirmations")
    fun confirmUserStop(@PathVariable @UUID driveId: String, @PathVariable @UUID userId: String, userToken: UserToken): ResponseEntity<Void>
    {
        if(userId != userToken.id) throw ForbiddenError(localizationService.getMessage("drive.userStop.confirmOthers", userToken.id, driveId))
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val drive: Drive = drivesRepository.findById(UUIDType.fromString(driveId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("drive.notFound", driveId))
        if(drive.passengers.none { it.id == user.id }) throw ForbiddenError(localizationService.getMessage("drive.user.notPassengerOf", user.id, driveId))
        if(drive.isCancelled) throw ForbiddenError(localizationService.getMessage("drive.cancelled", driveId))
        try{
            drive.route.confirmUserStop(user.id)
        } catch(error: NotAvailableError){
            throw NotFoundError(localizationService.getMessage("drive.userStop.notExists", user.id, driveId))
        } catch(error: RepeatedActionError) {
            throw BadRequestError(listOf(localizationService.getMessage("drive.userStop.alreadyConfirmed", user.id, driveId)))
        }

        drivesRepository.save(drive)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Cancel a specific user stop of a specific drive.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveId}/complete-route/user-stops/{userId}/cancellation")
    fun cancelUserStop(@PathVariable @UUID driveId: String, @PathVariable @UUID userId: String, userToken: UserToken): ResponseEntity<Void>
    {
        if(userId != userToken.id) throw ForbiddenError(localizationService.getMessage("drive.userStop.cancelOthers", userToken.id, driveId))
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val drive: Drive = drivesRepository.findById(UUIDType.fromString(driveId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("drive.notFound", driveId))
        if(drive.isCancelled) throw ForbiddenError(localizationService.getMessage("drive.cancelled", driveId))
        try{
            drive.route.cancelUserStop(user.id)
            drive.removePassenger(user)
        } catch(error: NotAvailableError){
            throw NotFoundError(localizationService.getMessage("drive.userStopNotExistsOrNotPassengerOf", user.id, driveId))
        }
        drive.route = geographyService.createCompleteRouteBasedOnConfirmableUserStops(drive.route.start, drive.route.userStops, drive.route.destination)

        drivesRepository.save(drive)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Cancel a specific drive.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveId}/cancellation")
    fun cancelDrive(@PathVariable @UUID driveId:String, userToken: UserToken): ResponseEntity<Void>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        val drive: Drive = drivesRepository.findById(UUIDType.fromString(driveId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("drive.notFound", driveId))
        if(drive.driver.id != user.id) throw ForbiddenError(localizationService.getMessage("drive.user.notDriverOf", user.id, driveId))
        if(drive.isCancelled) throw ForbiddenError(localizationService.getMessage("drive.cancelled", driveId))

        drive.isCancelled = true
        drivesRepository.save(drive)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Update the current position of a specific user")
    @CommonApiResponses @NoContentApiResponse
    @PatchMapping("/{driveId}/current-driver-position")
    fun updateCurrentPositionOfUser(@PathVariable @UUID driveId: String, @RequestBody coordinate: CoordinateDP, userToken: UserToken): ResponseEntity<Void> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        val currentDrive: Drive = drivesRepository.findById(UUIDType.fromString(driveId)).getOrNull() ?: throw NotFoundError("Drive with id $driveId not found.")
        if(UUIDType.fromString(userToken.id) != currentDrive.driver.id) throw ForbiddenError("Only the driver can update the current driver position.")

        currentDrive.currentPosition = Coordinate(coordinate.latitude, coordinate.longitude)
        messagingTemplate.convertAndSend("/v1/drives/${currentDrive.id}/current-driver-position", CoordinateDP.fromCoordinate(currentDrive.currentPosition!!))

        return ResponseEntity.noContent().build<Void>()
    }

    @SubscribeMapping("{driveId}/current-driver-position")
    fun subscribeToCurrentDriverPosition(userToken: UserToken, @DestinationVariable @UUID driveId: String): CoordinateDP? {
        val currentDrive: Drive = drivesRepository.findById(UUIDType.fromString(driveId)).getOrNull() ?: throw StompError(listOf("Drive with id $driveId not found."))
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw StompError(listOf("User with id ${userToken.id} does not exist in resource server."))
        if(actingUser !in currentDrive.passengers && actingUser != currentDrive.driver) throw StompError(listOf("User is not part of this drive with id ${userToken.id}"))

        return currentDrive.currentPosition?.let { CoordinateDP.fromCoordinate(it) }
    }
}
