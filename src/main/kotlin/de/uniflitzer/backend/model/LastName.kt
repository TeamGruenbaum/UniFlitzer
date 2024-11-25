package de.uniflitzer.backend.model

@JvmInline
value class LastName(val value: String) {
    init {
        require(value.count() in 1..100)
    }
}