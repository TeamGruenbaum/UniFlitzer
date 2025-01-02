package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.Valid
import jakarta.validation.constraints.Size

class DetailedPublicDriveRequestDP(
    id: String,
    containsFavoriteRequestingUser: Boolean,
    requestingUser: PartialUserDP,
    route: PartialRouteDP,
    scheduleTime: ScheduleTimeDP?,
    @field:Valid @field:Size(min = 0) val driveOffers: List<PartialDriveOfferDP>
) : DetailedDriveRequestDP(id, containsFavoriteRequestingUser, requestingUser, route, scheduleTime)