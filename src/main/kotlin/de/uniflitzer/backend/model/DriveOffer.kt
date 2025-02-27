package de.uniflitzer.backend.model

import de.uniflitzer.backend.model.errors.ConflictingActionError
import de.uniflitzer.backend.model.errors.MissingActionError
import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.model.errors.RepeatedActionError
import jakarta.persistence.*
import java.util.UUID

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "discriminator_type")
// Should be sealed, but Hibernate 6 does not support sealed classes
class DriveOffer(driver: User, car: Car, freeSeats: Seats, route: Route, scheduleTime: ScheduleTime?) {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @field:ManyToOne
    var driver: User = driver

    var car: Car = car

    var freeSeats: Seats = freeSeats

    var route: Route = route

    @field:ElementCollection
    private var _passengers: MutableList<UserStop> = mutableListOf() //Manual bidirectional relationship 2
    val passengers: List<UserStop> get() = _passengers

    @AttributeOverrides(
        AttributeOverride(name = "type", column = Column(name = "schedule_time_type"))
    )
    var scheduleTime: ScheduleTime? = scheduleTime

    init {
        this.driver = driver
        this.car = car
        this.freeSeats = freeSeats
        this.route = route
        this.scheduleTime = scheduleTime
    }

    @Throws(ConflictingActionError::class, NotAvailableError::class, RepeatedActionError::class)
    fun addPassenger(userStop: UserStop) {
        if (userStop.user in _passengers.map { it.user }) throw RepeatedActionError("User in passed user stop is already a passenger.")
        if (userStop.user == driver) throw ConflictingActionError("User in passed user stop is already the driver.")
        if (freeSeats.value == _passengers.count().toUInt()) throw NotAvailableError("No more seats available.")

        _passengers.add(userStop)
    }

    @Throws(NotAvailableError::class)
    fun removePassenger(user: User) {
        val searcherUserStop: UserStop = _passengers.find { it.user == user } ?: throw NotAvailableError("Passed user is not a passenger.")
        _passengers.remove(searcherUserStop)
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