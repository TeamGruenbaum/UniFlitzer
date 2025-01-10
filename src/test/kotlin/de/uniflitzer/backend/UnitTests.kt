package de.uniflitzer.backend

import de.uniflitzer.backend.applicationservices.geography.GeographyService
import de.uniflitzer.backend.model.Address
import de.uniflitzer.backend.model.Coordinate
import de.uniflitzer.backend.model.Position
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnitTests {
    private lateinit var applicationContext: ApplicationContext

    @BeforeAll
    fun `set up`() {
        System.setProperty("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")))
        applicationContext = runApplication<UniFlitzer>()
    }

    @Test
    fun `test coordinate value range`(){
        val wrongUpperLatitudeException: IllegalArgumentException = Assertions.assertThrows(IllegalArgumentException::class.java, { Coordinate(91.0, 0.0) })
        Assertions.assertEquals("Property latitude with value 91.0 is not between -90 and 90.", wrongUpperLatitudeException.message)

        val wrongLowerLatitudeException: IllegalArgumentException = Assertions.assertThrows(IllegalArgumentException::class.java, { Coordinate(-91.0, 0.0) })
        Assertions.assertEquals("Property latitude with value -91.0 is not between -90 and 90.", wrongLowerLatitudeException.message)

        val wrongUpperLongitudeException: IllegalArgumentException = Assertions.assertThrows(IllegalArgumentException::class.java, { Coordinate(0.0, 181.0) })
        Assertions.assertEquals("Property longitude with value 181.0 is not between -180 and 180.", wrongUpperLongitudeException.message)

        val wrongLowerLongitudeException: IllegalArgumentException = Assertions.assertThrows(IllegalArgumentException::class.java, { Coordinate(0.0, -181.0) })
        Assertions.assertEquals("Property longitude with value -181.0 is not between -180 and 180.", wrongLowerLongitudeException.message)
    }

    @Test
    fun `test correct position generation`() {
        val geographyService: GeographyService = applicationContext.getBean(GeographyService::class.java)

        val hofUniversityCoordinate: Coordinate = Coordinate(50.325478040266184, 11.94101979755218)
        val hofUniversityPosition: Position = geographyService.createPosition(hofUniversityCoordinate)
        Assertions.assertEquals(
            hofUniversityPosition,
            Position(hofUniversityCoordinate, Address("Alfons-Goppel-Platz", "1", "95028", "Hof")),
        )

        val southPacificCoordinate: Coordinate = Coordinate(-30.869306970138684, -138.52857306594365)
        val southPacificPosition: Position = geographyService.createPosition(southPacificCoordinate)
        Assertions.assertEquals(
            southPacificPosition,
            Position(southPacificCoordinate, null)
        )
    }

    @Test
    fun `test coordinates on route checks`() {

    }
}