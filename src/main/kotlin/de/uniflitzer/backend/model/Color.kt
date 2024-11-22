package de.uniflitzer.backend.model

@JvmInline
value class Color(val value: String){
    init {
        require(value.count() in 3..50)
    }
}