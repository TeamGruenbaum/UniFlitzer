package de.uniflitzer.backend.model

import android_maps_utils_3_10_0.LatLng
import android_maps_utils_3_10_0.PolyUtil
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.Duration

@Embeddable
class Route(start: Position, destination: Position, duration: Duration, polyline: GeoJsonLineString){
    @AttributeOverrides(
        AttributeOverride(name = "coordinate.latitude", column = Column(name = "route_coordinate_start_latitude")),
        AttributeOverride(name = "coordinate.longitude", column = Column(name = "route_coordinate_start_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "route_nearestAddress_start_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "route_nearestAddress_start_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "route_nearestAddress_start_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "route_nearestAddress_start_city"))
    )
    final var start: Position = start
        private set

    @AttributeOverrides(
        AttributeOverride(name = "coordinate.latitude", column = Column(name = "route_destination_coordinate_latitude")),
        AttributeOverride(name = "coordinate.longitude", column = Column(name = "route_destination_coordinate_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "route_destination_nearestAddress_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "route_destination_nearestAddress_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "route_destination_nearestAddress_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "route_destination_nearestAddress_city"))
    )
    final var destination: Position = destination
        private set

    final var duration: Duration = duration
        private set

    final var polyline: GeoJsonLineString = polyline
        private set

    init {
        this.start = start
        this.destination = destination
        this.duration = duration
        this.polyline = polyline
    }

    fun isCoordinateOnRoute(coordinate: Coordinate, tolerance: Meters): Boolean {
        return PolyUtil.isLocationOnPath(
            LatLng(coordinate.latitude, coordinate.longitude),
            polyline.coordinates.map { LatLng(it.latitude, it.longitude) },
            true,
            tolerance.value
        )
    }

    fun areCoordinatesInCorrectDirection(start: Coordinate, destination: Coordinate): Boolean {
        fun calculateClosestCoordinate(coordinate: Coordinate): UInt? {
            return polyline.coordinates
                .filterIndexed { index, _ -> index % 2 == 0 }
                .parallelStream()
                .map{ (it distanceTo coordinate).value }
                .toList()
                .withIndex()
                .minByOrNull{ it.value }
                ?.index
                ?.toUInt()
        }

        val indexOfStartOnRoute: UInt = calculateClosestCoordinate(start) ?: return false
        val indexOfDestinationOnRoute: UInt = calculateClosestCoordinate(destination) ?: return false

        return indexOfStartOnRoute < indexOfDestinationOnRoute
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Route) return false

        if (start != other.start) return false
        if (destination != other.destination) return false
        if (duration != other.duration) return false
        if (polyline != other.polyline) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + destination.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + polyline.hashCode()
        return result
    }
}