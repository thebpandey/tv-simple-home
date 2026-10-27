package tv.simplehome.launcher

import android.graphics.BitmapFactory
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * One validated entry of scenes.json: a painted 1920x1080 still of [theme], design number [index] (1..4), for the day or night sun
 * state. [luma] is the brightness of the quote band (0..255) when known.
 */
data class StillEntry(val id: String, val file: String, val theme: String, val night: Boolean, val index: Int, val luma: Int?)

sealed interface StillManifestResult {
    data class Success(val stills: List<StillEntry>, val skipped: Int) : StillManifestResult
    data class Failure(val reason: FailureReason) : StillManifestResult
}

/** Parses scenes/scenes.json (version 2, with version 1 accepted) with the same distrust as photos.json: it sits on shared-writable storage. */
object StillManifest {
    const val MAX_ENTRIES = 256
    const val MAX_BYTES = 128 * 1024L
    const val MAX_INDEX = 4

    val NATURE_THEMES = listOf("forest", "jungle", "beach", "mountains", "snowpeaks", "autumn")
    val FESTIVAL_THEMES = listOf(
        "diwali", "holi", "navratri", "dussehra", "janmashtami", "ganesh", "rakhi", "shivaratri", "sankranti", "lohri", "vasant", "karwa",
        "chhath", "halloween", "thanksgiving", "christmas", "newyear", "july4",
    )
    /** Only these themes are ever used; anything else in the file is ignored. */
    val THEMES: Set<String> = (NATURE_THEMES + FESTIVAL_THEMES).toSet()

    /** Version 1 ids (one painting per theme): the theme it stands for. Each is design 1. */
    private val V1_THEMES = mapOf(
        "diwali-night" to "diwali", "holi-day" to "holi", "navratri-night" to "navratri", "dussehra-dusk" to "dussehra",
        "janmashtami-night" to "janmashtami", "ganesh-day" to "ganesh", "rakhi-day" to "rakhi", "shivaratri-night" to "shivaratri",
        "sankranti-day" to "sankranti", "lohri-night" to "lohri", "vasant-day" to "vasant", "karwa-night" to "karwa", "chhath-dusk" to "chhath",
        "halloween-night" to "halloween", "thanksgiving-day" to "thanksgiving", "christmas-night" to "christmas", "newyear-night" to "newyear",
        "july4-night" to "july4", "autumn-day" to "autumn", "autumn-night" to "autumn",
    )

    private val ID_PATTERN = Regex("[a-z0-9][a-z0-9_-]{0,63}")

    fun parse(body: String?): StillManifestResult {
        if (body.isNullOrBlank()) return StillManifestResult.Failure(FailureReason.EMPTY_BODY)
        val root = try { JSONObject(body) } catch (e: JSONException) { return StillManifestResult.Failure(FailureReason.BAD_JSON) }
        val version = root.opt("version")
        if (version != 1 && version != 2) return StillManifestResult.Failure(FailureReason.WRONG_TYPE)
        val arr = root.optJSONArray("scenes") ?: return StillManifestResult.Failure(FailureReason.MISSING_FIELD)
        val out = ArrayList<StillEntry>()
        var skipped = 0
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i)?.let { if (version == 1) entryV1(it) else entryV2(it) }
            // The first entry of an id wins, and so does the first for a (theme, variant, design) slot.
            val dup = e != null && out.any { it.id == e.id || (it.theme == e.theme && it.night == e.night && it.index == e.index) }
            if (e == null || dup || out.size >= MAX_ENTRIES) skipped++ else out.add(e)
        }
        return StillManifestResult.Success(out, skipped)
    }

    private fun str(o: JSONObject, key: String) = (o.opt(key) as? String)?.trim()?.takeIf { it.isNotEmpty() }

    private fun variantOf(o: JSONObject): Boolean? = when (str(o, "variant")) { "day" -> false; "night" -> true; else -> null }

    private fun lumaOf(o: JSONObject) = (o.opt("luma") as? Number)?.toInt()?.takeIf { it in 0..255 }

    private fun entryV1(o: JSONObject): StillEntry? {
        val id = str(o, "id")?.takeIf { it in V1_THEMES } ?: return null
        val file = str(o, "file")?.takeIf(PhotoManifest::isSafeFileName) ?: return null
        val night = variantOf(o) ?: return null
        return StillEntry(id, file, V1_THEMES.getValue(id), night, 1, lumaOf(o))
    }

    private fun entryV2(o: JSONObject): StillEntry? {
        val id = str(o, "id")?.takeIf { ID_PATTERN.matches(it) } ?: return null
        val file = str(o, "file")?.takeIf(PhotoManifest::isSafeFileName) ?: return null
        val theme = str(o, "theme")?.takeIf { it in THEMES } ?: return null
        val night = variantOf(o) ?: return null
        val index = (o.opt("index") as? Int)?.takeIf { it in 1..MAX_INDEX } ?: return null
        return StillEntry(id, file, theme, night, index, lumaOf(o))
    }
}

class StillItem(val entry: StillEntry, val file: File, val width: Int, val height: Int)

