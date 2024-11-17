package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Pattern

data class DriveUpdateDP private constructor(
    @field:Pattern(regexp = DateTimeFormat) val actualDeparture: String?,
    @field:Pattern(regexp = DateTimeFormat) val arrival: String?
)