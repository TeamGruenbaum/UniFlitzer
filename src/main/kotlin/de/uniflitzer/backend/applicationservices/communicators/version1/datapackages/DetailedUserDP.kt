package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.formats.DateTimeFormat
import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class DetailedUserDP(
    @field:UUID val id: String,
    @field:Size(min = 3, max = 30) val username: String,
    @field:Size(min = 1, max = 100) val firstName: String,
    @field:Size(min = 1, max = 100) val lastName: String,
    @field:Pattern(regexp = DateTimeFormat) val birthday: String,
    val gender: GenderDP,
    @field:Email val emailAddress: String,
    val address: AddressDP?,
    @field:Size(min = 1, max=300) val description: String?,
    @field:Size(min = 2, max = 200) val studyProgramme: String,
    val isSmoking: Boolean?,
    @field:Size(min = 0, max = 10) val animals: List<AnimalDP>,
    val drivingStyle: DrivingStyleDP?,
    @field:Size(min = 0, max = 5) val cars: List<CarDP>,
)