package de.uniflitzer.backend

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.ThrowingSupplier
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.resource.ClientResource
import org.keycloak.representations.idm.ClientRepresentation
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTests {
	private lateinit var httpClient: HttpClient

	private lateinit var resourceServerUrl: String

	private lateinit var keycloakServerUrl: String
	private lateinit var keycloakAdministrator: Keycloak
	private lateinit var keycloakUniFlitzerRealmName: String
	private lateinit var keycloakUniFlitzerClientId: String
	private lateinit var keycloakUniFlitzerClientSecret: String

	@BeforeAll
	fun `set up`() {
		httpClient = HttpClient.newBuilder().build()

		resourceServerUrl = "http://localhost:8080"

		// NOTE: We have to use the Keycloak Admin API and set the keycloak client to non-public,
		// because normally registration is only possible with authorization code flow with PKCE which needs manual interaction with a browser.
		keycloakServerUrl = "http://localhost:7375"
		keycloakAdministrator = Keycloak.getInstance(keycloakServerUrl, "master", "admin", "admin", "admin-cli")
		keycloakUniFlitzerRealmName = "uniflitzer"
		keycloakUniFlitzerClientId = "uniflitzer_frontend"
		val keycloakUniFlitzerClientRepresentation: ClientRepresentation = keycloakAdministrator.realm(keycloakUniFlitzerRealmName).clients()
															.findByClientId("uniflitzer_frontend")
															.first()
															.apply { isPublicClient = false; isDirectAccessGrantsEnabled = true }
		val keycloakUniFlitzerClientResource: ClientResource = keycloakAdministrator.realm(keycloakUniFlitzerRealmName).clients().get(keycloakUniFlitzerClientRepresentation.getId());
		keycloakUniFlitzerClientResource.update(keycloakUniFlitzerClientRepresentation)
		keycloakUniFlitzerClientSecret = keycloakUniFlitzerClientResource.secret.value
	}

	private fun createTestUser(userName: String, password: String, firstName: String, lastName: String, birthday: String, gender: String, street: String, houseNumber: String, postalCode: String, city: String, studyProgramme: String): Pair<UUID, String> {
		keycloakAdministrator.realm(keycloakUniFlitzerRealmName).users().create(
			UserRepresentation().apply {
				isEnabled = true
				username = userName
				email = "${firstName.lowercase()}.${lastName.lowercase()}@hof-university.de"
				isEmailVerified = true
				credentials = listOf(
					CredentialRepresentation().apply {
						type = CredentialRepresentation.PASSWORD
						value = password
						isTemporary = false
					}
				)
			}
		)
		val userAccessToken: String = Keycloak.getInstance(keycloakServerUrl, keycloakUniFlitzerRealmName, userName, password, keycloakUniFlitzerClientId, keycloakUniFlitzerClientSecret).tokenManager().accessTokenString
		val userCreationResponse: HttpResponse<String> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/users"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $userAccessToken"
				)
				.POST(
					HttpRequest.BodyPublishers.ofString(
						"""
						{
							"firstName": "$firstName",
							"lastName": "$lastName",
							"birthday": "$birthday",
							"gender": "$gender",
							"address": {
								"street": "$street",
								"houseNumber": "$houseNumber",
								"postalCode": "$postalCode",
								"city": "$city"
							},
							"studyProgramme": "$studyProgramme"
						}
						"""
					)
				)
				.build(),
			HttpResponse.BodyHandlers.ofString()
		)
		println()
		Assertions.assertEquals(201, userCreationResponse.statusCode())
		val userId: UUID = Assertions.assertDoesNotThrow(ThrowingSupplier { UUID.fromString(ObjectMapper().readTree(userCreationResponse.body()).get("id").asText()) })

		return Pair(userId, userAccessToken)
	}

	@Test
	fun `test public drive offer joining`() {
		//Anna registers an account
		val (annasId: UUID, annasAccessToken: String) = createTestUser(
			"anna",
			"12345678",
			"Anna",
			"Riema",
			"2001-03-18T00:00:00+01:00",
			"Female",
			"Albert-Einstein-Straße",
			"2",
			"95028",
			"Hof",
			"Master Informatik"
		)

		//Hans registers an account
		val (hansId: UUID, hansAccessToken: String) = createTestUser(
			"hans",
			"abcdefgh",
			"Hans",
			"Girah",
			"2003-07-23T00:00:00+01:00",
			"Male",
			"Fabrikzeile",
			"26",
			"95028",
			"Hof",
			"Kommunikationsdesign"
		)

		//Anna adds a car
		val annasPorscheCaymenSCreationResponse: HttpResponse<Void> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/users/$annasId/cars"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $annasAccessToken"
				)
				.POST(
					HttpRequest.BodyPublishers.ofString(
						"""
						{
							"brand": "Porsche",
							"model": "Cayman S",
							"color": "Grau",
							"licencePlate": "HO PJ 817"
						}
						"""
					)
				)
				.build(),
			HttpResponse.BodyHandlers.discarding()
		)
		Assertions.assertEquals(201, annasPorscheCaymenSCreationResponse.statusCode())

		//Anna creates a public drive offer
		val annasPublicDriveOfferCreationResponse: HttpResponse<String> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/drive-offers"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $annasAccessToken"
				)
				.POST(
					HttpRequest.BodyPublishers.ofString(
						"""
						{
							"carIndex": 0,
							"freeSeats": 1,
							"route": {
								"start": {
									"latitude": 50.32540777316511,
									"longitude": 11.941037826886907
								},
								"destination": {
									"latitude": 50.322583862456696, 
									"longitude": 11.92298060964375
								}
							},
							"scheduleTime": {
								"time": "${ZonedDateTime.now().plusHours(2).toOffsetDateTime()}",
								"type": "Departure"
							},
							"type": "PublicDriveOfferCreationDP"
						}
						"""
					)
				)
				.build(),
			HttpResponse.BodyHandlers.ofString()
		)
		Assertions.assertEquals(201, annasPublicDriveOfferCreationResponse.statusCode())
		val annasPublicDriveOfferId: UUID = Assertions.assertDoesNotThrow(
			ThrowingSupplier { UUID.fromString(ObjectMapper().readTree(annasPublicDriveOfferCreationResponse.body()).get("id").asText()) }
		)

		//Hans request to join Anna's public drive offer
		val hansPublicDriverOfferSeatRequestResponse: HttpResponse<Void> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/drive-offers/$annasPublicDriveOfferId/requests"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $hansAccessToken"
				)
				.POST(HttpRequest.BodyPublishers.ofString(
					"""
					{
						"start": {
							"latitude": 50.32485691295483,
							"longitude": 11.940821727429373
						},
						"destination": {
							"latitude": 50.32205962507589,
							"longitude": 11.923879268641535
						}
					}
					"""
				))
				.build(),
			HttpResponse.BodyHandlers.discarding()
		)
		Assertions.assertEquals(204, hansPublicDriverOfferSeatRequestResponse.statusCode())

		//Anna accepts Hans' request
		val annasPublicDriverOfferSeatRequestAcceptanceResponse: HttpResponse<Void> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/drive-offers/$annasPublicDriveOfferId/requesting-users/$hansId/acceptances"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $annasAccessToken"
				)
				.POST(HttpRequest.BodyPublishers.noBody())
				.build(),
			HttpResponse.BodyHandlers.discarding()
		)
		Assertions.assertEquals(204, annasPublicDriverOfferSeatRequestAcceptanceResponse.statusCode())

		//Anna loads her drive offer
		val annasPublicDriveOfferResponse: HttpResponse<String> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/drive-offers/$annasPublicDriveOfferId"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $annasAccessToken"
				)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		)
		Assertions.assertEquals(200, annasPublicDriveOfferResponse.statusCode())
		Assertions.assertTrue(
			Regex("""\{"type":"DetailedPublicDriveOfferDP","containsFavoriteDriver":false,"id":"$annasPublicDriveOfferId","driver":\{"id":"$annasId","firstName":"Anna","lastName":"Riema","averageStars":null,"numberOfRatings":0\},"car":\{"brand":"Porsche","model":"Cayman S","color":"Grau","licencePlate":"HO PJ 817"\},"freeSeats":1,"route":\{"start":\{"coordinate":\{"latitude":50\.32540777316511,"longitude":11\.941037826886907\},"nearestAddress":.+?\},"destination":\{"coordinate":\{"latitude":50\.322583862456696,"longitude":11\.92298060964375\},"nearestAddress":.+?\},"duration":.+?\,"polyline":.+?\},"passengers":\[\{"user":\{"id":"$hansId","firstName":"Hans","lastName":"Girah","averageStars":null,"numberOfRatings":0\},"start":\{"coordinate":\{"latitude":50\.32485691295483,"longitude":11\.940821727429373\},"nearestAddress":.+?\},"destination":\{"coordinate":\{"latitude":50\.32205962507589,"longitude":11\.923879268641535\},"nearestAddress":.+?\}\}\],"scheduleTime":\{"time":".+?","type":"Departure"\},"requestingUsers":\[\]\}""")
			.matches(annasPublicDriveOfferResponse.body())
		)

		//Anna loads her drive offers where she is the driver and Hans loads his drive offers where he is a passenger
		val expectedDriveOffersResponseBody: Regex = Regex("""\{"maximumPage":1,"content":\[\{"type":"PartialPublicDriveOfferDP","id":"$annasPublicDriveOfferId","driver":\{"id":"$annasId","firstName":"Anna","lastName":"Riema","averageStars":null,"numberOfRatings":0\},"freeSeats":1,"route":\{"start":\{"coordinate":\{"latitude":50\.32540777316511,"longitude":11\.941037826886907\},"nearestAddress":.+?\},"destination":\{"coordinate":\{"latitude":50\.322583862456696,"longitude":11\.92298060964375\},"nearestAddress":.+?\},"duration":.+?\},"passengersCount":1,"scheduleTime":\{"time":".+?","type":"Departure"\},"requestingUserIds":\[\],"containsFavoriteDrive":false\}\]\}""")

		val annasDriveOffersAsDriver: HttpResponse<String> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/users/$annasId/drive-offers?role=Driver&pageNumber=1&perPage=10"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $annasAccessToken"
				)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		)
		Assertions.assertEquals(200, annasDriveOffersAsDriver.statusCode())
		Assertions.assertTrue(expectedDriveOffersResponseBody.matches(annasDriveOffersAsDriver.body()))

		val hansDriveOffersAsPassenger: HttpResponse<String> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/users/$hansId/drive-offers?role=Passenger&pageNumber=1&perPage=10"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $hansAccessToken"
				)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		)
		Assertions.assertEquals(200, hansDriveOffersAsPassenger.statusCode())
		Assertions.assertTrue(expectedDriveOffersResponseBody.matches(hansDriveOffersAsPassenger.body()))
	}

	@Test
	fun `test carpool joining`() {
		//Maria registers an account
		val (mariasId: UUID, mariasAccessToken: String) = createTestUser(
			"maria",
			"12344321",
			"Maria",
			"Lauer",
			"2002-08-10T00:00:00+01:00",
			"Female",
			"Albert-Einstein-Straße",
			"2",
			"95028",
			"Hof",
			"Maschinenbau"
		)

		//Tom registers an account
		val tomsUserName: String = "tom"
		val (tomsId: UUID, tomsAccessToken: String) = createTestUser(
			tomsUserName,
			"abcde123",
			"Tom",
			"Sommer",
			"2000-11-20T00:00:00+01:00",
			"Male",
			"Fabrikzeile",
			"26",
			"95028",
			"Hof",
			"Betriebswirtschaft"
		)

		//Maria creates a carpool
		val mariasCarpoolCreationResponse: HttpResponse<String> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/carpools"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $mariasAccessToken"
				)
				.POST(
					HttpRequest.BodyPublishers.ofString(
						"""
						{
							"name": "Uni-Pendler"
						}
						"""
					)
				)
				.build(),
			HttpResponse.BodyHandlers.ofString()
		)
		Assertions.assertEquals(201, mariasCarpoolCreationResponse.statusCode())
		val mariasCarpoolId: UUID = Assertions.assertDoesNotThrow(
			ThrowingSupplier { UUID.fromString(ObjectMapper().readTree(mariasCarpoolCreationResponse.body()).get("id").asText()) }
		)

		//Maria invites Tom to the carpool
		val mariasCarpoolInvitationResponse: HttpResponse<Void> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/carpools/$mariasCarpoolId/sent-invites/$tomsUserName"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $mariasAccessToken"
				)
				.POST(HttpRequest.BodyPublishers.noBody())
				.build(),
			HttpResponse.BodyHandlers.discarding()
		)
		Assertions.assertEquals(204, mariasCarpoolInvitationResponse.statusCode())

		//Tom accepts Maria's invitation to the carpool
		val tomsCarpoolInvitationAcceptanceResponse: HttpResponse<Void> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/carpools/$mariasCarpoolId/sent-invites/$tomsId/acceptances"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $tomsAccessToken"
				)
				.POST(HttpRequest.BodyPublishers.noBody())
				.build(),
			HttpResponse.BodyHandlers.discarding()
		)
		Assertions.assertEquals(204, tomsCarpoolInvitationAcceptanceResponse.statusCode())

		//Maria loads the carpool
		val mariasCarpoolResponse: HttpResponse<String> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/carpools/$mariasCarpoolId"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $mariasAccessToken"
				)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		)
		Assertions.assertEquals(200, mariasCarpoolResponse.statusCode())
		Assertions.assertTrue(
			Regex("""\{"id":"$mariasCarpoolId","name":"Uni-Pendler","users":\[\{"id":"$mariasId","firstName":"Maria","lastName":"Lauer","averageStars":null,"numberOfRatings":0\},\{"id":"$tomsId","firstName":"Tom","lastName":"Sommer","averageStars":null,"numberOfRatings":0\}\],"driveRequests":\[\],"driveOffers":\[\],"drives":\[\],"sentInvites":\[\]\}""")
				.matches(mariasCarpoolResponse.body())
		)

		//Maria loads her carpools and Tom loads his carpools
		val expectedCarpoolsResponseBody: Regex = Regex("""\{"maximumPage":1,"content":\[\{"id":"$mariasCarpoolId","name":"Uni-Pendler","users":\[\{"id":"$mariasId","firstName":"Maria","lastName":"Lauer","averageStars":null,"numberOfRatings":0\},\{"id":"$tomsId","firstName":"Tom","lastName":"Sommer","averageStars":null,"numberOfRatings":0\}\]\}\]\}""")

		val mariasCarpoolsResponse: HttpResponse<String> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/users/$mariasId/carpools?pageNumber=1&perPage=10"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $mariasAccessToken"
				)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		)
		Assertions.assertEquals(200, mariasCarpoolsResponse.statusCode())
		Assertions.assertTrue(expectedCarpoolsResponseBody.matches(mariasCarpoolsResponse.body()))

		val tomsCarpoolsResponse: HttpResponse<String> = httpClient.send(
			HttpRequest.newBuilder()
				.uri(URI.create("$resourceServerUrl/v1/users/$tomsId/carpools?pageNumber=1&perPage=10"))
				.headers(
					"Content-Type", "application/json",
					"Authorization", "Bearer $tomsAccessToken"
				)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		)
		Assertions.assertEquals(200, tomsCarpoolsResponse.statusCode())
		Assertions.assertTrue(expectedCarpoolsResponseBody.matches(tomsCarpoolsResponse.body()))
	}
}