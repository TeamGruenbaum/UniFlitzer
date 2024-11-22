package de.uniflitzer.backend.model

import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
class Drive {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @field:ManyToOne(fetch = FetchType.LAZY)
    var driver: User = null!!

    var car: Car = null!!

    @field:ManyToMany(fetch = FetchType.LAZY)
    var passengers: MutableList<User> = null!!

    var route: CompleteRoute = null!!
    var plannedDeparture: ZonedDateTime = null!!
    var actualDeparture: ZonedDateTime? = null
    var arrival: ZonedDateTime? = null

    @field:ElementCollection
    var messages: MutableList<Message> = null!!

    constructor(driver: User, car: Car, route: CompleteRoute, plannedDeparture: ZonedDateTime, actualDeparture: ZonedDateTime?, arrival: ZonedDateTime?) {
        this.driver = driver
        this.car = car
        this.passengers = mutableListOf()
        this.route = route
        this.plannedDeparture = plannedDeparture
        this.actualDeparture = actualDeparture
        this.arrival = arrival
        this.messages = mutableListOf()
    }
}

