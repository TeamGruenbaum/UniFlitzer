package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import jakarta.validation.constraints.Size

data class TraceableErrorDP(
    @field:UUID val traceId: String,
    @field:Size(min=3) val message: String
)
