package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.DriveOffer
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DriveOffersRepository: JpaRepository<DriveOffer, UUID>