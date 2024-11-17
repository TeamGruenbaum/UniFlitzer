package de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder.apiresponses

import io.swagger.v3.oas.annotations.responses.ApiResponse

@Target(AnnotationTarget.FUNCTION)
@ApiResponse(
    responseCode = "204"
)
annotation class NoContentApiResponse