package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.CarpoolDriveOffer
import de.uniflitzer.backend.model.DriveOffer
import de.uniflitzer.backend.model.PublicDriveOffer
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

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
    @field:Valid val route: PartialRouteDP,
    @field:Min(1) @field:Max(8) val passengersCount: Int,
    @field:Valid val scheduleTime: ScheduleTimeDP?
) {
    companion object {
        fun fromDriveOffer(driveOffer: DriveOffer, containsFavoriteDriver: Boolean): PartialDriveOfferDP {
            return when (driveOffer) {
                is PublicDriveOffer -> PartialPublicDriveOfferDP.fromPublicDriveOffer(driveOffer, containsFavoriteDriver)
                is CarpoolDriveOffer -> PartialCarpoolDriveOfferDP.fromCarpoolDriveOffer(driveOffer, containsFavoriteDriver)
                else -> throw IllegalArgumentException("Unknown DriveOffer type")
            }
        }
    }
}