package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.PublicDriveRequest
import jakarta.validation.constraints.Size

class PartialPublicDriveRequestDP(
    id: String,
    containsFavoriteRequestingUser: Boolean,
    requestingUser: PartialUserDP,
    route: RouteDP,
    plannedDeparture: String?,
    @field:Size(min = 0) val driveOffersCount: Int
): PartialDriveRequestDP(id, containsFavoriteRequestingUser, requestingUser, route, plannedDeparture) {
    companion object {
        fun fromPublicDriveRequest(publicDriveRequest: PublicDriveRequest, containsFavoriteRequestingUser: Boolean): PartialPublicDriveRequestDP {
            return PartialPublicDriveRequestDP(
                publicDriveRequest.id.toString(),
                containsFavoriteRequestingUser,
                PartialUserDP.fromUser(publicDriveRequest.requestingUser),
                RouteDP.fromRoute(publicDriveRequest.route),
                publicDriveRequest.plannedDeparture?.toString(),
                publicDriveRequest.driveOffers.size
            )
        }
    }
}