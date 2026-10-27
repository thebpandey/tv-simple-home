package tv.simplehome.launcher

import org.json.JSONException
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One photo entry from photos.json. Only validated values get here. */
data class PhotoEntry(
    val id: String, val file: String, val night: Boolean, val photographer: String,
    val photographerUrl: String, val photoUrl: String, val description: String,
    /** Optional 0..255 average brightness from the manifest; unknown fields like this one must never make an entry fail. */
    val luma: Int? = null,
)

sealed interface ManifestResult {
    data class Success(val photos: List<PhotoEntry>, val skipped: Int) : ManifestResult
    data class Failure(val reason: FailureReason) : ManifestResult
}

/** Parses photos.json defensively: it lives on shared storage, so it is untrusted input. */
object PhotoManifest {
    const val MAX_ENTRIES = 200
    private val EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp")

    /** A plain file name: no directories, no traversal, no hidden files, a known image extension. */
    fun isSafeFileName(name: String): Boolean =
        name.isNotEmpty() && name.length <= 128 && '/' !in name && '\\' !in name && ".." !in name && !name.startsWith(".") &&
            name.none { it.code < 32 } && EXTENSIONS.any { name.lowercase().endsWith(it) }

    fun parse(body: String?): ManifestResult {
        if (body.isNullOrBlank()) return ManifestResult.Failure(FailureReason.EMPTY_BODY)
        val root = try { JSONObject(body) } catch (e: JSONException) { return ManifestResult.Failure(FailureReason.BAD_JSON) }
        if (root.opt("version") != 1) return ManifestResult.Failure(FailureReason.WRONG_TYPE)
        val arr = root.optJSONArray("photos") ?: return ManifestResult.Failure(FailureReason.MISSING_FIELD)
        val out = ArrayList<PhotoEntry>()
        var skipped = 0
        for (i in 0 until arr.length()) {
            if (out.size >= MAX_ENTRIES) { skipped += arr.length() - i; break }
            val e = arr.optJSONObject(i)?.let(::entry)
            if (e == null) skipped++ else out.add(e)
        }
        return ManifestResult.Success(out, skipped)
    }

    private fun entry(o: JSONObject): PhotoEntry? {
        fun str(key: String) = (o.opt(key) as? String)?.trim()?.takeIf { it.isNotEmpty() }
        // Ids may be strings or numbers; everything unknown in an entry (e.g. "luma", future fields) is simply ignored.
        val id = str("id") ?: (o.opt("id") as? Number)?.toString() ?: return null
        val file = str("file")?.takeIf(::isSafeFileName) ?: return null
        val variant = str("variant") ?: return null
        if (variant != "day" && variant != "night") return null
        val photographer = str("photographer") ?: return null
        val luma = (o.opt("luma") as? Number)?.toInt()?.takeIf { it in 0..255 }
        return PhotoEntry(id, file, variant == "night", photographer, str("photographerUrl").orEmpty(), str("photoUrl").orEmpty(), str("description").orEmpty(), luma)
    }
}

enum class PhotoSource(val id: String, val label: String) {
    SCENES_AND_PHOTOS("both", "Scenes + Photos"),
    SCENES("scenes", "Scenes only"),
    PHOTOS("photos", "Photos only");

    companion object {
        /** Photos only is the default for anyone who has not chosen (SettingsMigration keeps existing users' behaviour). */
        fun parse(raw: String?): PhotoSource = entries.firstOrNull { it.id == raw } ?: PHOTOS
    }
}

sealed interface Pick {
    /** [key] is a base scene key (index * KEY_MULT + variant); the view adds the day offset. */
    data class SceneKey(val key: Int) : Pick
    data class Photo(val index: Int) : Pick
    /** [index] is into the list of nature paintings handed to the picker. */
    data class Painting(val index: Int) : Pick
}

/** Pure wallpaper selection across procedural scenes, festival scenes, paintings and photos. */
object WallpaperPicker {
    /** Which item of a pool for this 30-minute [slot]: variants matching the sun state first, any item if none match (every item once per cycle). */
    fun pickPhotoIndex(photoIsNight: List<Boolean>, night: Boolean, slot: Long): Int? {
        if (photoIsNight.isEmpty()) return null
        var candidates = photoIsNight.indices.filter { photoIsNight[it] == night }
        if (candidates.isEmpty()) candidates = photoIsNight.indices.toList()
        val n = candidates.size.toLong()
        val order = Quotes.shuffledOrder(candidates.size, seed = Math.floorDiv(slot, n)) // every item once per cycle, deterministic
        return candidates[order[Math.floorMod(slot, n).toInt()]]
    }

