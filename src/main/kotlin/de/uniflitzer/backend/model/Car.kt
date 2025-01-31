package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import jakarta.persistence.OneToOne

@Embeddable
class Car(brand: Brand, model:Model, color: Color, licencePlate: LicencePlate) {
    @OneToOne
    final var image: Image? = null

    final var brand: Brand = brand
        private set

    final var model: Model = model
        private set

    final var color: Color = color
        private set

    final var licencePlate: LicencePlate = licencePlate
        private set

    init {
        this.brand = brand
        this.model = model
        this.color = color
        this.licencePlate = licencePlate
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Car) return false

        if (image != other.image) return false
        if (brand != other.brand) return false
        if (model != other.model) return false
        if (color != other.color) return false
        if (licencePlate != other.licencePlate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = image?.hashCode() ?: 0
        result = 31 * result + brand.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + licencePlate.hashCode()
        return result
    }
}
