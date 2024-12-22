package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable


@Embeddable
class Address(street: String, houseNumber: String, postalCode: String, city: String) {
    final var street: String = street
        private set(value) {
            require(value.count() in 2..100) { "Property street with value $value is not between 2 and 100 characters long." }
            field = value
        }

    final var houseNumber: String = houseNumber
        private set(value) {
            require(value.count() in 1..5) { "Property houseNumber with value $value is not between 1 and 5 characters long." }
            field = value
        }

    final var postalCode: String = postalCode
        private set(value) {
            require(value.count() in 5..5) { "Property postalCode with value $value is not between 5 and 5 characters long." }
            field = value
        }

    final var city: String = city
        private set(value) {
            require(value.count() in 2..100) { "Property city with value $value is not between 2 and 100 characters long." }
            field = value
        }

    init {
        this.street = street
        this.houseNumber = houseNumber
        this.postalCode = postalCode
        this.city = city
    }
}