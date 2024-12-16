package de.uniflitzer.backend.model

import android_maps_utils_3_10_0.LatLng
import android_maps_utils_3_10_0.PolyUtil
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class Route(start: Position, destination: Position, polyline: GeoJsonLineString){
    @AttributeOverrides(
        AttributeOverride(name = "coordinate.latitude", column = Column(name = "start_latitude")),
        AttributeOverride(name = "coordinate.longitude", column = Column(name = "start_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "start_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "start_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "start_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "start_city"))
    )
    final var start: Position = start
        private set

    @AttributeOverrides(
        AttributeOverride(name = "coordinate.latitude", column = Column(name = "destination_latitude")),
        AttributeOverride(name = "coordinate.longitude", column = Column(name = "destination_longitude")),
        AttributeOverride(name = "nearestAddress.street", column = Column(name = "destination_street")),
        AttributeOverride(name = "nearestAddress.houseNumber", column = Column(name = "destination_houseNumber")),
        AttributeOverride(name = "nearestAddress.postalCode", column = Column(name = "destination_postalCode")),
        AttributeOverride(name = "nearestAddress.city", column = Column(name = "destination_city"))
    )
    final var destination: Position = destination
        private set

    final var polyline: GeoJsonLineString = polyline
        private set

    init {
        this.start = start
        this.destination = destination
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
}