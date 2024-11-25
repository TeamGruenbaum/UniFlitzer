package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import java.util.*

@Embeddable
class Image{
    final var fileNameFullQuality: UUID = UUID.randomUUID()
        private set

    final var fileNamePreviewQuality: UUID = UUID.randomUUID()
        private set
}