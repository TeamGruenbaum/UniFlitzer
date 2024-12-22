package de.uniflitzer.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import java.time.ZonedDateTime

@Entity
class CarpoolDriveOffer(driver: User, car: Car, freeSeats: Seats, route: Route, scheduleTime: ScheduleTime?, carpool: Carpool) : DriveOffer(driver, car, freeSeats, route, scheduleTime) {
    @field:ManyToOne
    var carpool: Carpool = carpool
}