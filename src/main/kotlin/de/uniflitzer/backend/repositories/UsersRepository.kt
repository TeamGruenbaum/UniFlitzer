package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UsersRepository: JpaRepository<User, UUID>