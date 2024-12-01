package de.uniflitzer.backend.applicationservices.authentication

import jakarta.ws.rs.NotFoundException
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.ClientRepresentation
import org.keycloak.representations.idm.ClientScopeRepresentation
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.ProtocolMapperRepresentation
import org.keycloak.representations.idm.RealmRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.keycloak.representations.userprofile.config.UPAttribute
import org.keycloak.representations.userprofile.config.UPAttributePermissions
import org.keycloak.representations.userprofile.config.UPAttributeRequired
import org.keycloak.representations.userprofile.config.UPConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import kotlin.system.exitProcess


@Component
class KeycloakSetuper(
    @field:Autowired private val keycloak: Keycloak,
    @field:Autowired private val environment: Environment
): InitializingBean {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val newRealmName: String? = environment.getProperty("keycloak.realm.name")

    override fun afterPropertiesSet() {
        when (environment.getProperty("keycloak.setup")) {
            "disabled" -> return@afterPropertiesSet
            "recreate-if-not-exists" -> if (runCatching{keycloak.realm(newRealmName)}.exceptionOrNull() is NotFoundException) recreate()
            "recreate-always" -> recreate()
            else -> logger.warn("Invalid value for keycloak.setup")
        }
    }

    private fun recreate() {
        try {
            try { keycloak.realm(newRealmName).remove() }
            catch(_: NotFoundException){}
            catch(exception:Exception){ throw exception }
            logger.info("Realm '$newRealmName' was removed")

            keycloak.realms().create(
                RealmRepresentation().apply {
                    realm = newRealmName
                    isEnabled = true
                    isEditUsernameAllowed = true
                    isRegistrationAllowed = true
                }
            )
            keycloak.realm(newRealmName).users().userProfile().update(
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
            logger.info("Realm was set up")

            keycloak.realm(newRealmName).clientScopes().create(
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
            logger.info("Client scope was set up")

            keycloak.realm(newRealmName).clients().create(
                ClientRepresentation().apply {
                    clientId = "uniflitzer_frontend"
                    protocol = "openid-connect"
                    isStandardFlowEnabled = true
                    isDirectGrantsOnly = false
                    webOrigins = environment.getProperty("keycloak.webOrigins")?.split(",") ?: listOf("*")
                    redirectUris = environment.getProperty("keycloak.redirect-uris")?.split(",") ?: listOf("*")
                    attributes = mapOf(Pair("pkce.code.challenge.method", "S256"))
                    defaultClientScopes = listOf("basic", "profile", "email", "roles", "web-origins", "resource_server")
                }
            )
            logger.info("Client was set up")

            (keycloak
                .realm(newRealmName)
                .flows()
                .getRequiredAction("CONFIGURE_TOTP")
                ?: throw Exception("Required action 'CONFIGURE_TOTP' not found")
                    )
                .apply { isEnabled = false }
                .let{
                    keycloak.realm(newRealmName).flows().updateRequiredAction("CONFIGURE_TOTP", it)
                    logger.info("Authentication was set up")
                }

            logger.info("Keycloak has been fully set up")
        }
        catch (exception: Exception) {
            logger.error("Failed to setup Keycloak")
            println(exception.stackTraceToString())
            exitProcess(1)
        }
    }
}