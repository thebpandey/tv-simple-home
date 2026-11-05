package tv.simplehome.launcher

/** A city for the second clock. The time zone comes with the city, so the user never picks one; DST follows the zone rules. */
data class City(val id: String, val name: String, val zone: String, val region: String)

/** Curated city list for the second clock, grouped by region in display order. Names stay short to fit the header beside the clock. */
object WorldCities {
    const val DEFAULT_ID = "new_delhi" // what the clock showed before it was selectable
    const val MAX_NAME_CHARS = 14

    val REGIONS = listOf("ASIA", "MIDDLE EAST AND AFRICA", "EUROPE", "AMERICAS", "OCEANIA")

    val ALL: List<City> = listOf(
        City("new_delhi", "New Delhi", "Asia/Kolkata", "ASIA"),
        City("mumbai", "Mumbai", "Asia/Kolkata", "ASIA"),
        City("kathmandu", "Kathmandu", "Asia/Kathmandu", "ASIA"),
        City("dhaka", "Dhaka", "Asia/Dhaka", "ASIA"),
        City("karachi", "Karachi", "Asia/Karachi", "ASIA"),
        City("bangkok", "Bangkok", "Asia/Bangkok", "ASIA"),
        City("jakarta", "Jakarta", "Asia/Jakarta", "ASIA"),
        City("singapore", "Singapore", "Asia/Singapore", "ASIA"),
        City("manila", "Manila", "Asia/Manila", "ASIA"),
        City("hong_kong", "Hong Kong", "Asia/Hong_Kong", "ASIA"),
        City("shanghai", "Shanghai", "Asia/Shanghai", "ASIA"),
        City("seoul", "Seoul", "Asia/Seoul", "ASIA"),
        City("tokyo", "Tokyo", "Asia/Tokyo", "ASIA"),
        City("dubai", "Dubai", "Asia/Dubai", "MIDDLE EAST AND AFRICA"),
        City("riyadh", "Riyadh", "Asia/Riyadh", "MIDDLE EAST AND AFRICA"),
        City("tel_aviv", "Tel Aviv", "Asia/Jerusalem", "MIDDLE EAST AND AFRICA"),
        City("cairo", "Cairo", "Africa/Cairo", "MIDDLE EAST AND AFRICA"),
        City("nairobi", "Nairobi", "Africa/Nairobi", "MIDDLE EAST AND AFRICA"),
        City("lagos", "Lagos", "Africa/Lagos", "MIDDLE EAST AND AFRICA"),
        City("johannesburg", "Johannesburg", "Africa/Johannesburg", "MIDDLE EAST AND AFRICA"),
        City("london", "London", "Europe/London", "EUROPE"),
        City("paris", "Paris", "Europe/Paris", "EUROPE"),
        City("amsterdam", "Amsterdam", "Europe/Amsterdam", "EUROPE"),
        City("berlin", "Berlin", "Europe/Berlin", "EUROPE"),
        City("istanbul", "Istanbul", "Europe/Istanbul", "EUROPE"),
        City("moscow", "Moscow", "Europe/Moscow", "EUROPE"),
        City("honolulu", "Honolulu", "Pacific/Honolulu", "AMERICAS"),
        City("anchorage", "Anchorage", "America/Anchorage", "AMERICAS"),
        City("los_angeles", "Los Angeles", "America/Los_Angeles", "AMERICAS"),
        City("san_francisco", "San Francisco", "America/Los_Angeles", "AMERICAS"),
        City("vancouver", "Vancouver", "America/Vancouver", "AMERICAS"),
        City("phoenix", "Phoenix", "America/Phoenix", "AMERICAS"),
        City("denver", "Denver", "America/Denver", "AMERICAS"),
        City("houston", "Houston", "America/Chicago", "AMERICAS"),
        City("chicago", "Chicago", "America/Chicago", "AMERICAS"),
        City("mexico_city", "Mexico City", "America/Mexico_City", "AMERICAS"),
        City("toronto", "Toronto", "America/Toronto", "AMERICAS"),
        City("new_york", "New York", "America/New_York", "AMERICAS"),
        City("sao_paulo", "São Paulo", "America/Sao_Paulo", "AMERICAS"),
        City("sydney", "Sydney", "Australia/Sydney", "OCEANIA"),
        City("melbourne", "Melbourne", "Australia/Melbourne", "OCEANIA"),
        City("auckland", "Auckland", "Pacific/Auckland", "OCEANIA"),
    )

    /** The saved city, or New Delhi when nothing (or an id from a removed entry) is saved. */
    fun resolve(savedId: String?): City = ALL.firstOrNull { it.id == savedId } ?: ALL.first { it.id == DEFAULT_ID }
}
