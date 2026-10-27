package tv.simplehome.launcher

import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToInt

data class AirQuality(val usAqi: Int, val pm25: Int, val dust: Int)

sealed interface AirQualityResult {
    data class Success(val data: AirQuality) : AirQualityResult
    data class Failure(val reason: FailureReason) : AirQualityResult
}

/** EPA US AQI categories. [shortLabel] is what the compact line shows. */
enum class AqiCategory(val shortLabel: String, val color: Int) {
    GOOD("Good", 0xFF00E400.toInt()),
    MODERATE("Moderate", 0xFFFFFF00.toInt()),
    SENSITIVE("Sensitive", 0xFFFF7E00.toInt()), // Unhealthy for Sensitive Groups
    UNHEALTHY("Unhealthy", 0xFFFF0000.toInt()),
    VERY_UNHEALTHY("Very Unhealthy", 0xFF8F3F97.toInt()),
    HAZARDOUS("Hazardous", 0xFF7E0023.toInt()),
}

/** Pure air-quality rules: URL, defensive parsing of the untrusted response, categories and the display line. */
object AirQualityRules {
    fun buildUrl(lat: Double, lon: Double): String =
        // Pollen is not requested here: Open-Meteo returns null for it in the US, so it comes from Google's Pollen API (Pollen.kt).
        "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi,pm2_5,dust&timezone=auto"

    fun category(aqi: Int): AqiCategory = when {
        aqi <= 50 -> AqiCategory.GOOD
        aqi <= 100 -> AqiCategory.MODERATE
        aqi <= 150 -> AqiCategory.SENSITIVE
        aqi <= 200 -> AqiCategory.UNHEALTHY
        aqi <= 300 -> AqiCategory.VERY_UNHEALTHY
        else -> AqiCategory.HAZARDOUS
    }

    /** The category colours are tuned for dark backgrounds; by day they are darkened ~28% to stay visible on the light scrim. */
    fun dotColor(category: AqiCategory, day: Boolean): Int {
        if (!day) return category.color
        fun ch(shift: Int) = (((category.color shr shift) and 255) * 0.72f).toInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    fun line(d: AirQuality): String = "AQI ${d.usAqi} · ${category(d.usAqi).shortLabel} · PM2.5 ${d.pm25} · Dust ${d.dust}"

    fun parse(body: String?): AirQualityResult {
        if (body.isNullOrBlank()) return fail(FailureReason.EMPTY_BODY)
        val root = try { JSONObject(body) } catch (e: JSONException) { return fail(FailureReason.BAD_JSON) }
        val current = root.opt("current")
        if (current == null) return fail(FailureReason.MISSING_FIELD)
        if (current !is JSONObject) return fail(FailureReason.WRONG_TYPE)
        val aqi = number(current.opt("us_aqi")) ?: return fail(reasonFor(current.opt("us_aqi")))
        val pm = number(current.opt("pm2_5")) ?: return fail(reasonFor(current.opt("pm2_5")))
        val dust = number(current.opt("dust")) ?: return fail(reasonFor(current.opt("dust")))
        val aqiInt = aqi.roundToInt()
        if (aqiInt !in 0..500 || pm < 0.0 || dust < 0.0) return fail(FailureReason.OUT_OF_RANGE)
        return AirQualityResult.Success(AirQuality(aqiInt, pm.roundToInt(), dust.roundToInt()))
    }

    private fun fail(r: FailureReason) = AirQualityResult.Failure(r)

    private fun number(v: Any?): Double? = if (v is Number && v.toDouble().isFinite()) v.toDouble() else null

    private fun reasonFor(v: Any?) = if (v == null) FailureReason.MISSING_FIELD else FailureReason.WRONG_TYPE
}
