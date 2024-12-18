package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.InternalServerError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.NotFoundError
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.Carpool
import de.uniflitzer.backend.model.Name
import de.uniflitzer.backend.model.User
import de.uniflitzer.backend.repositories.CarpoolsRepository
import de.uniflitzer.backend.repositories.UsersRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import kotlin.jvm.optionals.getOrNull
import java.util.UUID as UUIDType

@RestController
@RequestMapping("v1/carpools")
@Validated
@SecurityRequirement(name = "Token Authentication")
@Tag(name = "Carpools")
private class CarpoolsCommunicator(
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val carpoolsRepository: CarpoolsRepository,
    @field:Autowired private val keycloak: Keycloak,
    @field:Autowired private val environment: Environment
    )
{
    @Operation(description = "Create a new carpool.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("")
    fun createCarpool(@RequestBody @Valid carpoolCreation: CarpoolCreationDP, userToken: UserToken): ResponseEntity<IdDP>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        Carpool(Name(carpoolCreation.name), mutableListOf(user)).let {
            carpoolsRepository.save(it)
            return ResponseEntity.status(201).body(IdDP(it.id.toString()))
        }
    }

    @Operation(description = "Get details of a specific carpool.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{id}")
    fun getCarpool(@PathVariable @UUID id:String, userToken: UserToken): ResponseEntity<DetailedCarpoolDP>
    {
        val user: User = usersRepository.findById(java.util.UUID.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val carpool: Carpool = carpoolsRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("Carpool with id $id not found."))
        if(carpool.users.none { it.id == user.id }) throw ForbiddenError(ErrorDP("User with id ${user.id} is not part of carpool with id $id."))

        return ResponseEntity.ok(DetailedCarpoolDP.fromCarpool(carpool, user.favoriteUsers))
    }

    @Operation(description = "Delete a specific carpool.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{id}")
    fun deleteCarpool(@PathVariable @UUID id: String, userToken: UserToken): ResponseEntity<Void>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val carpool: Carpool = carpoolsRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("Carpool with id $id not found."))
        if(carpool.users.none { it.id == user.id }) throw ForbiddenError(ErrorDP("User with id ${user.id} is not part of carpool with id $id."))

        carpool.drives.filter { it.actualDeparture == null && it.arrival == null }.forEach { it.isCancelled = true }
        carpool.sentInvites.forEach { carpool.removeCarpoolInvite(it) }

        carpoolsRepository.delete(carpool)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Send an invite to a user to join a specific carpool.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{carpoolId}/sent-invites/{username}")
    fun sendInviteForCarpool(@PathVariable @UUID carpoolId: String, @PathVariable username: String, userToken: UserToken): ResponseEntity<Void>
    {
        val actingUser: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val carpool: Carpool = carpoolsRepository.findById(UUIDType.fromString(carpoolId)).getOrNull() ?: throw NotFoundError(ErrorDP("Carpool with id $carpoolId not found."))
        if(carpool.users.none { it.id == actingUser.id }) throw ForbiddenError(ErrorDP("User with id ${actingUser.id} is not part of carpool with id $carpoolId and cannot invite users to it."))

        val users: List<UserRepresentation> = keycloak.realm(
            environment.getProperty("keycloak.realm.name") ?: throw InternalServerError(ErrorDP("Keycloak realm name not defined."))
        ).users().search(username)
        if (users.isEmpty()) throw NotFoundError(ErrorDP("User with id ${userToken.id} does not exist in identity server."))
        val invitedUserId:String = users[0].id

        if(carpool.users.any { it.id == UUIDType.fromString(invitedUserId) }) throw ForbiddenError(ErrorDP("User with id $invitedUserId is already part of carpool with id $carpoolId."))
        if(carpool.sentInvites.any { it.id == UUIDType.fromString(invitedUserId) }) throw ForbiddenError(ErrorDP("User with id $invitedUserId is already invited to carpool with id $carpoolId."))

        val invitedUser: User = usersRepository.findById(UUIDType.fromString(invitedUserId)).getOrNull() ?: throw NotFoundError(ErrorDP("User with id $invitedUserId does not exist in resource server."))
        carpool.addCarpoolInvite(invitedUser)
        carpoolsRepository.save(carpool)

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Accept an invite to join a specific carpool.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{carpoolId}/sent-invites/{userId}/acceptances")
    fun acceptInviteForCarpool(@PathVariable @UUID carpoolId: String, @PathVariable @UUID userId: String, userToken: UserToken): ResponseEntity<Void>
    {
        if(userToken.id != userId) throw ForbiddenError(ErrorDP("The user can only accept his own invites."))
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val carpool: Carpool = carpoolsRepository.findById(UUIDType.fromString(carpoolId)).getOrNull() ?: throw NotFoundError(ErrorDP("Carpool with id $carpoolId not found."))
        if(carpool.users.any { it.id == user.id }) throw ForbiddenError(ErrorDP("User with id ${user.id} is already part of carpool with id $carpoolId."))
        if(carpool.sentInvites.none { it.id == user.id }) throw ForbiddenError(ErrorDP("User with id ${user.id} is not invited to carpool with id $carpoolId."))

        carpool.acceptCarpoolInvite(user)
        carpoolsRepository.save(carpool)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Reject an invite to join a specific carpool.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{carpoolId}/sent-invites/{userId}/rejections")
    fun rejectInviteForCarpool(@PathVariable @UUID carpoolId: String, @PathVariable @UUID userId: String, userToken: UserToken): ResponseEntity<Void>
    {
        if(userToken.id != userId) throw ForbiddenError(ErrorDP("The user can only reject his own invites."))
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val carpool: Carpool = carpoolsRepository.findById(UUIDType.fromString(carpoolId)).getOrNull() ?: throw NotFoundError(ErrorDP("Carpool with id $carpoolId not found."))
        if(carpool.users.any { it.id == user.id }) throw ForbiddenError(ErrorDP("User with id ${user.id} is already part of carpool with id $carpoolId."))
        if(carpool.sentInvites.none { it.id == user.id }) throw ForbiddenError(ErrorDP("User with id ${user.id} is not invited to carpool with id $carpoolId."))

        carpool.removeCarpoolInvite(user)
        carpoolsRepository.save(carpool)
        return ResponseEntity.noContent().build()
    }
}