package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class PositionDP private constructor(
    @field:Min(-90) @field:Max(90) val latitude: Double,
    @field:Min(0) @field:Max(360) val longitude: Double,
    val nearestAddress: AddressDP
)