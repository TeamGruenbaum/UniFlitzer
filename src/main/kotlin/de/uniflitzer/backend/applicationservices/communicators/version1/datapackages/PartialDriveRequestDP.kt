package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import com.fasterxml.jackson.annotation.JsonTypeInfo
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
    @field:Valid val requestingUser: PartialUserDP,
    @field:Valid val route: RouteDP,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val plannedDeparture: String?
)