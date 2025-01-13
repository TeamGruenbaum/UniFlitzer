package de.uniflitzer.backend.model

@JvmInline
value class Name(val value: String){
    init {
        require(value.count() in 2..100) { "Passed value is not between 2 and 100 characters long." }
    }
}