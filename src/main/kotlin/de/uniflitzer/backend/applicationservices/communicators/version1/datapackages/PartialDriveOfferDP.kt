package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import io.swagger.v3.oas.annotations.media.Schema
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
    @field:UUID val id: String,
    @field:UUID val driver: PartialUserDP,
    @field:Min(1) @field:Max(8) val freeSeats: Int,
    val route: RouteDP,
    @field:Min(1) @field:Max(8) val passengersCount: Int,
    @field:Pattern(regexp = DateTimeFormat) val plannedDepartureTime: String?
)