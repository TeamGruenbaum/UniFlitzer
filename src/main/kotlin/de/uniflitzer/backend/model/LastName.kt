package de.uniflitzer.backend.model

@JvmInline
value class LastName(val value: String) {
    init {
        require(value.count() in 1..100) { "Passed value is not between 1 and 100 characters long." }
    }
}