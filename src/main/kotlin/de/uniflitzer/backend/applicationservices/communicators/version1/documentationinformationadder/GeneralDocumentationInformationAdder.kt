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
    name = "Token Authentication (uniflitzer-auth.stevensolleder.de)",
    type = SecuritySchemeType.OAUTH2,
    bearerFormat = "JWT",
    scheme = "bearer",
    flows = OAuthFlows(
        authorizationCode = OAuthFlow(
            authorizationUrl = "https://uniflitzer-auth.stevensolleder.de/realms/uniflitzer/protocol/openid-connect/auth",
            tokenUrl = "https://uniflitzer-auth.stevensolleder.de/uniflitzer/protocol/openid-connect/token",
            scopes = [
                OAuthScope(name = "profile", description = "OpenID Connect built-in scope")
            ]
        )
    )
)
@SecurityScheme(
    name = "Token Authentication (localhost)",
    type = SecuritySchemeType.OAUTH2,
    bearerFormat = "JWT",
    scheme = "bearer",
    flows = OAuthFlows(
        authorizationCode = OAuthFlow(
            authorizationUrl = "http://localhost:7374/realms/uniflitzer/protocol/openid-connect/auth",
            tokenUrl = "http://localhost:7374/realms/uniflitzer/protocol/openid-connect/token",
            scopes = [
                OAuthScope(name = "profile", description = "OpenID Connect built-in scope")
            ]
        )
    )
)
class GeneralDocumentationInformationAdder