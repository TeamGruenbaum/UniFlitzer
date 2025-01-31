package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable

@Embeddable
class Position(coordinate: Coordinate, nearestAddress: Address?){
    final var coordinate: Coordinate = coordinate
        private set

    final var nearestAddress: Address? = nearestAddress
        private set

    init {
        this.coordinate = coordinate
        this.nearestAddress = nearestAddress
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Position) return false

        if (coordinate != other.coordinate) return false
        if (nearestAddress != other.nearestAddress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = coordinate.hashCode()
        result = 31 * result + (nearestAddress?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Position(coordinate=$coordinate, nearestAddress=$nearestAddress)"
    }
}