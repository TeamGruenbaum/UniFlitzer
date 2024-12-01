package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.GeoJsonLineString
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

data class GeoJsonLineStringDP(
    @field:Schema(description = "Has always the value LineString.") var type: String = "LineString",
    @field:Valid val coordinates: List<CoordinateDP>
) {
    companion object {
        fun fromGeoJsonLineString(geoJsonLineString: GeoJsonLineString): GeoJsonLineStringDP {
            return GeoJsonLineStringDP(geoJsonLineString.type, geoJsonLineString.coordinates.map { CoordinateDP.fromCoordinate(it) })
        }
    }
}