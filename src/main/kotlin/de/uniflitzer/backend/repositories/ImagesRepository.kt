package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.Image
import org.springframework.web.multipart.MultipartFile
import java.util.*

interface ImagesRepository {
    fun save(multipartFile: MultipartFile): Image
    fun getById(id:UUID, quality: Quality): Optional<ByteArray>
    fun deleteById(id:UUID)
    fun copy(image: Image): Image

    enum class Quality { Full, Preview }
}