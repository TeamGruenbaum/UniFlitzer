package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne

@Embeddable
class ConfirmableUserStop(user: User, position: Position, waitingConfirmed: Boolean){
    @field:ManyToOne(fetch = FetchType.LAZY)
    final var user: User = user
        private set

    final var position: Position = position
        private set

    final var waitingConfirmed: Boolean = waitingConfirmed
        private set

    init {
        this.user = user
        this.position = position
        this.waitingConfirmed = waitingConfirmed
    }

    fun confirm()
    {
        this.waitingConfirmed = true
    }
}