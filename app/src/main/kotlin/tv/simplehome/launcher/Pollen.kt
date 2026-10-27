package tv.simplehome.launcher

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

enum class PollenKind(val label: String) { TREE("Tree"), GRASS("Grass"), WEED("Weed") }

/** [upi] is Google's Universal Pollen Index, 0 (none) to 5 (very high); 0 also covers a type that is out of season. */
data class PollenLevel(val kind: PollenKind, val upi: Int)

data class PollenData(val levels: List<PollenLevel>)

sealed interface PollenResult {
    data class Success(val data: PollenData) : PollenResult
    data class Failure(val reason: FailureReason) : PollenResult
}

/** Pure pollen rules for the Google Pollen API: key validation, URL, defensive parsing of the untrusted response and the display line. */
object PollenRules {
    // A daily forecast: 4 requests a day is plenty, and keeps the billed API's usage tiny.
    const val REFRESH_MS = 6L * 60 * 60 * 1000
    const val HIDE_AFTER_MS = 12L * 60 * 60 * 1000
    const val KEY_FILE = "pollen.key"
    const val KEY_HEADER = "X-Goog-Api-Key"

    // Google API keys are 39 URL-safe characters. Anything else is rejected rather than sent, which also rules out header injection.
    private val KEY_SHAPE = Regex("^[A-Za-z0-9_-]{30,64}$")

    private val LABELS = arrayOf("None", "Very Low", "Low", "Moderate", "High", "Very High")

    /** The key lives only on the device (pushed next to the photos), never in the APK or the repo. Null when absent or malformed. */
    fun loadKey(dir: File?): String? {
        val f = dir?.let { File(it, KEY_FILE) } ?: return null
        val raw = try {
            if (!f.isFile || f.length() > 256) return null
            f.readText()
        } catch (e: IOException) {
            return null
        }
        return validKey(raw)
    }

    fun validKey(raw: String?): String? = raw?.trim()?.takeIf { KEY_SHAPE.matches(it) }

    fun buildUrl(lat: Double, lon: Double): String =
        "https://pollen.googleapis.com/v1/forecast:lookup?location.latitude=$lat&location.longitude=$lon" +
            "&days=1&plantsDescription=false&languageCode=en"

    // A failing request (API disabled, quota hit) is retried hourly, not on every 15-minute weather tick.
    const val RETRY_MS = 60L * 60 * 1000

    fun needsFetch(cacheAgeMs: Long?): Boolean = cacheAgeMs == null || cacheAgeMs < 0 || cacheAgeMs >= REFRESH_MS

    /**
     * [attemptAgeMs] is the age of the last request, successful or not; a negative age (clock moved back) allows a retry.
     * A replaced key skips the backoff, so a fixed key shows pollen at once instead of up to an hour later.
     */
    fun isDue(cacheAgeMs: Long?, attemptAgeMs: Long?, keyChanged: Boolean = false): Boolean =
        needsFetch(cacheAgeMs) && (keyChanged || attemptAgeMs == null || attemptAgeMs < 0 || attemptAgeMs >= RETRY_MS)

    /** Remembers which key the last attempt used without storing the key itself. */
    fun keyTag(key: String): Int = key.hashCode()

    fun shouldShow(cacheAgeMs: Long?): Boolean = cacheAgeMs != null && cacheAgeMs in 0 until HIDE_AFTER_MS

    fun label(upi: Int): String = LABELS[upi.coerceIn(0, 5)]

    /** Shares the air-quality colour scale so one glance reads both dots the same way: green up to Low, then yellow, orange, red. */
    fun dotCategory(upi: Int): AqiCategory = when {
        upi <= 2 -> AqiCategory.GOOD
        upi == 3 -> AqiCategory.MODERATE
        upi == 4 -> AqiCategory.SENSITIVE
        else -> AqiCategory.UNHEALTHY
    }

    fun peak(d: PollenData): Int = d.levels.maxOfOrNull { it.upi } ?: 0

    /**
     * Compact so the weather card never grows over the clock group: the peak level, naming the type(s) at that peak unless every
     * type ties. Worst case "Pollen · Tree, Grass Very High" is shorter than the air-quality line above it.
     */
    fun line(d: PollenData): String {
        val peak = peak(d)
        if (peak == 0) return "Pollen · None"
        val top = d.levels.filter { it.upi == peak }.sortedBy { it.kind.ordinal }
        if (top.size == d.levels.size) return "Pollen · ${label(peak)}"
        return "Pollen · ${top.joinToString(", ") { it.kind.label }} ${label(peak)}"
    }

    fun parse(body: String?): PollenResult {
        if (body.isNullOrBlank()) return fail(FailureReason.EMPTY_BODY)
        val root = try { JSONObject(body) } catch (e: JSONException) { return fail(FailureReason.BAD_JSON) }
        val days = root.opt("dailyInfo") ?: return fail(FailureReason.MISSING_FIELD)
        if (days !is JSONArray) return fail(FailureReason.WRONG_TYPE)
        val today = days.opt(0) ?: return fail(FailureReason.MISSING_FIELD)
        if (today !is JSONObject) return fail(FailureReason.WRONG_TYPE)
        // Locations without pollen coverage come back without types; that hides the line instead of claiming "None".
        val types = today.opt("pollenTypeInfo") ?: return fail(FailureReason.MISSING_FIELD)
        if (types !is JSONArray) return fail(FailureReason.WRONG_TYPE)

        val levels = ArrayList<PollenLevel>(3)
        for (i in 0 until types.length()) {
            val t = types.opt(i) as? JSONObject ?: return fail(FailureReason.WRONG_TYPE)
            val kind = PollenKind.values().firstOrNull { it.name == t.opt("code") } ?: continue // unknown future codes are skipped
            if (levels.any { it.kind == kind }) continue
            val info = t.opt("indexInfo")
            val upi = when {
                info == null -> 0 // out of season: Google omits the index
                info !is JSONObject -> return fail(FailureReason.WRONG_TYPE)
                else -> when (val v = info.opt("value")) {
                    null -> 0
                    is Int -> v
                    is Long -> if (v in 0..5) v.toInt() else return fail(FailureReason.OUT_OF_RANGE)
                    else -> return fail(FailureReason.WRONG_TYPE)
                }
            }
            if (upi !in 0..5) return fail(FailureReason.OUT_OF_RANGE)
            levels.add(PollenLevel(kind, upi))
        }
        if (levels.isEmpty()) return fail(FailureReason.MISSING_FIELD)
        return PollenResult.Success(PollenData(levels))
    }

    private fun fail(r: FailureReason) = PollenResult.Failure(r)
}
