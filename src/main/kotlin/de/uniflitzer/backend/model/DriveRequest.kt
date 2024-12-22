package de.uniflitzer.backend.model

import jakarta.persistence.*
import java.util.*

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "discriminator_type")
// Should be sealed, but Hibernate 6 does not support sealed classes
class DriveRequest(requestingUser: User, route: Route, scheduleTime: ScheduleTime?) {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @field:ManyToOne
    var requestingUser: User = requestingUser

    var route: Route = route

    @AttributeOverrides(
        AttributeOverride(name = "type", column = Column(name = "schedule_time_type"))
    )
    var scheduleTime: ScheduleTime? = scheduleTime

    init {
        this.requestingUser = requestingUser
        this.route = route
        this.scheduleTime = scheduleTime
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DriveRequest) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}