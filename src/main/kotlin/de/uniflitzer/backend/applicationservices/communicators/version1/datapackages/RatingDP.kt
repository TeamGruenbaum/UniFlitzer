package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormatExample
import de.uniflitzer.backend.model.Rating
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RatingDP(
    @field:Valid val author: PartialUserDP,
    @field:Valid val role: RoleDP,
    @Size(min = 1, max = 300) val content: String,
    @field:Min(0) @field:Max(5) val stars: Int,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val created: String
) {
    companion object
    {
        fun fromRating(rating: Rating): RatingDP
        {
            return RatingDP(
                PartialUserDP.fromUser(rating.author),
                RoleDP.fromRole(rating.role),
                rating.content.value,
                rating.stars.value.toInt(),
                rating.created.toString()
            )
        }
    }
}