package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.authentication.UserToken
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.InternalServerError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.NotFoundError
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.applicationservices.geography.GeographyService
import de.uniflitzer.backend.model.*
import de.uniflitzer.backend.model.errors.EntityNotFoundError
import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.repositories.DriveOffersRepository
import de.uniflitzer.backend.repositories.DriveRequestsRepository
import de.uniflitzer.backend.repositories.ImagesRepository
import de.uniflitzer.backend.repositories.UsersRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.ZonedDateTime
import kotlin.jvm.optionals.getOrNull
import java.util.UUID as UUIDType

@RestController
@RequestMapping("v1/drive-requests")
@Validated
@SecurityRequirement(name = "Token Authentication")
@Tag(name = "Drive Requests")
private class DriveRequestsCommunicator(
    @field:Autowired private val usersRepository: UsersRepository,
    @field:Autowired private val driveRequestsRepository: DriveRequestsRepository,
    @field:Autowired private val driveOffersRepository: DriveOffersRepository,
    @field:Autowired private val geographyService: GeographyService,
    @field:Autowired private val imagesRepository: ImagesRepository
)
{
    @Operation(description = "Create a new drive request.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("")
    fun createDriveRequest(@RequestBody @Valid driveRequestCreation: DriveRequestCreationDP, userToken: UserToken): ResponseEntity<IdDP>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        when (driveRequestCreation)
        {
            is CarpoolDriveRequestCreationDP -> { TODO() }
            is PublicDriveRequestCreationDP -> {
                PublicDriveRequest(
                    user,
                    Route(
                        geographyService.createPosition(driveRequestCreation.route.start.toCoordinate()),
                        geographyService.createPosition(driveRequestCreation.route.destination.toCoordinate())
                    ),
                    driveRequestCreation.plannedDeparture?.let { ZonedDateTime.parse(it) }
                ).let {
                    driveRequestsRepository.saveAndFlush(it)
                    return ResponseEntity.status(201).body(IdDP(it.id.toString()))
                }
            }
        }
    }

    @Operation(description = "Get all drive requests.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("")
    fun getDriveRequests(@RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int,
                         @RequestParam sortingDirection: SortingDirection?, userToken: UserToken): ResponseEntity<PageDP<PartialDriveRequestDP>>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val sort:Sort = if (sortingDirection == null || sortingDirection == SortingDirection.Ascending) Sort.by("id").ascending() else Sort.by("id").descending()
        val page:Page<DriveRequest> = driveRequestsRepository.findDriveRequests(PageRequest.of(pageNumber - 1, perPage, sort))

        // TODO("Outsource filtering to the repository.")
        return ResponseEntity.ok(
            PageDP(
                page.totalPages,
                page.content.filter {
                    when (it) {
                        is CarpoolDriveRequest -> user.carpools.contains(it.carpool)
                        is PublicDriveRequest -> true
                        else -> false
                    }
                }.map {
                    when (it) {
                        is CarpoolDriveRequest -> PartialCarpoolDriveRequestDP.fromCarpoolDriveRequest(it)
                        is PublicDriveRequest -> PartialPublicDriveRequestDP.fromPublicDriveRequest(it)
                        else -> throw Exception()
                    }
                }
            )
        )
    }

    @Operation(description = "Get details of a specific drive request.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{id}")
    fun getDriveRequest(@PathVariable @UUID id:String, userToken: UserToken): ResponseEntity<DetailedDriveRequestDP>
    {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("DriveRequest with id $id not found."))

        val detailedDriveRequestDP: DetailedDriveRequestDP = when(driveRequest)
        {
            is CarpoolDriveRequest -> DetailedCarpoolDriveRequestDP(
                driveRequest.id.toString(),
                PartialUserDP.fromUser(driveRequest.requestingUser),
                RouteDP.fromRoute(driveRequest.route),
                driveRequest.plannedDeparture.toString(),
                PartialCarpoolDP.fromCarpool(driveRequest.carpool)
            )
            is PublicDriveRequest -> DetailedPublicDriveRequestDP(
                driveRequest.id.toString(),
                PartialUserDP.fromUser(driveRequest.requestingUser),
                RouteDP.fromRoute(driveRequest.route),
                driveRequest.plannedDeparture.toString(),
                driveRequest.driveOffers.map { PartialPublicDriveOfferDP.fromPublicDriveOffer(it) }
            )
            else -> { throw InternalServerError(ErrorDP("DriveRequest is neither a CarpoolDriveRequest nor a PublicDriveRequest.")) }
        }

        return ResponseEntity.ok(detailedDriveRequestDP)
    }

    @Operation(description = "Create a new drive offer for a specific drive request. The drive request is either deleted if it's a CarpoolDriveRequest or its drive offers list is updated if it's a PublicDriveRequest.")
    @CommonApiResponses @CreatedApiResponse @NotFoundApiResponse
    @PostMapping("{id}/drive-offers")
    fun createDriveOfferForDriveRequest(@PathVariable @UUID id:String, @RequestBody @Valid driveOfferCreation: DriveOfferCreationDP, userToken: UserToken): ResponseEntity<IdDP>
    {
        val user: User = usersRepository.findById(UUIDType.fromString(userToken.id)).getOrNull() ?: throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(id)).getOrNull() ?: throw NotFoundError(ErrorDP("DriveRequest with id $id not found."))
        when(driveRequest)
        {
            is CarpoolDriveRequest -> {
                when (driveOfferCreation)
                {
                    is CarpoolDriveOfferCreationDP ->
                    {
                        TODO("Create and persist a new CarpoolDriveOffer.")
                        driveRequestsRepository.delete(driveRequest)
                    }
                    is PublicDriveOfferCreationDP -> { throw ForbiddenError(ErrorDP("PublicDriveOffer creation is not allowed for CarpoolDriveRequest.")) }
                }
            }
            is PublicDriveRequest -> {
                when (driveOfferCreation)
                {
                    is CarpoolDriveOfferCreationDP -> { throw ForbiddenError(ErrorDP("CarpoolDriveOffer creation is not allowed for PublicDriveRequest.")) }
                    is PublicDriveOfferCreationDP -> {
                        val car:Car = try{ user.getCarByIndex(driveOfferCreation.carIndex) } catch(error:NotAvailableError){ throw NotFoundError(ErrorDP(error.message!!)) }
                        val driveOffer:PublicDriveOffer = PublicDriveOffer(
                            user,
                            car,
                            Seats(driveOfferCreation.freeSeats.toUInt()),
                            Route(
                                geographyService.createPosition(driveOfferCreation.route.start.toCoordinate()),
                                geographyService.createPosition(driveOfferCreation.route.destination.toCoordinate())
                            ),
                            driveOfferCreation.plannedDepartureTime?.let { ZonedDateTime.parse(it) }
                        )
                        car.image?.let { driveOffer.car.image = imagesRepository.copy(it) }

                        driveRequest.addDriveOffer(driveOffer)
                        driveOffersRepository.saveAndFlush(driveOffer)
                        driveRequestsRepository.saveAndFlush(driveRequest)

                        return ResponseEntity.status(201).body(IdDP(driveOffer.id.toString()))
                    }
                }
            }
            else -> { throw InternalServerError(ErrorDP("DriveRequest is neither a CarpoolDriveRequest nor a PublicDriveRequest.")) }
        }
    }

    @Operation(description = "This endpoint is only allowed to use on a PublicRequestRequest. Reject a specific drive offer for a specific drive request. Neither the drive request nor the drive offer is deleted so other users can still see them.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveRequestId}/drive-offers/{driveOfferId}/rejections")
    fun rejectDriveOffer(@PathVariable @UUID driveRequestId:String, @PathVariable @UUID driveOfferId:String, userToken: UserToken): ResponseEntity<Void>
    {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(driveRequestId)).getOrNull() ?: throw NotFoundError(ErrorDP("DriveRequest with id $driveRequestId not found."))
        if(driveRequest.requestingUser.id != UUIDType.fromString(userToken.id)) throw ForbiddenError(ErrorDP("UserToken id does not match the requesting user id of the drive request."))

        when(driveRequest)
        {
            is CarpoolDriveRequest -> { TODO() }
            is PublicDriveRequest ->
            {
                try { driveRequest.rejectDriveOffer(UUIDType.fromString(driveOfferId)) }
                catch (entityNotFoundError: EntityNotFoundError) { throw NotFoundError(ErrorDP(entityNotFoundError.message!!)) }
                driveRequestsRepository.save(driveRequest)
            }
            else -> { throw InternalServerError(ErrorDP("DriveRequest is neither a CarpoolDriveRequest nor a PublicDriveRequest.")) }
        }

        return ResponseEntity.noContent().build()
    }

    @Operation(description = "This endpoint is only allowed to use on a PublicRequestRequest. Accept a specific drive offer for a specific drive request. The requesting user of the drive request is automatically accepted as a passenger and the drive request is deleted.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveRequestId}/drive-offers/{driveOfferId}/acceptances")
    fun acceptDriveOffer(@PathVariable @UUID driveRequestId:String, @PathVariable @UUID driveOfferId:String, userToken: UserToken): ResponseEntity<Void>
    {
        if(!usersRepository.existsById(UUIDType.fromString(userToken.id))) throw ForbiddenError(ErrorDP("User with id ${userToken.id} does not exist in resource server."))

        val driveRequest: DriveRequest = driveRequestsRepository.findById(UUIDType.fromString(driveRequestId)).getOrNull() ?: throw NotFoundError(ErrorDP("DriveRequest with id $driveRequestId not found."))
        if(driveRequest.requestingUser.id != UUIDType.fromString(userToken.id)) throw ForbiddenError(ErrorDP("UserToken id does not match the requesting user id of the drive request."))

        when(driveRequest) {
            is CarpoolDriveRequest -> { TODO() }
            is PublicDriveRequest -> {
                try { driveRequest.acceptDriveOffer(UUIDType.fromString(driveOfferId)) }
                catch (entityNotFoundError: EntityNotFoundError) { throw NotFoundError(ErrorDP(entityNotFoundError.message!!)) }
                driveRequestsRepository.saveAndFlush(driveRequest)
                driveRequestsRepository.delete(driveRequest)
            }
            else -> { throw InternalServerError(ErrorDP("DriveRequest is neither a CarpoolDriveRequest nor a PublicDriveRequest.")) }
        }

        return ResponseEntity.noContent().build()
    }
}