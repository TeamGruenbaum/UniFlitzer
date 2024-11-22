package de.uniflitzer.backend.model

import jakarta.persistence.*

@Embeddable
class CompleteRoute{
    @AttributeOverrides(
        AttributeOverride(name = "latitude", column = Column(name = "start_latitude")),
        AttributeOverride(name = "longitude", column = Column(name = "start_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "start_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "start_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "start_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "start_city"))
    )
    var start: Position = null!!

    @AttributeOverrides(
        AttributeOverride(name = "latitude", column = Column(name = "destination_latitude")),
        AttributeOverride(name = "longitude", column = Column(name = "destination_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "destination_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "destination_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "destination_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "destination_city"))
    )
    var destination: Position = null!!

    @field:ElementCollection
    var userStops: MutableList<ConfirmableUserStop> = null!!

    constructor(start: Position, destination: Position, userStops: MutableList<ConfirmableUserStop>) {
        this.start = start
        this.destination = destination
        this.userStops = userStops
    }
}