package de.uniflitzer.backend.model

import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable

@Embeddable
class GeoJsonLineString(coordinates: List<Coordinate>) {
    final var type: String = "LineString"
        private set

    @field:ElementCollection
    private final var _coordinates: MutableList<Coordinate> = coordinates.toMutableList()
    final val coordinates: List<Coordinate> get() = _coordinates

    init {
        this.type = "LineString"
        this._coordinates = coordinates.toMutableList()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeoJsonLineString) return false

        if (type != other.type) return false
        if (_coordinates != other._coordinates) return false
        if (coordinates != other.coordinates) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + _coordinates.hashCode()
        result = 31 * result + coordinates.hashCode()
        return result
    }
}