package de.uniflitzer.backend.repositories

import de.uniflitzer.backend.model.Animal
import de.uniflitzer.backend.model.Carpool
import de.uniflitzer.backend.model.DriveOffer
import de.uniflitzer.backend.model.DrivingStyle
import de.uniflitzer.backend.model.Gender
import de.uniflitzer.backend.model.User
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface DriveOffersRepository: JpaRepository<DriveOffer, UUID> {
    @Transactional(rollbackFor = [Throwable::class])
    @Query(
        """
        SELECT driveOffer
        FROM DriveOffer driveOffer
        WHERE
            :allowedAnimals IS NULL OR EXISTS (SELECT 1 FROM driveOffer.driver._animals animals WHERE animals NOT IN :allowedAnimals)
            AND (:isSmoking IS NULL OR driveOffer.driver.isSmoking = :isSmoking)
            AND (:allowedDrivingStyles IS NULL OR driveOffer.driver.drivingStyle IN :allowedDrivingStyles)
            AND (:allowedGenders IS NULL OR driveOffer.driver.gender IN :allowedGenders)
            AND driveOffer.driver NOT IN :blockedUsers
            AND TYPE(driveOffer) <> CarpoolDriveOffer
            AND driveOffer.driver <> :disallowedDriver
        """
    )
    fun findAll(
        @Param("allowedAnimals") allowedAnimals: List<Animal>?,
        @Param("isSmoking") isSmoking: Boolean?,
        @Param("allowedDrivingStyles") allowedDrivingStyles: List<DrivingStyle>?,
        @Param("allowedGenders") allowedGenders: List<Gender>?,
        @Param("blockedUsers") blockedUsers: List<User>,
        @Param("disallowedDriver") disallowedDriver: User,
        sort: Sort
    ): List<DriveOffer>
}