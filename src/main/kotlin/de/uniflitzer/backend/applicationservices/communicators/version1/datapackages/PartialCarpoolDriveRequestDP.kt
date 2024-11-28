package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.CarpoolDriveRequest

class PartialCarpoolDriveRequestDP(
    id: String,
    requestingUser: PartialUserDP,
    route: RouteDP,
    plannedDeparture: String?,
    @field:UUID val carpoolId: String
): PartialDriveRequestDP(id, requestingUser, route, plannedDeparture) {
    companion object {
        fun fromCarpoolDriveRequest(carpoolDriveRequest: CarpoolDriveRequest): PartialCarpoolDriveRequestDP {
            return PartialCarpoolDriveRequestDP(
                carpoolDriveRequest.id.toString(),
                PartialUserDP.fromUser(carpoolDriveRequest.requestingUser),
                RouteDP.fromRoute(carpoolDriveRequest.route),
                carpoolDriveRequest.plannedDeparture.toString(),
                carpoolDriveRequest.carpool.id.toString()
            )
        }
    }
}