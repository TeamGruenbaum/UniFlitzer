package de.uniflitzer.backend.applicationservices.authentication

import jakarta.ws.rs.NotFoundException
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.ClientRepresentation
import org.keycloak.representations.idm.CredentialRepresentation
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
class KeycloakSetuper(@field:Autowired private val environment: Environment): InitializingBean {
    private val logger: Logger = LoggerFactory.getLogger(KeycloakSetuper::class.java)

    private val keycloak = KeycloakBuilder.builder()
        .serverUrl(environment.getProperty("keycloakSetuper.keycloakUrl") ?: "http://localhost:8080")
        .realm(environment.getProperty("keycloakSetuper.administrator.realm.name") ?: "master")
        .clientId(environment.getProperty("keycloakSetuper.administrator.clientId") ?: "admin-cli")
        .username(environment.getProperty("keycloakSetuper.administrator.username") ?: "admin")
        .password(environment.getProperty("keycloakSetuper.administrator.password") ?: "admin")
        .build()

    private val newRealmName: String = "uniflitzer"

    override fun afterPropertiesSet() {
        when (environment.getProperty("keycloakSetuper.setup")) {
            "disabled" -> return@afterPropertiesSet
            "recreate-if-not-exists" -> if (runCatching{keycloak.realm(newRealmName)}.exceptionOrNull() is NotFoundException) recreate()
            "recreate-always" -> recreate()
            else -> logger.warn("Invalid value for keycloakSetuper.setup")
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
                            displayName = "\${email}" //TODO
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
                        }
                    )
                }
            )
            logger.info("Realm was set up")

            keycloak.realm(newRealmName).clients().create(
                ClientRepresentation().apply {
                    clientId = "uniflitzer_frontend"
                    protocol = "openid-connect"
                    isStandardFlowEnabled = true
                    isDirectGrantsOnly = false
                    webOrigins = listOf("http://localhost:8080")
                    redirectUris = listOf("*")
                    attributes = mapOf(Pair("pkce.code.challenge.method", "S256"))
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

            keycloak.realm(newRealmName).users().create(
                UserRepresentation().apply {
                    username = "example"
                    email = "example@example.org"
                    credentials = listOf(
                        CredentialRepresentation().apply {
                            type = CredentialRepresentation.PASSWORD
                            value = "1234"
                            isTemporary = false
                        }
                    )
                    isEnabled = true
                }
            )
            logger.info("Example user was created")

            logger.info("Keycloak has been fully set up")
        }
        catch (exception: Exception) {
            logger.error("Failed to setup Keycloak")
            println(exception.stackTraceToString())
            exitProcess(1)
        }
        finally {
            keycloak.close()
        }
    }
}