package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.CarpoolDriveOffer

class PartialCarpoolDriveOfferDP constructor(
    id: String,
    driver: PartialUserDP,
    freeSeats: Int,
    route: RouteDP,
    passengersCount: Int,
    plannedDepartureTime: String?,
    @field:UUID val carpoolId: String,
): PartialDriveOfferDP(id, driver, freeSeats, route, passengersCount, plannedDepartureTime) {
    companion object {
        fun fromCarpoolDriveOffer(carpoolDriveOffer: CarpoolDriveOffer): PartialCarpoolDriveOfferDP =
            PartialCarpoolDriveOfferDP(
                carpoolDriveOffer.id.toString(),
                PartialUserDP.fromUser(carpoolDriveOffer.driver),
                carpoolDriveOffer.freeSeats.value.toInt(),
                RouteDP.fromRoute(carpoolDriveOffer.route),
                carpoolDriveOffer.passengers.size,
                carpoolDriveOffer.plannedDeparture.toString(),
                carpoolDriveOffer.carpool.id.toString()
            )
    }
}