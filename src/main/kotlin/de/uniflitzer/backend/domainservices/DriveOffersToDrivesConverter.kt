package de.uniflitzer.backend.domainservices

import de.uniflitzer.backend.applicationservices.geography.GeographyService
import de.uniflitzer.backend.model.Car
import de.uniflitzer.backend.model.Drive
import de.uniflitzer.backend.repositories.DriveOffersRepository
import de.uniflitzer.backend.repositories.DrivesRepository
import de.uniflitzer.backend.repositories.ImagesRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class DriveOffersToDrivesConverter(
    @field:Autowired private val geographyService: GeographyService,
    @field:Autowired private val driveOffersRepository: DriveOffersRepository,
    @field:Autowired private val drivesRepository: DrivesRepository,
    @field:Autowired private val imagesRepository: ImagesRepository
) {
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    fun execute() {
        driveOffersRepository.findAllWithPlannedDepartureWithinTime(5.toUInt())
            .forEach {
                val newDrive: Drive = Drive(
                        it.driver,
                        Car(
                            it.car.brand,
                            it.car.model,
                            it.car.color,
                            it.car.licencePlate
                        ),
                        geographyService.createCompleteRouteBasedOnUserStops(it.route.start, it.passengers, it.route.destination),
                        it.passengers.map{it.user},
                        it.plannedDeparture!!
                    )

                it.car.image?.let { driveOfferCarImage -> newDrive.car.image = imagesRepository.copy(driveOfferCarImage) }
                drivesRepository.saveAndFlush(newDrive)
                driveOffersRepository.delete(it)
            }

        drivesRepository.flush()
        driveOffersRepository.flush()
    }
}