package de.uniflitzer.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import java.time.ZonedDateTime

@Entity
class CarpoolDriveRequest : DriveRequest{
    @field:ManyToOne
    var carpool: Carpool = null!!

    constructor(requestingUser: User, route: Route, plannedDeparture: ZonedDateTime, carpool: Carpool) : super(requestingUser, route, plannedDeparture) {
        this.carpool = carpool
    }
}