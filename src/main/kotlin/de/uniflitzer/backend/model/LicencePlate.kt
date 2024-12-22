package de.uniflitzer.backend.model

@JvmInline
value class LicencePlate(val value: String){
    init {
        require(Regex("^[A-ZÖÜÄ]{1,3} [A-ZÖÜÄ]{1,2} [1-9]{1}[0-9]{1,3}\$").matches(value)) { "Licence plate with value $value has no licence plate format." }
    }
}