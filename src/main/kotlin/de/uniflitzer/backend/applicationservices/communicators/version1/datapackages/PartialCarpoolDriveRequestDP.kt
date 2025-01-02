package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.CarpoolDriveRequest

class PartialCarpoolDriveRequestDP(
    id: String,
    containsFavoriteRequestingUser: Boolean,
    requestingUser: PartialUserDP,
    route: PartialRouteDP,
    scheduleTime: ScheduleTimeDP?,
    @field:UUID val carpoolId: String
): PartialDriveRequestDP(id, containsFavoriteRequestingUser, requestingUser, route, scheduleTime) {
    companion object {
        fun fromCarpoolDriveRequest(carpoolDriveRequest: CarpoolDriveRequest, containsFavoriteRequestingUser: Boolean): PartialCarpoolDriveRequestDP {
            return PartialCarpoolDriveRequestDP(
                carpoolDriveRequest.id.toString(),
                containsFavoriteRequestingUser,
                PartialUserDP.fromUser(carpoolDriveRequest.requestingUser),
                PartialRouteDP.fromRoute(carpoolDriveRequest.route),
                carpoolDriveRequest.scheduleTime?.let { ScheduleTimeDP.fromScheduleTime(it) },
                carpoolDriveRequest.carpool.id.toString()
            )
        }
    }
}