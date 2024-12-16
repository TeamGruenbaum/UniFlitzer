package de.uniflitzer.backend.model

import jakarta.persistence.*
import java.util.*


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

    fun addCarpoolInvite(user: User) {
        _sentInvites.add(user)
    }

    fun acceptCarpoolInvite(user: User) {
        _users.add(user)
        _sentInvites.remove(user)
    }

    fun removeCarpoolInvite(user: User) {
        _sentInvites.remove(user)
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