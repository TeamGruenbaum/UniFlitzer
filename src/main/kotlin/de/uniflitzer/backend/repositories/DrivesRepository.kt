package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.Drive
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import java.util.*

interface DrivesRepository: JpaRepository<Drive, UUID> {}