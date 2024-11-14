package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID

class PartialCarpoolDriveRequestDP(
    id: String,
    requestingUser: PartialUserDP,
    route: RouteDP,
    plannedDeparture: String?,
    @field:UUID val carpoolId: String
): PartialDriveRequestDP(id, requestingUser, route, plannedDeparture)