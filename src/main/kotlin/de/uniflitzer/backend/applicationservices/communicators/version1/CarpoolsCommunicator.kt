package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.CarpoolCreationDP
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.DetailedCarpoolDP
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.IdDP
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.NotFoundError
import de.uniflitzer.backend.applicationservices.communicators.version1.localization.LocalizationService
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.Carpool
import de.uniflitzer.backend.model.Name
import de.uniflitzer.backend.model.User
import de.uniflitzer.backend.model.errors.MissingActionError
import de.uniflitzer.backend.model.errors.RepeatedActionError
import de.uniflitzer.backend.repositories.CarpoolsRepository
import de.uniflitzer.backend.repositories.ImagesRepository
import de.uniflitzer.backend.repositories.UsersRepository
import de.uniflitzer.backend.repositories.errors.FileMissingError
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import kotlin.jvm.optionals.getOrNull
import java.util.UUID as UUIDType

@RestController @RequestMapping("v1/carpools")
@Tag(name = "Carpools") @SecurityRequirement(name = "Token Authentication")
@Validated
@Transactional(rollbackFor = [Throwable::class])
private class CarpoolsCommunicator(
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val carpoolsRepository: CarpoolsRepository,
    @field:Autowired private val imagesRepository: ImagesRepository,
    @field:Autowired private val authenticationAdministrator: Keycloak,
    @field:Autowired private val environment: Environment,
    @field:Autowired private val localizationService: LocalizationService
    )
{
    @Operation(description = "Create a new carpool.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("")
    fun createCarpool(@RequestBody @Valid carpoolCreation: CarpoolCreationDP, userToken: UserToken): ResponseEntity<IdDP>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        Carpool(Name(carpoolCreation.name), mutableListOf(user)).let {
            carpoolsRepository.save(it)
            return ResponseEntity.status(201).body(IdDP(it.id.toString()))
        }
    }

    @Operation(description = "Get details of a specific carpool.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{carpoolId}")
    fun getCarpool(@PathVariable @UUID carpoolId:String, userToken: UserToken): ResponseEntity<DetailedCarpoolDP>
    {
        val user: User = usersRepository.findById(java.util.UUID.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val carpool: Carpool = carpoolsRepository.findById(UUIDType.fromString(carpoolId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("carpool.notFound", carpoolId))
        if(carpool.users.none { it.id == user.id }) throw ForbiddenError(localizationService.getMessage("carpool.user.noMemberOf", user.id, carpoolId))

        return ResponseEntity.ok(DetailedCarpoolDP.fromCarpool(carpool, user.favoriteUsers))
    }

    @Operation(description = "Delete a specific carpool.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{carpoolId}")
    fun deleteCarpool(@PathVariable @UUID carpoolId: String, userToken: UserToken): ResponseEntity<Void>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val carpool: Carpool = carpoolsRepository.findById(UUIDType.fromString(carpoolId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("carpool.notFound", carpoolId))
        if(carpool.users.none { it.id == user.id }) throw ForbiddenError(localizationService.getMessage("carpool.user.noMemberOf", user.id, carpoolId))

        var driveOfferCarImageId: UUIDType
        carpool.driveOffers.forEach {
            if(it.car.image != null) {
                try {
                    driveOfferCarImageId = it.car.image!!.id
                    it.car.image = null
                    imagesRepository.deleteById(driveOfferCarImageId)
                } catch (error: FileMissingError) {
                    throw NotFoundError(localizationService.getMessage("driveOffer.car.image.notFound", it.id))
                }
            }
        }

        carpool.drives.filter { it.actualDeparture == null && it.actualArrival == null }.forEach { it.isCancelled = true }
        carpool.sentInvites.forEach {
            try {
                carpool.rejectInvite(it)
            } catch(error: RepeatedActionError) {
                throw BadRequestError(listOf(localizationService.getMessage("carpool.user.alreadyMemberOf", user.id, carpoolId)))
            } catch(error: MissingActionError) {
                throw NotFoundError(localizationService.getMessage("carpool.user.invite.notSent", user.id, carpoolId))
            }
        }

        carpoolsRepository.delete(carpool)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Send an invite to a user to join a specific carpool.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{carpoolId}/sent-invites/{username}")
    fun sendInviteForCarpool(@PathVariable @UUID carpoolId: String, @PathVariable username: String, userToken: UserToken): ResponseEntity<Void>
    {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val carpool: Carpool = carpoolsRepository.findById(UUIDType.fromString(carpoolId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("carpool.notFound", carpoolId))
        if(carpool.users.none { it.id == actingUser.id }) throw ForbiddenError(localizationService.getMessage("carpool.user.noMemberOf", actingUser.id, carpoolId))

        val users: List<UserRepresentation> = authenticationAdministrator.realm(
            environment.getProperty("keycloak.realm.name") ?: throw IllegalStateException("Keycloak realm name not defined.")
        ).users().search(username)
        if (users.isEmpty()) throw NotFoundError(localizationService.getMessage("identityServer.user.username.notExists", username))

        val invitedUser: User = usersRepository.findById(UUIDType.fromString(users[0].id)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("user.notExists", users[0].id))
        try {
            carpool.sendInvite(invitedUser)
        } catch (error: RepeatedActionError) {
            throw ForbiddenError(localizationService.getMessage("carpool.user.alreadyMemberOfOrAlreadyInvited", invitedUser.id, carpoolId))
        }
        carpoolsRepository.save(carpool)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Accept an invite to join a specific carpool.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{carpoolId}/sent-invites/{userId}/acceptances")
    fun acceptInviteForCarpool(@PathVariable @UUID carpoolId: String, @PathVariable @UUID userId: String, userToken: UserToken): ResponseEntity<Void>
    {
        if(userToken.id != userId) throw ForbiddenError(localizationService.getMessage("carpool.user.invite.acceptOthers", userToken.id, carpoolId))
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val carpool: Carpool = carpoolsRepository.findById(UUIDType.fromString(carpoolId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("carpool.notFound", carpoolId))
        try {
            carpool.acceptInvite(user)
        } catch(error: RepeatedActionError) {
            throw BadRequestError(listOf(localizationService.getMessage("carpool.user.alreadyMemberOf", user.id, carpoolId)))
        } catch(error: MissingActionError) {
            throw NotFoundError(localizationService.getMessage("carpool.user.invite.notSent", user.id, carpoolId))
        }
        carpoolsRepository.save(carpool)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Reject an invite to join a specific carpool.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{carpoolId}/sent-invites/{userId}/rejections")
    fun rejectInviteForCarpool(@PathVariable @UUID carpoolId: String, @PathVariable @UUID userId: String, userToken: UserToken): ResponseEntity<Void>
    {
        if(userToken.id != userId) throw ForbiddenError(localizationService.getMessage("carpool.user.invite.rejectOthers", userToken.id, carpoolId))
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(localizationService.getMessage("user.notExists", userToken.id))

        val carpool: Carpool = carpoolsRepository.findById(UUIDType.fromString(carpoolId)).getOrNull() ?: throw NotFoundError(localizationService.getMessage("carpool.notFound", carpoolId))
        try {
            carpool.rejectInvite(user)
        } catch(error: RepeatedActionError) {
            throw BadRequestError(listOf(localizationService.getMessage("carpool.user.alreadyMemberOf", user.id, carpoolId)))
        } catch(error: MissingActionError) {
            throw NotFoundError(localizationService.getMessage("carpool.user.invite.notSent", user.id, carpoolId))
        }
        carpoolsRepository.save(carpool)
        return ResponseEntity.noContent().build()
    }
}