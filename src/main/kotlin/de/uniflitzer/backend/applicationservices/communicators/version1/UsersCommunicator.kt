package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.BadRequestError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.InternalServerError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.NotFoundError
import de.uniflitzer.backend.applicationservices.communicators.version1.localization.LocalizationService
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.applicationservices.geography.GeographyService
import de.uniflitzer.backend.model.*
import de.uniflitzer.backend.model.errors.ConflictingActionError
import de.uniflitzer.backend.model.errors.ImpossibleActionError
import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.model.errors.RepeatedActionError
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
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull
import java.util.UUID as UUIDType

@RestController @RequestMapping("v1/users")
@Tag(name = "User") @SecurityRequirement(name = "Token Authentication")
@Validated
@Transactional(rollbackFor = [Throwable::class])
private class UsersCommunicator(
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val drivesRepository: DrivesRepository,
    @field:Autowired private val driveRequestsRepository: DriveRequestsRepository,
    @field:Autowired private val carpoolsRepository: CarpoolsRepository,
    @field:Autowired private val authenticationAdministrator: Keycloak,
    @field:Autowired private val environment: Environment,
    @field:Autowired private val imagesRepository: ImagesRepository,
    @field:Autowired private val geographyService: GeographyService,
    @field:Autowired private val localizationService: LocalizationService
) {
    @Operation(description = "Get details of a specific user.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{userId}")
    fun getUser(@PathVariable @UUID userId: String, userToken: UserToken): ResponseEntity<DetailedUserDP> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        val searchedUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw NotFoundError("User with id $userId not found.")
        val isActingUserLookingAtHisOwnProfile: Boolean = searchedUser.id == UUIDType.fromString(userToken.id)

        return ResponseEntity.ok(
            DetailedUserDP(
                searchedUser.id.toString(),
                searchedUser.firstName.value,
                searchedUser.lastName.value,
                searchedUser.birthday.toString(),
                GenderDP.valueOf(searchedUser.gender.name),
                if(isActingUserLookingAtHisOwnProfile) AddressDP.fromAddress(searchedUser.address) else null,
                searchedUser.description?.value,
                searchedUser.studyProgramme.value,
                searchedUser.isSmoking,
                searchedUser.animals.map { AnimalDP.fromAnimal(it) },
                DrivingStyleDP.fromDrivingStyle(searchedUser.drivingStyle),
                if(isActingUserLookingAtHisOwnProfile) searchedUser.cars.map { CarDP.fromCar(it) } else null,
                if(isActingUserLookingAtHisOwnProfile) searchedUser.favoriteUsers.map { PartialUserDP.fromUser(it) } else null,
                if(isActingUserLookingAtHisOwnProfile) searchedUser.blockedUsers.map { PartialUserDP.fromUser(it) } else null,
                if(isActingUserLookingAtHisOwnProfile) searchedUser.favoriteAddresses.map { AddressDP.fromAddress(it) } else null,
                searchedUser.ratings.map { RatingDP.fromRating(it) },
                searchedUser.receivedInvites.map { PartialCarpoolDP.fromCarpool(it) }
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

        val userResource: UserResource = authenticationAdministrator
            .realm(environment.getProperty("keycloak.realm.name") ?: throw IllegalStateException("Keycloak realm name not defined."))
            .users()
            .get(userToken.id)
        val user: UserRepresentation = userResource
            .toRepresentation()
            .singleAttribute<UserRepresentation>("hasUserInResourceServer", "true")
        userResource.update(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(IdDP(newUser.id.toString()))
    }

    @Operation(description = "Delete a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{userId}")
    fun deleteUser(@PathVariable @UUID userId: String, userToken: UserToken): ResponseEntity<Void> {
        if(userToken.id != userId) throw ForbiddenError("The User can only delete its own account.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw NotFoundError("User with id $userId not found.")

        actingUser.profilePicture?.id?.let { imagesRepository.deleteById(it) }
        actingUser.cars.forEach { car -> car.image?.let { image -> imagesRepository.deleteById(image.id) } }
        usersRepository.findAll().forEach { it.removeRatingsOfUser(actingUser) }
        actingUser.driveOffersAsPassenger.forEach { it.removePassenger(actingUser) }
        actingUser.driveOffersAsRequestingUser.forEach { it.rejectRequestFromUser(actingUser.id) }
        actingUser.drivesAsPassenger.forEach { it.route = geographyService.createCompleteRouteBasedOnConfirmableUserStops(it.route.start, it.route.userStops, it.route.destination) }
        usersRepository.delete(actingUser)
        usersRepository.flush()
        authenticationAdministrator
            .realm(environment.getProperty("keycloak.realm.name") ?: throw IllegalStateException("Keycloak realm name not defined."))
            .users()
            .delete(userId)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Update a specific user.")
    @CommonApiResponses @NoContentApiResponse
    @PatchMapping("{userId}")
    fun updateUser(@PathVariable @UUID userId: String, @RequestBody @Valid userUpdate: UserUpdateDP, userToken: UserToken): ResponseEntity<Void> {
        if(userToken.id != userId) throw ForbiddenError("The User can only update their own data.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError("User with id $userId does not exist in resource server.")

        actingUser.apply{
            userUpdate.firstName?.let { actingUser.firstName = FirstName(it) }
            userUpdate.lastName?.let { actingUser.lastName = LastName(it) }
            try { userUpdate.birthday?.let { actingUser.birthday = ZonedDateTime.parse(it) } } catch (_: ImpossibleActionError) { throw BadRequestError(listOf("Birthday must be in past")) }
            userUpdate.gender?.let { actingUser.gender = it.toGender() }
            userUpdate.address?.let { actingUser.address = it.toAddress() }
            userUpdate.studyProgramme?.let { actingUser.studyProgramme = StudyProgramme(it) }
            userUpdate.description?.let { descriptionOptional -> actingUser.description = descriptionOptional.getOrNull()?.let { descriptionOptionalUnwrapped -> Description(descriptionOptionalUnwrapped) } }
            userUpdate.isSmoking?.let { isSmokingOptional -> actingUser.isSmoking = isSmokingOptional.getOrNull() }
            try {
                userUpdate.animals?.let {
                    animalsOptional ->
                        actingUser.refillAnimals(
                            animalsOptional
                                .getOrElse { listOf() }
                                .map { animal -> animal.toAnimal() }
                        )
                }
            } catch (_: RepeatedActionError) { throw BadRequestError(listOf("Duplicate animal values")) }
            userUpdate.drivingStyle?.let { drivingStyleOptional -> actingUser.drivingStyle = drivingStyleOptional.getOrNull()?.toDrivingStyle() }
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Create an image for a specific user.")
    @CommonApiResponses @CreatedApiResponse @NotFoundApiResponse
    @PostMapping("{userId}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createImageForUser(@PathVariable @UUID userId: String, @RequestPart image: MultipartFile, userToken: UserToken): ResponseEntity<IdDP> {
        if (userToken.id != userId) throw ForbiddenError(localizationService.getMessage("user.profilePicture.uploadOthers", userToken.id))
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        if(user.profilePicture != null) throw BadRequestError(listOf(localizationService.getMessage("user.profilePicture.alreadyExists", user.id)))

        val imageEntity:Image
        try {
            imageEntity = imagesRepository.save(image.bytes, image.originalFilename?.substringAfterLast("."))
        } catch (error: FileCorruptedError) {
            throw BadRequestError(listOf(localizationService.getMessage("requestPart.image.invalid")))
        }
        catch (error: WrongFileFormatError) {
            throw BadRequestError(listOf(localizationService.getMessage("requestPart.image.wrongFormat")))
        }
        user.profilePicture = imageEntity
        usersRepository.save(user)

        return ResponseEntity.status(201).body(IdDP(imageEntity.id.toString()))
    }

    @Operation(description = "Delete the image of a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{userId}/image")
    fun deleteImageOfUser(@PathVariable @UUID userId: String, userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != userId) throw ForbiddenError(localizationService.getMessage("user.profilePicture.deleteOthers", userToken.id))
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        if(user.profilePicture == null) throw NotFoundError(localizationService.getMessage("user.profilePicture.notExists", userId))

        try {
            val profilePictureId:UUIDType = user.profilePicture!!.id
            user.profilePicture = null
            usersRepository.save(user)
            imagesRepository.deleteById(profilePictureId)
        }
        catch (error: FileMissingError) {
            throw NotFoundError(localizationService.getMessage("user.profilePicture.notFound", userId))
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
    @GetMapping("{userId}/image")
    fun getImageOfUser(@PathVariable @UUID userId: String, @RequestParam quality: QualityDP, userToken: UserToken): ResponseEntity<ByteArray> {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val user: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userId))
        if (user.profilePicture == null) throw NotFoundError(localizationService.getMessage("user.profilePicture.notExists", userId))

        val image:ByteArray = imagesRepository.getById(user.profilePicture!!.id, quality.toQuality()).getOrNull() ?: throw NotFoundError(localizationService.getMessage("user.profilePicture.notFound", userId))
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image)
    }

    @Operation(description = "Create a car for a specific user.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("{userId}/cars")
    fun createCarForUser(@PathVariable @UUID userId: String, @RequestBody @Valid carCreation: CarCreationDP, userToken: UserToken): ResponseEntity<Void> {
        if(userToken.id != userId) throw ForbiddenError("The user can only create a car for themselves.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError("User with id $userId does not exist in resource server.")

        actingUser.addCar(carCreation.toCar())
        usersRepository.save(actingUser)

        return ResponseEntity.status(HttpStatus.CREATED).build<Void>()
    }

    @Operation(description = "Create an image for a specific car of a specific user.")
    @CommonApiResponses @CreatedApiResponse @NotFoundApiResponse
    @PostMapping("{userId}/cars/{carIndex}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createImageForCarOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) carIndex: Int, @RequestPart image: MultipartFile, userToken: UserToken):ResponseEntity<IdDP> {
        if (userToken.id != userId) throw ForbiddenError(localizationService.getMessage("user.car.index.image.uploadOthers", userToken.id))
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val car: Car
        try { car = user.getCarByIndex(carIndex.toUInt()) } catch (error: NotAvailableError) { throw NotFoundError(localizationService.getMessage("user.car.index.notExists", carIndex, userId)) }
        if(car.image != null) throw BadRequestError(listOf(localizationService.getMessage("user.car.index.image.alreadyExists", carIndex, userId)))

        val imageEntity:Image
        try {
            imageEntity = imagesRepository.save(image.bytes, image.originalFilename?.substringAfterLast("."))
        } catch (error: FileCorruptedError) {
            throw BadRequestError(listOf(localizationService.getMessage("requestPart.image.invalid")))
        }
        catch (error: WrongFileFormatError) {
            throw BadRequestError(listOf(localizationService.getMessage("requestPart.image.wrongFormat")))
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
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val user: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userId))

        val car: Car = try { user.getCarByIndex(carIndex.toUInt()) } catch (_: NotAvailableError) { throw NotFoundError(localizationService.getMessage("user.car.index.notExists", carIndex, userId)) }
        if (car.image == null) throw NotFoundError(localizationService.getMessage("user.car.index.image.notExists", carIndex, userId))

        val image:ByteArray = imagesRepository.getById(car.image!!.id, quality.toQuality()).getOrNull() ?: throw NotFoundError(localizationService.getMessage("user.car.index.image.notFound", carIndex, userId))
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image)
    }

    @Operation(description = "Delete the car of a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{userId}/cars/{carIndex}")
    fun deleteCarOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) carIndex: Int, userToken: UserToken): ResponseEntity<Void> {
        if(userToken.id != userId) throw ForbiddenError("The user can only delete their own car.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError("User with id $userId does not exist in resource server.")
        val carToDelete: Car = try { actingUser.getCarByIndex(carIndex.toUInt()) } catch (_: NotAvailableError) { throw NotFoundError("Car with index $carIndex not found.") }

        try { actingUser.removeCarAtIndex(carIndex.toUInt()) } catch (_: NotAvailableError) { throw NotFoundError("Car with index $carIndex not found.") }
        imagesRepository.deleteById(carToDelete.image?.id ?: throw NotFoundError("Image of car with index $carIndex not found."))
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Create a favorite address for a specific user.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("{userId}/favorite-addresses")
    fun addFavoriteAddressForUser(@PathVariable @UUID userId: String, @RequestBody @Valid address: AddressDP, userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != userId) throw ForbiddenError("Users can only add favorite addresses to their own account.")
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError("User with id $userId does not exist in resource server.")

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

        try {
            actingUser.addFavoriteUser(userToFavorite)
        } catch (_: RepeatedActionError) {
            throw BadRequestError(listOf("User with id ${actingUser.id} is already a favorite user."))
        } catch (_: ConflictingActionError) {
            throw BadRequestError(listOf("User with id ${actingUser.id} cannot be a favorite user of itself."))
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Delete favorite user of a specific user")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("/{userId}/favorite-users/{favoriteUserId}")
    fun deleteFavoriteUserOfUser(@PathVariable @UUID userId: String, @PathVariable @UUID favoriteUserId: String, userToken: UserToken): ResponseEntity<Void> {
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

        try {
            actingUser.addBlockedUser(userToBlock)
        } catch (_: RepeatedActionError) {
            throw BadRequestError(listOf("User with id ${actingUser.id} is already a blocked user."))
        } catch (_: ConflictingActionError) {
            throw BadRequestError(listOf("User with id ${actingUser.id} cannot be a blocked user of itself."))
        }
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
    @GetMapping("{userId}/drive-offers")
    fun getDriveOffersOfUser(@PathVariable @UUID userId: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(200) perPage: Int, @RequestParam sortingDirection: SortingDirectionDP = SortingDirectionDP.Ascending, role: DriverOfferRoleDP? = null, userToken: UserToken): ResponseEntity<PageDP<PartialDriveOfferDP>> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError("User with id ${userToken.id} does not exist in resource server.")
        if(actingUser.id != UUIDType.fromString(userId)) throw ForbiddenError("The user can only get their own drive offers.")

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
            .let { resultingDriveOffersOfUser ->
                when(sortingDirection) {
                    SortingDirectionDP.Ascending -> resultingDriveOffersOfUser.sortedBy { it.scheduleTime?.time }
                    SortingDirectionDP.Descending -> resultingDriveOffersOfUser.sortedByDescending { it.scheduleTime?.time }
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
    @GetMapping("{userId}/drive-requests")
    fun getDriveRequestsOfUser(@PathVariable @UUID userId: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(200) perPage: Int, @RequestParam sortingDirection: SortingDirectionDP = SortingDirectionDP.Ascending, userToken: UserToken): ResponseEntity<PageDP<PartialDriveRequestDP>> {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        if(user.id != UUIDType.fromString(userId)) throw ForbiddenError(localizationService.getMessage("user.resource.driveRequests.getOthers", userToken.id))

        val driveRequests: List<DriveRequest> = driveRequestsRepository.findAllDriveRequests(
            Sort.by(
                when(sortingDirection) {
                    SortingDirectionDP.Ascending -> Sort.Direction.ASC
                    SortingDirectionDP.Descending -> Sort.Direction.DESC
                },
                "scheduleTime.time"
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
    @GetMapping("{userId}/drives")
    fun getDrivesOfUser(@PathVariable @UUID userId: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(200) perPage: Int, @RequestParam sortingDirection: SortingDirectionDP = SortingDirectionDP.Ascending, userToken: UserToken): ResponseEntity<PageDP<PartialDriveDP>> {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        if(user.id != UUIDType.fromString(userId)) throw ForbiddenError(localizationService.getMessage("user.resource.drives.getOthers", userToken.id))

        val sort: Sort = if (sortingDirection == SortingDirectionDP.Ascending) Sort.by("plannedDeparture").ascending() else Sort.by("plannedDeparture").descending()
        val page: Page<Drive> = drivesRepository.findAllDrives(PageRequest.of(pageNumber - 1, perPage, sort), user.id)

        return ResponseEntity.ok(
            PartialDrivePageDP(page.totalPages, page.content.map { PartialDriveDP.fromDrive(it) })
        )
    }

    @Operation(description = "Get all carpools of a specific user.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("{userId}/carpools")
    fun getCarpoolsOfUser(@PathVariable @UUID userId: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(200) perPage: Int, @RequestParam sortingDirection: SortingDirectionDP = SortingDirectionDP.Ascending, userToken: UserToken): ResponseEntity<PageDP<PartialCarpoolDP>> {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        if(user.id != UUIDType.fromString(userId)) throw ForbiddenError(localizationService.getMessage("user.resource.carpools.getOthers", userToken.id))

        val sort: Sort = if (sortingDirection == SortingDirectionDP.Ascending) Sort.by("name").ascending() else Sort.by("name").descending()
        val page: Page<Carpool> = carpoolsRepository.findAllCarpools(PageRequest.of(pageNumber - 1, perPage, sort), user.id)

        return ResponseEntity.ok(
            PartialCarpoolPageDP(page.totalPages, page.content.map { PartialCarpoolDP.fromCarpool(it) })
        )
    }

    @Operation(description = "Create a rating for a specific user.")
    @CommonApiResponses @CreatedApiResponse @NotFoundApiResponse
    @PostMapping("{userId}/ratings")
    fun createRatingForUser(@PathVariable @UUID userId: String, @RequestBody @Valid ratingCreation: RatingCreationDP, userToken: UserToken): ResponseEntity<Void>
    {
        if(userToken.id == userId) throw ForbiddenError(localizationService.getMessage("user.rating.createOwn", userId))
        val author: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        val ratedUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("user.notExists", userId))

        when(ratingCreation.role) {
            RoleDP.Driver -> {
                if(ratedUser.drivesAsDriver
                    .filter { (it.actualArrival?.isAfter(ZonedDateTime.now().minusDays(3)) == true) && (it.actualArrival?.isBefore(ZonedDateTime.now()) == true) }
                    .none { it.passengers.contains(author) })
                    throw BadRequestError(listOf(localizationService.getMessage("user.rating.notDriverOf", ratedUser.id, author.id)))

                if(ratedUser.ratings.any { it.author == author && it.role == Role.Driver && it.created.isAfter(ZonedDateTime.now().minusDays(3)) })
                    throw BadRequestError(listOf(localizationService.getMessage("user.rating.alreadyExistsAsDriver", author.id, ratedUser.id)))
            }
            RoleDP.Passenger -> {
                if(ratedUser.drivesAsPassenger
                    .filter { (it.actualArrival?.isAfter(ZonedDateTime.now().minusDays(3)) == true) && (it.actualArrival?.isBefore(ZonedDateTime.now()) == true) }
                    .none { it.driver == author })
                    throw BadRequestError(listOf(localizationService.getMessage("user.rating.notPassengerOf", ratedUser.id, author.id)))

                if(ratedUser.ratings.any { it.author == author && it.role == Role.Passenger && it.created.isAfter(ZonedDateTime.now().minusDays(3)) })
                    throw BadRequestError(listOf(localizationService.getMessage("user.rating.alreadyExistsAsPassenger", author.id, ratedUser.id)))
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