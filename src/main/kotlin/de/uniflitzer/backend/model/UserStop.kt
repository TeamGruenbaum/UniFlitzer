package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import jakarta.persistence.ManyToOne

@Embeddable
class UserStop(user: User, position: Position) {
    @field:ManyToOne
    var user: User = user //final

    var position: Position = position //final
}