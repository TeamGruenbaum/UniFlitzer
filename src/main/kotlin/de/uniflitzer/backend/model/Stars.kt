package de.uniflitzer.backend.model

@JvmInline
value class Stars(val value: UInt) {
    init {
        require(value in 0u..5u)
    }
}