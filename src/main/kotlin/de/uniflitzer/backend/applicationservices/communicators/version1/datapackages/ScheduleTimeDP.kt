package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormatExample
import de.uniflitzer.backend.model.ScheduleTime
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import java.time.ZonedDateTime

data class ScheduleTimeDP private constructor(
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val time: String,
    @field:Valid val type: ScheduleTimeTypeDP
) {
    companion object {
        fun fromScheduleTime(scheduleTime: ScheduleTime): ScheduleTimeDP {
            return ScheduleTimeDP(
                scheduleTime.time.toString(),
                ScheduleTimeTypeDP.fromScheduleTimeType(scheduleTime.type)
            )
        }
    }

    fun toScheduleTime(): ScheduleTime {
        return ScheduleTime(
            ZonedDateTime.parse(time),
            type.toScheduleTimeType()
        )
    }
}