package de.uniflitzer.backend.model

import jakarta.persistence.*
import java.util.UUID
import java.time.ZonedDateTime

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
// Should be sealed, but Hibernate 6 does not support sealed classes
class DriveOffer(driver: User, car: Car, freeSeats: Seats, route: Route, plannedDeparture: ZonedDateTime?) {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @field:ManyToOne
    var driver: User = driver

    var car: Car = car

    var freeSeats: Seats = freeSeats

    var route: Route = route

    @field:ElementCollection
    protected var _passengers: MutableList<UserStop> = mutableListOf()
    val passengers: List<UserStop> get() = _passengers

    var plannedDeparture: ZonedDateTime? = plannedDeparture

    init {
        this.driver = driver
        this.car = car
        this.freeSeats = freeSeats
        this.route = route
        this.plannedDeparture = plannedDeparture
    }

    fun addPassenger(userStop: UserStop) {
        _passengers.add(userStop)
    }
}