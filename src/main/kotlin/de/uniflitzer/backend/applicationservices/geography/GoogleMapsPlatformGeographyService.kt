package de.uniflitzer.backend.applicationservices.geography

import com.fasterxml.jackson.databind.JsonNode
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
    private fun computeRoute(start: Coordinate, stops: List<Waypoint>?, destination: Coordinate): JsonNode
    {
        val responseBody: String = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create("https://routes.googleapis.com/directions/v2:computeRoutes?key=${environment.getProperty("google.maps.platform.api-key") ?: throw IllegalStateException("google.maps.platform.api-key is not set")}&fields=routes.polyline,routes.optimized_intermediate_waypoint_index"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(ObjectMapper().writeValueAsString(
                    ComputeRoutesRequest(
                        origin = Waypoint(
                            location = Location(LatLng(start.latitude, start.longitude))
                        ),
                        destination = Waypoint(
                            location = Location(LatLng(destination.latitude, destination.longitude))
                        ),
                        intermediates = stops
                    )
                ))).build(),
            HttpResponse.BodyHandlers.ofString()
        ).body()

        return ObjectMapper()
            .readTree(responseBody)
            .path("routes")
            .get(0) ?: throw RouteComputationError("Route could not be computed.")
    }

    override fun createCompleteRouteBasedOnUserStops(start: Position, stops: List<UserStop>, destination: Position): CompleteRoute {
        return computeRoute(
            start.coordinate,
            stops.flatMap {
                listOf(
                    Waypoint(
                        location = Location(LatLng(it.start.coordinate.latitude, it.start.coordinate.longitude))
                    ),
                    Waypoint(
                        location = Location(LatLng(it.destination.coordinate.latitude, it.destination.coordinate.longitude))
                    )
                )
            },
            destination.coordinate
        )
        .let {
            CompleteRoute(
                start = start,
                destination = destination,
                userStops = stops.map { ConfirmableUserStop(it.user, it.start, it.destination, false) },
                polyline = GeoJsonLineString(
                    it.path("polyline")
                        .path("geoJsonLinestring")
                        .path("coordinates")
                        .toList().map { coordinate -> Coordinate(coordinate[1].asDouble(), coordinate[0].asDouble()) })
            )
        }
    }

    override fun createCompleteRouteBasedOnConfirmableUserStops(start: Position, stops: List<ConfirmableUserStop>, destination: Position): CompleteRoute
    {
        return computeRoute(
            start.coordinate,
            stops.flatMap {
                listOf(
                    Waypoint(
                        location = Location(LatLng(it.start.coordinate.latitude, it.start.coordinate.longitude))
                    ),
                    Waypoint(
                        location = Location(LatLng(it.destination.coordinate.latitude, it.destination.coordinate.longitude))
                    )
                )
            },
            destination.coordinate
        )
        .let {
            CompleteRoute(
                start = start,
                destination = destination,
                userStops = stops.map { ConfirmableUserStop(it.user, it.start, it.destination, false) },
                polyline = GeoJsonLineString(
                    it.path("polyline")
                        .path("geoJsonLinestring")
                        .path("coordinates")
                        .toList().map { coordinate -> Coordinate(coordinate[1].asDouble(), coordinate[0].asDouble()) })
            )
        }
    }

    override fun createRoute(start: Position, destination: Position): Route
    {
        return computeRoute(
            start.coordinate,
            null,
            destination.coordinate
        )
        .let {
            Route(
                start = start,
                destination = destination,
                polyline = GeoJsonLineString(
                    it.path("polyline")
                        .path("geoJsonLinestring")
                        .path("coordinates")
                        .toList().map { coordinate -> Coordinate(coordinate[1].asDouble(), coordinate[0].asDouble()) })
            )
        }
    }

    override fun createPosition(coordinate: Coordinate): Position {
        var responseBody: String = httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create("https://maps.googleapis.com/maps/api/geocode/json?latlng=${coordinate.latitude},${coordinate.longitude}&language=de&key=${environment.getProperty("google.maps.platform.api-key") ?: throw IllegalStateException("google.maps.platform.api-key is not set")}"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        .body()

        val address: Address? = ObjectMapper()
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
                    street = it.find { entry -> "route" in entry.key }?.value ?: return@let null,
                    houseNumber = it.find { entry -> "street_number" in entry.key}?.value ?: return@let null,
                    postalCode = it.find { entry -> "postal_code" in entry.key }?.value ?: return@let null,
                    city = it.find { entry -> "locality" in entry.key }?.value ?: return@let null
                )
            }

        return Position(coordinate, address)
    }

    private data class ComputeRoutesRequest(
        val origin: Waypoint,
        val destination: Waypoint,
        val intermediates: List<Waypoint>? = null,
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

    class RouteComputationError(message: String): RuntimeException(message)
}