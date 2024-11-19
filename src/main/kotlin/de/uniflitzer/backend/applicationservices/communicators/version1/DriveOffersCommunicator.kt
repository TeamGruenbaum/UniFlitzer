package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.hibernate.query.SortDirection
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v1/drive-offers")
@Validated
@Tag(name = "Drive Offers")
@SecurityRequirement(name = "bearerAuthentication")
private class DriveOffersCommunicator {
    @Operation(description = "Get all drive offers.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("")
    fun getDriveOffers(@RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int, @RequestParam sortDirection: SortDirection?): ResponseEntity<PageDP<PartialDriveOfferDP>> {
        TODO()
    }

    @Operation(description = "Get details of a specific drive offer.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{id}")
    fun getDriveOffer(@PathVariable @UUID id: String):RequestEntity<DetailedDriveOfferDP> {
        TODO()
    }

    @Operation(description = "Get the image of a specific car of a specific drive offer.")
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
    fun getImageOfCar(@PathVariable @UUID id: String, @RequestParam quality: QualityDP): ResponseEntity<ByteArray> {
        TODO()
    }

    @Operation(description = "Create a new drive offer.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping
    fun createDriveOffer(@RequestBody @Valid driveOfferCreation: DriveOfferCreationDP):RequestEntity<IdDP> {
        TODO()
    }

    @Operation(description = "Update a specific drive offer. Only the planned departure time can be updated.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PatchMapping("{id}")
    fun updateDriveOffer(@PathVariable @UUID id: String, @RequestBody @Valid driveOfferUpdate: DriverOfferUpdateDP):ResponseEntity<Void> {
        TODO()
    }

    @Operation(description = "Request the ride for a specific drive offer.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{id}/requests")
    fun requestRide(@PathVariable @UUID id: String):ResponseEntity<Void> {
        TODO()
    }

    @Operation(description = "Accept a requesting user for a specific drive offer.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveOfferId}/requesting-users/{requestingUserId}/acceptances")
    fun acceptRequestingUser(@PathVariable @UUID driveOfferId: String, @PathVariable @UUID requestingUserId: String):ResponseEntity<Void> {
        TODO()
    }

    @Operation(description = "Reject a requesting user for a specific drive offer")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveOfferId}/requesting-users/{requestingUserId}/rejections")
    fun rejectRequestingUser(@PathVariable @UUID driveOfferId: String, @PathVariable @UUID requestingUserId: String):ResponseEntity<Void> {
        TODO()
    }
}
