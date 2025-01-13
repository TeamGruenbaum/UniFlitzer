package de.uniflitzer.backend.model

@JvmInline
value class Brand(val value: String){
    init {
        require(value.count() in 2..50) { "Passed value is not between 2 and 50 characters long." }
    }
}