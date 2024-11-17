package de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorsDP
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses

@Target(AnnotationTarget.FUNCTION)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "400",
            content = [Content(schema = Schema(implementation = ErrorsDP::class))]
        ),
        ApiResponse(
            responseCode = "401",
            content = [Content(schema = Schema(implementation = ErrorDP::class))]
        ),
        ApiResponse(
            responseCode = "403",
            content = [Content(schema = Schema(implementation = ErrorDP::class))]
        ),
        ApiResponse(
            responseCode = "500",
            content = [Content(schema = Schema(implementation = ErrorDP::class))]
        )
    ]
)
annotation class CommonApiResponses