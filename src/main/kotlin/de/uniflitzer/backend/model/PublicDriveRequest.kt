package de.uniflitzer.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import java.time.ZonedDateTime

@Entity
class PublicDriveRequest : DriveRequest {
    @field:OneToMany
    var driveOffers: MutableList<PublicDriveOffer> = null!!

    constructor(requestingUser: User, route: Route, plannedDeparture: ZonedDateTime) : super(requestingUser, route, plannedDeparture) {
        this.driveOffers = mutableListOf()
    }
}