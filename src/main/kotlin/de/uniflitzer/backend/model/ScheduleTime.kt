package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import java.time.ZonedDateTime

@Embeddable
data class ScheduleTime(val time: ZonedDateTime, val type: ScheduleTimeType)