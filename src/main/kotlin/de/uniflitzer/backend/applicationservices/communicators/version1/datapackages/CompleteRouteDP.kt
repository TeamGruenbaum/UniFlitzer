package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.CompleteRoute
import jakarta.validation.Valid
import java.time.Duration

data class CompleteRouteDP(
    @field:Valid val start: PositionDP,
    @field:Valid val destination: PositionDP,
    @field:Valid val userStops: List<PartialConfirmableUserStop>,
    val duration: Duration,
    @field:Valid val polyline: GeoJsonLineStringDP
) {
    companion object {
        fun fromCompleteRoute(completeRoute: CompleteRoute): CompleteRouteDP {
            return CompleteRouteDP(
                PositionDP.fromPosition(completeRoute.start),
                PositionDP.fromPosition(completeRoute.destination),
                completeRoute.userStops.map { PartialConfirmableUserStop.fromConfirmableUserStop(it) },
                completeRoute.duration,
                GeoJsonLineStringDP.fromGeoJsonLineString(completeRoute.polyline)
            )
        }
    }
}