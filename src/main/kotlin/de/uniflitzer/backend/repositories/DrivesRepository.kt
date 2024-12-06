package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.Drive
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface DrivesRepository: JpaRepository<Drive, UUID>
{
    @Query("""SELECT d FROM Drive d WHERE (d.driver.id = :userId OR :userId IN (SELECT p.id FROM d._passengers p))""")
    fun findDrives(pageable: Pageable, @Param("userId") userId: UUID): Page<Drive>
}