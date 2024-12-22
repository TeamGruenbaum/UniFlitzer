package de.uniflitzer.backend.model

@JvmInline
value class StudyProgramme(val value: String) {
    init {
        require(value.count() in 2..200) { "Study programme with value $value is not between 2 and 200 characters long." }
    }
}