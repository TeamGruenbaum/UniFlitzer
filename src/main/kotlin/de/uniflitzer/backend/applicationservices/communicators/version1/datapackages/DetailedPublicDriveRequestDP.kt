package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Size

class DetailedPublicDriveRequestDP(
    id: String,
    requestingUser: PartialUserDP,
    route: RouteDP,
    plannedDeparture: String?,
    @field:Size(min = 0) val driveOffers: List<PartialDriveOfferDP>
) : DetailedDriveRequestDP(id, requestingUser, route, plannedDeparture)