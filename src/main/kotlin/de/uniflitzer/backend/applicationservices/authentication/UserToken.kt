package de.uniflitzer.backend.applicationservices.authentication

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class UserToken(private val jwt: Jwt, private val authenticated:Boolean) : JwtAuthenticationToken(jwt)
{
    init {
        super.setAuthenticated(authenticated)
    }

    val id: String = jwt.claims?.get("sub") as? String ?: throw IllegalStateException("ID (Claim \"sub\") not found in JWT")
}

