package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Size

data class AddressDP private constructor(
    @field:Size(min = 2, max = 100) val street: String,
    @field:Size(min = 1, max = 5000) val houseNumber: String,
    @field:Size(min = 5, max = 5) val postalCode: String,
    @field:Size(min = 2, max = 100) val city: String,
)