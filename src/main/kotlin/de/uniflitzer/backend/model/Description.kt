package de.uniflitzer.backend.model

@JvmInline
value class Description(val value: String) {
    init {
        require(value.count() in 1..300) { "Passed value is not between 1 and 300 characters long." }
    }
}
