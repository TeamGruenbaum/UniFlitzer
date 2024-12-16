package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.CarpoolDriveOffer

class PartialCarpoolDriveOfferDP (
    containsFavoriteDriver: Boolean,
    id: String,
    driver: PartialUserDP,
    freeSeats: Int,
    route: RouteDP,
    passengersCount: Int,
    plannedDepartureTime: String?,
    @field:UUID val carpoolId: String,
): PartialDriveOfferDP(containsFavoriteDriver, id, driver, freeSeats, route, passengersCount, plannedDepartureTime) {
    companion object {
        fun fromCarpoolDriveOffer(carpoolDriveOffer: CarpoolDriveOffer, containsFavoriteDriver: Boolean): PartialCarpoolDriveOfferDP =
            PartialCarpoolDriveOfferDP(
                containsFavoriteDriver,
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