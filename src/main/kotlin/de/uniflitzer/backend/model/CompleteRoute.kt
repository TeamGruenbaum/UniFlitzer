package de.uniflitzer.backend.model

import de.uniflitzer.backend.model.errors.NotAvailableError
import jakarta.persistence.*
import java.util.*

@Embeddable
class CompleteRoute(start: Position, destination: Position, userStops: List<ConfirmableUserStop>, polyline: GeoJsonLineString) {
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

    final var polyline: GeoJsonLineString = polyline
        private set

    init {
        this.start = start
        this.destination = destination
        this._userStops = userStops.toMutableList()
        this.polyline = polyline
    }
    
    @Throws(NotAvailableError::class)
    fun confirmUserStop(userId: UUID)
    {
        if(userStops.none { it.user.id == userId }) throw NotAvailableError("No user stop for user with id $userId available.")
        userStops.first { it.user.id == userId }.confirm()
    }

    @Throws(NotAvailableError::class)
    fun cancelUserStop(userId: UUID)
    {
        if(userStops.none { it.user.id == userId }) throw NotAvailableError("No user stop for user with id $userId available.")
        _userStops.remove(userStops.first { it.user.id == userId })
    }
}