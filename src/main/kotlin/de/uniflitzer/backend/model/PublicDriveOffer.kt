package de.uniflitzer.backend.model

import de.uniflitzer.backend.model.errors.MissingActionError
import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.model.errors.RepeatedActionError
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.jvm.Throws

@Entity
class PublicDriveOffer(driver: User, car: Car, freeSeats: Seats, route: Route, plannedDeparture: ZonedDateTime?) : DriveOffer(driver, car, freeSeats, route, plannedDeparture) {
    @field:ElementCollection
    private var _requestingUsers: MutableList<UserStop> = mutableListOf()
    val requestingUsers: List<UserStop> get() = _requestingUsers

    @Throws(NotAvailableError::class, RepeatedActionError::class)
    fun addRequestFromUser(user: User, start: Position, destination: Position) {
        if(requestingUsers.any { it.user.id == user.id }) throw RepeatedActionError("User with id ${user.id} already requested a seat for this public drive offer with id $id.")

        super.addPassenger(UserStop(user, start, destination))
    }

    @Throws(MissingActionError::class, NotAvailableError::class)
    fun acceptRequestFromUser(userId: UUID) {
        val userStop = requestingUsers.find { it.user.id == userId } ?: throw MissingActionError("User with id $userId did not request a seat for this public drive offer with id $id.")
        if (passengers.size.toUInt() >= freeSeats.value) throw NotAvailableError("No free seats left for this public drive offer with id $id.")

        super.addPassenger(userStop)
        _requestingUsers.remove(userStop)
    }

    @Throws(MissingActionError::class)
    fun rejectRequestFromUser(userId: UUID) {
        val userStop = requestingUsers.find { it.user.id == userId } ?: throw MissingActionError("User with id $userId did not request a seat for this public drive offer with id $id.")
        _requestingUsers.remove(userStop)
    }
}