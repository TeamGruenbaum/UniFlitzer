package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.hibernate.query.SortDirection
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated

@RestController
@RequestMapping("v1/drive-requests")
@Validated
@SecurityRequirement(name = "bearerAuthentication")
@Tag(name = "DriveRequests")
private class DriveRequestsCommunicator
{
    @Operation(
        description = "Create a new drive request.",
        responses = [
            ApiResponse(
                responseCode = "201"),
            ApiResponse(
                responseCode = "400",
                content = [Content(schema = Schema(implementation = ErrorsDP::class))]),
            ApiResponse(
                responseCode = "401",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]),
            ApiResponse(
                responseCode = "500",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]),
        ]
    )
    @PostMapping("")
    fun createDriveRequest(@RequestBody @Valid driveRequestCreationDP: DriveRequestCreationDP): ResponseEntity<IdDP>
    {
        TODO()
    }

    @Operation(
        description = "Get all drive requests.",
        responses = [
            ApiResponse(
                responseCode = "200"),
            ApiResponse(
                responseCode = "400",
                content = [Content(schema = Schema(implementation = ErrorsDP::class))]),
            ApiResponse(
                responseCode = "401",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]),
            ApiResponse(
                responseCode = "500",
                content = [Content(schema = Schema(implementation = ErrorDP::class))]),
        ]
    )
    @GetMapping("")
    fun getDriveRequests(@RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int,
                         @RequestParam sortDirection: SortDirection?): ResponseEntity<DriveRequestsDP>
    {
        TODO()
    }

    @Operation(
        description = "Get details for a specific drive request.",
        responses = [
            ApiResponse(
                responseCode = "200"),
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
    @GetMapping("{id}")
    fun getDriveRequestById(@PathVariable @UUID id:String): ResponseEntity<DetailedDriveRequestDP>
    {
        TODO()
    }

    @Operation(
        description = "Create a new drive offer for a specific drive request.",
        responses = [
            ApiResponse(
                responseCode = "201"),
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
    @PostMapping("{id}/drive-offers")
    fun createDriveOfferForDriveRequest(@PathVariable @UUID id:String, @RequestBody @Valid driveOfferCreationDP: DriveOfferCreationDP): ResponseEntity<IdDP>
    {
        TODO("DriveRequest must be either deleted if it's a CarpoolDriveRequest or its driveOffers must be updated if it's a PublicDriveRequest.")
    }

    @Operation(
        description = "Reject a specific drive offer for a specific drive request.",
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
    @PostMapping("{driveRequestId}/drive-offers/{driveOfferId}/rejections")
    fun rejectDriveOffer(@PathVariable @UUID driveRequestId:String, @PathVariable @UUID driveOfferId:String): ResponseEntity<Void>
    {
        TODO("Neither the DriveRequest nor the DriveOffer is deleted.")
    }

    @Operation(
        description = "Accept a specific drive offer for a specific drive request.",
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
    @PostMapping("{driveRequestId}/drive-offers/{driveOfferId}/acceptances")
    fun acceptDriveOffer(@PathVariable @UUID driveRequestId:String, @PathVariable @UUID driveOfferId:String): ResponseEntity<Void>
    {
        TODO("Driver doesn't have to accept the passenger and DriveRequest must be deleted.")
    }
}