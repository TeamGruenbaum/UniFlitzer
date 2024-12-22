package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.ScheduleTime

class PublicDriveOfferCreationDP private constructor(
    carIndex: Int,
    freeSeats: Int,
    route: RouteCreationDP,
    scheduleTime: ScheduleTimeDP?,
) : DriveOfferCreationDP(carIndex, freeSeats, route, scheduleTime)