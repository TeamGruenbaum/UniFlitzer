package de.uniflitzer.backend.model

import de.uniflitzer.backend.model.errors.ConflictingActionError
import de.uniflitzer.backend.model.errors.ImpossibleActionError
import de.uniflitzer.backend.model.errors.MissingActionError
import de.uniflitzer.backend.model.errors.NotAvailableError
import de.uniflitzer.backend.model.errors.RepeatedActionError
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
        @Throws(ImpossibleActionError::class)
        set(value) {
            if (value.isAfter(ZonedDateTime.now())) throw ImpossibleActionError("Passed value must be in the past.")
            field = value
        }

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
    private var _favoritePositions: MutableList<Position> = mutableListOf()
    val favoritePositions: List<Position> get() = _favoritePositions

    @field:ManyToMany(fetch = FetchType.LAZY)
    private var _favoriteUsers: MutableList<User> = mutableListOf()
    val favoriteUsers: List<User> get() = _favoriteUsers

    @field:ManyToMany(fetch = FetchType.LAZY)
    private var _blockedUsers: MutableList<User> = mutableListOf()
    val blockedUsers: List<User> get() = _blockedUsers

    @field:OneToMany(mappedBy = "requestingUser", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private var _driveRequests: MutableList<DriveRequest> = mutableListOf()
    val driveRequests: List<DriveRequest> get() = _driveRequests

    @field:OneToMany(mappedBy = "driver", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private var _driveOffersAsDriver: MutableList<DriveOffer> = mutableListOf()
    val driveOffersAsDriver: List<DriveOffer> get () = _driveOffersAsDriver

    @field:ManyToMany(fetch = FetchType.LAZY)
    private var _driveOffersAsPassenger: MutableList<DriveOffer> = mutableListOf() //Manual bidirectional relationship 2
    val driveOffersAsPassenger: List<DriveOffer> get() = _driveOffersAsPassenger

    @field:ManyToMany(fetch = FetchType.LAZY)
    private var _driveOffersAsRequestingUser: MutableList<PublicDriveOffer> = mutableListOf() //Manual bidirectional relationship 1
    val driveOffersAsRequestingUser: List<PublicDriveOffer> get() = _driveOffersAsRequestingUser

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
        if (index.toInt() >= _cars.size) throw NotAvailableError("Passed index is out of bounds.")
        _cars.removeAt(index.toInt())
    }

    @Throws(NotAvailableError::class)
    fun getCarByIndex(index: UInt): Car {
        return cars.getOrNull(index.toInt()) ?: throw NotAvailableError("Passed index does not exist.")
    }

    @Throws(RepeatedActionError::class)
    fun refillAnimals(animals: List<Animal>) {
        if(animals.distinct().count() != animals.count()) throw RepeatedActionError("Passed list of animals contains duplicates.")
        _animals.clear()
        _animals.addAll(animals)
    }

    fun addFavoritePosition(position: Position) {
        if(position in _favoritePositions) throw RepeatedActionError("Passed position is already a favorite position.")
        _favoritePositions.add(position)
    }

    @Throws(NotAvailableError::class)
    fun removeFavoritePositionByIndex(index: UInt) {
        if (index.toInt() >= _favoritePositions.size) throw NotAvailableError("Passed index is out of bounds.")
        _favoritePositions.removeAt(index.toInt())
    }

    fun addRating(rating: Rating) = _ratings.add(rating)

    val averageStars
        get(): Double? = if (ratings.isEmpty()) null else ratings.map { it.stars.value.toDouble() }.sum() / ratings.size

    fun addFavoriteUser(user: User) {
        if(user in _favoriteUsers) throw RepeatedActionError("Passed user is already a favorite user.")
        if(user == this) throw ConflictingActionError("Passed user cannot be a favorite user of himself.")
        if(user in _blockedUsers) throw ConflictingActionError("Passed user cannot be a favorite user and a blocked user at the same time.")

        _favoriteUsers.add(user)
    }

    @Throws(NotAvailableError::class)
    fun removeFavoriteUser(user: User) {
        if(user !in _favoriteUsers)throw NotAvailableError("Passed user is not a favorite user.")
        _favoriteUsers.remove(user)
    }

    fun addBlockedUser(user: User) {
        if(user in _blockedUsers) throw RepeatedActionError("Passed user is already a blocked user.")
        if(user == this) throw ConflictingActionError("Passed user cannot be a blocked user of himself.")
        if(user in _favoriteUsers) throw ConflictingActionError("Passed user cannot be a favorite user and a blocked user at the same time.")

        _blockedUsers.add(user)
    }

    @Throws(NotAvailableError::class)
    fun removeBlockedUser(user: User) {
        if(user !in _blockedUsers) throw NotAvailableError("Passed user is not a blocked user.")
        _blockedUsers.remove(user)
    }

    @Throws(RepeatedActionError::class)
    fun joinDriveOfferAsRequestingUser(driveOffer: PublicDriveOffer) {
        if(driveOffer in _driveOffersAsRequestingUser) throw RepeatedActionError("User is already a requesting user of the passed public drive offer.")

        _driveOffersAsRequestingUser.add(driveOffer)
    }

    @Throws(MissingActionError::class)
    fun leaveDriveOfferAsRequestingUser(driveOffer: DriveOffer) {
        if(driveOffer !in _driveOffersAsRequestingUser) throw MissingActionError("User is not a passenger of the passed drive offer.")

        _driveOffersAsRequestingUser.remove(driveOffer)
    }

    @Throws(RepeatedActionError::class)
    fun joinDriveOfferAsPassenger(driveOffer: DriveOffer) {
        if(driveOffer in _driveOffersAsPassenger) throw RepeatedActionError("User is already a passenger of the passed drive offer.")

        _driveOffersAsPassenger.add(driveOffer)
        _driveOffersAsRequestingUser.remove(driveOffer)
    }

    @Throws(NotAvailableError::class)
    fun leaveDriveOfferAsPassenger(driveOffer: DriveOffer) {
        if(driveOffer !in _driveOffersAsPassenger) throw NotAvailableError("User is not a passenger of passed driver offer.")

        _driveOffersAsPassenger.remove(driveOffer)
    }

    fun removeRatingsOfUser(user: User) = _ratings.removeIf { it.author == user }

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