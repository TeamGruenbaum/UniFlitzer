package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class PageDP<ContentType>(
    @field:Min(0) val maximumPage: Int,
    @field:Valid @field:Size(min = 0) val content: List<ContentType>
)