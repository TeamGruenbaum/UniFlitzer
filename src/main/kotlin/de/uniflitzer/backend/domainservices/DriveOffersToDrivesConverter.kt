package de.uniflitzer.backend.domainservices

import de.uniflitzer.backend.applicationservices.geography.GeographyService
import de.uniflitzer.backend.model.Car
import de.uniflitzer.backend.model.CarpoolDriveOffer
import de.uniflitzer.backend.model.CompleteRoute
import de.uniflitzer.backend.model.Drive
import de.uniflitzer.backend.model.PublicDriveOffer
import de.uniflitzer.backend.model.ScheduleTimeType
import de.uniflitzer.backend.repositories.CarpoolsRepository
import de.uniflitzer.backend.repositories.DriveOffersRepository
import de.uniflitzer.backend.repositories.DrivesRepository
import de.uniflitzer.backend.repositories.ImagesRepository
import de.uniflitzer.backend.repositories.UsersRepository
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
    @field:Autowired private val usersRepository: UsersRepository,
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
            .forEach { driveOfferToConvert ->
                val completeRoute: CompleteRoute = geographyService.createCompleteRouteBasedOnUserStops(driveOfferToConvert.route.start, driveOfferToConvert.passengers, driveOfferToConvert.route.destination)

                val newDrive: Drive = Drive(
                    driveOfferToConvert.driver,
                    Car(
                        driveOfferToConvert.car.brand,
                        driveOfferToConvert.car.model,
                        driveOfferToConvert.car.color,
                        driveOfferToConvert.car.licencePlate
                    ),
                    completeRoute,
                    driveOfferToConvert.passengers.map { it.user },
                    when(driveOfferToConvert.scheduleTime?.type) {
                        ScheduleTimeType.Arrival -> driveOfferToConvert.scheduleTime?.time?.minus(completeRoute.duration) ?: return@forEach
                        ScheduleTimeType.Departure -> driveOfferToConvert.scheduleTime?.time ?: return@forEach
                        null -> return@forEach
                    },
                    when(driveOfferToConvert.scheduleTime?.type) {
                        ScheduleTimeType.Arrival -> driveOfferToConvert.scheduleTime?.time ?: return@forEach
                        ScheduleTimeType.Departure -> driveOfferToConvert.scheduleTime?.time?.plus(completeRoute.duration) ?: return@forEach
                        null -> return@forEach
                    }
                )
                driveOfferToConvert.car.image?.let { it -> newDrive.car.image = imagesRepository.copy(it) }
                val storedDrive = drivesRepository.saveAndFlush(newDrive)

                if(driveOfferToConvert is CarpoolDriveOffer) {
                    driveOfferToConvert.carpool.addDrive(storedDrive)
                    carpoolsRepository.save(driveOfferToConvert.carpool)
                }

                driveOfferToConvert.passengers.forEach { it.user.leaveDriveOfferAsPassenger(driveOfferToConvert) }
                usersRepository.saveAll(driveOfferToConvert.passengers.map { it.user })
                usersRepository.flush()
                if(driveOfferToConvert is PublicDriveOffer) {
                    driveOfferToConvert.requestingUsers.forEach { it.user.leaveDriveOfferAsRequestingUser(driveOfferToConvert) }
                    usersRepository.saveAll(driveOfferToConvert.requestingUsers.map { it.user })
                    usersRepository.flush()
                }
                driveOffersRepository.delete(driveOfferToConvert)
            }

        drivesRepository.flush()
        driveOffersRepository.flush()
    }
}