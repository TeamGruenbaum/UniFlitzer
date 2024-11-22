package de.uniflitzer.backend.model

import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
class DriveRequest {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @field:ManyToOne
    var requestingUser: User = null!!

    var route: Route = null!!
    var plannedDeparture: ZonedDateTime? = null

    constructor(requestingUser: User, route: Route, plannedDeparture: ZonedDateTime) {
        this.requestingUser = requestingUser
        this.route = route
        this.plannedDeparture = plannedDeparture
    }
}