package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.formats.LicencePlateFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CarDP private constructor(
    @field:Size(min = 3, max = 50) val brand: String,
    @field:Size(min = 3, max = 50) val model: String,
    @field:Size(min = 3, max = 50) val color: String,
    @field:Pattern(regexp = LicencePlateFormat) val licencePlate: String
)