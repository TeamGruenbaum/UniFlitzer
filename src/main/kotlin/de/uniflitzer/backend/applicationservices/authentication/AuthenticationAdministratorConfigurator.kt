package de.uniflitzer.backend.applicationservices.authentication

import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component


@Component
class AuthenticationAdministratorConfigurator(
    @field:Autowired private val environment: Environment
) {
    @Bean
    fun authenticationAdministrator(): Keycloak {
        return KeycloakBuilder.builder()
            .serverUrl(environment.getProperty("keycloak.url") ?: throw IllegalStateException("keycloak.url is not set"))
            .realm(environment.getProperty("keycloak.administrator.realm.name") ?: throw IllegalStateException("keycloak.administrator.realm.name is not set"))
            .clientId(environment.getProperty("keycloak.administrator.clientId") ?: throw IllegalStateException("keycloak.administrator.clientId is not set"))
            .username(environment.getProperty("keycloak.administrator.username") ?: throw IllegalStateException("keycloak.administrator.username is not set"))
            .password(environment.getProperty("keycloak.administrator.password") ?: throw IllegalStateException("keycloak.administrator.password is not set"))
            .build()
    }
}