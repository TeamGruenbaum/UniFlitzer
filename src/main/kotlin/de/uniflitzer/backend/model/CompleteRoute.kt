package de.uniflitzer.backend.model

import jakarta.persistence.*

@Embeddable
class CompleteRoute(start: Position, destination: Position, userStops: MutableList<ConfirmableUserStop>){
    @AttributeOverrides(
        AttributeOverride(name = "latitude", column = Column(name = "start_latitude")),
        AttributeOverride(name = "longitude", column = Column(name = "start_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "start_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "start_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "start_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "start_city"))
    )
    final var start: Position = start
        private set

    @AttributeOverrides(
        AttributeOverride(name = "latitude", column = Column(name = "destination_latitude")),
        AttributeOverride(name = "longitude", column = Column(name = "destination_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "destination_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "destination_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "destination_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "destination_city"))
    )
    final var destination: Position = destination
        private set

    @field:ElementCollection
    private final var _userStops: MutableList<ConfirmableUserStop> = userStops
    final val userStops: List<ConfirmableUserStop> get() = _userStops

    init {
        this.start = start
        this.destination = destination
        this._userStops = userStops
    }
}