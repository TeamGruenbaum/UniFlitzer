package de.uniflitzer.backend.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne

@Entity
class CarpoolDriveRequest(requestingUser: User, route: Route, scheduleTime: ScheduleTime?, carpool: Carpool) : DriveRequest(requestingUser, route, scheduleTime) {
    @field:ManyToOne(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var carpool: Carpool = carpool
}