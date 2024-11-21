package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

class PublicDriveOfferCreationDP private constructor(
    carIndex: Int,
    freeSeats: Int,
    route: RouteDP,
    plannedDepartureTime: String?,
) : DriveOfferCreationDP(carIndex, freeSeats, route, plannedDepartureTime)