package de.uniflitzer.backend.model

@JvmInline
value class Model(val value: String){
    init {
        require(value.count() in 1..50) { "Model with value $value is not between 1 and 50 characters long." }
    }
}