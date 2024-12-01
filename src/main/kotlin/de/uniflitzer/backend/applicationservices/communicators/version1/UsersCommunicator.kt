package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.InternalServerError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.NotFoundError
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.*
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
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.resource.UserResource
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.ZonedDateTime
import kotlin.jvm.optionals.getOrNull
import java.util.UUID as UUIDType

@SecurityRequirement(name = "Token Authentication")
@RestController
@RequestMapping("v1/users")
@Validated
@Tag(name = "User")
private class UsersCommunicator(
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val keycloak: Keycloak,
    @field:Autowired private val environment: Environment
) {
    @Autowired
    private lateinit var defaultErrorAttributes: DefaultErrorAttributes

    @GetMapping("test")
    fun test():ResponseEntity<Void>{
        throw IllegalStateException("Test")
        return ResponseEntity.ok().build()
    }

    @Operation(description = "Get details of a specific user.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{id}")
    fun getUser(@PathVariable @UUID id: String, userToken: UserToken): ResponseEntity<DetailedUserDP> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))
        val searchedUser: User = usersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("User with id $id not found."))

        return ResponseEntity.ok(
            DetailedUserDP(
                searchedUser.id.toString(),
                searchedUser.firstName.value,
                searchedUser.lastName.value,
                searchedUser.birthday.toString(),
                GenderDP.valueOf(searchedUser.gender.name),
                AddressDP.fromAddress(searchedUser.address),
                searchedUser.description?.value,
                searchedUser.studyProgramme.value,
                searchedUser.isSmoking,
                searchedUser.animals.map { AnimalDP.fromAnimal(it) },
                DrivingStyleDP.fromDrivingStyle(searchedUser.drivingStyle),
                searchedUser.cars.map { CarDP.fromCar(it) }
            )
        )
    }

    @Operation(description = "Create a new user.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("")
    fun createUser(@RequestBody @Valid userCreation: UserCreationDP, userToken: UserToken): ResponseEntity<IdDP> {
        if (usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(ErrorDP("User with id ${userToken.id} already exists in resource server."))

        val newUser: User = User(
            UUIDType.fromString(userToken.id),
            FirstName(userCreation.firstName),
            LastName(userCreation.lastName),
            ZonedDateTime.parse(userCreation.birthday),
            Gender.valueOf(userCreation.gender.toString()),
            userCreation.address.toAddress(),
            StudyProgramme(userCreation.studyProgramme)
        )
        usersRepository.save(newUser)

        val userResource: UserResource = keycloak.realm(
            environment.getProperty("keycloak.realm.name") ?: throw InternalServerError(ErrorDP("Keycloak realm name not defined."))
        ).users().get(userToken.id)
        val user: UserRepresentation  = userResource.toRepresentation();
        user.singleAttribute<UserRepresentation>("hasUserInResourceServer", "true")
        userResource.update(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(IdDP(newUser.id.toString()))
    }

    @Operation(description = "Delete a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{id}")
    fun deleteUser(@PathVariable @UUID id: String): ResponseEntity<Void> {
        TODO()
    }

    @Operation(description = "Update a specific user.")
    @CommonApiResponses @NoContentApiResponse
    @PatchMapping("{id}")
    fun updateUser(@PathVariable @UUID id: String, @RequestBody @Valid userUpdate: UserUpdateDP, userToken: UserToken): ResponseEntity<Void> {
        if(userToken.id != id) throw ForbiddenError(ErrorDP("The User can only update their own data."))
        val actingUser: User = usersRepository.findById(UUIDType.fromString(id)).orElseThrow { ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server.")) }

        actingUser.apply{
            userUpdate.firstName?.let { actingUser.firstName = FirstName(it) }
            userUpdate.lastName?.let { actingUser.lastName = LastName(it) }
            userUpdate.birthday?.let { actingUser.birthday = ZonedDateTime.parse(it) }
            userUpdate.gender?.let { actingUser.gender = it.toGender() }
            userUpdate.address?.let { actingUser.address = it.toAddress() }
            userUpdate.studyProgramme?.let { actingUser.studyProgramme = StudyProgramme(it) }
            userUpdate.description?.ifPresent { actingUser.description = Description(it) }
            userUpdate.isSmoking?.ifPresent { actingUser.isSmoking = it }
            userUpdate.animals?.ifPresent { actingUser.refillAnimals(it.map { it.toAnimal() }) }
            userUpdate.drivingStyle?.ifPresent { actingUser.drivingStyle = it.toDrivingStyle() }
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Create an image for a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{id}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createImageForUser(@PathVariable @UUID id: String, @RequestPart image: MultipartFile): ResponseEntity<Void> {
        TODO()
    }

    @Operation(description = "Delete the image of a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{id}/image")
    fun deleteImageOfUser(@PathVariable @UUID id: String): ResponseEntity<Void> {
        TODO()
    }

    @Operation(description = "Get the image of a specific user.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content =  [Content(mediaType = MediaType.IMAGE_JPEG_VALUE)]
            )
        ]
    )
    @CommonApiResponses @NotFoundApiResponse
    @GetMapping("{id}/image")
    fun getImageOfUser(@PathVariable @UUID id: String, @RequestParam quality: QualityDP): ResponseEntity<ByteArray> {
        TODO()
    }

    @Operation(description = "Create a car for a specific user.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("{id}/cars")
    fun createCarForUser(@PathVariable @UUID id: String, @RequestBody @Valid carCreation: CarCreationDP, userToken: UserToken): ResponseEntity<Void> {
        if(userToken.id != id) throw ForbiddenError(ErrorDP("The user can only create a car for themselves."))
        val actingUser: User = usersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id $id does not exist in resource server."))

        actingUser.addCar(carCreation.toCar())
        usersRepository.save(actingUser)

        return ResponseEntity.status(HttpStatus.CREATED).build<Void>()
    }

    @Operation(description = "Create an image for a specific car of a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{userId}/cars/{carIndex}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createImageForCarOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) carIndex: Int, @RequestPart image: MultipartFile):ResponseEntity<Void> {
        TODO()
    }

    @Operation(description = "Get the image of a specific car of a specific user.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content =  [Content(mediaType = MediaType.IMAGE_JPEG_VALUE)]
            )
        ]
    )
    @CommonApiResponses @NotFoundApiResponse
    @GetMapping("{userId}/cars/{carIndex}/image")
    fun getImageOfCarOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) carIndex: Int, @RequestParam quality: QualityDP): ResponseEntity<ByteArray> {
        TODO()
    }

    @Operation(description = "Delete the car of a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{userId}/cars/{carIndex}")
    fun deleteCarOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) carIndex: Int, userToken: UserToken): ResponseEntity<Void> {
        if(userToken.id != userId) throw ForbiddenError(ErrorDP("The user can only delete their own car."))
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id $userId does not exist in resource server."))

        try {
            actingUser.removeCarAtIndex(carIndex.toUInt())
        } catch (_: IndexOutOfBoundsException) {
            throw NotFoundError(ErrorDP("Car with index $carIndex not found."))
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Get all drive offers of a specific user.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("{id}/drive-offers")
    fun getDriveOffersOfUser(@PathVariable @UUID id: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int, @RequestParam sortingDirection: SortingDirection = SortingDirection.Ascending, userToken: UserToken): ResponseEntity<PageDP<PartialDriveOfferDP>> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))
        if(actingUser.id != UUIDType.fromString(id)) throw ForbiddenError(ErrorDP("The user can only get their own drive offers."))

        val allDriveOffersOfUser: List<DriveOffer> = (actingUser.driveOffersAsRequestingUser + actingUser.driveOffersAsPassenger + actingUser.driveOffersAsDriver)
                                                        .let {
                                                            when(sortingDirection) {
                                                                SortingDirection.Ascending -> it.sortedBy(DriveOffer::plannedDeparture)
                                                                SortingDirection.Descending -> it.sortedByDescending(DriveOffer::plannedDeparture)
                                                            }
                                                        }

        return ResponseEntity.ok(
            PageDP(
                maximumPage = (allDriveOffersOfUser.size / perPage) + 1,
                content = allDriveOffersOfUser
                    .subList((pageNumber - 1) * perPage, pageNumber * perPage)
                    .map { PartialDriveOfferDP.fromDriveOffer(it) }
            )
        )
    }

    @Operation(description = "Get all drive requests of a specific user.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("{id}/drive-requests")
    fun getDriveRequestsOfUser(@PathVariable @UUID id: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int, @RequestParam sortingDirection: SortingDirection = SortingDirection.Ascending): ResponseEntity<PageDP<PartialDriveRequestDP>> {
        TODO()
    }

    @Operation(description = "Get all drives of a specific user.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("{id}/drives")
    fun getDrivesOfUser(@PathVariable @UUID id: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int, @RequestParam sortDirection: SortingDirection = SortingDirection.Ascending): ResponseEntity<PageDP<DriveDP>> {
        TODO()
    }
}

