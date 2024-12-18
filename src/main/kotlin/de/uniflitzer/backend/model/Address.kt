package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable


@Embeddable
class Address(street: String, houseNumber: String, postalCode: String, city: String) {
    final var street: String = street
        private set(value) {
            require(value.count() in 2..100)
            field = value
        }

    final var houseNumber: String = houseNumber
        private set(value) {
            require(value.count() in 1..5)
            field = value
        }

    final var postalCode: String = postalCode
        private set(value) {
            require(value.count() in 5..5)
            field = value
        }

    final var city: String = city
        private set(value) {
            require(value.count() in 2..100)
            field = value
        }

    init {
        this.street = street
        this.houseNumber = houseNumber
        this.postalCode = postalCode
        this.city = city
    }
}