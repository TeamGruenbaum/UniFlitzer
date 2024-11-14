package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern

@Schema(
    subTypes = [
        DetailedPublicDriveRequestDP::class,
        DetailedCarpoolDriveRequestDP::class
    ]
)
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class DetailedDriveRequestDP(
    @field:UUID val id: String,
    val requestingUser: PartialUserDP,
    val route: RouteDP,
    @field:Pattern(regexp = DateTimeFormat) val plannedDeparture: String?
)