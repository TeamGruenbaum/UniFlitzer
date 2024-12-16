package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.Role

enum class RoleDP {
    Driver, Passenger;

    fun toRole(): Role =
        when(this) {
            RoleDP.Driver -> Role.Driver
            RoleDP.Passenger -> Role.Passenger
        }

    companion object {
        fun fromRole(role: Role): RoleDP =
            when(role) {
                Role.Driver -> RoleDP.Driver
                Role.Passenger -> RoleDP.Passenger
            }
    }
}