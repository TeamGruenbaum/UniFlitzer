package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

class PublicDriveRequestCreationDP private constructor(
    route: RouteCreationDP,
    scheduleTime: ScheduleTimeDP?
) : DriveRequestCreationDP(route, scheduleTime)