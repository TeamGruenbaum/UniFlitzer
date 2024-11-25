package de.uniflitzer.backend.model

@JvmInline
value class Description(val value: String) {
    init {
        require(value.count() in 1..300)
    }
}
