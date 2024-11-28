package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID

data class PartialConfirmableUserStop(
    @field:UUID val userId: String,
    val position: PositionDP,
    val waitingConfirmed: Boolean
)