package de.uniflitzer.backend.model

@JvmInline
value class Brand(val value: String){
    init {
        require(value.count() in 2..50)
    }
}