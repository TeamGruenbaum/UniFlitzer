package de.uniflitzer.backend.model

@JvmInline
value class FirstName(val value: String) {
    init {
        require(value.count() in 1..100)
    }
}