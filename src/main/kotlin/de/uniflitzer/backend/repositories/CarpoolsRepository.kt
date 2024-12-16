package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.Carpool
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CarpoolsRepository: JpaRepository<Carpool, UUID> {}