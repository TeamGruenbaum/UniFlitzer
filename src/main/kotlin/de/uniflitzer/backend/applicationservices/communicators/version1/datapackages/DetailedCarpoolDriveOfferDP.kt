package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

class DetailedCarpoolDriveOfferDP private constructor(
    id: String,
    driver: PartialUserDP,
    car: CarDP,
    freeSeats: Int,
    route: RouteDP,
    passengers: List<UserStopDP>,
    plannedDepartureTime: String?,
    val carpool: PartialCarpoolDP,
): DetailedDriveOfferDP(id, driver, car, freeSeats, route, passengers, plannedDepartureTime)