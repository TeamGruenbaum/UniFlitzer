package de.uniflitzer.backend.model

@JvmInline
value class Stars(val value: UInt) {
    init {
        require(value in 0u..5u) { "Passed value is not between 0 and 5." }
    }
}