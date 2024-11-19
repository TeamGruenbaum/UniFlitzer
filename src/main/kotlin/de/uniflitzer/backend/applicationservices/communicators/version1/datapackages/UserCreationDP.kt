package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Email
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UserCreationDP private constructor (
    @field:Size(min = 3, max = 30) val username: String,
    @field:Size(min = 1, max = 100) val firstName: String,
    @field:Size(min = 1, max = 100) val lastName: String,
    @field:Pattern(regexp = DateTimeFormat) val birthday: String,
    val gender: GenderDP?,
    @field:Email val emailAddress: String,
    val address: AddressDP?,
    @field:Size(min = 2, max = 100) val studyProgramme: String
)