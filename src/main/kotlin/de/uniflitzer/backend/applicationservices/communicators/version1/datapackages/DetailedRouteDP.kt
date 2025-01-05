package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.Route
import jakarta.validation.Valid

data class DetailedRouteDP private constructor(
    @field:Valid val start: PositionDP,
    @field:Valid val destination: PositionDP,
    val duration: Long,
    val polyline: GeoJsonLineStringDP
) {
    companion object {
        fun fromRoute(route: Route): DetailedRouteDP {
            return DetailedRouteDP(
                PositionDP.fromPosition(route.start),
                PositionDP.fromPosition(route.destination),
                route.duration.seconds,
                GeoJsonLineStringDP.fromGeoJsonLineString(route.polyline)
            )
        }
    }
}