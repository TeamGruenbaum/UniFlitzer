package de.uniflitzer.backend.model

import de.uniflitzer.backend.model.errors.MissingActionError
import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.model.errors.RepeatedActionError
import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*
import kotlin.jvm.Throws

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

    @Throws(NotAvailableError::class)
    fun removePassenger(user: User) {
        if (user !in _passengers) throw NotAvailableError("User with id ${user.id} is not a passenger of drive with id $id.")
        _passengers.remove(user)
    }

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