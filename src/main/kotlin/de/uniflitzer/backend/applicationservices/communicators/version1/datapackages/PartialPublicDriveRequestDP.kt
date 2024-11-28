package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.PublicDriveRequest
import jakarta.validation.constraints.Size

class PartialPublicDriveRequestDP(
    id: String,
    requestingUser: PartialUserDP,
    route: RouteDP,
    plannedDeparture: String?,
    @field:Size(min = 0) val driveOffersCount: Int
): PartialDriveRequestDP(id, requestingUser, route, plannedDeparture) {
    companion object {
        fun fromPublicDriveRequest(publicDriveRequest: PublicDriveRequest): PartialPublicDriveRequestDP {
            return PartialPublicDriveRequestDP(
                publicDriveRequest.id.toString(),
                PartialUserDP.fromUser(publicDriveRequest.requestingUser),
                RouteDP.fromRoute(publicDriveRequest.route),
                publicDriveRequest.plannedDeparture.toString(),
                publicDriveRequest.driveOffers.size
            )
        }
    }
}