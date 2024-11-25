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
}
