package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

data class CompleteRouteDP(
    val start: PositionDP,
    val destination: PositionDP,
    val userStops: List<PartialConfirmableUserStopResponse>
)