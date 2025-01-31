package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormatExample
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.Drive
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern

data class PartialDriveDP private constructor(
    @field:UUID val id: String,
    @field:Valid val driver: PartialUserDP,
    @field:Min(1) @field:Max(8) val passengersCount: Int,
    @field:Valid val route: CompleteRouteDP,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val plannedDeparture: String,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val actualDeparture: String?,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val plannedArrival: String,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val actualArrival: String?,
    val isCancelled: Boolean
) {
    companion object {
        fun fromDrive(drive: Drive): PartialDriveDP {
            return PartialDriveDP(
                drive.id.toString(),
                PartialUserDP.fromUser(drive.driver),
                drive.passengers.size,
                CompleteRouteDP.fromCompleteRoute(drive.route),
                drive.plannedDeparture.toString(),
                drive.actualDeparture?.toString(),
                drive.plannedArrival.toString(),
                drive.actualArrival?.toString(),
                drive.isCancelled
            )
        }
    }
}