package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import java.time.ZonedDateTime

@Embeddable
class Message{
    @field:ManyToOne(fetch = FetchType.LAZY)
    var user: User = null!!

    var content: Content = null!!

    var sent: ZonedDateTime = null!!

    constructor(user: User, content: Content, sent: ZonedDateTime) {
        this.user = user
        this.content = content
        this.sent = sent
    }
}