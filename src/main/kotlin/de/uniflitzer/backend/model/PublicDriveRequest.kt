package de.uniflitzer.backend.model

import de.uniflitzer.backend.model.errors.EntityNotFoundError
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import java.time.ZonedDateTime
import java.util.UUID

@Entity
class PublicDriveRequest(requestingUser: User, route: Route, plannedDeparture: ZonedDateTime?) : DriveRequest(requestingUser, route, plannedDeparture) {
    @field:OneToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    private var _driveOffers: MutableList<PublicDriveOffer> = mutableListOf()
    val driveOffers: List<PublicDriveOffer> get() = _driveOffers

    fun addDriveOffer(driveOffer: PublicDriveOffer)
    {
        this._driveOffers.add(driveOffer)
    }

    @Throws(EntityNotFoundError::class)
    fun rejectDriveOffer(driveOfferId: UUID)
    {
        val driveOffer: PublicDriveOffer = this.driveOffers.find { it.id == driveOfferId } ?: throw EntityNotFoundError("DriveOffer with id $driveOfferId not found.")
        this._driveOffers.remove(driveOffer)
    }

    @Throws(EntityNotFoundError::class)
    fun acceptDriveOffer(driveOfferId: UUID)
    {
        val driveOffer: PublicDriveOffer = this.driveOffers.find { it.id == driveOfferId } ?: throw EntityNotFoundError("DriveOffer with id $driveOfferId not found.")
        driveOffer.addPassenger(UserStop(this.requestingUser, this.route.start, this.route.destination))
    }
}