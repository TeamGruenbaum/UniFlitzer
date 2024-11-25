package de.uniflitzer.backend.model

@JvmInline
value class Username(val value: String) {
    init {
        require(value.count() in 3..50)
    }
}