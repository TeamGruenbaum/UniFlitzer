package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages;

import de.uniflitzer.backend.model.UserStop

data class UserStopDP private constructor(
    val user: PartialUserDP,
    val stop: PositionDP
) {
    companion object {
        fun fromUserStop(userStop: UserStop): UserStopDP =
            UserStopDP(
                PartialUserDP.fromUser(userStop.user),
                PositionDP.fromPosition(userStop.position)
            )
    }
}
