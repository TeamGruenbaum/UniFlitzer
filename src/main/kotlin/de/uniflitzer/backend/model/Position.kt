package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable

@Embeddable
class Position{
    var latitude: Double = 0.0
        set(value) {
            require(value in (-90.0..90.0))
            field = value
        }

    var longitude: Double = 0.0
        set(value) {
            require(value in (0.0..360.0))
            field = value
        }

    var nearestAddress: Address = null!!

    constructor(latitude: Double, longitude: Double, nearestAddress: Address) {
        this.latitude = latitude
        this.longitude = longitude
        this.nearestAddress = nearestAddress
    }
}