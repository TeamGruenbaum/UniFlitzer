package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Size

data class ErrorsDP(
    @field:Size(min=1) val messages: List<String>
)