package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.Valid

class DetailedCarpoolDriveRequestDP(
    id: String,
    requestingUser: PartialUserDP,
    route: RouteDP,
    plannedDeparture: String?,
    @field:Valid val carpool: PartialCarpoolDP
) : DetailedDriveRequestDP(id, requestingUser, route, plannedDeparture)