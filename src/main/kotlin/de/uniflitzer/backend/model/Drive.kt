package de.uniflitzer.backend.model

import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
class Drive(driver: User, car: Car, route: CompleteRoute, passenger: List<User>, plannedDeparture: ZonedDateTime) {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @field:ManyToOne(fetch = FetchType.LAZY)
    var driver: User = driver

    var car: Car = car

    @field:ManyToMany(fetch = FetchType.LAZY)
    private var _passengers: MutableList<User> = passenger.toMutableList()
    val passengers: List<User> get() = _passengers

    var route: CompleteRoute = route
    var plannedDeparture: ZonedDateTime = plannedDeparture
    var actualDeparture: ZonedDateTime? = null
    var arrival: ZonedDateTime? = null

    @field:ElementCollection
    private var _messages: MutableList<Message> = mutableListOf()
    val messages: List<Message> get() = _messages
}

