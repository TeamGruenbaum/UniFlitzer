package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.CompleteRoute
import jakarta.validation.Valid

data class CompleteRouteDP(
    @field:Valid val start: PositionDP,
    @field:Valid val destination: PositionDP,
    @field:Valid val userStops: List<PartialConfirmableUserStop>,
    val duration: Long,
    @field:Valid val polyline: GeoJsonLineStringDP
) {
    companion object {
        fun fromCompleteRoute(completeRoute: CompleteRoute): CompleteRouteDP {
            return CompleteRouteDP(
                PositionDP.fromPosition(completeRoute.start),
                PositionDP.fromPosition(completeRoute.destination),
                completeRoute.userStops.map { PartialConfirmableUserStop.fromConfirmableUserStop(it) },
                completeRoute.duration.seconds,
                GeoJsonLineStringDP.fromGeoJsonLineString(completeRoute.polyline)
            )
        }
    }
}