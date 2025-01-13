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

    @Throws(RepeatedActionError::class)
    fun addDriveOffer(driveOffer: PublicDriveOffer)
    {
        if(driveOffer in this.driveOffers) throw RepeatedActionError("Passed public drive offer has already been added.")
        this._driveOffers.add(driveOffer)
    }

    @Throws(NotAvailableError::class)
    fun rejectDriveOffer(driveOfferId: UUID)
    {
        val driveOffer: PublicDriveOffer = this.driveOffers.find { it.id == driveOfferId } ?: throw NotAvailableError("Public driver offer with passed id not found.")
        this._driveOffers.remove(driveOffer)
    }

    @Throws(NotAvailableError::class, ConflictingActionError::class, RepeatedActionError::class)
    fun acceptDriveOffer(driveOfferId: UUID)
    {
        val driveOffer: PublicDriveOffer = this.driveOffers.find { it.id == driveOfferId } ?: throw NotAvailableError("Public drive offer with passed id not found.")
        driveOffer.addPassenger(UserStop(this.requestingUser, this.route.start, this.route.destination))
    }
}