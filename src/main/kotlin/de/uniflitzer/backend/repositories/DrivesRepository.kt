package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.Drive
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface DrivesRepository: JpaRepository<Drive, UUID>
{
    @Transactional(rollbackFor = [Throwable::class])
    @Query("""SELECT drive FROM Drive drive WHERE (drive.driver.id = :userId OR :userId IN (SELECT passenger.id FROM drive._passengers passenger))""")
    fun findAllDrives(pageable: Pageable, @Param("userId") userId: UUID): Page<Drive>
}