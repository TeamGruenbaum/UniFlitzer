package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.model.Gender

enum class GenderDP {
    Male, Female, Diverse, PreferNotToSay;

    fun toGender(): Gender =
        when(this) {
            GenderDP.Male -> Gender.Male
            GenderDP.Female -> Gender.Female
            GenderDP.Diverse -> Gender.Diverse
            GenderDP.PreferNotToSay -> Gender.PreferNotToSay
        }
}