package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormatExample
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime

data class DetailedUserDP(
    @field:UUID val id: String,
    @field:Size(min = 1, max = 100) val firstName: String,
    @field:Size(min = 1, max = 100) val lastName: String,
    @field:Pattern(regexp = DateTimeFormat) @field:Schema(example = DateTimeFormatExample) val birthday: String,
    val gender: GenderDP,
    val address: AddressDP?,
    @field:Size(min = 1, max=300) val description: String?,
    @field:Size(min = 2, max = 200) val studyProgramme: String,
    val isSmoking: Boolean?,
    @field:Size(min = 0, max = 10) val animals: List<AnimalDP>,
    val drivingStyle: DrivingStyleDP?,
    @field:Size(min = 0, max = 5) val cars: List<CarDP>,
)