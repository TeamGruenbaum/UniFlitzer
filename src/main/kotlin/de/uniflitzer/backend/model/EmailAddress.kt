package de.uniflitzer.backend.model

@JvmInline
value class EmailAddress(val value: String) {
    init {
        require(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$").matches(value)) { "Passed value has no email format." }
    }
}