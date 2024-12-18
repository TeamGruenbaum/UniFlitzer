package de.uniflitzer.backend.applicationservices.authentication

import com.fasterxml.jackson.databind.ObjectMapper
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP
import jakarta.servlet.http.HttpServletResponse
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.Environment
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class AuthenticationConfigurator(
    @field:Autowired private val environment: Environment
) {
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
                .authenticationEntryPoint { _, response, _ ->
                    response.apply {
                        status = HttpServletResponse.SC_UNAUTHORIZED
                        contentType = "application/json"
                        writer.write(ObjectMapper().writeValueAsString(ErrorDP("You need to provide a valid token to access this resource.")))
                    }
                }
            }
            .sessionManagement { sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .csrf { csrf -> csrf.disable() }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/api-documentation/**").permitAll()
                    .anyRequest().authenticated()
            }
            .cors(Customizer.withDefaults())

        return http.build()
    }

    @Bean
    fun keycloakAdministrator(): Keycloak {
        return KeycloakBuilder.builder()
            .serverUrl(environment.getProperty("keycloak.url") ?: "http://localhost:8080")
            .realm(environment.getProperty("keycloak.administrator.realm.name") ?: "master")
            .clientId(environment.getProperty("keycloak.administrator.clientId") ?: "admin-cli")
            .username(environment.getProperty("keycloak.administrator.username") ?: "admin")
            .password(environment.getProperty("keycloak.administrator.password") ?: "admin")
            .build()
    }
}