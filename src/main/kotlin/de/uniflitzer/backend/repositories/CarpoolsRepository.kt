package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.Carpool
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface CarpoolsRepository: JpaRepository<Carpool, UUID> {
    @Transactional(rollbackFor = [Throwable::class])
    @Query("""SELECT carpool FROM Carpool carpool WHERE (:userId IN (SELECT user.id FROM carpool._users user))""")
    fun findAllCarpools(pageable: Pageable, @Param("userId") userId: UUID): Page<Carpool>
}