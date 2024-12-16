package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormatExample
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.CarpoolDriveOffer
import de.uniflitzer.backend.model.DriveOffer
import de.uniflitzer.backend.model.PublicDriveOffer
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern

@Schema(
    subTypes = [
        DetailedPublicDriveOfferDP::class,
        DetailedCarpoolDriveOfferDP::class
    ]
)
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class DetailedDriveOfferDP(
    val containsFavoriteDriver: Boolean,
    @field:UUID val id: String,
    @field:Valid val driver: PartialUserDP,
    @field:Valid val car: CarDP,
    @field:Min(1) @field:Max(8) val freeSeats: Int,
    @field:Valid val route: RouteDP,
    @field:Valid val passengers: List<UserStopDP>,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) open val plannedDepartureTime: String?
) {
    companion object {
        fun fromDriveOffer(driveOffer: DriveOffer, containsFavoriteDriver: Boolean): DetailedDriveOfferDP {
            return when (driveOffer) {
                is PublicDriveOffer ->
                    DetailedPublicDriveOfferDP(
                        containsFavoriteDriver,
                        driveOffer.id.toString(),
                        PartialUserDP.fromUser(driveOffer.driver),
                        CarDP.fromCar(driveOffer.car),
                        driveOffer.freeSeats.value.toInt(),
                        RouteDP.fromRoute(driveOffer.route),
                        driveOffer.passengers.map { UserStopDP.fromUserStop(it) },
                        driveOffer.plannedDeparture.toString(),
                        driveOffer.requestingUsers.map { UserStopDP.fromUserStop(it) }
                    )
                is CarpoolDriveOffer -> TODO()
                else -> throw IllegalArgumentException("Unknown DriveOffer type")
            }
        }
    }
}