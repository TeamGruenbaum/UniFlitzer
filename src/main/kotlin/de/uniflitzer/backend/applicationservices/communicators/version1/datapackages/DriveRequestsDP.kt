package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class DriveRequestsDP(
    @field:Min(0) val maximumPage: Int,
    @field:Size(min = 0) val driveRequests: List<PartialDriveRequestDP>
)