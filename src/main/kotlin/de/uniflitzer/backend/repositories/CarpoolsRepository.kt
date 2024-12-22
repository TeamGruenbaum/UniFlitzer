package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.Carpool
import de.uniflitzer.backend.model.Drive
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface CarpoolsRepository: JpaRepository<Carpool, UUID> {
    @Query("""SELECT carpool FROM Carpool carpool WHERE (:userId IN (SELECT user.id FROM carpool._users user))""")
    fun findCarpools(pageable: Pageable, @Param("userId") userId: UUID): Page<Carpool>
}