package de.uniflitzer.backend.applicationservices.authentication

import com.fasterxml.jackson.databind.ObjectMapper
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.stereotype.Component

@Configuration
@EnableWebSecurity
class AuthenticationConfigurator {
    @Bean
    fun resourceServerSecurityFilterChain(http: HttpSecurity, authenticationConverter: Converter<Jwt?, AbstractAuthenticationToken?>?): SecurityFilterChain {
        http
            .oauth2ResourceServer { resourceServer ->
                resourceServer.jwt { jwtDecoder ->
                    jwtDecoder.jwtAuthenticationConverter(authenticationConverter)
                }
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwtConfigurer ->
                    jwtConfigurer.jwtAuthenticationConverter { jwt ->
                        //Only after the jwt token has been validated, the UserToken-object is created, so we can call pass true for the authenticated parameter
                        UserToken(jwt, true)
                    }
                }
            }
            .sessionManagement { sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .csrf { csrf -> csrf.disable() }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { _, response, _ ->
                    response.apply {
                        status = HttpServletResponse.SC_UNAUTHORIZED
                        contentType = "application/json"
                        writer.write(ObjectMapper().writeValueAsString(ErrorDP("You need to provide a valid token to access this resource.")))
                    }
                }
            }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/api-documentation/**").permitAll()
                    .anyRequest().authenticated()
            }
            .cors(Customizer.withDefaults())

        return http.build()
    }
}