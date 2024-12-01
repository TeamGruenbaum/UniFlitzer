package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.ConfirmableUserStop
import jakarta.validation.Valid

data class PartialConfirmableUserStop(
    @field:UUID val userId: String,
    @field:Valid val position: PositionDP,
    val waitingConfirmed: Boolean
) {
    companion object {
        fun fromConfirmableUserStop(confirmableUserStop: ConfirmableUserStop): PartialConfirmableUserStop {
            return PartialConfirmableUserStop(
                confirmableUserStop.user.id.toString(),
                PositionDP.fromPosition(confirmableUserStop.position),
                confirmableUserStop.waitingConfirmed
            )
        }
    }
}