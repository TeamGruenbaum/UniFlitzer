package de.uniflitzer.backend.model

import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import java.time.ZonedDateTime

@Entity
class PublicDriveOffer(driver: User, car: Car, freeSeats: Seats, route: Route, plannedDeparture: ZonedDateTime?) : DriveOffer(driver, car, freeSeats, route, plannedDeparture) {
    @field:ElementCollection
    private var _requestingUsers: MutableList<UserStop> = mutableListOf()
    val requestingUsers: List<UserStop> get() = _requestingUsers
}