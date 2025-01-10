package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import org.gavaghan.geodesy.Ellipsoid
import org.gavaghan.geodesy.GeodeticCalculator
import org.gavaghan.geodesy.GlobalCoordinates

@Embeddable
class Coordinate(latitude: Double, longitude: Double) {
    final var latitude: Double = latitude
        private set(value) {
            require(value in (-90.0..90.0)) { "Property latitude with value $value is not between -90 and 90." }
            field = value
        }

    final var longitude: Double = longitude
        private set(value) {
            require(value in (-180.0..180.0)) { "Property longitude with value $value is not between -180 and 180." }
            field = value
        }

    infix fun distanceTo(coordinate: Coordinate): Meters {
        return Meters(
            GeodeticCalculator().calculateGeodeticCurve(
                Ellipsoid.WGS84,
                GlobalCoordinates(this.latitude, this.longitude),
                GlobalCoordinates(coordinate.latitude, coordinate.longitude)
            )
            .ellipsoidalDistance
        )
    }

    init {
        this.latitude = latitude
        this.longitude = longitude
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Coordinate) return false

        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false

        return true
    }

    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        return result
    }

    override fun toString(): String {
        return "Coordinate(latitude=$latitude, longitude=$longitude)"
    }
}