    fun pick(
        mode: WallpaperMode, sources: SourceSet, nowMs: Long, festivalKey: Int?,
        photoIsNight: List<Boolean>, night: Boolean, paintingIsNight: List<Boolean> = emptyList(),
    ): Pick? {
        val slot = nowMs / WallpaperSchedule.PERIOD_MS
        fun scene(index: Long) = Pick.SceneKey(Math.floorMod(index, WallpaperSchedule.SCENE_COUNT.toLong()).toInt() * WallpaperSchedule.KEY_MULT)
        fun photo(photoSlot: Long) = pickPhotoIndex(photoIsNight, night, photoSlot)?.let { Pick.Photo(it) }
        fun painting(paintSlot: Long) = pickPhotoIndex(paintingIsNight, night, paintSlot)?.let { Pick.Painting(it) }
        return when (mode) {
            WallpaperMode.Off -> null
            is WallpaperMode.Pinned -> Pick.SceneKey(mode.index.coerceIn(0, WallpaperSchedule.SCENE_COUNT - 1) * WallpaperSchedule.KEY_MULT)
            WallpaperMode.Photos -> photo(slot) ?: scene(slot)
            WallpaperMode.Paintings -> painting(slot) ?: scene(slot)
            WallpaperMode.Auto -> {
                if (festivalKey != null) return Pick.SceneKey(festivalKey) // festivals override Auto, as before
                val kinds = SourcePicker.available(sources, photoIsNight.isNotEmpty(), paintingIsNight.isNotEmpty())
                val inner = SourcePicker.innerSlot(kinds, slot)
                when (SourcePicker.kindAt(kinds, slot)) {
                    SourceKind.PHOTOS -> photo(inner)
                    SourceKind.PAINTINGS -> painting(inner)
                    SourceKind.SCENES -> scene(inner)
                }
            }
        }
    }
}

/** Slow pan and zoom over a photo that can never expose an edge. */
object KenBurns {
    const val DURATION_SEC = 1800f // the 30-minute display period
    const val OVERSCAN = 1.04f // headroom for the drift at the start
    const val ZOOM_RANGE = 0.08f // zoom grows by 8% over the period
    const val DRIFT_FRACTION = 0.85f // never uses more than 85% of the available slack

    data class Transform(val scale: Float, val tx: Float, val ty: Float)

    fun transform(elapsedSec: Float, photoW: Float, photoH: Float, viewW: Float, viewH: Float): Transform {
        val out = FloatArray(3)
        transformInto(out, elapsedSec, photoW, photoH, viewW, viewH)
        return Transform(out[0], out[1], out[2])
    }

    /** Allocation-free form for the frame loop: out = [scale, tx, ty]. [scale] maps photo pixels to view pixels; tx/ty move the photo's centre. */
    fun transformInto(out: FloatArray, elapsedSec: Float, photoW: Float, photoH: Float, viewW: Float, viewH: Float) {
        val cover = maxOf(viewW / photoW, viewH / photoH)
        val progress = (elapsedSec / DURATION_SEC).coerceIn(0f, 1f)
        val eased = 0.5f - 0.5f * cos(PI.toFloat() * progress) // sine easing: no linear jumps
        val scale = cover * (OVERSCAN + ZOOM_RANGE * eased)
        val slackX = (photoW * scale - viewW) / 2f
        val slackY = (photoH * scale - viewH) / 2f
        val tau = (2.0 * PI).toFloat()
        val dx = 0.6f * sin(tau * elapsedSec / 97f) + 0.4f * sin(tau * elapsedSec / 151f + 1.1f)
        val dy = 0.6f * sin(tau * elapsedSec / 131f + 0.5f) + 0.4f * sin(tau * elapsedSec / 179f + 2.3f)
        out[0] = scale
        out[1] = DRIFT_FRACTION * slackX * dx
        out[2] = DRIFT_FRACTION * slackY * dy
    }

    fun credit(e: PhotoEntry) = "Photo: ${e.photographer} / Unsplash"
}
