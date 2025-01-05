package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.Valid

class DetailedCarpoolDriveOfferDP constructor(
    containsFavoriteDriver: Boolean,
    id: String,
    driver: PartialUserDP,
    car: CarDP,
    freeSeats: Int,
    route: DetailedRouteDP,
    passengers: List<UserStopDP>,
    scheduleTime: ScheduleTimeDP?,
    @field:Valid val carpool: PartialCarpoolDP,
): DetailedDriveOfferDP(containsFavoriteDriver, id, driver, car, freeSeats, route, passengers, scheduleTime)