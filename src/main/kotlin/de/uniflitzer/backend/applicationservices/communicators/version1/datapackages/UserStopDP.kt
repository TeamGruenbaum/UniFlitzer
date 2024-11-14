package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages;

data class UserStopDP private constructor(
    val user: PartialUserDP,
    val stop: PositionDP
)
