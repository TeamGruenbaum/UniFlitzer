package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import de.uniflitzer.backend.repositories.ImagesRepository

enum class QualityDP {
     Full, Preview;

    fun toQuality(): ImagesRepository.Quality {
        return when (this) {
            Full -> ImagesRepository.Quality.Full
            Preview -> ImagesRepository.Quality.Preview
        }
    }
}