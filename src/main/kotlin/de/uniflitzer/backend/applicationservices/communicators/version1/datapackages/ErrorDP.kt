package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Size

data class ErrorDP(
    @field:Size(min=3) val message: String
)