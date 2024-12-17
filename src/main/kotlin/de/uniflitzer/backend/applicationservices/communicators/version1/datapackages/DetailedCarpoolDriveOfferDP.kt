package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.Valid

class DetailedCarpoolDriveOfferDP constructor(
    containsFavoriteDriver: Boolean,
    id: String,
    driver: PartialUserDP,
    car: CarDP,
    freeSeats: Int,
    route: RouteDP,
    passengers: List<UserStopDP>,
    plannedDepartureTime: String?,
    @field:Valid val carpool: PartialCarpoolDP,
): DetailedDriveOfferDP(containsFavoriteDriver, id, driver, car, freeSeats, route, passengers, plannedDepartureTime)