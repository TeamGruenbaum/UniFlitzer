package de.uniflitzer.backend.model

@JvmInline
value class Meters(val value: Double) {
    init {
        require(value >= 0)
    }
}