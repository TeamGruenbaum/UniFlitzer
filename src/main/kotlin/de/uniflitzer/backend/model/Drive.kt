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
    private var _passengers: MutableList<User> = null!!
    val passengers: List<User> get() = _passengers

    var route: CompleteRoute = null!!
    var plannedDeparture: ZonedDateTime = null!!
    var actualDeparture: ZonedDateTime? = null
    var arrival: ZonedDateTime? = null

    @field:ElementCollection
    private var _messages: MutableList<Message> = null!!
    val messages: List<Message> get() = _messages

    constructor(driver: User, car: Car, route: CompleteRoute, plannedDeparture: ZonedDateTime, actualDeparture: ZonedDateTime?, arrival: ZonedDateTime?) {
        this.driver = driver
        this.car = car
        this._passengers = mutableListOf()
        this.route = route
        this.plannedDeparture = plannedDeparture
        this.actualDeparture = actualDeparture
        this.arrival = arrival
        this._messages = mutableListOf()
    }
}

