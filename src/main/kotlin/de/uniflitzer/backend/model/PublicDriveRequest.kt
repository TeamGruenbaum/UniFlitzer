package de.uniflitzer.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import java.time.ZonedDateTime

@Entity
class PublicDriveRequest(requestingUser: User, route: Route, plannedDeparture: ZonedDateTime) : DriveRequest(requestingUser, route, plannedDeparture) {
    @field:OneToMany
    private var _driveOffers: MutableList<PublicDriveOffer> = mutableListOf()
    val driveOffers: List<PublicDriveOffer> get() = _driveOffers
}