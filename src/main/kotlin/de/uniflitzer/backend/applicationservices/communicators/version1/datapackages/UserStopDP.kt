package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages;

import de.uniflitzer.backend.model.UserStop
import jakarta.validation.Valid

data class UserStopDP private constructor(
    @field:Valid val user: PartialUserDP,
    @field:Valid val stop: PositionDP
) {
    companion object {
        fun fromUserStop(userStop: UserStop): UserStopDP =
            UserStopDP(
                PartialUserDP.fromUser(userStop.user),
                PositionDP.fromPosition(userStop.position)
            )
    }
}
