package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

class PublicDriveOfferCreationDP private constructor(
    freeSeats: Int,
    route: RouteDP,
    plannedDepartureTime: String,
) : DriveOfferCreationDP(freeSeats, route, plannedDepartureTime)