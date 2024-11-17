package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.*

data class UserUpdateDP private constructor(
    @field:Size(min = 3, max = 30) val username: String?,
    @field:Size(min = 1, max = 100) val firstName: String?,
    @field:Size(min = 1, max = 100) val lastName: String?,
    @field:Pattern(regexp = DateFormat) val birthday: String?,
    val gender: GenderDP?,
    @field:Email val email: String?,
    val address: AddressDP?,
    @field:Size(min = 2, max = 200) val studyProgramme: String?,

    @field:Size(min = 1, max=300) val description: Optional<String>?,
    val isSmoking: Optional<Boolean>?,
    @field:Size(min = 1, max=30) val animals: Optional<List<AnimalDP>>?,
    val drivingStyle: Optional<DrivingStyleDP>?,
)