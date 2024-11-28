package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.Position

data class PositionDP private constructor(
    val coordinate: CoordinateDP,
    val nearestAddress: AddressDP
) {
    companion object {
        fun fromPosition(position: Position): PositionDP {
            return PositionDP(
                CoordinateDP.fromCoordinate(position.coordinate),
                AddressDP.fromAddress(position.nearestAddress)
            )
        }
    }
}

