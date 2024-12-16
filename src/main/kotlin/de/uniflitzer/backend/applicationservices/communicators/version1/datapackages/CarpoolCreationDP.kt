package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Size

data class CarpoolCreationDP private constructor(
    @field:Size(min = 2, max = 100) val name:String
){}