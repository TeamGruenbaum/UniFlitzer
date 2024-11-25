package de.uniflitzer.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import java.time.ZonedDateTime

@Entity
class CarpoolDriveRequest(requestingUser: User, route: Route, plannedDeparture: ZonedDateTime, carpool: Carpool) : DriveRequest(requestingUser, route, plannedDeparture) {
    @field:ManyToOne
    var carpool: Carpool = carpool
}