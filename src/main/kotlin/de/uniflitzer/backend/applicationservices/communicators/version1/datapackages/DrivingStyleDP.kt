package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.DrivingStyle

enum class DrivingStyleDP {
    Defensive, Relaxed, Normal, Passionate;

    fun toDrivingStyle(): DrivingStyle =
        when (this) {
            DrivingStyleDP.Defensive -> DrivingStyle.Defensive
            DrivingStyleDP.Relaxed -> DrivingStyle.Relaxed
            DrivingStyleDP.Normal -> DrivingStyle.Normal
            DrivingStyleDP.Passionate -> DrivingStyle.Passionate
        }

    companion object {
        fun fromDrivingStyle(drivingStyle: DrivingStyle?): DrivingStyleDP? =
            when (drivingStyle) {
                DrivingStyle.Defensive -> Defensive
                DrivingStyle.Relaxed -> Relaxed
                DrivingStyle.Normal -> Normal
                DrivingStyle.Passionate -> Passionate
                null -> null
            }
    }
}