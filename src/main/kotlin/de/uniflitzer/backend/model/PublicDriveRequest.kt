package de.uniflitzer.backend.model

import de.uniflitzer.backend.model.errors.ConflictingActionError
import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.model.errors.RepeatedActionError
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import java.util.UUID

@Entity
class PublicDriveRequest(requestingUser: User, route: Route, scheduleTime: ScheduleTime?) : DriveRequest(requestingUser, route, scheduleTime) {
    @field:OneToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    private var _driveOffers: MutableList<PublicDriveOffer> = mutableListOf()
    val driveOffers: List<PublicDriveOffer> get() = _driveOffers

    fun addDriveOffer(driveOffer: PublicDriveOffer)
    {
        this._driveOffers.add(driveOffer)
    }

    @Throws(NotAvailableError::class)
    fun rejectDriveOffer(driveOfferId: UUID)
    {
        val driveOffer: PublicDriveOffer = this.driveOffers.find { it.id == driveOfferId } ?: throw NotAvailableError("DriveOffer with id $driveOfferId not found.")
        this._driveOffers.remove(driveOffer)
    }

    @Throws(NotAvailableError::class, ConflictingActionError::class, RepeatedActionError::class)
    fun acceptDriveOffer(driveOfferId: UUID)
    {
        val driveOffer: PublicDriveOffer = this.driveOffers.find { it.id == driveOfferId } ?: throw NotAvailableError("DriveOffer with id $driveOfferId not found.")
        driveOffer.addPassenger(UserStop(this.requestingUser, this.route.start, this.route.destination))
    }
}