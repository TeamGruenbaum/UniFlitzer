package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne

@Embeddable
class ConfirmableUserStop{
    @field:ManyToOne(fetch = FetchType.LAZY)
    var user: User = null!!
    var position: Position = null!!
    var waitingConfirmed: Boolean = null!!

    constructor(user: User, position: Position, waitingConfirmed: Boolean) {
        this.user = user
        this.position = position
        this.waitingConfirmed = waitingConfirmed
    }
}