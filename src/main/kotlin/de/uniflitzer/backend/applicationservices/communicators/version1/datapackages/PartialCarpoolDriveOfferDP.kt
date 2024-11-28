package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID

class PartialCarpoolDriveOfferDP constructor(
    id: String,
    driver: PartialUserDP,
    freeSeats: Int,
    route: RouteDP,
    passengersCount: Int,
    plannedDepartureTime: String?,
    @field:UUID val carpoolId: String,
): PartialDriveOfferDP(id, driver, freeSeats, route, passengersCount, plannedDepartureTime)