package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID

class CarpoolDriveRequestCreationDP private constructor(
    route: RouteCreationDP,
    plannedDeparture: String?,
    @field:UUID val carpoolId: String
) : DriveRequestCreationDP(route, plannedDeparture)