package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import jakarta.validation.constraints.Pattern

data class DriveDP(
    @field:UUID val id: String,
    val driver: PartialUserDP,
    val passengers: List<PartialUserDP>,
    val route: CompleteRouteDP,
    @field:Pattern(regexp = DateTimeFormat) val plannedDeparture: String,
    @field:Pattern(regexp = DateTimeFormat) val actualDeparture: String?,
    @field:Pattern(regexp = DateTimeFormat) val arrival: String?
)