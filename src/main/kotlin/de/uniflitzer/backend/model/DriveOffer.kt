package de.uniflitzer.backend.model

import de.uniflitzer.backend.model.errors.MissingActionError
import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.model.errors.RepeatedActionError
import jakarta.persistence.*
import java.util.UUID
import java.time.ZonedDateTime

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "discriminator_type")
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
    private var _passengers: MutableList<UserStop> = mutableListOf()
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
        if (userStop.user in _passengers.map { it.user }) throw RepeatedActionError("User is already a passenger of this drive offer")
        if (userStop.user == driver) throw MissingActionError("Driver cannot be a passenger of a drive offer")
        if (freeSeats.value == _passengers.count().toUInt()) throw NotAvailableError("No more seats available for this drive offer")

        _passengers.add(userStop)
    }

    fun removePassenger(user: User) {
        val searcherUserStop: UserStop = _passengers.find { it.user == user } ?: throw NotAvailableError("User is not a passenger of this drive offer")
        _passengers.remove(searcherUserStop)
    }

    @PostRemove
    private fun doAfterRemove() {
        _passengers.forEach {
            try {
                it.user.leftDriveOfferAsRequestingUser(this)
                it.user.leftDriveOfferAsPassenger(this)
            }
            catch (_: NotAvailableError) {}
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DriveOffer) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}