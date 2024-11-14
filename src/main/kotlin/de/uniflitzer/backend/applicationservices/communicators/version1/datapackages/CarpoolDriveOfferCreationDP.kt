package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

class CarpoolDriveOfferCreationDP private constructor(
    freeSeats: Int,
    route: RouteDP,
    plannedDepartureTime: String,
    @field:UUID val carpoolId: String
) : DriveOfferCreationDP(freeSeats, route, plannedDepartureTime)