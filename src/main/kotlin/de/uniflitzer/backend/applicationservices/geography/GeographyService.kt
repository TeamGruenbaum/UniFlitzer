package de.uniflitzer.backend.applicationservices.geography

import de.uniflitzer.backend.model.*

// The first two methods should be also named createRoute but due to type erasure their JVM signatures are considered the same
interface GeographyService {
    fun createCompleteRouteBasedOnUserStops(start: Position, stops: List<UserStop>, destination: Position): CompleteRoute
    fun createCompleteRouteBasedOnConfirmableUserStops(start: Position, stops: List<ConfirmableUserStop>, destination: Position): CompleteRoute
    fun createRoute(start: Position, destination: Position): Route
    fun createPosition(coordinate: Coordinate): Position
}