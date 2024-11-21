package de.uniflitzer.backend.applicationservices.authentication

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class UserToken(private val jwt: Jwt, private val authenticated:Boolean) : JwtAuthenticationToken(jwt)
{
    init {
        super.setAuthenticated(authenticated)
    }

    val id: String = jwt.claims?.get("sub") as? String ?: throw IllegalArgumentException("ID (Claim \"sub\") not found in JWT")
    val username: String = jwt.claims?.get("preferred_username") as? String ?: throw IllegalArgumentException("Username (Claim \"preferred_username\") not found in JWT")
    val email: String = jwt.claims?.get("email") as? String ?: throw IllegalArgumentException("E-Mail (Claim \"email\") not found in JWT")
}

