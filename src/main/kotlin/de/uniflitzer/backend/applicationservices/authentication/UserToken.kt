package de.uniflitzer.backend.applicationservices.authentication

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class UserToken(private val jwt: Jwt, private val authenticated:Boolean) : JwtAuthenticationToken(jwt)
{
    init {
        super.isAuthenticated = authenticated
    }

    val id: String = jwt.claims?.get("sub") as? String ?: throw IllegalArgumentException("ID (Claim \"sub\") not found in JWT")
}

