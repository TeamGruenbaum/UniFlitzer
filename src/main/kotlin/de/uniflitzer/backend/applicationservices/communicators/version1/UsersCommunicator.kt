package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.BadRequestError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
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
class UsersCommunicator(
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
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        val searchedUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("user.notFound", userId))
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
                if(isActingUserLookingAtHisOwnProfile) searchedUser.favoritePositions.map { PositionDP.fromPosition(it) } else null,
                searchedUser.ratings.map { RatingDP.fromRating(it) },
                if(isActingUserLookingAtHisOwnProfile) searchedUser.receivedInvites.map { PartialCarpoolDP.fromCarpool(it) } else null
            )
        )
    }

    @Operation(description = "Create a new user.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("")
    fun createUser(@RequestBody @Valid userCreation: UserCreationDP, userToken: UserToken): ResponseEntity<IdDP> {
        if (usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(localizationService.getMessage("user.exists", userToken.id))

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

    @Operation(description = "Update a specific user.")
    @CommonApiResponses @NoContentApiResponse
    @PatchMapping("{userId}")
    fun updateUser(@PathVariable @UUID userId: String, @RequestBody @Valid userUpdate: UserUpdateDP, userToken: UserToken): ResponseEntity<Void> {
        if(userToken.id != userId) throw ForbiddenError(localizationService.getMessage("user.account.updateOthers", userToken.id))
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userId))

        actingUser.apply{
            userUpdate.firstName?.let { actingUser.firstName = FirstName(it) }
            userUpdate.lastName?.let { actingUser.lastName = LastName(it) }
            try { userUpdate.birthday?.let { actingUser.birthday = ZonedDateTime.parse(it) } } catch (_: ImpossibleActionError) { throw BadRequestError(listOf(localizationService.getMessage("user.birthday.inFuture"))) }
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
            } catch (_: RepeatedActionError) { throw BadRequestError(listOf(localizationService.getMessage("user.animals.duplicateValues"))) }
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
        if(userToken.id != userId) throw ForbiddenError(localizationService.getMessage("user.car.createOthers", userToken.id))
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userId))

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
        if(userToken.id != userId) throw ForbiddenError(localizationService.getMessage("user.car.deleteOthers", userToken.id))
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userId))
        val carToDelete: Car = try { actingUser.getCarByIndex(carIndex.toUInt()) } catch (_: NotAvailableError) { throw NotFoundError(localizationService.getMessage("user.car.index.notExists", carIndex, userId)) }

        try { actingUser.removeCarAtIndex(carIndex.toUInt()) } catch (_: NotAvailableError) { throw NotFoundError(localizationService.getMessage("user.car.index.notExists", carIndex, userId)) }
        if (carToDelete.image != null)
        {
            try {
                val imageId:UUIDType = carToDelete.image!!.id
                carToDelete.image = null
                usersRepository.save(actingUser)
                imagesRepository.deleteById(imageId)
            }
            catch (error: FileMissingError) {
                throw NotFoundError(localizationService.getMessage("user.car.index.image.notFound", carIndex, userId))
            }
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Create a favorite position for a specific user.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("{userId}/favorite-positions")
    fun addFavoritePositionForUser(@PathVariable @UUID userId: String, @RequestBody @Valid coordinate: CoordinateDP, userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != userId) throw ForbiddenError(localizationService.getMessage("user.favoritePosition.addOthers", userToken.id))
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userId))

        try {
            actingUser.addFavoritePosition(geographyService.createPosition(coordinate.toCoordinate()))
        } catch (_: RepeatedActionError) {
            throw BadRequestError(listOf(localizationService.getMessage("user.favoritePosition.alreadyExists")))
        }
        usersRepository.save(actingUser)

        return ResponseEntity.status(HttpStatus.CREATED).build<Void>()
    }

    @Operation(description = "Delete a favorite position of a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{userId}/favorite-positions/{positionIndex}")
    fun deleteFavoritePositionOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) positionIndex: Int, userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != userId) throw ForbiddenError(localizationService.getMessage("user.favoritePosition.index.deleteOthers", userToken.id))
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userId))

        try {
            actingUser.removeFavoritePositionByIndex(positionIndex.toUInt())
        } catch (_: NotAvailableError) {
            throw NotFoundError(localizationService.getMessage("user.favoritePosition.index.notFound", positionIndex, userId))
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Add favorite user to a specific user")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse @ConflictApiResponse
    @PostMapping("{userId}/favorite-users")
    fun addFavoriteUserToUser(@PathVariable @UUID userId: String, @RequestBody @Valid favoriteUserAddition: UserAdditionDP, userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != userId) throw ForbiddenError(localizationService.getMessage("user.favoriteUser.addOthers", userToken.id))
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userId))
        val userToFavorite = usersRepository.findById(UUIDType.fromString(favoriteUserAddition.id)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("user.notFound", favoriteUserAddition.id))

        try {
            actingUser.addFavoriteUser(userToFavorite)
        } catch (_: RepeatedActionError) {
            throw BadRequestError(listOf(localizationService.getMessage("user.favoriteUser.alreadyExists", userToFavorite.id, userId)))
        } catch (_: ConflictingActionError) {
            throw ConflictError(localizationService.getMessage("user.favoriteUser.conflict", userToFavorite.id, userId))
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Delete favorite user of a specific user")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{userId}/favorite-users/{favoriteUserId}")
    fun deleteFavoriteUserOfUser(@PathVariable @UUID userId: String, @PathVariable @UUID favoriteUserId: String, userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != userId) throw ForbiddenError(localizationService.getMessage("user.favoriteUser.deleteOthers", userToken.id))
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userId))
        val favoriteUserToDelete = usersRepository.findById(UUIDType.fromString(favoriteUserId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("user.favoriteUser.notFound", favoriteUserId, userId))

        try {
            actingUser.removeFavoriteUser(favoriteUserToDelete)
        } catch (_: NotAvailableError) {
            throw NotFoundError(localizationService.getMessage("user.notFound", favoriteUserId))
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Add blocked user to a specific user")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse @ConflictApiResponse
    @PostMapping("{userId}/blocked-users")
    fun addBlockedUserToUser(@PathVariable @UUID userId: String, @RequestBody @Valid blockedUserAddition: UserAdditionDP, userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != userId) throw ForbiddenError(localizationService.getMessage("user.blockedUser.addOthers", userToken.id))
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userId))
        val userToBlock = usersRepository.findById(UUIDType.fromString(blockedUserAddition.id)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("user.notExists", blockedUserAddition.id))

        try {
            actingUser.addBlockedUser(userToBlock)
        } catch (_: RepeatedActionError) {
            throw BadRequestError(listOf(localizationService.getMessage("user.blockedUser.alreadyExists", userToBlock.id, actingUser.id)))
        } catch (_: ConflictingActionError) {
            throw ConflictError(localizationService.getMessage("user.blockedUser.conflict", userToBlock.id, actingUser.id))
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Delete blocked user of a specific user")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{userId}/blocked-users/{blockedUserId}")
    fun deleteBlockedUserOfUser(@PathVariable @UUID userId: String, @PathVariable @UUID blockedUserId: String,  userToken: UserToken): ResponseEntity<Void> {
        if (userToken.id != userId) throw ForbiddenError(localizationService.getMessage("user.blockedUser.deleteOthers", userToken.id))
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userId))
        val blockedUserToDelete = usersRepository.findById(UUIDType.fromString(blockedUserId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("user.notFound", blockedUserId))

        try {
            actingUser.removeBlockedUser(blockedUserToDelete)
        } catch (_: NotAvailableError) {
            throw NotFoundError(localizationService.getMessage("user.blockedUser.notFound", blockedUserId, actingUser.id))
        }
        usersRepository.save(actingUser)

        return ResponseEntity.noContent().build<Void>()
    }

    @Operation(description = "Get all drive offers of a specific user.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("{userId}/drive-offers")
    fun getDriveOffersOfUser(@PathVariable @UUID userId: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(200) perPage: Int, @RequestParam sortingDirection: SortingDirectionDP = SortingDirectionDP.Ascending, @RequestParam role: DriverOfferRoleDP? = null, userToken: UserToken): ResponseEntity<PageDP<PartialDriveOfferDP>> {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))
        if(actingUser.id != UUIDType.fromString(userId)) throw ForbiddenError(localizationService.getMessage("user.resource.driveOffers.getOthers", userId))

        val resultingDriveOffersOfUser: List<DriveOffer> =
            if(role == null) {
                actingUser.driveOffersAsRequestingUser.plus(actingUser.driveOffersAsPassenger).plus(actingUser.driveOffersAsDriver)
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