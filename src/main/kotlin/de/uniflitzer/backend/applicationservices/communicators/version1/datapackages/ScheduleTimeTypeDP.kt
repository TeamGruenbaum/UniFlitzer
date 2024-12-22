package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.ScheduleTimeType

enum class ScheduleTimeTypeDP {
    Arrival, Departure;

    companion object {
        fun fromScheduleTimeType(scheduleTimeType: ScheduleTimeType): ScheduleTimeTypeDP {
            return when (scheduleTimeType) {
                ScheduleTimeType.Arrival -> Arrival
                ScheduleTimeType.Departure -> Departure
            }
        }
    }

    fun toScheduleTimeType(): ScheduleTimeType {
        return when (this) {
            Arrival -> ScheduleTimeType.Arrival
            Departure -> ScheduleTimeType.Departure
        }
    }
}