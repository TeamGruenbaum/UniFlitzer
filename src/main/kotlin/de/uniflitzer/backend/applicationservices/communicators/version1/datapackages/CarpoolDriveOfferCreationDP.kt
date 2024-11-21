package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID

class CarpoolDriveOfferCreationDP private constructor(
    carIndex: Int,
    freeSeats: Int,
    route: RouteDP,
    plannedDepartureTime: String?,
    @field:UUID val carpoolId: String
) : DriveOfferCreationDP(carIndex, freeSeats, route, plannedDepartureTime)