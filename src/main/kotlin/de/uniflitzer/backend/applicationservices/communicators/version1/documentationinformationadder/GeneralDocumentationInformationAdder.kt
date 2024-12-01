package de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.annotations.security.OAuthFlows
import io.swagger.v3.oas.annotations.security.OAuthFlow
import io.swagger.v3.oas.annotations.security.OAuthScope

@OpenAPIDefinition(
    servers = [
        Server(url = "\${base.url}"),
    ],
    info = Info(
        title = "UniFlitzer API",
        version = "v0.0.1",
        description = "The UniFlitzer API enables users to offer, join or request rides and communicate with each other. It also promotes the formation of long-term carpools. Verifying through university login and providing detailed information about drivers and passengers, ensures a safe, student-only community",
    )
)
@SecurityScheme(
    name = "Token Authentication",
    type = SecuritySchemeType.OAUTH2,
    bearerFormat = "JWT",
    scheme = "bearer",
    flows = OAuthFlows(
        authorizationCode = OAuthFlow(
            authorizationUrl = "\${keycloak.url.authorization}",
            tokenUrl = "\${keycloak.url.token}",
            refreshUrl = "\${keycloak.url.refresh}"
        )
    )
)
class GeneralDocumentationInformationAdder