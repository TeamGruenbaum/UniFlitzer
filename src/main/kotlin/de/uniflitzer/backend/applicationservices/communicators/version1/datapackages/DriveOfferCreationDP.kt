package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern

@Schema(
    subTypes = [
        PublicDriveOfferCreationDP::class,
        CarpoolDriveOfferCreationDP::class
    ]
)
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class DriveOfferCreationDP(
    @field:Min(0) val carIndex: Int,
    @field:Min(1) @field:Max(8) val freeSeats: Int,
    val route: RouteDP,
    @field:Pattern(regexp = DateTimeFormat) val plannedDepartureTime: String?,
)