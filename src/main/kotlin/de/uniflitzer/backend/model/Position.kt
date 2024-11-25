package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable

@Embeddable
class Position(latitude: Double, longitude: Double, nearestAddress: Address){
    final var latitude: Double = latitude
        private set(value) {
            require(value in (-90.0..90.0))
            field = value
        }

    final var longitude: Double = longitude
        private set(value) {
            require(value in (0.0..360.0))
            field = value
        }

    private var nearestAddress: Address = nearestAddress
        private set

    init {
        this.latitude = latitude
        this.longitude = longitude
        this.nearestAddress = nearestAddress
    }
}