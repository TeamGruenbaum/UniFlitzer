package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.InternalServerError
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormatExample
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.CarpoolDriveRequest
import de.uniflitzer.backend.model.DriveRequest
import de.uniflitzer.backend.model.PublicDriveRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern

@Schema(
    subTypes = [
        PartialPublicDriveRequestDP::class,
        PartialCarpoolDriveRequestDP::class
    ]
)
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class PartialDriveRequestDP(
    @field:UUID val id: String,
    val containsFavoriteRequestingUser: Boolean,
    @field:Valid val requestingUser: PartialUserDP,
    @field:Valid val route: RouteDP,
    @field:Valid val scheduleTimeDP: ScheduleTimeDP?
) {
    companion object {
        fun fromDriveRequest(driveRequest: DriveRequest, containsFavoriteRequestingUser: Boolean): PartialDriveRequestDP {
            return when (driveRequest) {
                is PublicDriveRequest -> PartialPublicDriveRequestDP.fromPublicDriveRequest(driveRequest, containsFavoriteRequestingUser)
                is CarpoolDriveRequest -> PartialCarpoolDriveRequestDP.fromCarpoolDriveRequest(driveRequest, containsFavoriteRequestingUser)
                else -> throw InternalServerError(ErrorDP("DriveRequest is neither a PublicDriveRequest nor a CarpoolDriveRequest."))
            }
        }
    }
}