package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne

@Embeddable
class ConfirmableUserStop(user: User, start: Position, destination: Position, waitingConfirmed: Boolean){
    @field:ManyToOne(fetch = FetchType.LAZY)
    final var user: User = user
        private set

    final var start: Position = start
        private set

    final var destination: Position = destination
        private set

    final var waitingConfirmed: Boolean = waitingConfirmed
        private set

    init {
        this.user = user
        this.start = start
        this.destination = destination
        this.waitingConfirmed = waitingConfirmed
    }

    fun confirm()
    {
        this.waitingConfirmed = true
    }
}