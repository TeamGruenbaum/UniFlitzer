package de.uniflitzer.backend.model

@JvmInline
value class Content(val value: String){
    init {
        require(value.count() in 1..300)
    }
}