package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.DriveOffer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import java.util.*

interface DriveOffersRepository: JpaRepository<DriveOffer, UUID> {
}