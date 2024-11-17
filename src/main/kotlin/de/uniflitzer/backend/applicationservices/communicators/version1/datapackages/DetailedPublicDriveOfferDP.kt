package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

class DetailedPublicDriveOfferDP private constructor(
    id: String,
    driver: PartialUserDP,
    car: CarDP,
    freeSeats: Int,
    route: RouteDP,
    passengers: List<UserStopDP>,
    plannedDepartureTime: String?,
    val requestingUsers: List<PartialUserDP>?
): DetailedDriveOfferDP(id, driver, car, freeSeats, route, passengers, plannedDepartureTime)