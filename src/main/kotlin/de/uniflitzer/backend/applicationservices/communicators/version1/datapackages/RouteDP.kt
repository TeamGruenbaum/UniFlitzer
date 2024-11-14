package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

data class RouteDP private constructor(
    val start: PositionDP,
    val destination: PositionDP
)