package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class AddressDP private constructor(
    @field:Size(min = 2, max = 100) val street: String,
    @field:Min(1) @field:Max(5000) val houseNumber: Int,
    @field:Min(5) @field:Max(5) val postalCode: Int,
    @field:Size(min = 2, max = 100) val city: String,
)