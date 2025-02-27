package de.uniflitzer.backend.applicationservices.authentication

import com.fasterxml.jackson.databind.ObjectMapper
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP
import de.uniflitzer.backend.applicationservices.communicators.version1.localization.LocalizationService
import jakarta.servlet.http.HttpServletResponse
import jakarta.ws.rs.NotFoundException
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.ClientRepresentation
import org.keycloak.representations.idm.ClientScopeRepresentation
import org.keycloak.representations.idm.ProtocolMapperRepresentation
import org.keycloak.representations.idm.RealmRepresentation
import org.keycloak.representations.userprofile.config.UPAttribute
import org.keycloak.representations.userprofile.config.UPAttributePermissions
import org.keycloak.representations.userprofile.config.UPAttributeRequired
import org.keycloak.representations.userprofile.config.UPConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.Environment
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.UUID
import kotlin.system.exitProcess

@Configuration
@EnableWebSecurity
class AuthenticationConfigurator(
    @field:Autowired private val environment: Environment,
    @field:Autowired private val authenticationConfigurator: Keycloak,
    @field:Autowired private val localizationService: LocalizationService
):InitializingBean {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val newRealmName: String? = environment.getProperty("keycloak.realm.name")
        .let {
            if(it == null){
                logger.error("keycloak.realm.name is not set.")
                exitProcess(1)
            }

            return@let it
        }

    @Bean
    fun configureAuthentication(http: HttpSecurity, authenticationConverter: Converter<Jwt?, AbstractAuthenticationToken?>?): SecurityFilterChain {
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
                        characterEncoding = "UTF-8"
                        writer.write(ObjectMapper().writeValueAsString(ErrorDP(localizationService.getMessage("error.unauthorized"))))
                    }
                }
            }
            .sessionManagement { sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .csrf { csrf -> csrf.disable() }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/api-documentation/**").permitAll()
                    .requestMatchers("/authenticationClientCallback").permitAll()
                    .anyRequest().authenticated()
            }
            .cors {
                it.configurationSource(
                    UrlBasedCorsConfigurationSource().apply {
                        registerCorsConfiguration(
                            "/**",
                            CorsConfiguration().apply {
                                allowedOrigins = listOf(environment.getProperty("keycloak.url") ?: throw IllegalStateException("keycloak.webOrigins is not set."));
                        });
                    }
                )
            }

        return http.build()
    }

    override fun afterPropertiesSet() {
        when (environment.getProperty("keycloak.setup")) {
            "disabled" -> return@afterPropertiesSet
            "recreate-if-not-exists" -> {
                try { authenticationConfigurator.realm(newRealmName).toRepresentation() }
                catch (_: NotFoundException) { recreateAuthenticationData() }
            }
            "recreate-always" -> recreateAuthenticationData()
            else -> logger.warn("keycloak.setup is not set.")
        }
    }

    private fun recreateAuthenticationData() {
        val logId: UUID = UUID.randomUUID()

        try {
            try { authenticationConfigurator.realm(newRealmName).remove() }
            catch(_: NotFoundException){}
            catch(exception: Exception){ throw exception }
            logger.info("$logId: Keycloak realm with name $newRealmName was removed.")

            authenticationConfigurator.realms().create(
                RealmRepresentation().apply {
                    realm = newRealmName
                    isEnabled = true
                    isEditUsernameAllowed = true
                    isRegistrationAllowed = true
                    accessTokenLifespan = 3600
                    ssoSessionIdleTimeout = 1209600
                    clientSessionIdleTimeout = 1209600
                }
            )
            authenticationConfigurator.realm(newRealmName).users().userProfile().update(
                UPConfig().apply {
                    attributes = listOf(
                        UPAttribute().apply {
                            name = "username"
                            isMultivalued = false
                            displayName = "\${username}"
                            permissions = UPAttributePermissions().apply {
                                view = setOf("user", "admin")
                                edit = setOf("user", "admin")
                            }
                            validations = mapOf(
                                "length" to mapOf("min" to 1, "max" to 100),
                                "username-prohibited-characters" to mapOf(),
                                "up-username-not-idn-homograph" to mapOf()
                            )
                        },
                        UPAttribute().apply {
                            name = "email"
                            displayName = "\${email}"
                            isMultivalued = false
                            required = UPAttributeRequired().apply {
                                this.roles = setOf("user")
                            }
                            permissions = UPAttributePermissions().apply {
                                view = setOf("user", "admin")
                                edit = setOf("user", "admin")
                            }
                            validations = mapOf(
                                "length" to mapOf("max" to 255),
                                "email" to mapOf(),
                                "pattern" to mapOf(
                                    "min" to "",
                                    "max" to "",
                                    "pattern" to "^[^\\s@]+@hof-university\\.de\$",
                                    "error-message" to "Value must end with @hof-university.de"
                                )
                            )
                        },
                        UPAttribute().apply {
                            name = "hasUserInResourceServer"
                            displayName = "\${hasUserInResourceServer}"
                            isMultivalued = false
                            permissions = UPAttributePermissions().apply {
                                view = setOf("admin")
                                edit = setOf("admin")
                            }
                            validations = mapOf(
                                "pattern" to mapOf(
                                    "pattern" to "^(?i)(true|false)",
                                    "error-message" to "Value must be either 'true' or 'false'"
                                )
                            )
                        }
                    )
                }
            )
            logger.info("$logId: Keycloak realm with name $newRealmName was created.")

            authenticationConfigurator.realm(newRealmName).clientScopes().create(
                ClientScopeRepresentation().apply {
                    name = "resource_server"
                    description = "Provides convenient information about the resource server."
                    protocol = "openid-connect"
                    this.attributes = mapOf(
                        "include.in.token.scope" to "true"
                    )
                    protocolMappers = listOf(
                        ProtocolMapperRepresentation().apply {
                            protocolMapper = "oidc-usermodel-attribute-mapper"
                            protocol = "openid-connect"
                            name = "hasUserInResourceServer"
                            config = mapOf(
                                "user.attribute" to "hasUserInResourceServer",
                                "claim.name" to "hasUserInResourceServer",
                                "jsonType.label" to "boolean",
                                "id.token.claim" to "true",
                                "access.token.claim" to "true",
                                "userinfo.token.claim" to "true",
                                "introspection.token.claim" to "true",
                                "multivalued" to "false"
                            )
                        }
                    )
                }
            )
            logger.info("$logId: Client scope resource_server in Keycloak realm with name $newRealmName was added.")

            authenticationConfigurator.realm(newRealmName).clients().create(
                ClientRepresentation().apply {
                    clientId = "uniflitzer_frontend"
                    protocol = "openid-connect"
                    isStandardFlowEnabled = true
                    isDirectGrantsOnly = false
                    webOrigins = environment.getProperty("keycloak.webOrigins")?.split(",") ?: throw IllegalStateException("keycloak.webOrigins is not set")
                    redirectUris = environment.getProperty("keycloak.redirect-uris")?.split(",") ?: throw IllegalStateException("keycloak.redirect-uris is not set")
                    attributes = mapOf(Pair("pkce.code.challenge.method", "S256"))
                    defaultClientScopes = listOf("basic", "profile", "email", "roles", "web-origins", "resource_server")
                }
            )
            logger.info("$logId: Client with Client ID uniflitzer_frontend in Keycloak realm with name $newRealmName was set up.")

            (authenticationConfigurator
                .realm(newRealmName)
                .flows()
                .getRequiredAction("CONFIGURE_TOTP")
                ?: throw Exception("Required action 'CONFIGURE_TOTP' not found")
                    )
                .apply { isEnabled = false }
                .let{
                    authenticationConfigurator.realm(newRealmName).flows().updateRequiredAction("CONFIGURE_TOTP", it)
                    logger.info("$logId: Required actions in Keycloak realm with name $newRealmName were configured.")
                }

            logger.info("$logId: Keycloak realm with name $newRealmName was fully set up.")
        }
        catch (exception: Exception) {
            logger.error("$logId: Failed to setup Keycloak.", exception)
            exitProcess(1)
        }
    }
}