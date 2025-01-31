package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.PublicDriveRequest
import jakarta.validation.constraints.Size

class PartialPublicDriveRequestDP private constructor(
    id: String,
    containsFavoriteRequestingUser: Boolean,
    requestingUser: PartialUserDP,
    route: PartialRouteDP,
    scheduleTime: ScheduleTimeDP?,
    @field:Size(min = 0) val driveOffersCount: Int
): PartialDriveRequestDP(id, containsFavoriteRequestingUser, requestingUser, route, scheduleTime) {
    companion object {
        fun fromPublicDriveRequest(publicDriveRequest: PublicDriveRequest, containsFavoriteRequestingUser: Boolean): PartialPublicDriveRequestDP {
            return PartialPublicDriveRequestDP(
                publicDriveRequest.id.toString(),
                containsFavoriteRequestingUser,
                PartialUserDP.fromUser(publicDriveRequest.requestingUser),
                PartialRouteDP.fromRoute(publicDriveRequest.route),
                publicDriveRequest.scheduleTime?.let { ScheduleTimeDP.fromScheduleTime(it) },
                publicDriveRequest.driveOffers.size
            )
        }
    }
}