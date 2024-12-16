package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.Route
import jakarta.validation.Valid

data class RouteDP private constructor(
    @field:Valid val start: PositionDP,
    @field:Valid val destination: PositionDP,
    @field:Valid val polyline: GeoJsonLineStringDP
) {
    companion object {
        fun fromRoute(route: Route): RouteDP {
            return RouteDP(
                PositionDP.fromPosition(route.start),
                PositionDP.fromPosition(route.destination),
                GeoJsonLineStringDP.fromGeoJsonLineString(route.polyline)
            )
        }
    }
}