/** Which painting theme stands in for a festival or a nature scene, and which light overlay goes with each painting. */
object StillMapping {
    private val FESTIVAL_THEMES = mapOf(
        "diwali" to "diwali", "holi" to "holi",
        "chaitra_navratri" to "navratri", "sharad_navratri" to "navratri", "durga_ashtami" to "navratri",
        "dussehra" to "dussehra", "janmashtami" to "janmashtami", "ganesh_chaturthi" to "ganesh",
        "raksha_bandhan" to "rakhi", "maha_shivaratri" to "shivaratri", "makar_sankranti" to "sankranti",
        "lohri" to "lohri", "vasant_panchami" to "vasant", "karwa_chauth" to "karwa", "chhath" to "chhath",
        "halloween" to "halloween", "thanksgiving" to "thanksgiving", "christmas" to "christmas",
        "new_year" to "newyear", "us_independence" to "july4",
        // Ram Navami and Hanuman Jayanti deliberately keep their procedural scenes.
    )

    fun themeForFestival(festivalId: String): String? = FESTIVAL_THEMES[festivalId]

    fun isNature(theme: String): Boolean = theme in StillManifest.NATURE_THEMES

    fun overlayFor(entry: StillEntry): StillOverlay = when (entry.theme) {
        "diwali" -> if (entry.night && entry.index == 1) StillOverlay.DIWALI else StillOverlay.DIWALI_SOFT // the diya positions are matched to the night design 1 painting (v1 "diwali-night" is design 1 too)
        "newyear" -> StillOverlay.FIREWORKS
        "july4" -> StillOverlay.FIREWORKS_RWB
        "christmas" -> StillOverlay.SNOW
        "holi" -> StillOverlay.PUFFS
        "halloween" -> StillOverlay.BATS
        "autumn" -> StillOverlay.LEAVES
        "navratri", "dussehra", "janmashtami", "shivaratri", "lohri", "karwa", "chhath" -> if (entry.night) StillOverlay.TWINKLE else StillOverlay.NONE
        "forest" -> if (entry.night) StillOverlay.MIST_FIREFLIES else StillOverlay.MIST
        "jungle" -> StillOverlay.SPORES
        "beach" -> StillOverlay.GLINTS
        "mountains" -> StillOverlay.CLOUDS
        "snowpeaks" -> StillOverlay.SNOW_LIGHT
        else -> StillOverlay.NONE // sankranti (the kites are painted) and the bright day festival stills
    }

    /** Nature theme of procedural scene [sceneIndex] (0..5). */
    fun natureTheme(sceneIndex: Int): String? = StillManifest.NATURE_THEMES.getOrNull(sceneIndex)
}

/** Pure pool handling: which designs of a theme exist and which one a slot shows. */
object StillPools {
    /** Indices (into [stills]) of the designs of [theme], ordered by design number so the rotation does not depend on the manifest's line order. */
    fun designsOf(stills: List<StillEntry>, theme: String): List<Int> =
        stills.indices.filter { stills[it].theme == theme }.sortedWith(compareBy({ stills[it].index }, { stills[it].night }))

    /**
     * The design for this 30-minute [slot]: matching the sun state ([night]); if the theme has none of that kind, the other kind; null when the
     * theme has no paintings at all (the caller then uses the procedural scene). Deterministic, and every design is shown once per cycle.
     */
    fun pick(stills: List<StillEntry>, theme: String, night: Boolean, slot: Long): Int? {
        val designs = designsOf(stills, theme)
        val i = WallpaperPicker.pickPhotoIndex(designs.map { stills[it].night }, night, slot) ?: return null
        return designs[i]
    }
}

/** UI light/dark follows the shown image's brightness (so white text never lands on a pastel painting); scenes follow the sun. */
object UiTheme {
    const val LIGHT_LUMA = 110

    /** [imageLuma] is null while a procedural scene is showing. An explicit Always day / Always night choice always wins. */
    fun isDay(mode: DayNightMode, sunSaysDay: Boolean, imageLuma: Int?): Boolean = when {
        mode != DayNightMode.AUTO -> mode.resolve(sunSaysDay)
        imageLuma != null -> imageLuma >= LIGHT_LUMA
        else -> sunSaysDay
    }
}

/** Loads scenes/scenes.json and the stills it lists. Blocking: call off the UI thread. */
object StillStore {
    private const val TAG = "StillStore"

    fun load(dir: File?): List<StillItem> {
        if (dir == null) return emptyList()
        val manifest = File(dir, "scenes.json")
        if (!manifest.isFile || manifest.length() > StillManifest.MAX_BYTES) return emptyList()
        val body = try { manifest.readText() } catch (e: IOException) { return emptyList() }
        val parsed = StillManifest.parse(body) as? StillManifestResult.Success ?: return emptyList()
        val root = try { dir.canonicalFile } catch (e: IOException) { return emptyList() }
        return parsed.stills.mapNotNull { entry ->
            val file = File(dir, entry.file)
            val inside = try { file.canonicalFile.parentFile == root } catch (e: IOException) { false }
            if (!inside || !file.isFile) return@mapNotNull null
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, opts)
            if (opts.outWidth <= 0 || opts.outHeight <= 0 || opts.outMimeType?.startsWith("image/") != true) {
                if (BuildConfig.DEBUG) Log.d(TAG, "skip ${entry.file}: not an image")
                null
            } else StillItem(entry, file, opts.outWidth, opts.outHeight)
        }
    }
}
