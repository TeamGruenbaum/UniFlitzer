package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.Address
import jakarta.validation.constraints.Size

data class AddressDP constructor(
    @field:Size(min = 2, max = 100) val street: String,
    @field:Size(min = 1, max = 5) val houseNumber: String,
    @field:Size(min = 5, max = 5) val postalCode: String,
    @field:Size(min = 2, max = 100) val city: String,
) {
    fun toAddress(): Address =
        Address(
            this.street,
            this.houseNumber,
            this.postalCode,
            this.city
        )

    companion object {
        fun fromAddress(address: Address): AddressDP =
            AddressDP(
                address.street,
                address.houseNumber,
                address.postalCode,
                address.city
            )
    }
}