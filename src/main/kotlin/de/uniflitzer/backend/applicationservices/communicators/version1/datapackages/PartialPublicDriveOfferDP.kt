package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Size

class PartialPublicDriveOfferDP constructor(
    id: String,
    driver: PartialUserDP,
    freeSeats: Int,
    route: RouteDP,
    passengersCount: Int,
    plannedDepartureTime: String?,
    @field:Size(min = 0) val requestingUserIds: List<String>?
): PartialDriveOfferDP(id, driver, freeSeats, route, passengersCount, plannedDepartureTime)