package de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.servers.Server

@OpenAPIDefinition(
    servers = [
        Server(url = "https://uniflitzer-api.stevensolleder.de"),
        Server(url = "http://localhost:8080")
    ],
    info = Info(
        title = "UniFlitzer API",
        version = "v0.0.1",
        description = "The UniFlitzer API enables users to offer, join or request rides and communicate with each other. It also promotes the formation of long-term carpools. Verifying through university login and providing detailed information about drivers and passengers, ensures a safe, student-only community",
    )
)
@SecurityScheme(
    name = "bearerAuthentication",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
)
class GeneralDocumentationInformationAdder