package de.uniflitzer.backend.model

import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
class Drive(driver: User, car: Car, route: CompleteRoute, passenger: List<User>, plannedDeparture: ZonedDateTime, plannedArrival: ZonedDateTime) {
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
    var plannedArrival: ZonedDateTime = plannedArrival
    var actualArrival: ZonedDateTime? = null
    var currentPosition: Coordinate? = null
    var isCancelled: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Drive) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}