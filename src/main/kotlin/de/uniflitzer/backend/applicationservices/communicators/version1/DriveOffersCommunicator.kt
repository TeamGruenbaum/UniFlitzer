package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.hibernate.query.SortDirection
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/drive-offers")
@Validated
@Tag(name = "Drive Offers")
@SecurityRequirement(name = "bearerAuthentication")
private class DriveOffersCommunicator {
    @Operation(
        description = "Get all drive offers.",
        responses = [
            ApiResponse(
                responseCode = "200"
            ),
            ApiResponse(
                responseCode = "400",
                content = [Content(schema = Schema(implementation = ErrorsDP::class))]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]
            ),
            ApiResponse(
                responseCode = "500",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]
            ),
        ]
    )
    @GetMapping("")
    fun getDriveOffers(@RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int, @RequestParam sortDirection: SortDirection?): ResponseEntity<DriveOffersDP> {
        TODO()
    }

    @Operation(
        description = "Get details for a specific drive offer.",
        responses = [
            ApiResponse(
                responseCode = "200"),
            ApiResponse(
                responseCode = "400",
                content = [Content(schema = Schema(implementation = ErrorsDP::class))]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]
            ),
            ApiResponse(
                responseCode = "404",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]
            ),
            ApiResponse(
                responseCode = "500",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]
            ),
        ]
    )
    @GetMapping("{id}")
    fun getDriveOfferById(@PathVariable @UUID id: String):RequestEntity<DetailedDriveOfferDP> {
        TODO()
    }

    @Operation(
        description = "Create a new drive offer.",
        responses = [
            ApiResponse(
                responseCode = "201"
            ),
            ApiResponse(
                responseCode = "400",
                content = [Content(schema = Schema(implementation = ErrorsDP::class))]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]
            ),
            ApiResponse(
                responseCode = "500",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]
            ),
        ]
    )
    @PostMapping
    fun createDriveOffer(@RequestBody @Valid driveOfferCreation: DriveOfferCreationDP):RequestEntity<IdDP> {
        TODO()
    }

    @Operation(
        description = "Update a specific drive offer. Only the planned departure time can be updated.",
        responses = [
            ApiResponse(
                responseCode = "204"
            ),
            ApiResponse(
                responseCode = "400",
                content = [Content(schema = Schema(implementation = ErrorsDP::class))]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]
            ),
            ApiResponse(
                responseCode = "404",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]
            ),
            ApiResponse(
                responseCode = "500",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]
            ),
        ]
    )
    @PatchMapping("{id}")
    fun updateDriveOffer(@PathVariable @UUID id: String, @RequestBody @Valid driveOfferUpdateRequest: DriverOfferUpdateDP):ResponseEntity<Void> {
        TODO()
    }

    @Operation(
        description = "Request the ride for a specific drive offer.",
        responses = [
            ApiResponse(
                responseCode = "204"),
            ApiResponse(
                responseCode = "400",
                content = [Content(schema = Schema(implementation = ErrorsDP::class))]),
            ApiResponse(
                responseCode = "401",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]),
            ApiResponse(
                responseCode = "404",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]),
            ApiResponse(
                responseCode = "500",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]),
        ]
    )
    @PostMapping("{id}/requests")
    fun requestRide(@PathVariable @UUID id: String):ResponseEntity<Void> {
        TODO()
    }

    @Operation(
        description = "Accept a requesting user for a specific drive offer.",
        responses = [
            ApiResponse(
                responseCode = "204"),
            ApiResponse(
                responseCode = "400",
                content = [Content(schema = Schema(implementation = ErrorsDP::class))]),
            ApiResponse(
                responseCode = "401",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]),
            ApiResponse(
                responseCode = "404",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]),
            ApiResponse(
                responseCode = "500",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]),
        ]
    )
    @PostMapping("{driveOfferId}/requesting-users/{requestingUserId}/acceptances")
    fun acceptRequestingUser(@PathVariable @UUID driveOfferId: String, @PathVariable @UUID requestingUserId: String):ResponseEntity<Void> {
        TODO()
    }

    @Operation(
        description = "Reject a requesting user for a specific drive offer",
        responses = [
            ApiResponse(
                responseCode = "204"),
            ApiResponse(
                responseCode = "400",
                content = [Content(schema = Schema(implementation = ErrorsDP::class))]),
            ApiResponse(
                responseCode = "401",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]),
            ApiResponse(
                responseCode = "404",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]),
            ApiResponse(
                responseCode = "500",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]),
        ]
    )
    @PostMapping("{driveOfferId}/requesting-users/{requestingUserId}/rejections")
    fun rejectRequestingUser(@PathVariable @UUID driveOfferId: String, @PathVariable @UUID requestingUserId: String):ResponseEntity<Void> {
        TODO()
    }
}
