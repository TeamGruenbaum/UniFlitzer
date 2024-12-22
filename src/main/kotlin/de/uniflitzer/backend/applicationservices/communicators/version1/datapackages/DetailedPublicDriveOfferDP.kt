package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.Valid

class DetailedPublicDriveOfferDP constructor(
    containsFavoriteDriver: Boolean,
    id: String,
    driver: PartialUserDP,
    car: CarDP,
    freeSeats: Int,
    route: RouteDP,
    passengers: List<UserStopDP>,
    scheduleTime: ScheduleTimeDP?,
    @field:Valid val requestingUsers: List<UserStopDP>?
): DetailedDriveOfferDP(containsFavoriteDriver, id, driver, car, freeSeats, route, passengers, scheduleTime)