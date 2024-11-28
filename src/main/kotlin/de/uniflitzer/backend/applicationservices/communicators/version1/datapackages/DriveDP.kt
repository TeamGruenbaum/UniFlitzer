package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormatExample
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern

data class DriveDP(
    @field:UUID val id: String,
    val driver: PartialUserDP,
    val car: CarDP,
    val passengers: List<PartialUserDP>,
    val route: CompleteRouteDP,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val plannedDeparture: String,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val actualDeparture: String?,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val arrival: String?
)