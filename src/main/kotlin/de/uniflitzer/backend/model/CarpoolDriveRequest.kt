package de.uniflitzer.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne

@Entity
class CarpoolDriveRequest(requestingUser: User, route: Route, scheduleTime: ScheduleTime?, carpool: Carpool) : DriveRequest(requestingUser, route, scheduleTime) {
    @field:ManyToOne
    var carpool: Carpool = carpool
}