package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.Valid

class DetailedPublicDriveOfferDP(
    containsFavoriteDriver: Boolean,
    id: String,
    driver: PartialUserDP,
    car: CarDP,
    freeSeats: Int,
    route: DetailedRouteDP,
    passengers: List<UserStopDP>,
    scheduleTime: ScheduleTimeDP?,
    @field:Valid val requestingUsers: List<UserStopDP>?
): DetailedDriveOfferDP(containsFavoriteDriver, id, driver, car, freeSeats, route, passengers, scheduleTime)