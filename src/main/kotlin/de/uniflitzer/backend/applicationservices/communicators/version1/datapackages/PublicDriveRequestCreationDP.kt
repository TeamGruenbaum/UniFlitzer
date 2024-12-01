package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

class PublicDriveRequestCreationDP private constructor(
    route: RouteCreationDP,
    plannedDeparture: String?
) : DriveRequestCreationDP(route, plannedDeparture)