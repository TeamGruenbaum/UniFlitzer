package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

@Schema(
    subTypes = [
        DetailedPublicDriveRequestDP::class,
        DetailedCarpoolDriveRequestDP::class
    ]
)
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class DetailedDriveRequestDP(
    @field:UUID val id: String,
    val containsFavoriteRequestingUser: Boolean,
    @field:Valid val requestingUser: PartialUserDP,
    @field:Valid val route: DetailedRouteDP,
    @field:Valid val scheduleTime: ScheduleTimeDP?
)