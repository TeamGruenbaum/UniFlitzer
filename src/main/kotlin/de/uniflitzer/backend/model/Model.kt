package de.uniflitzer.backend.model

@JvmInline
value class Model(val value: String){
    init {
        require(value.count() in 1..50)
    }
}