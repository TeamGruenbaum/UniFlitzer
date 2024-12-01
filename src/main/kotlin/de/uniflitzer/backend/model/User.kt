package de.uniflitzer.backend.model

import jakarta.persistence.CascadeType
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Transient
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "\"user\"") //user is a SQL keyword, so we need to escape it
class User(id: UUID, firstName: FirstName, lastName: LastName, birthday: ZonedDateTime, gender: Gender, address: Address, studyProgramme: StudyProgramme) {
    @field:Id
    var id: UUID = id

    //username is in Keycloak

    var profilePicture: Image? = null

    var firstName: FirstName = firstName
    var lastName: LastName = lastName
    var birthday: ZonedDateTime = birthday

    @field:Enumerated(EnumType.STRING)
    var gender: Gender = gender

    //emailAddress is in Keycloak

    var address: Address = address

    var description: Description? = null
    var studyProgramme: StudyProgramme = studyProgramme
    var isSmoking: Boolean? = null

    @field:Enumerated(EnumType.STRING)
    @field:ElementCollection
    private var _animals: MutableList<Animal> = mutableListOf()
    val animals: List<Animal> get() = _animals

    fun refillAnimals(animals: List<Animal>) {
        _animals.clear()
        _animals.addAll(animals)
    }

    @field:Enumerated(EnumType.STRING)
    var drivingStyle: DrivingStyle? = null

    @field:ElementCollection
    private var _cars: MutableList<Car> = mutableListOf()
    val cars: List<Car> get() = _cars

    @field:OneToMany(mappedBy = "requestingUser", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private var _driveRequests: MutableList<DriveRequest> = mutableListOf()
    val driveRequests: List<DriveRequest> get() = _driveRequests

    @field:OneToMany(mappedBy = "driver", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var driveOffersAsDriver: MutableList<DriveOffer> = mutableListOf()

    @field:OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var driveOffersAsPassenger: MutableList<DriveOffer> = mutableListOf()

    @field:OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var driveOffersAsRequestingUser: MutableList<PublicDriveOffer> = mutableListOf()

    @field:ManyToMany(mappedBy = "_users", fetch = FetchType.LAZY)
    private var _carpools: MutableList<Carpool> = mutableListOf()
    val carpools: List<Carpool> get() = _carpools

    @field:OneToMany(mappedBy = "driver", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private var _drivesAsDriver: MutableList<Drive> = mutableListOf()
    val drivesAsDriver: List<Drive> get() = _drivesAsDriver

    @field:ManyToMany(mappedBy = "_passengers", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private var _drivesAsPassenger: MutableList<Drive> = mutableListOf()
    val drivesAsPassenger: List<Drive> get() = _drivesAsPassenger

    @field:ElementCollection
    private var _ratings: MutableList<Rating> = mutableListOf()
    val ratings: List<Rating> get() = _ratings

    @field:ManyToMany(mappedBy = "_sentInvites", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private var _receivedInvites: MutableList<Carpool> = mutableListOf()
    val receivedInvites: List<Carpool> get() = _receivedInvites

    init {
        this.lastName = lastName
        this.birthday = birthday
        this.gender = gender
        this.address = address
        this.studyProgramme = studyProgramme
    }
}