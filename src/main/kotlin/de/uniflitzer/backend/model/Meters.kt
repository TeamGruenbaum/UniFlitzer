package de.uniflitzer.backend.model

@JvmInline
value class Meters(val value: Double) {
    init {
        require(value >= 0) { "Meters with value $value is not greater than or equal to 0." }
    }
}