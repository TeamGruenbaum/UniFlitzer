package de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Target(AnnotationTarget.FUNCTION)
@ApiResponse(
    responseCode = "404",
    content = [Content(schema = Schema(implementation = ErrorDP::class))]
)
annotation class NotFoundApiResponse