package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.CarpoolDriveOffer

class PartialCarpoolDriveOfferDP private constructor(
    containsFavoriteDriver: Boolean,
    id: String,
    driver: PartialUserDP,
    freeSeats: Int,
    route: PartialRouteDP,
    passengersCount: Int,
    scheduleTime: ScheduleTimeDP?,
    @field:UUID val carpoolId: String,
): PartialDriveOfferDP(containsFavoriteDriver, id, driver, freeSeats, route, passengersCount, scheduleTime) {
    companion object {
        fun fromCarpoolDriveOffer(carpoolDriveOffer: CarpoolDriveOffer, containsFavoriteDriver: Boolean): PartialCarpoolDriveOfferDP =
            PartialCarpoolDriveOfferDP(
                containsFavoriteDriver,
                carpoolDriveOffer.id.toString(),
                PartialUserDP.fromUser(carpoolDriveOffer.driver),
                carpoolDriveOffer.freeSeats.value.toInt(),
                PartialRouteDP.fromRoute(carpoolDriveOffer.route),
                carpoolDriveOffer.passengers.size,
                carpoolDriveOffer.scheduleTime?.let { ScheduleTimeDP.fromScheduleTime(it) },
                carpoolDriveOffer.carpool.id.toString()
            )
    }
}