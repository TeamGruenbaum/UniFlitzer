package de.uniflitzer.backend

import de.uniflitzer.backend.applicationservices.geography.GeographyService
import de.uniflitzer.backend.model.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.ThrowingSupplier
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
        val correctCoordinate: Coordinate = Assertions.assertDoesNotThrow(ThrowingSupplier{ Coordinate(0.0, 0.0) })

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
    fun `test address value range`(){
        val validStreet: String = "Alfons-Goppel-Platz"
        val validHouseNumber: String = "1"
        val validPostalCode: String = "95028"
        val validCity: String = "Hof"

        val correctAddress: Address = Assertions.assertDoesNotThrow(ThrowingSupplier { Address(validStreet, validHouseNumber, validPostalCode, validCity) } )

        val wrongUpperAddressException: IllegalArgumentException = Assertions.assertThrows(IllegalArgumentException::class.java, { Address("a".repeat(101), validHouseNumber, validPostalCode, validCity) })
        Assertions.assertEquals("Property street with value ${"a".repeat(101)} is not between 2 and 100 characters long.", wrongUpperAddressException.message)

        val wrongLowerAddressException: IllegalArgumentException = Assertions.assertThrows(IllegalArgumentException::class.java, { Address("", validHouseNumber, validPostalCode, validCity) })
        Assertions.assertEquals("Property street with value ${""} is not between 2 and 100 characters long.", wrongLowerAddressException.message)

        val wrongUpperHouseNumberException: IllegalArgumentException = Assertions.assertThrows(IllegalArgumentException::class.java, { Address(validStreet, "123456", validPostalCode, validCity) })
        Assertions.assertEquals("Property houseNumber with value 123456 is not between 1 and 5 characters long.", wrongUpperHouseNumberException.message)

        val wrongLowerHouseNumberException: IllegalArgumentException = Assertions.assertThrows(IllegalArgumentException::class.java, { Address(validStreet, "", validPostalCode, validCity) })
        Assertions.assertEquals("Property houseNumber with value ${""} is not between 1 and 5 characters long.", wrongLowerHouseNumberException.message)

        val wrongUpperPostalCodeException: IllegalArgumentException = Assertions.assertThrows(IllegalArgumentException::class.java, { Address(validStreet, validHouseNumber, "123456", validCity) })
        Assertions.assertEquals("Property postalCode with value 123456 is not between 5 and 5 characters long.", wrongUpperPostalCodeException.message)

        val wrongLowerPostalCodeException: IllegalArgumentException = Assertions.assertThrows(IllegalArgumentException::class.java, { Address(validStreet, validHouseNumber, "1234", validCity) })
        Assertions.assertEquals("Property postalCode with value 1234 is not between 5 and 5 characters long.", wrongLowerPostalCodeException.message)

        val wrongUpperCityException: IllegalArgumentException = Assertions.assertThrows(IllegalArgumentException::class.java, { Address(validStreet, validHouseNumber, validPostalCode, "a".repeat(101)) })
        Assertions.assertEquals("Property city with value ${"a".repeat(101)} is not between 2 and 100 characters long.", wrongUpperCityException.message)

        val wrongLowerCityException: IllegalArgumentException = Assertions.assertThrows(IllegalArgumentException::class.java, { Address(validStreet, validHouseNumber, validPostalCode, "a") })
        Assertions.assertEquals("Property city with value a is not between 2 and 100 characters long.", wrongLowerCityException.message)
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
        val geographyService: GeographyService = applicationContext.getBean(GeographyService::class.java)

        val blumenGruenertCoordinate: Coordinate = Coordinate(50.321914930754, 11.935010426656035)
        val blumenGruenertPosition: Position = geographyService.createPosition(blumenGruenertCoordinate)

        val michaelisApothekeCoordinate: Coordinate = Coordinate(50.321934460945286, 11.924205570055074)
        val michaelisApothekePosition: Position = geographyService.createPosition(michaelisApothekeCoordinate)

        val blumenGruenertToMichaelisApothekeRoute: Route = geographyService.createRoute(blumenGruenertPosition, michaelisApothekePosition)

        val coordinateOnRoute1: Coordinate = Coordinate(50.32152222422006, 11.924410447237701)
        val coordinateOnRoute2: Coordinate = Coordinate(50.32156971650696, 11.92964458757005)

        Assertions.assertTrue(blumenGruenertToMichaelisApothekeRoute.isCoordinateOnRoute(coordinateOnRoute1, Meters(100.0)))
        Assertions.assertTrue(blumenGruenertToMichaelisApothekeRoute.isCoordinateOnRoute(coordinateOnRoute2, Meters(100.0)))

        val coordinateNotOnRoute1: Coordinate = Coordinate(50.32017986625885, 11.927788820708288)
        val coordinateNotOnRoute2: Coordinate = Coordinate(50.32554786264958, 11.940987792633141)

        Assertions.assertFalse(blumenGruenertToMichaelisApothekeRoute.isCoordinateOnRoute(coordinateNotOnRoute1, Meters(100.0)))
        Assertions.assertFalse(blumenGruenertToMichaelisApothekeRoute.isCoordinateOnRoute(coordinateNotOnRoute2, Meters(100.0)))

        val coordinateNearStart: Coordinate = Coordinate(50.32208785993731, 11.932764423621471)
        val coordinateNearDestination: Coordinate = Coordinate(50.322126045213274, 11.923888028046658)

        Assertions.assertTrue(blumenGruenertToMichaelisApothekeRoute.areCoordinatesInCorrectDirection(coordinateNearStart, coordinateNearDestination))
        Assertions.assertFalse(blumenGruenertToMichaelisApothekeRoute.areCoordinatesInCorrectDirection(coordinateNearDestination, coordinateNearStart))
    }
}