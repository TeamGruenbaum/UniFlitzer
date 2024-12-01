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
}