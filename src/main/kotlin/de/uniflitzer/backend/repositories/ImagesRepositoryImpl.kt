package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.Image
import de.uniflitzer.backend.repositories.errors.*
import fi.solita.clamav.ClamAVClient
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import net.coobird.thumbnailator.Thumbnails
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.*

@Component
@Transactional(rollbackFor = [Throwable::class])
class ImagesRepositoryImpl(@field:Autowired private val environment:Environment): ImagesRepository {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun save(data:ByteArray, fileEnding:String?): Image {
        val reply: ByteArray
        try {
            reply = ClamAVClient(
                environment.getProperty("clamav.ip") ?: throw IllegalStateException("clamav.ip is not set."),
                environment.getProperty("clamav.port")?.toInt() ?: throw IllegalStateException("clamav.port is not set.")
            ).scan(data)
        } catch (exception: Exception) {
            throw FileCheckError("Uploaded file could not be scanned for infections.")
        }
        if (!ClamAVClient.isCleanReply(reply)) throw FileCorruptedError("Uploaded file is infected.")


        var fileType = try {
            Tika().detect(data)
        } catch (exception: Exception) {
            throw FileCheckError("File type could not be detected.")
        }
        when (fileType) {
            "image/jpeg" -> if (fileEnding != "jpg" && fileEnding != "jpeg") throw FileCorruptedError("Uploaded file is not a valid image.")
            "image/png" -> if (fileEnding != "png") throw FileCorruptedError("Uploaded file is not a valid image.")
            "image/gif" -> if (fileEnding != "gif") throw FileCorruptedError("Uploaded file is not a valid image.")
            else -> throw WrongFileFormatError("Uploaded file is not an image.")
        }


        val imagesDirectory = Paths.get(environment.getProperty("directory.images") ?: throw ApplicationPropertyMissingError("Property directory.images is missing."))
        if (!Files.exists(imagesDirectory)) Files.createDirectories(imagesDirectory)

        val image: Image = Image(".$fileEnding")
        val fileFullQuality = File(imagesDirectory.toString(), image.fileNameFullQuality)
        fileFullQuality.writeBytes(data)

        val filePreviewQuality = File(imagesDirectory.toString(), image.fileNamePreviewQuality)
        Thumbnails
            .of(fileFullQuality)
            .size(640, 480)
            .toFile(filePreviewQuality)


        try {
            setFilePermissions(fileFullQuality)
            setFilePermissions(filePreviewQuality)
        } catch (exception: Exception) {
            throw FilePermissionsError("File permissions could not be set.")
        }

        entityManager.persist(image)
        entityManager.flush()
        return image
    }

    override fun getById(id: UUID, quality: ImagesRepository.Quality): Optional<ByteArray> {
        val image: Image = entityManager.find(Image::class.java, id)

        val imagesDirectory = Paths.get(environment.getProperty("directory.images") ?: throw ApplicationPropertyMissingError("Property directory.images is missing."))
        if (!Files.exists(imagesDirectory)) throw ImageDirectoryMissingError("Image directory does not exist.")

        val file: File = File(
            imagesDirectory.toString(),
            if (quality == ImagesRepository.Quality.Full) image.fileNameFullQuality else image.fileNamePreviewQuality
        )
        if (!file.exists()) return Optional.empty()
        return Optional.of(Files.readAllBytes(file.toPath()))
    }

    override fun deleteById(id: UUID) {
        val image: Image = entityManager.find(Image::class.java, id)

        val imagesDirectory = Paths.get(environment.getProperty("directory.images") ?: throw ApplicationPropertyMissingError("Property directory.images is missing."))
        if (!Files.exists(imagesDirectory)) throw ImageDirectoryMissingError("Image directory does not exist.")

        val fileFullQuality: File = File(imagesDirectory.toString(), image.fileNameFullQuality)
        if (!fileFullQuality.exists()) {
            throw FileMissingError("Full quality image with id $id does not exist.")
        }
        val filePreviewQuality: File = File(imagesDirectory.toString(), image.fileNamePreviewQuality)
        if (!filePreviewQuality.exists()) {
            throw FileMissingError("Preview quality image with id $id does not exist.")
        }

        if (!fileFullQuality.delete()) throw FileDeletionError("Full quality image with id $id could not be deleted.")
        if (!filePreviewQuality.delete()) throw FileDeletionError("Preview quality image with id $id could not be deleted.")

        entityManager.remove(image)
    }

    override fun copy(image: Image): Image {
        val imagesDirectory = Paths.get(environment.getProperty("directory.images") ?: throw ApplicationPropertyMissingError("Property directory.images is missing."))
        if (!Files.exists(imagesDirectory)) throw ImageDirectoryMissingError("Image directory does not exist.")

        val copiedImage: Image = Image(".${image.fileNameFullQuality.substringAfterLast(".")}")

        val copiedFileFullQuality = File(imagesDirectory.toString(), copiedImage.fileNameFullQuality)
        val copiedFilePreviewQuality = File(imagesDirectory.toString(), copiedImage.fileNamePreviewQuality)
        Files.copy(
            File(imagesDirectory.toString(), image.fileNameFullQuality).toPath(),
            copiedFileFullQuality.toPath()
        )
        Files.copy(
            File(imagesDirectory.toString(), image.fileNamePreviewQuality).toPath(),
            copiedFilePreviewQuality.toPath()
        )

        try {
            setFilePermissions(copiedFileFullQuality)
            setFilePermissions(copiedFilePreviewQuality)
        } catch (exception: Exception) {
            throw FilePermissionsError("File permissions could not be set.")
        }

        entityManager.persist(copiedImage)
        entityManager.flush()
        return copiedImage
    }

    private fun setFilePermissions(file: File) {
        val permissions: MutableSet<PosixFilePermission> = mutableSetOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.OTHERS_READ
        )
        Files.setPosixFilePermissions(file.toPath(), permissions)
    }
}