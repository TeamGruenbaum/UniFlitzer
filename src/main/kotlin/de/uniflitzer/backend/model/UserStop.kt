package de.uniflitzer.backend.model

import jakarta.persistence.*

@Embeddable
class UserStop(user: User, start: Position, destination: Position) {
    @field:ManyToOne
    var user: User = user

    @AttributeOverrides(
        AttributeOverride(name = "coordinate.latitude", column = Column(name = "userstop_start_coordinate_latitude")),
        AttributeOverride(name = "coordinate.longitude", column = Column(name = "userstop_start_coordinate_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "userstop_start_nearestAddress_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "userstop_start_nearestAddress_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "userstop_start_nearestAddress_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "userstop_start_nearestAddress_city"))
    )
    var start: Position = start

    @AttributeOverrides(
        AttributeOverride(name = "coordinate.latitude", column = Column(name = "userstop_destination_coordinate_latitude")),
        AttributeOverride(name = "coordinate.longitude", column = Column(name = "userstop_destination_coordinate_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "userstop_destination_nearestAddress_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "userstop_destination_nearestAddress_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "userstop_destination_nearestAddress_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "userstop_destination_nearestAddress_city"))
    )
    var destination: Position = destination

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserStop) return false

        if (user != other.user) return false
        if (start != other.start) return false
        if (destination != other.destination) return false

        return true
    }

    override fun hashCode(): Int {
        var result = user.hashCode()
        result = 31 * result + start.hashCode()
        result = 31 * result + destination.hashCode()
        return result
    }
}