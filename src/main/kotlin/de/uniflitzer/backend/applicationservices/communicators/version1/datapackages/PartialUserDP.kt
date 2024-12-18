package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import de.uniflitzer.backend.model.User
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class PartialUserDP(
    @field:UUID val id: String,
    @field:Size(min=1, max=100) val firstName: String,
    @field:Size(min=1, max=100) val lastName: String,
    @field:Min(0) @field:Max(5) val averageStars: Double?,
    @field:Min(0) val numberOfRatings: Int
) {
    companion object {
        fun fromUser(user: User): PartialUserDP =
            PartialUserDP(
                user.id.toString(),
                user.firstName.value,
                user.lastName.value,
                user.getAverageStars(),
                user.ratings.size
            )
    }
}