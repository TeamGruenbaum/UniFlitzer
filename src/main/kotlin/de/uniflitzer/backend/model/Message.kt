package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import java.time.ZonedDateTime

@Embeddable
class Message(user: User, content: Content, sent: ZonedDateTime){
    @field:ManyToOne(fetch = FetchType.LAZY)
    final var user: User = user
        private set

    final var content: Content = content
        private set

    final var sent: ZonedDateTime = sent
        private set

    init {
        this.user = user
        this.content = content
        this.sent = sent
    }
}