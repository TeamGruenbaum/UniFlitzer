package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormatExample
import de.uniflitzer.backend.model.Coordinate
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class CoordinateDP private constructor(
    @field:Min(-90) @field:Max(90) @field:Schema(example = "50.32540777316511") val latitude: Double,
    @field:Min(-180) @field:Max(180) @field:Schema(example = "11.941037826886907") val longitude: Double
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