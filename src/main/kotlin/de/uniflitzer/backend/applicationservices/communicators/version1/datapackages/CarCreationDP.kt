package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.applicationservices.communicators.version1.formats.LicencePlateFormat
import de.uniflitzer.backend.model.Brand
import de.uniflitzer.backend.model.Car
import de.uniflitzer.backend.model.Color
import de.uniflitzer.backend.model.LicencePlate
import de.uniflitzer.backend.model.Model
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CarCreationDP private constructor(
    @field:Size(min = 3, max = 50) val brand:String,
    @field:Size(min = 3, max = 50) val model:String,
    @field:Size(min = 3, max = 50) val color:String,
    @field:Pattern(regexp = LicencePlateFormat) var licencePlate:String
) {
    fun toCar() = Car(Brand(this.brand), Model(model), Color(color), LicencePlate(licencePlate))
}