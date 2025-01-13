package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable


@Embeddable
class Address(street: String, houseNumber: String, postalCode: String, city: String) {
    final var street: String = street
        private set(value) {
            require(value.count() in 2..100) { "Passed value is not between 2 and 100 characters long." }
            field = value
        }

    final var houseNumber: String = houseNumber
        private set(value) {
            require(value.count() in 1..5) { "Passed value is not between 1 and 5 characters long." }
            field = value
        }

    final var postalCode: String = postalCode
        private set(value) {
            require(value.count() in 5..5) { "Passed value is not between 5 and 5 characters long." }
            field = value
        }

    final var city: String = city
        private set(value) {
            require(value.count() in 2..100) { "Passed value is not between 2 and 100 characters long." }
            field = value
        }

    init {
        this.street = street
        this.houseNumber = houseNumber
        this.postalCode = postalCode
        this.city = city
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Address) return false

        if (street != other.street) return false
        if (houseNumber != other.houseNumber) return false
        if (postalCode != other.postalCode) return false
        if (city != other.city) return false

        return true
    }

    override fun hashCode(): Int {
        var result = street.hashCode()
        result = 31 * result + houseNumber.hashCode()
        result = 31 * result + postalCode.hashCode()
        result = 31 * result + city.hashCode()
        return result
    }

    override fun toString(): String {
        return "Address(street='$street', houseNumber='$houseNumber', postalCode='$postalCode', city='$city')"
    }
}