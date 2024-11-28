package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.Route

data class RouteDP private constructor(
    val start: PositionDP,
    val destination: PositionDP
) {
    companion object {
        fun fromRoute(route: Route): RouteDP {
            return RouteDP(
                PositionDP.fromPosition(route.start),
                PositionDP.fromPosition(route.destination)
            )
        }
    }
}