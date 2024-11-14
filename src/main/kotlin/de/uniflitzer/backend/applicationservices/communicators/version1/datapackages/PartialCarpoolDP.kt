package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import jakarta.validation.constraints.Size

data class PartialCarpoolDP(
    @field:UUID val id: String,
    @field:Size(min = 1, max = 50) val name: String,
)