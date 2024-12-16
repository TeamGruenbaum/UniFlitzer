package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.PublicDriveOffer
import jakarta.validation.constraints.Size

class PartialPublicDriveOfferDP constructor(
    containsFavoriteDriver: Boolean,
    id: String,
    driver: PartialUserDP,
    freeSeats: Int,
    route: RouteDP,
    passengersCount: Int,
    plannedDepartureTime: String?,
    @field:Size(min = 0) val requestingUserIds: List<String>?
): PartialDriveOfferDP(containsFavoriteDriver, id, driver, freeSeats, route, passengersCount, plannedDepartureTime) {
    companion object {
        fun fromPublicDriveOffer(publicDriveOffer: PublicDriveOffer, containsFavoriteDriver: Boolean): PartialPublicDriveOfferDP {
            return PartialPublicDriveOfferDP(
                containsFavoriteDriver,
                publicDriveOffer.id.toString(),
                PartialUserDP.fromUser(publicDriveOffer.driver),
                publicDriveOffer.freeSeats.value.toInt(),
                RouteDP.fromRoute(publicDriveOffer.route),
                publicDriveOffer.passengers.size,
                publicDriveOffer.plannedDeparture?.toString(),
                publicDriveOffer.requestingUsers.map { it.user.id.toString() }
            )
        }
    }
}