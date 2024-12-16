package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ManyToOne
import java.time.ZonedDateTime

@Embeddable
class Rating(author: User, role: Role, content: Content, stars: Stars, created: ZonedDateTime) {
    @field:ManyToOne
    final var author: User = author
        private set

    @field:Enumerated(EnumType.STRING)
    final var role: Role = role
        private set

    final var content: Content = content
        private set

    final var stars: Stars = stars
        private set

    final var created: ZonedDateTime = created
        private set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Rating) return false

        if (author != other.author) return false
        if (role != other.role) return false
        if (content != other.content) return false
        if (stars != other.stars) return false
        if (created != other.created) return false

        return true
    }

    override fun hashCode(): Int {
        var result = author.hashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + stars.hashCode()
        result = 31 * result + created.hashCode()
        return result
    }


}
