package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern

@Schema(
    subTypes = [
        CarpoolDriveRequestCreationDP::class,
        PublicDriveRequestCreationDP::class
    ]
)
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class DriveRequestCreationDP(
    val route: RouteDP,
    @field:Pattern(regexp = DateTimeFormat) val plannedDeparture: String?
)