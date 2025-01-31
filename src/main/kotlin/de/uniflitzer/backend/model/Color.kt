package de.uniflitzer.backend.model

@JvmInline
value class Color(val value: String){
    init {
        require(value.count() in 3..50) { "Passed value is not between 3 and 50 characters long." }
    }
}