package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.Position
import jakarta.validation.Valid

data class PositionDP private constructor(
    @field:Valid val coordinate: CoordinateDP,
    @field:Valid val nearestAddress: AddressDP?
) {
    companion object {
        fun fromPosition(position: Position): PositionDP {
            return PositionDP(
                CoordinateDP.fromCoordinate(position.coordinate),
                position.nearestAddress?.let{AddressDP.fromAddress(it)}
            )
        }
    }
}