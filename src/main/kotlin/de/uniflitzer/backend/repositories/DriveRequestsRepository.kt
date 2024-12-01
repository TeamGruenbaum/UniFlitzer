package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.DriveRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface DriveRequestsRepository: JpaRepository<DriveRequest, UUID>
{
    @Query("""SELECT d FROM DriveRequest d WHERE (:requestingUserId IS NULL OR d.requestingUser.id = :requestingUserId)""")
    fun findDriveRequests(pageable: Pageable, @Param("requestingUserId") requestingUserId: UUID? = null): Page<DriveRequest>
}