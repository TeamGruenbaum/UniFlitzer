package de.uniflitzer.backend.model

import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.model.errors.RepeatedActionError
import jakarta.persistence.*
import java.time.Duration
import java.util.*

@Embeddable
class CompleteRoute(start: Position, destination: Position, userStops: List<ConfirmableUserStop>, duration: Duration, polyline: GeoJsonLineString) {
    @AttributeOverrides(
        AttributeOverride(name = "coordinate.latitude", column = Column(name = "completeroute_start_coordinate_latitude")),
        AttributeOverride(name = "coordinate.longitude", column = Column(name = "completeroute_start_coordinate_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "completeroute_start_nearestAddress_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "completeroute_start_nearestAddress_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "completeroute_start_nearestAddress_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "completeroute_start_nearestAddress_city"))
    )
    final var start: Position = start
        private set

    @AttributeOverrides(
        AttributeOverride(name = "coordinate.latitude", column = Column(name = "completeroute_destination_coordinate_latitude")),
        AttributeOverride(name = "coordinate.longitude", column = Column(name = "completeroute_destination_coordinate_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "completeroute_destination_nearestAddress_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "completeroute_destination_nearestAddress_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "completeroute_destination_nearestAddress_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "completeroute_destination_nearestAddress_city"))
    )
    final var destination: Position = destination
        private set

    @field:ElementCollection
    private final var _userStops: MutableList<ConfirmableUserStop> = userStops.toMutableList()
    final val userStops: List<ConfirmableUserStop> get() = _userStops

    final var duration: Duration = duration
        private set

    final var polyline: GeoJsonLineString = polyline
        private set

    init {
        this.start = start
        this.destination = destination
        this._userStops = userStops.toMutableList()
        this.duration = duration
        this.polyline = polyline
    }

    @Throws(NotAvailableError::class, RepeatedActionError::class)
    fun confirmUserStop(userStopUserId: UUID)
    {
        if(userStops.none { it.user.id == userStopUserId }) throw NotAvailableError("No user stop for user with id $userStopUserId available.")
        userStops.first { it.user.id == userStopUserId }.confirm()
    }

    @Throws(NotAvailableError::class)
    fun cancelUserStop(userStopUserId: UUID)
    {
        if(userStops.none { it.user.id == userStopUserId }) throw NotAvailableError("No user stop for user with id $userStopUserId available.")
        _userStops.remove(userStops.first { it.user.id == userStopUserId })
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompleteRoute) return false

        if (start != other.start) return false
        if (destination != other.destination) return false
        if (_userStops != other._userStops) return false
        if (duration != other.duration) return false
        if (polyline != other.polyline) return false
        if (userStops != other.userStops) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + destination.hashCode()
        result = 31 * result + _userStops.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + polyline.hashCode()
        result = 31 * result + userStops.hashCode()
        return result
    }
}