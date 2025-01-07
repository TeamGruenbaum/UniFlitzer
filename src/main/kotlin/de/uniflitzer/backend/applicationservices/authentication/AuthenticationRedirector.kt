package de.uniflitzer.backend.applicationservices.authentication

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthenticationRedirector {
    @GetMapping("/authenticationClientCallback")
    fun redirect(request: HttpServletRequest, response: HttpServletResponse) {
        val pathAndQuery = if (request.queryString != null) "${request.requestURI}?${request.queryString}" else request.requestURI
        val newUrl = pathAndQuery.replace("/authenticationClientCallback", "de.flitzer.app://callback")

        response.sendRedirect(newUrl)
    }
}