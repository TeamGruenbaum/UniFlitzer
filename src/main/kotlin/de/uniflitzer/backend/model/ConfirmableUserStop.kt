package de.uniflitzer.backend.model

import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.model.errors.RepeatedActionError
import jakarta.persistence.*
import kotlin.jvm.Throws

@Embeddable
class ConfirmableUserStop(user: User, start: Position, destination: Position, waitingConfirmed: Boolean){
    @field:ManyToOne(fetch = FetchType.LAZY)
    final var user: User = user
        private set

    @AttributeOverrides(
        AttributeOverride(name = "coordinate.latitude", column = Column(name = "confirmableuserstop_start_coordinate_latitude")),
        AttributeOverride(name = "coordinate.longitude", column = Column(name = "confirmableuserstop_start_coordinate_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "confirmableuserstop_start_nearestAddress_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "confirmableuserstop_start_nearestAddress_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "confirmableuserstop_start_nearestAddress_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "confirmableuserstop_start_nearestAddress_city"))
    )
    final var start: Position = start
        private set

    @AttributeOverrides(
        AttributeOverride(name = "coordinate.latitude", column = Column(name = "confirmableuserstop_destination_coordinate_latitude")),
        AttributeOverride(name = "coordinate.longitude", column = Column(name = "confirmableuserstop_destination_coordinate_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "confirmableuserstop_destination_nearestAddress_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "confirmableuserstop_destination_nearestAddress_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "confirmableuserstop_destination_nearestAddress_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "confirmableuserstop_destination_nearestAddress_city"))
    )
    final var destination: Position = destination
        private set

    final var waitingConfirmed: Boolean = waitingConfirmed
        private set

    init {
        this.user = user
        this.start = start
        this.destination = destination
        this.waitingConfirmed = waitingConfirmed
    }

    @Throws(RepeatedActionError::class)
    fun confirm()
    {
        if(waitingConfirmed) throw RepeatedActionError("Confirmable user stop is already confirmed.")
        this.waitingConfirmed = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConfirmableUserStop) return false

        if (waitingConfirmed != other.waitingConfirmed) return false
        if (user != other.user) return false
        if (start != other.start) return false
        if (destination != other.destination) return false

        return true
    }

    override fun hashCode(): Int {
        var result = waitingConfirmed.hashCode()
        result = 31 * result + user.hashCode()
        result = 31 * result + start.hashCode()
        result = 31 * result + destination.hashCode()
        return result
    }
}