package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID

class DetailedCarpoolDriveRequestDP(
    id: String,
    requestingUser: PartialUserDP,
    route: RouteDP,
    plannedDeparture: String?,
    @field:UUID val carpool: PartialCarpoolDP
) : DetailedDriveRequestDP(id, requestingUser, route, plannedDeparture)