package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.Animal

enum class AnimalDP {
    Dog, Cat, Horse, Rodent, Bird;

    fun toAnimal(): Animal =
        when (this) {
            Dog -> Animal.Dog
            Cat -> Animal.Cat
            Horse -> Animal.Horse
            Rodent -> Animal.Rodent
            Bird -> Animal.Bird
        }

    companion object {
        fun fromAnimal(animal: Animal): AnimalDP =
            when (animal) {
                Animal.Dog -> Dog
                Animal.Cat -> Cat
                Animal.Horse -> Horse
                Animal.Rodent -> Rodent
                Animal.Bird -> Bird
            }
    }
}