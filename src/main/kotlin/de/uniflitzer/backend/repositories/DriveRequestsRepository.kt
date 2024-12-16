package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.DriveRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface DriveRequestsRepository: JpaRepository<DriveRequest, UUID>
{
    @Query("""SELECT driveRequest FROM DriveRequest driveRequest WHERE (:requestingUserId IS NULL OR driveRequest.requestingUser.id = :requestingUserId)""")
    fun findAll(sort: Sort, @Param("requestingUserId") requestingUserId: UUID? = null): List<DriveRequest>
}