package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.Carpool
import de.uniflitzer.backend.model.User
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class DetailedCarpoolDP private constructor(
    @field:UUID val id: String,
    @field:Size(min = 2, max = 100) val name: String,
    @field:Valid val users: List<PartialUserDP>,
    @field:Valid @field:Size(min = 0) val driveRequests: List<PartialDriveRequestDP>,
    @field:Valid @field:Size(min = 0) val driveOffers: List<PartialDriveOfferDP>,
    @field:Valid @field:Size(min = 0) val drives: List<PartialDriveDP>,
    @field:Valid val sentInvites: List<PartialUserDP>
) {
    companion object {
        fun fromCarpool(carpool: Carpool, favoriteUsers: List<User>): DetailedCarpoolDP {
            return DetailedCarpoolDP(
                carpool.id.toString(),
                carpool.name.value,
                carpool.users.map { PartialUserDP.fromUser(it) },
                carpool.driveRequests.map { PartialDriveRequestDP.fromDriveRequest(it, it.requestingUser in favoriteUsers) },
                carpool.driveOffers.map { PartialDriveOfferDP.fromDriveOffer(it, it.driver in favoriteUsers) },
                carpool.drives.map { PartialDriveDP.fromDrive(it) },
                carpool.sentInvites.map { PartialUserDP.fromUser(it) }
            )
        }
    }
}