package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Size

class PartialPublicDriveRequestDP(
    id: String,
    requestingUser: PartialUserDP,
    route: RouteDP,
    plannedDeparture: String?,
    @field:Size(min = 0) val driveOffersCount: Int
): PartialDriveRequestDP(id, requestingUser, route, plannedDeparture)