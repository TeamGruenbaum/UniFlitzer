package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
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
    @field:UUID val id: String,
    val driver: PartialUserDP,
    val car: CarDP,
    @field:Min(1) @field:Max(8) val freeSeats: Int,
    val route: RouteDP,
    val passengers: List<UserStopDP>,
    @field:Pattern(regexp = DateTimeFormat) open val plannedDepartureTime: String
)