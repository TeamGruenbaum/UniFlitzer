package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormatExample
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.Drive
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern

data class DetailedDriveDP(
    @field:UUID val id: String,
    @field:Valid val driver: PartialUserDP,
    @field:Valid val car: CarDP,
    @field:Valid val passengers: List<PartialUserDP>,
    @field:Valid val route: CompleteRouteDP,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val plannedDeparture: String,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val actualDeparture: String?,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val plannedArrival: String,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val actualArrival: String?,
    val isCancelled: Boolean
) {
    companion object {
        fun fromDrive(drive: Drive): DetailedDriveDP {
            return DetailedDriveDP(
                drive.id.toString(),
                PartialUserDP.fromUser(drive.driver),
                CarDP.fromCar(drive.car),
                drive.passengers.map { PartialUserDP.fromUser(it) },
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