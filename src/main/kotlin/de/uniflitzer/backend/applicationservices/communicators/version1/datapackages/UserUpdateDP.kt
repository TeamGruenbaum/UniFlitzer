package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.constraints.Email
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormatExample
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.*

data class UserUpdateDP private constructor(
    @field:Size(min = 1, max = 100) val firstName: String?,
    @field:Size(min = 1, max = 100) val lastName: String?,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val birthday: String?,
    val gender: GenderDP?,
    @field:Valid val address: AddressDP?,
    @field:Size(min = 2, max = 200) val studyProgramme: String?,

    val description: Optional<@Size(min = 1, max=300) String>?,
    val isSmoking: Optional<Boolean>?,
    @field:Valid val animals: Optional<@Size(min = 0, max = 10) List<AnimalDP>>?,
    val drivingStyle: Optional<DrivingStyleDP>?,
)