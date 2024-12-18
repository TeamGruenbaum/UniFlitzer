package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormatExample
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern

data class DriverOfferUpdateDP private constructor(
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val plannedDeparture: String,
)