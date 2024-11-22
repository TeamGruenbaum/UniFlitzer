package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable

@Embeddable
class Car{
    var image: Image? = null

    var brand: Brand = null!!
    var model: Model = null!!
    var color: Color = null!!
    var licencePlate: LicencePlate = null!!

    constructor(brand:Brand, model:Model, color: Color, licencePlate: LicencePlate) {
        this.brand = brand
        this.model = model
        this.color = color
        this.licencePlate = licencePlate
    }
}