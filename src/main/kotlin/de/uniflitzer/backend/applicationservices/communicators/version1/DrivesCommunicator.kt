package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.CommonApiResponses
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.NoContentApiResponse
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.NotFoundApiResponse
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.OkApiResponse
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.NotFoundError
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.applicationservices.geography.GoogleMapsPlatformGeographyService
import de.uniflitzer.backend.model.*
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
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.ZonedDateTime
import kotlin.jvm.optionals.getOrNull
import java.util.UUID as UUIDType

@RestController
@RequestMapping("v1/drives")
@Validated
@SecurityRequirement(name = "Token Authentication")
@Tag(name = "Drives")
private class DrivesCommunicator(@field:Autowired private val usersRepository: UsersRepository, @field:Autowired private val drivesRepository: DrivesRepository, @field:Autowired private val geographyService: GoogleMapsPlatformGeographyService, @field:Autowired private val imagesRepository: ImagesRepository)
{
    @Operation(description = "Get details of a specific drive.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{id}")
    fun getDrive(@PathVariable @UUID id:String, userToken: UserToken): ResponseEntity<DriveDP>
    {
        val user: User = usersRepository.findById(java.util.UUID.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val drive: Drive = drivesRepository.findById(java.util.UUID.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("Drive with id $id not found."))
        if(drive.driver.id != user.id && drive.passengers.none { it.id == user.id }) throw ForbiddenError(ErrorDP("UserToken id does neither match the driver id nor any of the passenger ids."))

        return ResponseEntity.ok(
            DriveDP
            (
                drive.id.toString(),
                PartialUserDP.fromUser(drive.driver),
                CarDP.fromCar(drive.car),
                drive.passengers.map { PartialUserDP.fromUser(it) },
                CompleteRouteDP.fromCompleteRoute(drive.route),
                drive.plannedDeparture.toString(),
                drive.actualDeparture?.toString(),
                drive.arrival?.toString()
            )
        )
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
    @GetMapping("{id}/car/image")
    fun getImageOfCar(@PathVariable @UUID id: String, @RequestParam quality: QualityDP, userToken: UserToken): ResponseEntity<ByteArray> {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val drive: Drive = drivesRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("Drive with id $id not found."))
        if(drive.driver.id != user.id && drive.passengers.none { it.id == user.id }) throw ForbiddenError(ErrorDP("UserToken id does neither match the driver id nor any of the passenger ids."))

        val car: Car = drive.car
        if (car.image == null) throw NotFoundError(ErrorDP("Car has no image."))

        try {
            val image:ByteArray = imagesRepository.getById(car.image!!.id, if(quality == QualityDP.Preview) ImagesRepository.Quality.Preview else ImagesRepository.Quality.Full).getOrNull() ?: throw NotFoundError(ErrorDP("Image not found."))
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image)
        } catch (error: ImageDirectoryMissingError) {
            throw NotFoundError(ErrorDP(error.message!!))
        }
    }

    @Operation(description = "Update the actual departure or the arrival of a specific drive. If the actual departure was updated once, it must not be updated again. The same applies to the arrival.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PatchMapping("{id}")
    fun updateDrive(@PathVariable @UUID id:String, @RequestBody @Valid driveUpdateRequest: DriveUpdateDP, userToken: UserToken): ResponseEntity<Void>
    {
        val user: User = usersRepository.findById(java.util.UUID.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val drive: Drive = drivesRepository.findById(java.util.UUID.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("Drive with id $id not found."))
        if(drive.driver.id != user.id) throw ForbiddenError(ErrorDP("UserToken id does not match the driver id."))

        driveUpdateRequest.actualDeparture?.let {
            if(drive.actualDeparture != null) throw Exception()
            drive.actualDeparture = ZonedDateTime.parse(it)
        }
        driveUpdateRequest.arrival?.let {
            if(drive.arrival != null) throw Exception()
            drive.arrival = ZonedDateTime.parse(it)
        }

        drivesRepository.save(drive)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Confirm that your are waiting at a specific user stop of a specific drive.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveId}/complete-route/user-stops/{userId}/confirmations")
    fun confirmUserStop(@PathVariable @UUID driveId: String, @PathVariable @UUID userId: String, userToken: UserToken): ResponseEntity<Void>
    {
        if(userId != userToken.id) throw ForbiddenError(ErrorDP("UserToken id does not match the userId."))
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val drive: Drive = drivesRepository.findById(UUIDType.fromString(driveId)).getOrNull() ?: throw NotFoundError(ErrorDP("Drive with id $driveId not found."))
        if(drive.passengers.none { it.id == user.id }) throw ForbiddenError(ErrorDP("User is not a passenger of this drive."))
        drive.route.confirmUserStop(user.id)
        
        drivesRepository.save(drive)
        return ResponseEntity.noContent().build()
    }

    @OkApiResponse
    @GetMapping("compute-route")
    fun testComputeRoute(userToken: UserToken): ResponseEntity<CompleteRouteDP>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))
        val completeRoute:CompleteRoute = geographyService.createRoute(
            Position(Coordinate(50.316944, 11.927306), Address("Testingston", "23","95028","Hof")),
            listOf(
                UserStop(user, Position(Coordinate(50.321694, 11.924083), Address("Test", "3","95028","Hof"))),
                UserStop(user, Position(Coordinate(50.319001, 11.931654), Address("Test", "56","95028","Hof")))
            ),
            Position(Coordinate(50.324833, 11.941250), Address("Testinger", "45","95028","Hof"))
        )
        return ResponseEntity.ok(CompleteRouteDP.fromCompleteRoute(completeRoute))
    }
}