package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.PublicDriveOffer
import jakarta.validation.constraints.Size

class PartialPublicDriveOfferDP constructor(
    containsFavoriteDriver: Boolean,
    id: String,
    driver: PartialUserDP,
    freeSeats: Int,
    route: PartialRouteDP,
    passengersCount: Int,
    scheduleTime: ScheduleTimeDP?,
    @field:Size(min = 0) val requestingUserIds: List<String>?
): PartialDriveOfferDP(containsFavoriteDriver, id, driver, freeSeats, route, passengersCount, scheduleTime) {
    companion object {
        fun fromPublicDriveOffer(publicDriveOffer: PublicDriveOffer, containsFavoriteDriver: Boolean): PartialPublicDriveOfferDP {
            return PartialPublicDriveOfferDP(
                containsFavoriteDriver,
                publicDriveOffer.id.toString(),
                PartialUserDP.fromUser(publicDriveOffer.driver),
                publicDriveOffer.freeSeats.value.toInt(),
                PartialRouteDP.fromRoute(publicDriveOffer.route),
                publicDriveOffer.passengers.size,
                publicDriveOffer.scheduleTime?.let { ScheduleTimeDP.fromScheduleTime(it) },
                publicDriveOffer.requestingUsers.map { it.user.id.toString() }
            )
        }
    }
}