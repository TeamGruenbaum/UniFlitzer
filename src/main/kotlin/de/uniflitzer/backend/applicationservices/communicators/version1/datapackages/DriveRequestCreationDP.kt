package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormatExample
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern

@Schema(
    subTypes = [
        CarpoolDriveRequestCreationDP::class,
        PublicDriveRequestCreationDP::class
    ]
)
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class DriveRequestCreationDP(
    @field:Valid val route: RouteCreationDP,
    @field:Valid val scheduleTime: ScheduleTimeDP?
)