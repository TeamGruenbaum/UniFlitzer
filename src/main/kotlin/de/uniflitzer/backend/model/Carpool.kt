package de.uniflitzer.backend.model

import jakarta.persistence.*
import java.util.*


@Entity
class Carpool{
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    var name: Name = null!!

    @field:ManyToMany(fetch = FetchType.LAZY)
    private var _users: MutableList<User> = null!!
    val users: List<User> get() = _users

    @field:OneToMany(mappedBy = "carpool", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private var _driveRequests: MutableList<CarpoolDriveRequest> = null!!
    val driveRequests: List<CarpoolDriveRequest> = _driveRequests

    @field:OneToMany(mappedBy = "carpool", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private var _driveOffers: MutableList<CarpoolDriveOffer> = null!!
    val driveOffers: List<CarpoolDriveOffer> = _driveOffers

    @field:OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private var _drives: MutableList<Drive> = null!!
    val drives: List<Drive> = _drives

    @field:ElementCollection
    private var _messages: MutableList<Message> = null!!
    val messages: List<Message> = _messages

    constructor(name: Name, users: MutableList<User>) {
        this.name = name
        this._users = users
        this._driveRequests = mutableListOf()
        this._driveOffers = mutableListOf()
        this._drives = mutableListOf()
        this._messages = mutableListOf()
    }
}