package de.uniflitzer.backend.applicationservices.authentication

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class AuthenticationConfigurator {
    @Bean
    fun resourceServerSecurityFilterChain(http: HttpSecurity, authenticationConverter: Converter<Jwt?, AbstractAuthenticationToken?>?): SecurityFilterChain {
        http.oauth2ResourceServer { resourceServer ->
            resourceServer.jwt { jwtDecoder ->
                jwtDecoder.jwtAuthenticationConverter(authenticationConverter)
            }
        }

        http.oauth2ResourceServer { oauth2 ->
            oauth2.jwt { jwtConfigurer ->
                jwtConfigurer.jwtAuthenticationConverter { jwt ->
                    //Only after the jwt token has been validated, the UserToken-object is created, so we can call pass true for the authenticated parameter
                    UserToken(jwt, true)
                }
            }
        }

        http
            .sessionManagement { sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .csrf { csrf -> csrf.disable() }

        http.authorizeHttpRequests { requests ->
            requests
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/api-documentation/**").permitAll()
                .anyRequest().authenticated()
        }

        http.cors(Customizer.withDefaults())

        return http.build()
    }
}
