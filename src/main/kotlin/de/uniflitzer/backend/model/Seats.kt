package de.uniflitzer.backend.model

@JvmInline
value class Seats(val value: UInt){
    init {
        require(value in 1u..8u)
    }
}