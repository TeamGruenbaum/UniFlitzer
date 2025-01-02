package de.uniflitzer.backend.model


import de.uniflitzer.backend.model.errors.MissingActionError
import de.uniflitzer.backend.model.errors.RepeatedActionError
import jakarta.persistence.*
import java.util.*
import kotlin.jvm.Throws


@Entity
class Carpool(name: Name, users: MutableList<User>){
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    var name: Name = name

    @field:ManyToMany(fetch = FetchType.LAZY)
    private var _users: MutableList<User> = users
    val users: List<User> get() = _users

    @field:OneToMany(mappedBy = "carpool", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private var _driveRequests: MutableList<CarpoolDriveRequest> = mutableListOf()
    val driveRequests: List<CarpoolDriveRequest> get() = _driveRequests

    @field:OneToMany(mappedBy = "carpool", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private var _driveOffers: MutableList<CarpoolDriveOffer> = mutableListOf()
    val driveOffers: List<CarpoolDriveOffer> get() = _driveOffers

    @field:OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private var _drives: MutableList<Drive> = mutableListOf()
    val drives: List<Drive> get() = _drives

    @field:ManyToMany(fetch = FetchType.LAZY)
    private var _sentInvites: MutableList<User> = mutableListOf()
    val sentInvites: List<User> get () = _sentInvites

    init {
        this.name = name
        this._users = users
    }

    @Throws(RepeatedActionError::class)
    fun sendInvite(user: User) {
        if(user in _users) throw RepeatedActionError("User with id ${user.id} is already a member of carpool with id ${this.id}.")
        if(user in _sentInvites) throw RepeatedActionError("User with id ${user.id} has already been invited to carpool with id ${this.id}.")
        _sentInvites.add(user)
    }

    @Throws(RepeatedActionError::class, MissingActionError::class)
    fun acceptInvite(user: User) {
        if(user in _users) throw RepeatedActionError("User with id ${user.id} is already a member of carpool with id ${this.id}.")
        if(user !in _sentInvites) throw MissingActionError("User with id ${user.id} has not been invited to carpool with id ${this.id}.")
        _users.add(user)
        _sentInvites.remove(user)
    }

    @Throws(RepeatedActionError::class, MissingActionError::class)
    fun rejectInvite(user: User) {
        if(user in _users) throw RepeatedActionError("User with id ${user.id} is already a member of carpool with id ${this.id}.")
        if(user !in _sentInvites) throw MissingActionError("User with id ${user.id} has not been invited to carpool with id ${this.id}.")
        _sentInvites.remove(user)
    }

    fun addDrive(drive: Drive) {
        if(drive in _drives) throw RepeatedActionError("Drive with id ${drive.id} already exists in carpool.")
        _drives.add(drive)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Carpool) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}