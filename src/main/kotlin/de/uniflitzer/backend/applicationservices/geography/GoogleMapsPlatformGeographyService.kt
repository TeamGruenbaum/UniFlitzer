package de.uniflitzer.backend.applicationservices.geography

import com.fasterxml.jackson.databind.ObjectMapper
import de.uniflitzer.backend.model.Address
import de.uniflitzer.backend.model.CompleteRoute
import de.uniflitzer.backend.model.Coordinate
import de.uniflitzer.backend.model.Position
import de.uniflitzer.backend.model.UserStop
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class GoogleMapsPlatformGeographyService(
    @field:Autowired private val environment: Environment,
    @field:Autowired private val httpClient: HttpClient
): GeographyService {
    override fun createRoute(start: Position, stops: List<UserStop>, destination: Position): CompleteRoute {
        TODO()
    }

    override fun createPosition(coordinate: Coordinate): Position {
        var responseBody: String = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create("https://maps.googleapis.com/maps/api/geocode/json?latlng=${coordinate.latitude},${coordinate.longitude}&language=de&key=${environment.getProperty("google.maps.platform.api-key")}"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        .body()

        val address: Address = ObjectMapper()
            .readTree(responseBody)
            .path("results")
            .get(0)
            .path("address_components")
            .associateBy(
                { it.path("types").toList().map{it.textValue()} },
                { it.path("long_name").asText() }
            )
            .entries
            .let {
                Address(
                    street = it.find { entry -> entry.key.contains("route") }?.value!!,
                    houseNumber = it.find { entry -> entry.key.contains("street_number") }?.value!!,
                    postalCode = it.find { entry -> entry.key.contains("postal_code") }?.value!!,
                    city = it.find { entry -> entry.key.contains("locality") }?.value!!
                )
            }

        return Position(coordinate, address)
    }
}