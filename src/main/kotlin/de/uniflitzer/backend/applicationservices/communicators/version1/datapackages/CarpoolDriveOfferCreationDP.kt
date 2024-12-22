package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.ScheduleTime

class CarpoolDriveOfferCreationDP private constructor(
    carIndex: Int,
    freeSeats: Int,
    route: RouteCreationDP,
    scheduleTime: ScheduleTimeDP?,
    @field:UUID val carpoolId: String
) : DriveOfferCreationDP(carIndex, freeSeats, route, scheduleTime)