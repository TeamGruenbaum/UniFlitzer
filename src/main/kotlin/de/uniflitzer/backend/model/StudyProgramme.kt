package de.uniflitzer.backend.model

@JvmInline
value class StudyProgramme(val value: String) {
    init {
        require(value.count() in 2..200)
    }
}