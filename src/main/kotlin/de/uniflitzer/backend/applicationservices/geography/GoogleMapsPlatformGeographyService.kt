package de.uniflitzer.backend.applicationservices.geography

import com.fasterxml.jackson.databind.ObjectMapper
import de.uniflitzer.backend.model.*
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
        val responseBody: String = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create("https://routes.googleapis.com/directions/v2:computeRoutes?key=${environment.getProperty("google.maps.platform.api-key")}&fields=routes.polyline,routes.optimized_intermediate_waypoint_index"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(ObjectMapper().writeValueAsString(
                    ComputeRoutesRequest(
                        origin = Waypoint(
                            location = Location(LatLng(start.coordinate.latitude, start.coordinate.longitude))
                        ),
                        destination = Waypoint(
                            location = Location(LatLng(destination.coordinate.latitude, destination.coordinate.longitude))
                        ),
                        intermediates = stops.map {
                            Waypoint(
                                location = Location(LatLng(it.position.coordinate.latitude, it.position.coordinate.longitude))
                            )
                        }
                    )
                ))).build(),
            HttpResponse.BodyHandlers.ofString()
        ).body()

        val completeRoute:CompleteRoute = ObjectMapper()
            .readTree(responseBody)
            .path("routes")
            .get(0)
            .let {
                CompleteRoute(
                    start = start,
                    destination = destination,
                    userStops = stops.map { ConfirmableUserStop(it.user, it.position, false) },
                    polyline = GeoJsonLineString(
                        it.path("polyline")
                            .path("geoJsonLinestring")
                            .path("coordinates")
                            .toList().map { coordinate -> Coordinate(coordinate[1].asDouble(), coordinate[0].asDouble()) })
                )
            }

        return completeRoute
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

    private data class ComputeRoutesRequest(
        val origin: Waypoint,
        val destination: Waypoint,
        val intermediates: List<Waypoint>,
        val travelMode: String = "DRIVE",
        val polylineQuality: String = "HIGH_QUALITY",
        val polylineEncoding: String = "GEO_JSON_LINESTRING",
        val computeAlternativeRoutes: Boolean = false,
        val optimizeWaypointOrder: Boolean = true
    )

    private data class Waypoint(
        val via: Boolean = false,
        val location: Location
    )

    private data class Location(val latLng: LatLng)

    private data class LatLng(
        val latitude: Double,
        val longitude: Double
    )
}