package tv.simplehome.launcher

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToInt

enum class Condition(val label: String) {
    CLEAR("Clear"), PARTLY_CLOUDY("Partly cloudy"), CLOUDY("Cloudy"), FOG("Fog"),
    DRIZZLE("Drizzle"), RAIN("Rain"), SNOW("Snow"), THUNDERSTORM("Thunderstorm"),
}

data class WeatherData(val tempF: Int, val highF: Int, val lowF: Int, val condition: Condition)

enum class FailureReason { EMPTY_BODY, BAD_JSON, MISSING_FIELD, WRONG_TYPE, OUT_OF_RANGE, NETWORK, HTTP_ERROR }

sealed interface WeatherResult {
    data class Success(val data: WeatherData) : WeatherResult
    data class Failure(val reason: FailureReason) : WeatherResult
}

enum class Freshness { FRESH, NEEDS_REFRESH, TOO_STALE }

/** Pure weather rules and response parsing. The JSON is untrusted input: nothing is assumed about its shape. */
object Weather {
    const val REFRESH_MS = 15L * 60 * 1000
    const val HIDE_AFTER_MS = 3L * 60 * 60 * 1000

    fun buildUrl(lat: Double, lon: Double): String =
        "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min" +
            "&temperature_unit=fahrenheit&timezone=auto&forecast_days=1"

    /** WMO weather interpretation codes as documented by Open-Meteo. Unknown codes degrade to CLOUDY. */
    fun conditionFor(code: Int): Condition = when (code) {
        0, 1 -> Condition.CLEAR
        2 -> Condition.PARTLY_CLOUDY
        3 -> Condition.CLOUDY
        45, 48 -> Condition.FOG
        in 51..57 -> Condition.DRIZZLE
        in 61..67, in 80..82 -> Condition.RAIN
        in 71..77, 85, 86 -> Condition.SNOW
        95, 96, 99 -> Condition.THUNDERSTORM
        else -> Condition.CLOUDY
    }

    /** A negative age (clock moved backwards) is untrustworthy, so it refreshes rather than hiding or trusting it. */
    fun freshness(ageMs: Long): Freshness = when {
        ageMs < 0 -> Freshness.NEEDS_REFRESH
        ageMs >= HIDE_AFTER_MS -> Freshness.TOO_STALE
        ageMs >= REFRESH_MS -> Freshness.NEEDS_REFRESH
        else -> Freshness.FRESH
    }

    data class FetchPlan(val weather: Boolean, val air: Boolean)

    /** A cache that is missing or at least [REFRESH_MS] old needs a fetch; weather and air quality are decided independently. */
    fun needsFetch(cacheAgeMs: Long?): Boolean = cacheAgeMs == null || freshness(cacheAgeMs) != Freshness.FRESH

    fun fetchPlan(weatherAgeMs: Long?, airAgeMs: Long?): FetchPlan = FetchPlan(needsFetch(weatherAgeMs), needsFetch(airAgeMs))

    /** Delay until the next periodic fetch: the earliest time either cache goes stale (never negative). */
    fun nextTickDelayMs(weatherAgeMs: Long?, airAgeMs: Long?): Long {
        if (needsFetch(weatherAgeMs) || needsFetch(airAgeMs)) return REFRESH_MS
        return minOf(REFRESH_MS - weatherAgeMs!!, REFRESH_MS - airAgeMs!!).coerceAtLeast(0L)
    }

    /** Each cache (weather, air quality) decides on its own: a missing or stale one hides only itself. */
    fun shouldShow(cacheAgeMs: Long?): Boolean = cacheAgeMs != null && freshness(cacheAgeMs) != Freshness.TOO_STALE

    fun parse(body: String?): WeatherResult {
        if (body.isNullOrBlank()) return WeatherResult.Failure(FailureReason.EMPTY_BODY)
        val root = try {
            JSONObject(body)
        } catch (e: JSONException) {
            return WeatherResult.Failure(FailureReason.BAD_JSON)
        }
        val current = root.opt("current")
        val daily = root.opt("daily")
        if (current == null || daily == null) return fail(FailureReason.MISSING_FIELD)
        if (current !is JSONObject || daily !is JSONObject) return fail(FailureReason.WRONG_TYPE)

        val temp = number(current.opt("temperature_2m"))
        val code = integer(current.opt("weather_code"))
        val high = number(firstOf(daily.opt("temperature_2m_max")))
        val low = number(firstOf(daily.opt("temperature_2m_min")))
        for (f in listOf(temp, code, high, low)) if (f is Field.Bad) return fail(f.reason)
        return WeatherResult.Success(
            WeatherData(
                tempF = (temp as Field.Ok).v.roundToInt(),
                highF = (high as Field.Ok).v.roundToInt(),
                lowF = (low as Field.Ok).v.roundToInt(),
                condition = conditionFor((code as Field.Ok).v.toInt()),
            ),
        )
    }

    private fun fail(r: FailureReason) = WeatherResult.Failure(r)

    private sealed interface Field {
        data class Ok(val v: Double) : Field
        data class Bad(val reason: FailureReason) : Field
    }

    private fun number(v: Any?): Field = when {
        v == null -> Field.Bad(FailureReason.MISSING_FIELD)
        v is Number && v.toDouble().isFinite() -> Field.Ok(v.toDouble())
        else -> Field.Bad(FailureReason.WRONG_TYPE) // strings, booleans, JSON null, NaN
    }

    // weather_code must be a true integer: "3" and 3.5 are rejected rather than coerced.
    private fun integer(v: Any?): Field = when {
        v == null -> Field.Bad(FailureReason.MISSING_FIELD)
        v is Int -> Field.Ok(v.toDouble())
        v is Long && v in Int.MIN_VALUE..Int.MAX_VALUE -> Field.Ok(v.toDouble())
        else -> Field.Bad(FailureReason.WRONG_TYPE)
    }

    /** First element of a daily array; null (missing) when the array is absent or empty. */
    private fun firstOf(v: Any?): Any? = when {
        v == null -> null
        v is JSONArray -> if (v.length() == 0) null else v.opt(0)
        else -> Any() // present but not an array: forces WRONG_TYPE downstream
    }
}
