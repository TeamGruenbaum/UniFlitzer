package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import jakarta.persistence.ManyToOne

@Embeddable
class UserStop(user: User, start: Position, destination: Position) {
    @field:ManyToOne
    var user: User = user //final

    var start: Position = start //final
    var destination: Position = destination
}