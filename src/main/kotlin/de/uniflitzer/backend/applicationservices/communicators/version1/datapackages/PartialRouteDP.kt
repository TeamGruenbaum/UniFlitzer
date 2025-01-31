package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.Route
import jakarta.validation.Valid
import java.time.Duration

data class PartialRouteDP private constructor(
    @field:Valid val start: PositionDP,
    @field:Valid val destination: PositionDP,
    val duration: Long
) {
    companion object {
        fun fromRoute(route: Route): PartialRouteDP {
            return PartialRouteDP(
                PositionDP.fromPosition(route.start),
                PositionDP.fromPosition(route.destination),
                route.duration.seconds
            )
        }
    }
}