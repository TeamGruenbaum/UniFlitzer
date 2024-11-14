import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.LicencePlateFormat
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CarDP private constructor(
    @field:UUID val image:ImageDP,
    @field:Size(min = 3, max = 50) val brand: String,
    @field:Size(min = 3, max = 50) val model: String,
    @field:Size(min = 3, max = 50) val color: String,
    @field:Pattern(regexp = LicencePlateFormat) val licencePlate: String
)