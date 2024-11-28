package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable

@Embeddable
class Position(coordinate: Coordinate, nearestAddress: Address){
    final var coordinate: Coordinate = coordinate
        private set

    final var nearestAddress: Address = nearestAddress
        private set

    init {
        this.coordinate = coordinate
        this.nearestAddress = nearestAddress
    }
}