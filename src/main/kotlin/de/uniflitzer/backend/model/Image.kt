package de.uniflitzer.backend.model

import jakarta.persistence.*
import java.util.*

@Entity
class Image(fileEnding: String){
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    val fileNameFullQuality: String = UUID.randomUUID().toString() + fileEnding

    val fileNamePreviewQuality: String = UUID.randomUUID().toString() + fileEnding
}