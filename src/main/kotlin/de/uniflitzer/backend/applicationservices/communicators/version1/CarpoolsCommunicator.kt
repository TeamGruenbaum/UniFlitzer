package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.CarpoolCreationDP
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.IdDP
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.CommonApiResponses
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.CreatedApiResponse
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.NoContentApiResponse
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.NotFoundApiResponse
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.jvm.optionals.getOrNull
import java.util.UUID as UUIDType

@RestController
@RequestMapping("v1/carpools")
@Validated
@SecurityRequirement(name = "Token Authentication")
@Tag(name = "Carpools")
private class CarpoolsCommunicator(
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val carpoolsRepository: CarpoolsRepository
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
    @PostMapping("{carpoolId}/sent-invites/{userId}")
    fun sendInviteForCarpool(@PathVariable @UUID carpoolId: String, @PathVariable @UUID userId: String, userToken: UserToken): ResponseEntity<Void>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val carpool: Carpool = carpoolsRepository.findById(UUIDType.fromString(carpoolId)).getOrNull() ?: throw NotFoundError(ErrorDP("Carpool with id $carpoolId not found."))
        if(carpool.users.none { it.id == user.id }) throw ForbiddenError(ErrorDP("User with id ${user.id} is not part of carpool with id $carpoolId and cannot invite users to it."))
        if(carpool.users.any { it.id == UUIDType.fromString(userId) }) throw ForbiddenError(ErrorDP("User with id $userId is already part of carpool with id $carpoolId."))
        if(carpool.sentInvites.any { it.id == UUIDType.fromString(userId) }) throw ForbiddenError(ErrorDP("User with id $userId is already invited to carpool with id $carpoolId."))

        val invitedUser: User = usersRepository.findById(UUIDType.fromString(userId)).getOrNull() ?: throw NotFoundError(ErrorDP("User with id $userId does not exist in resource server."))

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