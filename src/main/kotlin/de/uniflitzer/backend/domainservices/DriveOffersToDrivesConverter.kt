package de.uniflitzer.backend.domainservices

import de.uniflitzer.backend.applicationservices.geography.GeographyService
import de.uniflitzer.backend.model.Car
import de.uniflitzer.backend.model.CarpoolDriveOffer
import de.uniflitzer.backend.model.CompleteRoute
import de.uniflitzer.backend.model.Drive
import de.uniflitzer.backend.model.ScheduleTimeType
import de.uniflitzer.backend.repositories.CarpoolsRepository
import de.uniflitzer.backend.repositories.DriveOffersRepository
import de.uniflitzer.backend.repositories.DrivesRepository
import de.uniflitzer.backend.repositories.ImagesRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@Component
class DriveOffersToDrivesConverter(
    @field:Autowired private val geographyService: GeographyService,
    @field:Autowired private val driveOffersRepository: DriveOffersRepository,
    @field:Autowired private val drivesRepository: DrivesRepository,
    @field:Autowired private val imagesRepository: ImagesRepository,
    @field:Autowired private val carpoolsRepository: CarpoolsRepository
) {
    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    @Transactional(rollbackFor = [Throwable::class])
    fun execute() {
        driveOffersRepository.findAll()
            .filter {
                when(it.scheduleTime?.type) {
                    ScheduleTimeType.Arrival -> ZonedDateTime.now().isAfter(it.scheduleTime!!.time.minus(it.route.duration).minusMinutes(30))
                    ScheduleTimeType.Departure -> ZonedDateTime.now().isAfter(it.scheduleTime!!.time.minusMinutes(30))
                    null -> false
                }
            }
            .forEach {
                val completeRoute: CompleteRoute = geographyService.createCompleteRouteBasedOnUserStops(it.route.start, it.passengers, it.route.destination)

                val newDrive: Drive = Drive(
                        it.driver,
                        Car(
                            it.car.brand,
                            it.car.model,
                            it.car.color,
                            it.car.licencePlate
                        ),
                        completeRoute,
                        it.passengers.map{it.user},
                        when(it.scheduleTime?.type) {
                            ScheduleTimeType.Arrival -> it.scheduleTime?.time?.minus(completeRoute.duration) ?: return@forEach
                            ScheduleTimeType.Departure -> it.scheduleTime?.time ?: return@forEach
                            null -> return@forEach
                        },
                        when(it.scheduleTime?.type) {
                            ScheduleTimeType.Arrival -> it.scheduleTime?.time ?: return@forEach
                            ScheduleTimeType.Departure -> it.scheduleTime?.time?.plus(completeRoute.duration) ?: return@forEach
                            null -> return@forEach
                        }
                    )
                it.car.image?.let { driveOfferCarImage -> newDrive.car.image = imagesRepository.copy(driveOfferCarImage) }
                val storedDrive = drivesRepository.saveAndFlush(newDrive)
                if(it is CarpoolDriveOffer) {
                    it.carpool.addDrive(storedDrive)
                    carpoolsRepository.save(it.carpool)
                }
                driveOffersRepository.delete(it)
            }

        drivesRepository.flush()
        driveOffersRepository.flush()
    }
}