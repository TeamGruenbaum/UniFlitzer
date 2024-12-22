package de.uniflitzer.backend.model

@JvmInline
value class FirstName(val value: String) {
    init {
        require(value.count() in 1..100) { "First name with value $value is not between 1 and 100 characters long." }
    }
}