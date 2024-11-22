package de.uniflitzer.backend.model

import jakarta.persistence.Embeddable
import java.util.*

@Embeddable
class Image{
    var fileNameFullQuality: UUID = UUID.randomUUID()
    var fileNamePreviewQuality: UUID = UUID.randomUUID()

    constructor() {
        this.fileNameFullQuality = UUID.randomUUID()
        this.fileNamePreviewQuality = UUID.randomUUID()
    }
}