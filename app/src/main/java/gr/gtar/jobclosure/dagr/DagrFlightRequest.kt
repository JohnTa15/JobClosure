package gr.gtar.jobclosure.dagr

import gr.gtar.jobclosure.data.Booking
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Which venue of a booking a flight request is being raised for. */
enum class DagrVenue(val key: String, val label: String) {
    CHURCH("church", "Εκκλησία"),
    RECEPTION("reception", "Δεξίωση"),
    ;

    companion object {
        fun fromKey(key: String?): DagrVenue = entries.firstOrNull { it.key == key } ?: CHURCH
    }
}

/**
 * The parts of a flight request that belong to the operator rather than the job, and so are typed
 * once in Settings instead of per booking.
 *
 * The altitude default is 120 m because that is the ceiling of the EU open category the vast
 * majority of these flights sit in; it is editable because a specific authorisation can allow more.
 */
data class DagrOperatorProfile(
    val operatorRegistration: String = "",
    val pilotName: String = "",
    val uasModel: String = "",
    val maxAltitudeMeters: Int = 120,
    val radiusMeters: Int = 200,
    val purpose: String = "Εναέρια φωτογράφιση και βιντεοσκόπηση ιδιωτικής εκδήλωσης",
)

/**
 * One field of DAGR's flight-request form, as the app understands it.
 *
 * [matches] is the whole point: DAGR's markup is not published and can be changed without notice,
 * so fields are found by what they are *called* on screen - label, name, id, placeholder,
 * aria-label - rather than by a hardcoded selector that would break on the first redesign. Both
 * languages are listed because the site is bilingual and the pilot's own language setting decides
 * which one is rendered.
 */
data class DagrField(
    val key: String,
    val label: String,
    val value: String,
    val matches: List<String>,
)

data class DagrFlightRequest(
    val venue: DagrVenue,
    val venueName: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val profile: DagrOperatorProfile,
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null

    /**
     * Every value the app can offer the form. Fields whose value is blank are dropped rather than
     * filled with an empty string - overwriting something DAGR pre-filled, or that the pilot typed,
     * with nothing would be worse than leaving it alone.
     */
    fun fields(): List<DagrField> = listOfNotNull(
        latitude?.let {
            DagrField(
                key = "latitude",
                label = "Γεωγραφικό πλάτος",
                value = formatCoordinate(it),
                matches = listOf("latitude", "lat", "πλάτος", "πλατος"),
            )
        },
        longitude?.let {
            DagrField(
                key = "longitude",
                label = "Γεωγραφικό μήκος",
                value = formatCoordinate(it),
                matches = listOf("longitude", "lon", "lng", "μήκος", "μηκος"),
            )
        },
        field("radius", "Ακτίνα (m)", profile.radiusMeters.toString(), listOf("radius", "ακτίνα", "ακτινα")),
        field(
            "altitude",
            "Μέγιστο ύψος (m)",
            profile.maxAltitudeMeters.toString(),
            listOf("altitude", "height", "agl", "ύψος", "υψος", "υψόμετρο", "υψομετρο"),
        ),
        field("date", "Ημερομηνία", date.format(DATE), listOf("date", "ημερομηνία", "ημερομηνια")),
        field(
            "startTime",
            "Ώρα έναρξης",
            startTime.format(TIME),
            listOf("start time", "starttime", "start", "έναρξη", "εναρξη"),
        ),
        field(
            "endTime",
            "Ώρα λήξης",
            endTime.format(TIME),
            listOf("end time", "endtime", "until", "λήξη", "ληξη"),
        ),
        field(
            "operator",
            "Αριθμός μητρώου χειριστή",
            profile.operatorRegistration,
            listOf("operator", "registration", "μητρώο", "μητρωο", "εκμεταλλευόμενου", "εκμεταλλευομενου"),
        ),
        field("pilot", "Ονοματεπώνυμο χειριστή", profile.pilotName, listOf("pilot", "χειριστ", "ονοματ")),
        field("uas", "Μοντέλο drone", profile.uasModel, listOf("uas", "model", "drone", "μοντέλο", "μοντελο", "αεροσκάφ", "αεροσκαφ")),
        field(
            "purpose",
            "Σκοπός πτήσης",
            profile.purpose,
            listOf("purpose", "reason", "description", "remarks", "σκοπός", "σκοπος", "περιγραφή", "περιγραφη", "παρατηρήσεις", "παρατηρησεις"),
        ),
        field("location", "Τοποθεσία", listOfNotNull(venueName.ifBlank { null }, address.ifBlank { null }).joinToString(", "), listOf("location", "place", "address", "τοποθεσία", "τοποθεσια", "διεύθυνση", "διευθυνση", "περιοχή", "περιοχη")),
    )

    private fun field(key: String, label: String, value: String, matches: List<String>): DagrField? =
        value.takeIf { it.isNotBlank() }?.let { DagrField(key, label, it, matches) }

    companion object {
        private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.UK)
        private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)

        fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

        /**
         * Builds the request for one venue of [booking]. The window is the venue's own start and
         * end plus [PAD_MINUTES] either side, because a request that ends the minute the ceremony
         * does leaves no room for the shots before and after it - and a flight outside the approved
         * window is the thing being approved against.
         */
        fun from(
            booking: Booking,
            venue: DagrVenue,
            coordinates: Pair<Double, Double>?,
            profile: DagrOperatorProfile,
        ): DagrFlightRequest {
            val start = when (venue) {
                DagrVenue.CHURCH -> booking.ceremonyStart
                DagrVenue.RECEPTION -> booking.receptionStart ?: booking.ceremonyStart
            }
            val durationMinutes = when (venue) {
                DagrVenue.CHURCH -> booking.ceremonyDurationMinutes
                DagrVenue.RECEPTION -> booking.receptionDurationMinutes
            }
            return DagrFlightRequest(
                venue = venue,
                venueName = when (venue) {
                    DagrVenue.CHURCH -> booking.churchName
                    DagrVenue.RECEPTION -> booking.receptionVenueName
                },
                address = when (venue) {
                    DagrVenue.CHURCH -> booking.churchAddress
                    DagrVenue.RECEPTION -> booking.receptionVenueAddress
                },
                latitude = coordinates?.first,
                longitude = coordinates?.second,
                date = start.toLocalDate(),
                startTime = start.minusMinutes(PAD_MINUTES).toLocalTime(),
                endTime = start.plusMinutes(durationMinutes + PAD_MINUTES).toLocalTime(),
                profile = profile,
            )
        }

        const val PAD_MINUTES = 30L
    }
}
