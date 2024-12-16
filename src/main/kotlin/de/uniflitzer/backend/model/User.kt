package de.uniflitzer.backend.model

import de.uniflitzer.backend.model.errors.NotAvailableError
import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "\"user\"") //user is a SQL keyword, so we need to escape it
class User(id: UUID, firstName: FirstName, lastName: LastName, birthday: ZonedDateTime, gender: Gender, address: Address, studyProgramme: StudyProgramme) {
    @field:Id
    var id: UUID = id

    //username is in Keycloak

    @OneToOne
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

    @field:Enumerated(EnumType.STRING)
    var drivingStyle: DrivingStyle? = null

    @field:ElementCollection
    private var _cars: MutableList<Car> = mutableListOf()
    val cars: List<Car> get() = _cars

    @field:ElementCollection
    private var _favoriteAddresses: MutableList<Address> = mutableListOf()
    val favoriteAddresses: List<Address> get() = _favoriteAddresses

    @field:ManyToMany(fetch = FetchType.LAZY)
    private var _favoriteUsers: MutableList<User> = mutableListOf()
    val favoriteUsers: List<User> get() = _favoriteUsers

    @field:ManyToMany(fetch = FetchType.LAZY)
    private var _blockedUsers: MutableList<User> = mutableListOf()
    val blockedUsers: List<User> get() = _blockedUsers

    @field:OneToMany(mappedBy = "requestingUser", fetch = FetchType.LAZY)
    private var _driveRequests: MutableList<DriveRequest> = mutableListOf()
    val driveRequests: List<DriveRequest> get() = _driveRequests

    @field:OneToMany(mappedBy = "driver", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var driveOffersAsDriver: MutableList<DriveOffer> = mutableListOf()

    @field:OneToMany(fetch = FetchType.LAZY)
    var driveOffersAsPassenger: MutableList<DriveOffer> = mutableListOf()

    @field:OneToMany(fetch = FetchType.LAZY)
    var driveOffersAsRequestingUser: MutableList<PublicDriveOffer> = mutableListOf()

    @field:ManyToMany(mappedBy = "_users", fetch = FetchType.LAZY)
    private var _carpools: MutableList<Carpool> = mutableListOf()
    val carpools: List<Carpool> get() = _carpools

    @field:OneToMany(mappedBy = "driver", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private var _drivesAsDriver: MutableList<Drive> = mutableListOf()
    val drivesAsDriver: List<Drive> get() = _drivesAsDriver

    @field:ManyToMany(mappedBy = "_passengers", fetch = FetchType.LAZY)
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

    fun addCar(car: Car) = _cars.add(car)

    @Throws(NotAvailableError::class)
    fun removeCarAtIndex(index: UInt) {
        if (index.toInt() >= _cars.size) throw NotAvailableError("Index out of bounds")
        _cars.removeAt(index.toInt())
    }

    @Throws(NotAvailableError::class)
    fun getCarByIndex(index: Int): Car {
        return cars.getOrNull(index) ?: throw NotAvailableError("The car with index $index does not exist.")
    }

    fun refillAnimals(animals: List<Animal>) {
        _animals.clear()
        _animals.addAll(animals)
    }

    fun addFavoriteAddress(address: Address) = _favoriteAddresses.add(address)

    @Throws(NotAvailableError::class)
    fun removeFavoriteAddressByIndex(index: UInt) {
        if (index.toInt() >= _favoriteAddresses.size) throw NotAvailableError("Index out of bounds")
        _favoriteAddresses.removeAt(index.toInt())
    }

    fun addFavoriteUser(user: User) = _favoriteUsers.add(user)

    @Throws(NotAvailableError::class)
    fun removeFavoriteUser(user: User) {
        if(user !in _favoriteUsers)throw NotAvailableError("The user is not a favorite user.")
        _favoriteUsers.remove(user)
    }

    fun addBlockedUser(user: User) = _blockedUsers.add(user)

    @Throws(NotAvailableError::class)
    fun removeBlockedUser(user: User) {
        if(user !in _blockedUsers) throw NotAvailableError("The user is not a blocked user.")
        _blockedUsers.remove(user)
    }

    fun leftDriveOfferAsRequestingUser(driveOffer: DriveOffer) {
        if(driveOffer !in driveOffersAsRequestingUser)  throw NotAvailableError("The user is not a passenger of the drive offer.")
        driveOffersAsPassenger.remove(driveOffer)
    }

    fun leftDriveOfferAsPassenger(driveOffer: DriveOffer) {
        if(driveOffer !in driveOffersAsPassenger) throw NotAvailableError("The user is not a requesting user of the drive offer.")
        driveOffersAsRequestingUser.remove(driveOffer)
    }

    fun removeRatingOfUser(user: User) = _ratings.removeIf { it.author == user }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}