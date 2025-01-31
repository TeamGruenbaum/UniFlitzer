package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.Valid

data class UserStopCreationDP private constructor(@field:Valid val start: CoordinateDP, @field:Valid val destination: CoordinateDP)