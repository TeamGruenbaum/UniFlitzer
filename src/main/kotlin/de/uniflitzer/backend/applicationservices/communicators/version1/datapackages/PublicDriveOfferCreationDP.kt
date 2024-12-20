package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

class PublicDriveOfferCreationDP private constructor(
    carIndex: Int,
    freeSeats: Int,
    route: RouteCreationDP,
    plannedDeparture: String?,
) : DriveOfferCreationDP(carIndex, freeSeats, route, plannedDeparture)