package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.DriveRequest
import org.springframework.data.repository.CrudRepository
import java.util.*

interface DriveRequestsRepository: CrudRepository<DriveRequest, UUID> {}