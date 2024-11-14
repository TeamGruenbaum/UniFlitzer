package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Pattern

data class DriverOfferUpdateDP private constructor(
    @field:Pattern(regexp = DateTimeFormat) val plannedDepartureTime: String,
)