package de.uniflitzer.backend.model

import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
// Should be sealed, but Hibernate 6 does not support sealed classes
class DriveRequest(requestingUser: User, route: Route, plannedDeparture: ZonedDateTime?) {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @field:ManyToOne
    var requestingUser: User = requestingUser

    var route: Route = route
    var plannedDeparture: ZonedDateTime? = null

    init {
        this.requestingUser = requestingUser
        this.route = route
        this.plannedDeparture = plannedDeparture
    }
}