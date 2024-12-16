package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class RatingCreationDP private constructor(
    val role: RoleDP,
    @Size(min = 1, max = 300) val content: String,
    @field:Min(0) @field:Max(5) val stars: Int
) {}