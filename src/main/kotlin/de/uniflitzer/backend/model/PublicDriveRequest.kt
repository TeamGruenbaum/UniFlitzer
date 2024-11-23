package de.uniflitzer.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import java.time.ZonedDateTime

@Entity
class PublicDriveRequest : DriveRequest {
    @field:OneToMany
    private var _driveOffers: MutableList<PublicDriveOffer> = null!!
    val driveOffers: List<PublicDriveOffer> get() = _driveOffers

    constructor(requestingUser: User, route: Route, plannedDeparture: ZonedDateTime) : super(requestingUser, route, plannedDeparture) {
        this._driveOffers = mutableListOf()
    }
}