package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import org.gavaghan.geodesy.Ellipsoid
import org.gavaghan.geodesy.GeodeticCalculator
import org.gavaghan.geodesy.GlobalCoordinates

@Embeddable
class Coordinate(latitude: Double, longitude: Double) {
    final var latitude: Double = latitude
        private set(value) {
            require(value in (-90.0..90.0))
            field = value
        }

    final var longitude: Double = longitude
        private set(value) {
            require(value in (-180.0..180.0))
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
}
