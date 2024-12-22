package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.BadRequestError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.InternalServerError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.NotFoundError
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.applicationservices.geography.GeographyService
import de.uniflitzer.backend.model.*
import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.repositories.*
import de.uniflitzer.backend.repositories.errors.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content as MediaContent
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
import org.springframework.core.env.Environment
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.ZonedDateTime
import kotlin.jvm.optionals.getOrNull
import java.util.UUID as UUIDType



@SecurityRequirement(name = "Token Authentication")
@Transactional(rollbackFor = [Throwable::class])
@RestController
@RequestMapping("v1/users")
@Validated
@Tag(name = "User")
private class UsersCommunicator(
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val drivesRepository: DrivesRepository,
    @field:Autowired private val driveRequestsRepository: DriveRequestsRepository,
    @field:Autowired private val carpoolsRepository: CarpoolsRepository,
    @field:Autowired private val authenticationAdministrator: Keycloak,
    @field:Autowired private val environment: Environment,
    @field:Autowired private val imagesRepository: ImagesRepository,
    @field:Autowired private val geographyService: GeographyService,
) {
    @Operation(description = "Get details of a specific user.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{id}")
    fun getUser(@PathVariable @UUID id: String, userToken: UserToken): ResponseEntity<DetailedUserDP> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        val searchedUser: User = usersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError("User with id $id not found.")

        return ResponseEntity.ok(
            DetailedUserDP(
                searchedUser.id.toString(),
                searchedUser.firstName.value,
                searchedUser.lastName.value,
                searchedUser.birthday.toString(),
                GenderDP.valueOf(searchedUser.gender.name),
                if(searchedUser.id == UUIDType.fromString(userToken.id)) AddressDP.fromAddress(searchedUser.address) else null,
                searchedUser.description?.value,
                searchedUser.studyProgramme.value,
                searchedUser.isSmoking,
                searchedUser.animals.map { AnimalDP.fromAnimal(it) },
                DrivingStyleDP.fromDrivingStyle(searchedUser.drivingStyle),
                searchedUser.cars.map { CarDP.fromCar(it) },
                searchedUser.favoriteAddresses.map { AddressDP.fromAddress(it) },
                searchedUser.ratings.map { RatingDP.fromRating(it) }
            )
        )
    }

    @Operation(description = "Create a new user.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("")
    fun createUser(@RequestBody @Valid userCreation: UserCreationDP, userToken: UserToken): ResponseEntity<IdDP> {
        if (usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError("User with id ${userToken.id} already exists in resource server.")

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

        val userResource: UserResource = authenticationAdministrator.realm(
            environment.getProperty("keycloak.realm.name") ?: throw InternalServerError("Keycloak realm name not defined.")
        ).users().get(userToken.id)
        val user: UserRepresentation  = userResource.toRepresentation();
        user.singleAttribute<UserRepresentation>("hasUserInResourceServer", "true")
        userResource.update(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(IdDP(newUser.id.toString()))
    }

    @Operation(description = "Delete a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{id}")
    fun deleteUser(@PathVariable @UUID id: String, userToken: UserToken): ResponseEntity<Void> {
        if(userToken.id != id) throw ForbiddenError("The User can only its own account.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError("User with id $id not found.")

        imagesRepository.deleteById(UUIDType.fromString(id))
        actingUser.cars.forEach { car -> car.image?.let { image -> imagesRepository.deleteById(image.id) } }
        usersRepository.findAll().forEach { it.removeRatingOfUser(actingUser) }
        actingUser.driveOffersAsPassenger.forEach { it.removePassenger(actingUser) }
        actingUser.driveOffersAsRequestingUser.forEach { it.rejectRequestFromUser(actingUser.id) }
        actingUser.drivesAsPassenger.forEach { it.route = geographyService.createCompleteRouteBasedOnConfirmableUserStops(it.route.start, it.route.userStops, it.route.destination) }
        usersRepository.delete(actingUser)
        usersRepository.flush()
        authenticationAdministrator
            .realm(environment.getProperty("keycloak.realm.name") ?: throw InternalServerError("Keycloak realm name not defined."))
            .users()
            .delete(userToken.id)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Update a specific user.")
    @CommonApiResponses @NoContentApiResponse
    @PatchMapping("{id}")
    fun updateUser(@PathVariable @UUID id: String, @RequestBody @Valid userUpdate: UserUpdateDP, userToken: UserToken): ResponseEntity<Void> {
        if(userToken.id != id) throw ForbiddenError("The User can only update their own data.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")

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
    @CommonApiResponses @CreatedApiResponse @NotFoundApiResponse
    @PostMapping("{id}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createImageForUser(@PathVariable @UUID id: String, @RequestPart image: MultipartFile, userToken: UserToken): ResponseEntity<IdDP> {
        if (userToken.id != id) throw ForbiddenError("UserToken id does not match the user id.")
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")

        if(user.profilePicture != null) throw BadRequestError(listOf("User already has a profile picture."))

        val imageEntity:Image
        try {
            imageEntity = imagesRepository.save(image)
        } catch (error: FileCorruptedError) {
            throw BadRequestError(listOf(error.message!!))
        }
        catch (error: WrongFileFormatError) {
            throw BadRequestError(listOf(error.message!!))
        }
        user.profilePicture = imageEntity
        usersRepository.save(user)

        return ResponseEntity.status(201).body(IdDP(imageEntity.id.toString()))
    }

    @Operation(description = "Delete the image of a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{id}/image")
    fun deleteImageOfUser(@PathVariable @UUID id: String, userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != id) throw ForbiddenError("UserToken id does not match the user id.")
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        if(user.profilePicture == null) throw NotFoundError("User has no profile picture.")

        try {
            val profilePictureId:UUIDType = user.profilePicture!!.id
            user.profilePicture = null
            usersRepository.save(user)
            imagesRepository.deleteById(profilePictureId)
        }
        catch (error: ImageDirectoryMissingError) {
            throw NotFoundError(error.message ?: "Image not found.")
        }
        catch (error: FileMissingError) {
            throw NotFoundError(error.message ?: "Image not found.")
        }
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Get the image of a specific user.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content =  [MediaContent(mediaType = MediaType.IMAGE_JPEG_VALUE)]
            )
        ]
    )

    @CommonApiResponses @NotFoundApiResponse
    @GetMapping("{id}/image")
    fun getImageOfUser(@PathVariable @UUID id: String, @RequestParam quality: QualityDP, userToken: UserToken): ResponseEntity<ByteArray> {
        if (userToken.id != id) throw ForbiddenError("UserToken id does not match the user id.")
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")

        if (user.profilePicture == null) throw NotFoundError("User has no profile picture.")

        try {
            val image:ByteArray = imagesRepository.getById(user.profilePicture!!.id, if(quality == QualityDP.Preview) ImagesRepository.Quality.Preview else ImagesRepository.Quality.Full).getOrNull() ?: throw NotFoundError("Image not found.")
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image)
        } catch (error: ImageDirectoryMissingError) {
            throw NotFoundError(error.message!!)
        }
    }

    @Operation(description = "Create a car for a specific user.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("{id}/cars")
    fun createCarForUser(@PathVariable @UUID id: String, @RequestBody @Valid carCreation: CarCreationDP, userToken: UserToken): ResponseEntity<Void> {
        if(userToken.id != id) throw ForbiddenError("The user can only create a car for themselves.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw ForbiddenError("User with id $id does not exist in resource server.")

        actingUser.addCar(carCreation.toCar())
        usersRepository.save(actingUser)

        return ResponseEntity.status(HttpStatus.CREATED).build<Void>()
    }

    @Operation(description = "Create an image for a specific car of a specific user.")
    @CommonApiResponses @CreatedApiResponse @NotFoundApiResponse
    @PostMapping("{userId}/cars/{carIndex}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createImageForCarOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) carIndex: Int, @RequestPart image: MultipartFile, userToken: UserToken):ResponseEntity<IdDP> {
        if (userToken.id != userId) throw ForbiddenError("UserToken id does not match the user id.")
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")

        val car: Car
        try { car = user.getCarByIndex(carIndex) } catch (error: NotAvailableError) { throw NotFoundError(error.message!!) }
        if(car.image != null) throw BadRequestError(listOf("Car already has an image."))

        val imageEntity:Image
        try {
            imageEntity = imagesRepository.save(image)
        } catch (error: FileCorruptedError) {
            throw BadRequestError(listOf(error.message!!))
        }
        catch (error: WrongFileFormatError) {
            throw BadRequestError(listOf(error.message!!))
        }
        car.image = imageEntity
        usersRepository.save(user)

        return ResponseEntity.status(201).body(IdDP(imageEntity.id.toString()))
    }

    @Operation(description = "Get the image of a specific car of a specific user.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content =  [MediaContent(mediaType = MediaType.IMAGE_JPEG_VALUE)]
            )
        ]
    )
    @CommonApiResponses @NotFoundApiResponse
    @GetMapping("{userId}/cars/{carIndex}/image")
    fun getImageOfCarOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) carIndex: Int, @RequestParam quality: QualityDP, userToken: UserToken): ResponseEntity<ByteArray> {
        if (userToken.id != userId) throw ForbiddenError("UserToken id does not match the user id.")
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")

        val car: Car
        try { car = user.getCarByIndex(carIndex) } catch (error: NotAvailableError) { throw NotFoundError(error.message!!) }
        if (car.image == null) throw NotFoundError("Car has no image.")

        try {
            val image:ByteArray = imagesRepository.getById(car.image!!.id, if(quality == QualityDP.Preview) ImagesRepository.Quality.Preview else ImagesRepository.Quality.Full).getOrNull() ?: throw NotFoundError("Image not found.")
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image)
        } catch (error: ImageDirectoryMissingError) {
            throw NotFoundError(error.message!!)
        }
    }

    @Operation(description = "Delete the car of a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{userId}/cars/{carIndex}")
    fun deleteCarOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) carIndex: Int, userToken: UserToken): ResponseEntity<Void> {
        if(userToken.id != userId) throw ForbiddenError("The user can only delete their own car.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError("User with id $userId does not exist in resource server.")

        try {
            actingUser.removeCarAtIndex(carIndex.toUInt())
        } catch (_: IndexOutOfBoundsException) {
            throw NotFoundError("Car with index $carIndex not found.")
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Create a favorite address for a specific user.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("{id}/favorite-addresses")
    fun addFavoriteAddressForUser(@PathVariable @UUID id: String, @RequestBody @Valid address: AddressDP, userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != id) throw ForbiddenError("Users can only add favorite addresses to their own account.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw ForbiddenError("User with id $id does not exist in resource server.")

        actingUser.addFavoriteAddress(address.toAddress())
        usersRepository.save(actingUser)

        return ResponseEntity.status(HttpStatus.CREATED).build<Void>()
    }

    @Operation(description = "Delete a favorite address of a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{userId}/favorite-addresses/{addressIndex}")
    fun deleteFavoriteAddressOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) addressIndex: Int, userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != userId) throw ForbiddenError("Users can only delete favorite addresses from their own account.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError("User with id $userId does not exist in resource server.")

        try {
            actingUser.removeFavoriteAddressByIndex(addressIndex.toUInt())
        } catch (_: NotAvailableError) {
            throw NotFoundError("Favorite address with index $addressIndex not found.")
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Add favorite user to a specific user")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("/{userId}/favorite-users/")
    fun addFavoriteUserToUser(@PathVariable @UUID userId: String, @RequestBody @Valid favoriteUserAddition: UserAdditionDP, userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != userId) throw ForbiddenError("Users can only add favorite users to their own account.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError("Acting user with id $userId does not exist in resource server.")
        val userToFavorite = usersRepository.findById(UUIDType.fromString(favoriteUserAddition.id)).getOrNull() ?: throw NotFoundError("User to favorite with id ${favoriteUserAddition.id} was not found")

        actingUser.addFavoriteUser(userToFavorite)
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Delete favorite user of a specific user")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("/{userId}/favorite-users/{favoriteUserId}")
    fun deleteFavoriteUserOfUser(@PathVariable @UUID userId: String, @PathVariable @UUID favoriteUserId: String,  userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != userId) throw ForbiddenError("Users can only delete favorite users of their own account.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError("Acting user with id $userId does not exist in resource server.")
        val favoriteUserToDelete = usersRepository.findById(UUIDType.fromString(favoriteUserId)).getOrNull() ?: throw NotFoundError("Favorite user to delete with id $favoriteUserId was not found")

        try {
            actingUser.removeFavoriteUser(favoriteUserToDelete)
        } catch (_: NotAvailableError) {
            throw NotFoundError("Favorite user with id $favoriteUserId was not found.")
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Add blocked user to a specific user")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("/{userId}/blocked-users/")
    fun addBlockedUserToUser(@PathVariable @UUID userId: String, @RequestBody @Valid blockedUserAddition: UserAdditionDP, userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != userId) throw ForbiddenError("Users can only add blocked users to their own account.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError("Acting user with id $userId does not exist in resource server.")
        val userToBlock = usersRepository.findById(UUIDType.fromString(blockedUserAddition.id)).getOrNull() ?: throw NotFoundError("User to block with ${blockedUserAddition.id} was not found")

        actingUser.addBlockedUser(userToBlock)
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Delete blocked user of a specific user")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("/{userId}/blocked-users/{blockedUserId}")
    fun deleteBlockedUserOfUser(@PathVariable @UUID userId: String, @PathVariable @UUID blockedUserId: String,  userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != userId) throw ForbiddenError("Users can only delete blocked users of their own account.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError("Acting user with id $userId does not exist in resource server.")
        val blockedUserToDelete = usersRepository.findById(UUIDType.fromString(blockedUserId)).getOrNull() ?: throw NotFoundError("Blocked user to delete with id $blockedUserId was not found")

        try {
            actingUser.removeBlockedUser(blockedUserToDelete)
        } catch (_: NotAvailableError) {
            throw NotFoundError("Blocked user with id $blockedUserId was not found.")
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Get all drive offers of a specific user.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("{id}/drive-offers")
    fun getDriveOffersOfUser(@PathVariable @UUID id: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int, @RequestParam sortingDirection: SortingDirectionDP = SortingDirectionDP.Ascending, role: DriverOfferRoleDP? = null, userToken: UserToken): ResponseEntity<PageDP<PartialDriveOfferDP>> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        if(actingUser.id != UUIDType.fromString(id)) throw ForbiddenError("The user can only get their own drive offers.")

        val resultingDriveOffersOfUser: List<DriveOffer> =
            if(role == null) {
                actingUser.driveOffersAsRequestingUser + actingUser.driveOffersAsPassenger + actingUser.driveOffersAsDriver
            }
            else {
                when (role) {
                    DriverOfferRoleDP.Driver -> actingUser.driveOffersAsDriver
                    DriverOfferRoleDP.Passenger -> actingUser.driveOffersAsPassenger
                    DriverOfferRoleDP.Requester -> actingUser.driveOffersAsRequestingUser
                }
            }
            .let {
                when(sortingDirection) {
                    SortingDirectionDP.Ascending -> it.sortedBy(DriveOffer::plannedDeparture)
                    SortingDirectionDP.Descending -> it.sortedByDescending(DriveOffer::plannedDeparture)
                }
            }

        return ResponseEntity.ok(
            PartialDriveOfferPageDP.fromList(
                resultingDriveOffersOfUser.map { PartialDriveOfferDP.fromDriveOffer(it, it.driver in actingUser.favoriteUsers) },
                pageNumber.toUInt(),
                perPage.toUInt()
            )
        )
    }

    @Operation(description = "Get all drive requests of a specific user.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("{id}/drive-requests")
    fun getDriveRequestsOfUser(@PathVariable @UUID id: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int, @RequestParam sortingDirection: SortingDirectionDP = SortingDirectionDP.Ascending, userToken: UserToken): ResponseEntity<PageDP<PartialDriveRequestDP>> {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        if(user.id != UUIDType.fromString(id)) throw ForbiddenError("The user can only get his own drive requests.")

        val driveRequests: List<DriveRequest> = driveRequestsRepository.findAllDriveRequests(
            Sort.by(
                when(sortingDirection) {
                    SortingDirectionDP.Ascending -> Sort.Direction.ASC
                    SortingDirectionDP.Descending -> Sort.Direction.DESC
                },
                DriveRequest::plannedDeparture.name
            ),
            user.id
        )

        return ResponseEntity.ok(
            PartialDriveRequestPageDP.fromList(
                driveRequests.map {
                    when (it) {
                        is CarpoolDriveRequest -> PartialCarpoolDriveRequestDP.fromCarpoolDriveRequest(it, false)
                        is PublicDriveRequest -> PartialPublicDriveRequestDP.fromPublicDriveRequest(it, false)
                        else -> throw Exception()
                    }
                },
                pageNumber.toUInt(),
                perPage.toUInt()
            )
        )
    }

    @Operation(description = "Get all drives of a specific user.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("{id}/drives")
    fun getDrivesOfUser(@PathVariable @UUID id: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int, @RequestParam sortingDirection: SortingDirectionDP = SortingDirectionDP.Ascending, userToken: UserToken): ResponseEntity<PageDP<PartialDriveDP>> {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        if(user.id != UUIDType.fromString(id)) throw ForbiddenError("The user can only get his own drives.")

        val sort: Sort = if (sortingDirection == SortingDirectionDP.Ascending) Sort.by("plannedDeparture").ascending() else Sort.by("plannedDeparture").descending()
        val page: Page<Drive> = drivesRepository.findDrives(PageRequest.of(pageNumber - 1, perPage, sort), user.id)

        return ResponseEntity.ok(
            PartialDrivePageDP(page.totalPages, page.content.map { PartialDriveDP.fromDrive(it) })
        )
    }

    @Operation(description = "Get all carpools of a specific user.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("{id}/carpools")
    fun getCarpoolsOfUser(@PathVariable @UUID id: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int, @RequestParam sortingDirection: SortingDirectionDP = SortingDirectionDP.Ascending, userToken: UserToken): ResponseEntity<PageDP<PartialCarpoolDP>> {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        if(user.id != UUIDType.fromString(id)) throw ForbiddenError("The user can only get his own carpools.")

        val sort: Sort = if (sortingDirection == SortingDirectionDP.Ascending) Sort.by("name").ascending() else Sort.by("name").descending()
        val page: Page<Carpool> = carpoolsRepository.findCarpools(PageRequest.of(pageNumber - 1, perPage, sort), user.id)

        return ResponseEntity.ok(
            PartialCarpoolPageDP(page.totalPages, page.content.map { PartialCarpoolDP.fromCarpool(it) })
        )
    }

    @Operation(description = "Create a rating for a specific user.")
    @CommonApiResponses @CreatedApiResponse @NotFoundApiResponse
    @PostMapping("{id}/ratings")
    fun createRatingForUser(@PathVariable @UUID id: String, @RequestBody @Valid ratingCreation: RatingCreationDP, userToken: UserToken): ResponseEntity<Void>
    {
        if(userToken.id == id) throw ForbiddenError("The user cannot create a rating for himself.")
        val author: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        val ratedUser: User = usersRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError("User with id $id does not exist in resource server.")

        when(ratingCreation.role) {
            RoleDP.Driver -> {
                if(ratedUser.drivesAsDriver
                    .filter { (it.arrival?.isAfter(ZonedDateTime.now().minusDays(3)) ?: false) && (it.arrival?.isBefore(ZonedDateTime.now()) ?: false) }
                    .none { it.passengers.contains(author) })
                    throw BadRequestError(listOf("User with id ${ratedUser.id} was no driver of user wit id ${author.id}."))

                if(ratedUser.ratings.any { it.author == author && it.role == Role.Driver && it.created.isAfter(ZonedDateTime.now().minusDays(3)) })
                    throw BadRequestError(listOf("User with id ${author.id} already rated user with id ${ratedUser.id} as driver in the last three days."))
            }
            RoleDP.Passenger -> {
                if(ratedUser.drivesAsPassenger
                    .filter { (it.arrival?.isAfter(ZonedDateTime.now().minusDays(3)) ?: false) && (it.arrival?.isBefore(ZonedDateTime.now()) ?: false) }
                    .none { it.driver == author })
                    throw BadRequestError(listOf("User with id ${ratedUser.id} was no passenger of user wit id ${author.id}."))

                if(ratedUser.ratings.any { it.author == author && it.role == Role.Passenger && it.created.isAfter(ZonedDateTime.now().minusDays(3)) })
                    throw BadRequestError(listOf("User with id ${author.id} already rated user with id ${ratedUser.id} as passenger in the last three days."))
            }
        }

        ratedUser.addRating(Rating(
            author,
            ratingCreation.role.toRole(),
            Content(ratingCreation.content),
            Stars(ratingCreation.stars.toUInt()),
            ZonedDateTime.now()
        ))
        usersRepository.save(ratedUser)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}

