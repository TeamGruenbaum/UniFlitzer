package de.uniflitzer.backend.applicationservices.geography

import de.uniflitzer.backend.model.CompleteRoute
import de.uniflitzer.backend.model.Coordinate
import de.uniflitzer.backend.model.Position
import de.uniflitzer.backend.model.UserStop
import org.springframework.stereotype.Service

interface GeographyService {
    fun createRoute(start: Position, stops: List<UserStop>, destination: Position): CompleteRoute
    fun createPosition(coordinate: Coordinate): Position
}