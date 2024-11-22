package de.uniflitzer.backend.model

import jakarta.persistence.*
import java.util.*


@Entity
class Carpool{
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    var name: Name = null!!

    @field:ManyToMany
    var users: MutableList<User> = null!!

    @field:OneToMany(mappedBy = "carpool", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var driveRequests: MutableList<CarpoolDriveRequest> = null!!

    @field:OneToMany(mappedBy = "carpool", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var driveOffers: MutableList<CarpoolDriveOffer> = null!!

    @field:OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var drives: MutableList<Drive> = null!!

    @field:ElementCollection
    var messages: MutableList<Message> = null!!

    constructor(name: Name, users: MutableList<User>) {
        this.name = name
        this.users = users
        this.driveRequests = mutableListOf()
        this.driveOffers = mutableListOf()
        this.drives = mutableListOf()
        this.messages = mutableListOf()
    }
}