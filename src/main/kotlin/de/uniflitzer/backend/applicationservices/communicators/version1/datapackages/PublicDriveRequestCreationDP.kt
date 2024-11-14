package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

class PublicDriveRequestCreationDP private constructor(
    route: RouteDP,
    plannedDeparture: String?
) : DriveRequestCreationDP(route, plannedDeparture)