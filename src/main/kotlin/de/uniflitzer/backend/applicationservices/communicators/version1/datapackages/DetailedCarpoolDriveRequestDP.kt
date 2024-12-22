package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.Valid

class DetailedCarpoolDriveRequestDP(
    id: String,
    containsFavoriteRequestingUser: Boolean,
    requestingUser: PartialUserDP,
    route: RouteDP,
    scheduleTime: ScheduleTimeDP?,
    @field:Valid val carpool: PartialCarpoolDP
) : DetailedDriveRequestDP(id, containsFavoriteRequestingUser, requestingUser, route, scheduleTime)