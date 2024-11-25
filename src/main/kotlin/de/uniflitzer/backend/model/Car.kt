package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable

@Embeddable
class Car(brand: Brand, model:Model, color: Color, licencePlate: LicencePlate) {
    final var image: Image? = null
        private set

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
}
