package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormatExample
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.CarpoolDriveOffer
import de.uniflitzer.backend.model.DriveOffer
import de.uniflitzer.backend.model.PublicDriveOffer
import de.uniflitzer.backend.model.Seats
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern

@Schema(
    subTypes = [
        PartialPublicDriveOfferDP::class,
        PartialCarpoolDriveOfferDP::class
    ]
)
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class PartialDriveOfferDP(
    val containsFavoriteDrive: Boolean,
    @field:UUID val id: String,
    @field:Valid val driver: PartialUserDP,
    @field:Min(1) @field:Max(8) val freeSeats: Int,
    @field:Valid val route: RouteDP,
    @field:Min(1) @field:Max(8) val passengersCount: Int,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val plannedDepartureTime: String?
) {
    companion object {
        fun fromDriveOffer(driveOffer: DriveOffer, containsFavoriteDriver: Boolean): PartialDriveOfferDP {
            return when (driveOffer) {
                is PublicDriveOffer ->
                    PartialPublicDriveOfferDP(
                        containsFavoriteDriver,
                        driveOffer.id.toString(),
                        PartialUserDP.fromUser(driveOffer.driver),
                        driveOffer.freeSeats.value.toInt(),
                        RouteDP.fromRoute(driveOffer.route),
                        driveOffer.passengers.size,
                        driveOffer.plannedDeparture?.toString(),
                        driveOffer.requestingUsers.map { it.user.id.toString() }
                    )
                is CarpoolDriveOffer -> TODO()
                else -> throw IllegalArgumentException("Unknown DriveOffer type")
            }
        }
    }
}