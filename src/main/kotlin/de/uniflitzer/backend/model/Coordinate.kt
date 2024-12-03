package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

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

    //Copied from: https://www.baeldung.com/java-find-distance-between-points#calculate-the-distance-using-vincentys-formula
    infix fun distanceTo(coordinate: Coordinate): Double {
        val SEMI_MAJOR_AXIS_MT = 6378137.0
        val SEMI_MINOR_AXIS_MT = 6356752.314245
        val FLATTENING = 1 / 298.257223563
        val ERROR_TOLERANCE = 1e-12

        val U1: Double = atan((1 - FLATTENING) * tan(Math.toRadians(this.latitude)))
        val U2: Double = atan((1 - FLATTENING) * tan(Math.toRadians(coordinate.latitude)))

        val sinU1: Double = sin(U1)
        val cosU1: Double = cos(U1)
        val sinU2: Double = sin(U2)
        val cosU2: Double = cos(U2)

        var longitudeDifference = Math.toRadians(coordinate.longitude - this.longitude)
        var previousLongitudeDifference: Double

        var sinSigma: Double
        var cosSigma: Double
        var sigma: Double
        var sinAlpha: Double
        var cosSqAlpha: Double
        var cos2SigmaM: Double

        do {
            sinSigma = sqrt(
                (cosU2 * sin(longitudeDifference)).pow(2) + (cosU1 * sinU2 - sinU1 * cosU2 * cos(
                    longitudeDifference
                )).pow(2)
            )
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cos(longitudeDifference)
            sigma = atan2(sinSigma, cosSigma)
            sinAlpha = cosU1 * cosU2 * sin(longitudeDifference) / sinSigma
            cosSqAlpha = 1 - sinAlpha.pow(2)
            cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha
            if (java.lang.Double.isNaN(cos2SigmaM)) {
                cos2SigmaM = 0.0
            }
            previousLongitudeDifference = longitudeDifference
            val C = FLATTENING / 16 * cosSqAlpha * (4 + FLATTENING * (4 - 3 * cosSqAlpha))
            longitudeDifference =
                Math.toRadians(coordinate.longitude - this.longitude) + (1 - C) * FLATTENING * sinAlpha *
                        (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM.pow(
                            2
                        ))))
        } while (abs(longitudeDifference - previousLongitudeDifference) > ERROR_TOLERANCE)

        val uSq: Double =
            cosSqAlpha * (SEMI_MAJOR_AXIS_MT.pow(2) - SEMI_MINOR_AXIS_MT.pow(2)) / SEMI_MINOR_AXIS_MT.pow(
                2
            )

        val A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)))
        val B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)))

        val deltaSigma: Double =
            B * sinSigma * (cos2SigmaM + B / 4 * (cosSigma * (-1 + 2 * cos2SigmaM.pow(2))
                    - B / 6 * cos2SigmaM * (-3 + 4 * sinSigma.pow(2)) * (-3 + 4 * cos2SigmaM.pow(2))))

        val distanceMt = SEMI_MINOR_AXIS_MT * A * (sigma - deltaSigma)
        return distanceMt / 1000
    }
}
