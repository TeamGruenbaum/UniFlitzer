package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.DriveDP
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.DriveUpdateDP
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.QualityDP
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.CommonApiResponses
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.NoContentApiResponse
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.NotFoundApiResponse
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.OkApiResponse
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v1/drives")
@Validated
@SecurityRequirement(name = "Token Authentication (uniflitzer-auth.stevensolleder.de)")
@SecurityRequirement(name = "Token Authentication (localhost)")
@Tag(name = "Drives")
private class DrivesCommunicator
{
    @Operation(description = "Get details of a specific drive.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{id}")
    fun getDrive(@PathVariable @UUID id:String): ResponseEntity<DriveDP>
    {
        TODO()
    }

    @Operation(description = "Get the image of a specific car of a specific drive.")
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

    @Operation(description = "Update the actual departure or the arrival of a specific drive.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PatchMapping("{id}")
    fun updateDrive(@PathVariable @UUID id:String, @RequestBody @Valid driveUpdateRequest: DriveUpdateDP): ResponseEntity<Void>
    {
        TODO("If the actual departure was updated once, it must not be updated again. The same applies to the arrival.")
    }

    @Operation(description = "Confirm that your are waiting at a specific user stop of a specific drive.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{driveId}/complete-route/user-stops/{userId}/confirmations")
    fun confirmUserStop(@PathVariable @UUID driveId: String, @PathVariable @UUID userId: String): ResponseEntity<Void>
    {
        TODO()
    }
}