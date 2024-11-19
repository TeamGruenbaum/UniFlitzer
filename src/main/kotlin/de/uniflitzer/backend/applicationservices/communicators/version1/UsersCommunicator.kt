package de.uniflitzer.backend.applicationservices.communicators.version1


import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.*
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.media.Content
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.hibernate.query.SortDirection
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@SecurityRequirement(name = "bearerAuthentication")
@RestController
@RequestMapping("v1/users")
@Validated
@Tag(name = "User")
private class UsersCommunicator {
    @Operation(description = "Get details of a specific user.")
    @CommonApiResponses @OkApiResponse @NotFoundApiResponse
    @GetMapping("{id}")
    fun getUser(@PathVariable @UUID id: String): ResponseEntity<DetailedUserDP> {
        TODO()
    }

    @Operation(description = "Create a new user.")
    @CommonApiResponses @CreatedApiResponse
    @PostMapping("")
    fun createUser(@RequestBody @Valid userCreation: UserCreationDP): ResponseEntity<IdDP> {
        TODO()
    }

    @Operation(description = "Delete a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{id}")
    fun deleteUser(@PathVariable @UUID id: String): ResponseEntity<Void> {
        TODO()
    }

    @Operation(description = "Update a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PatchMapping("{id}")
    fun updateUser(@PathVariable @UUID id: String, @RequestBody @Valid userUpdate: UserUpdateDP): ResponseEntity<Void> {
        TODO()
    }

    @Operation(description = "Create an image for a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{id}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createImageForUser(@PathVariable @UUID id: String, @RequestPart image: MultipartFile): ResponseEntity<Void> {
        TODO()
    }

    @Operation(description = "Delete the image of a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{id}/image")
    fun deleteImageOfUser(@PathVariable @UUID id: String): ResponseEntity<Void> {
        TODO()
    }

    @Operation(description = "Get the image of a specific user.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content =  [Content(mediaType = MediaType.IMAGE_JPEG_VALUE)]
            )
        ]
    )
    @CommonApiResponses @NotFoundApiResponse
    @GetMapping("{id}/image")
    fun getImageOfUser(@PathVariable @UUID id: String, @RequestParam quality: QualityDP): ResponseEntity<ByteArray> {
        TODO()
    }

    @Operation(description = "Create a car for a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{id}/cars")
    fun createCarForUser(@PathVariable @UUID id: String, @RequestBody @Valid carCreation: CarCreationDP): ResponseEntity<IdDP> {
        TODO()
    }

    @Operation(description = "Update a specific car of a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PatchMapping("{userId}/cars/{carIndex}")
    fun updateCarOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) carIndex: Int, @RequestBody @Valid carUpdate: CarUpdateDP):ResponseEntity<Void> {
        TODO()
    }

    @Operation(description = "Create an image for a specific car of a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @PostMapping("{userId}/cars/{carIndex}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createImageForCarOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) carIndex: Int, @RequestPart image: MultipartFile):ResponseEntity<Void> {
        TODO()
    }

    @Operation(description = "Get the image of a specific car of a specific user.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content =  [Content(mediaType = MediaType.IMAGE_JPEG_VALUE)]
            )
        ]
    )
    @CommonApiResponses @NotFoundApiResponse
    @GetMapping("{userId}/cars/{carIndex}/image")
    fun getImageOfCarOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) carIndex: Int, @RequestParam quality: QualityDP): ResponseEntity<ByteArray> {
        TODO()
    }

    @Operation(description = "Delete the car of a specific user.")
    @CommonApiResponses @NoContentApiResponse @NotFoundApiResponse
    @DeleteMapping("{userId}/cars/{carIndex}")
    fun deleteCarOfUser(@PathVariable @UUID userId: String, @PathVariable @Min(0) carIndex: Int): ResponseEntity<CarDP> {
        TODO()
    }

    @Operation(description = "Get all drive offers of a specific user.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("{id}/drive-offers")
    fun getDriveOffersOfUser(@PathVariable @UUID id: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int, @RequestParam sortDirection: SortDirection?): ResponseEntity<PageDP<PartialDriveOfferDP>> {
        TODO()
    }

    @Operation(description = "Get all drive requests of a specific user.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("{id}/drive-requests")
    fun getDriveRequestsOfUser(@PathVariable @UUID id: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int, @RequestParam sortDirection: SortDirection?): ResponseEntity<PageDP<PartialDriveRequestDP>> {
        TODO()
    }

    @Operation(description = "Get all drives of a specific user.")
    @CommonApiResponses @OkApiResponse
    @GetMapping("{id}/drives")
    fun getDrivesOfUser(@PathVariable @UUID id: String, @RequestParam @Min(1) pageNumber: Int, @RequestParam @Min(1) @Max(50) perPage: Int, @RequestParam sortDirection: SortDirection?): ResponseEntity<PageDP<DriveDP>> {
        TODO()
    }
}