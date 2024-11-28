package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.Coordinate
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class CoordinateDP(
    @field:Min(-90) @field:Max(90) val latitude: Double,
    @field:Min(180) @field:Max(360) val longitude: Double
) {
    companion object {
        fun fromCoordinate(coordinate: Coordinate): CoordinateDP =
            CoordinateDP(
                coordinate.latitude,
                coordinate.longitude
            )
        }


    fun toCoordinate(): Coordinate =
        Coordinate(
            latitude,
            longitude
        )
}