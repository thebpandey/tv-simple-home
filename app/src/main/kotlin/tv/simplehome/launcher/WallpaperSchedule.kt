package tv.simplehome.launcher

sealed interface WallpaperMode {
    data object Auto : WallpaperMode
    data object Off : WallpaperMode
    data class Pinned(val index: Int) : WallpaperMode
    /** Photos only, auto-rotating (falls back to scenes when there are none). */
    data object Photos : WallpaperMode
    /** Nature paintings only, auto-rotating (falls back to scenes when there are none). */
    data object Paintings : WallpaperMode
}

/** Pure scheduling rules for the wallpaper (no Android types) so they are JVM-testable. */
object WallpaperSchedule {
    const val PERIOD_MS = 30L * 60 * 1000
    const val FADE_MS = 3000L
    /** Sunrise/sunset changes are slow on purpose: a 30 second crossfade is noticed as a gradual change, not a switch. */
    const val DAY_NIGHT_FADE_MS = 30_000L
    // 60Hz vsync is 16.7ms; a 30ms threshold passes every 2nd vsync (33.3ms) with jitter headroom.
    const val MIN_FRAME_INTERVAL_MS = 30L

    val SCENE_NAMES = listOf("Forest", "Jungle", "Beach", "Mountains", "Snow Peaks", "Autumn Valley")
    val SCENE_COUNT = SCENE_NAMES.size

    /** Festival scenes follow the regular ones in the key space. A key = sceneIndex * KEY_MULT + variant. */
    const val KEY_MULT = 10
    val FESTIVAL_SCENE_IDS = listOf(
        "diwali", "holi", "navratri_dussehra", "janmashtami", "ganesh", "raksha_bandhan", "shivaratri",
        "sankranti_lohri", "vasant", "karwa_chauth", "chhath", "halloween", "thanksgiving", "christmas", "new_year",
    )
    /** Day variants live DAY_OFFSET above the night key of the same scene, so the cache is per (scene, variant, day). */
    const val DAY_OFFSET = 1000
    /** Photo keys live above every scene key: PHOTO_BASE + index into the photo list. */
    const val PHOTO_BASE = 2000
    /** Painted stills live above the photos: STILL_BASE + index into the still list. Both count as "image" keys (Ken Burns, no day/night variant). */
    val STILL_BASE = PHOTO_BASE + PhotoManifest.MAX_ENTRIES
    val KEY_SPACE = STILL_BASE + StillManifest.MAX_ENTRIES
    /** True for photo AND still keys: images, as opposed to procedural scenes. */
    fun isPhotoKey(key: Int): Boolean = key >= PHOTO_BASE
    fun isStillKey(key: Int): Boolean = key >= STILL_BASE

    fun withDay(key: Int, day: Boolean): Int = if (day) key + DAY_OFFSET else key
    fun isDayKey(key: Int): Boolean = key in DAY_OFFSET until PHOTO_BASE
    fun baseKey(key: Int): Int = key % DAY_OFFSET

    fun festivalSceneKey(sceneId: String, variant: Int): Int? {
        val i = FESTIVAL_SCENE_IDS.indexOf(sceneId)
        return if (i < 0) null else (SCENE_COUNT + i) * KEY_MULT + variant
    }

    /**
     * Scene key to draw now. A festival overrides ONLY Auto: a pinned scene or Off is the user's explicit choice,
     * so a festival must not hijack it.
     */
    fun keyAt(mode: WallpaperMode, nowMs: Long, festivalKey: Int?): Int? = when (mode) {
        WallpaperMode.Off -> null
        WallpaperMode.Auto, WallpaperMode.Photos, WallpaperMode.Paintings -> festivalKey ?: sceneAt(mode, nowMs)?.let { it * KEY_MULT }
        is WallpaperMode.Pinned -> sceneAt(mode, nowMs)?.let { it * KEY_MULT }
    }

    /** Scene to show now, or null when the wallpaper is off. */
    fun sceneAt(mode: WallpaperMode, nowMs: Long, sceneCount: Int = SCENE_COUNT): Int? = when (mode) {
        WallpaperMode.Off -> null
        WallpaperMode.Auto, WallpaperMode.Photos, WallpaperMode.Paintings -> ((nowMs / PERIOD_MS) % sceneCount).toInt()
        is WallpaperMode.Pinned -> mode.index.coerceIn(0, sceneCount - 1)
    }

    fun isFading(elapsedMs: Long, durationMs: Long = FADE_MS): Boolean = elapsedMs in 0 until durationMs

    /** 0 = old scene only, 1 = new scene only. */
    fun fadeProgress(elapsedMs: Long, durationMs: Long = FADE_MS): Float = (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)

    fun shouldSkipFrame(frameMs: Long, lastRenderMs: Long): Boolean =
        lastRenderMs >= 0 && frameMs - lastRenderMs < MIN_FRAME_INTERVAL_MS

    /** Off never animates; animator scale 0 means the user disabled animations, so draw one still frame. */
    fun wantsFrames(mode: WallpaperMode, animatorScale: Float): Boolean =
        mode != WallpaperMode.Off && animatorScale > 0f

    fun serialize(mode: WallpaperMode): String = when (mode) {
        WallpaperMode.Auto -> "auto"
        WallpaperMode.Photos -> "photos"
        WallpaperMode.Paintings -> "paintings"
        WallpaperMode.Off -> "off"
        is WallpaperMode.Pinned -> "scene:${mode.index}"
    }

    /** Anything missing or unrecognised falls back to Auto, the default. */
    fun parse(raw: String?, sceneCount: Int = SCENE_COUNT): WallpaperMode = when {
        raw == "off" -> WallpaperMode.Off
        raw == "photos" -> WallpaperMode.Photos
        raw == "paintings" -> WallpaperMode.Paintings
        raw != null && raw.startsWith("scene:") ->
            raw.removePrefix("scene:").toIntOrNull()?.takeIf { it in 0 until sceneCount }
                ?.let { WallpaperMode.Pinned(it) } ?: WallpaperMode.Auto
        else -> WallpaperMode.Auto
    }

    /** Fade length: only the day/night flag changing (same scene) is the slow sunrise/sunset fade. */
    fun fadeDurationMs(fromKey: Int, toKey: Int): Long =
        if (!isPhotoKey(fromKey) && !isPhotoKey(toKey) && baseKey(fromKey) == baseKey(toKey) && isDayKey(fromKey) != isDayKey(toKey)) DAY_NIGHT_FADE_MS else FADE_MS
}

/** User choice for the day/night variant. */
enum class DayNightMode(val id: String, val label: String) {
    AUTO("auto", "Auto (sunrise/sunset)"),
    DAY("day", "Always day"),
    NIGHT("night", "Always night");

    /** [sunSaysDay] comes from SunTimes for the saved location. */
    fun resolve(sunSaysDay: Boolean): Boolean = when (this) {
        AUTO -> sunSaysDay
        DAY -> true
        NIGHT -> false
    }

    companion object {
        /** Missing or unknown values fall back to AUTO, the default. */
        fun parse(raw: String?): DayNightMode = entries.firstOrNull { it.id == raw } ?: AUTO
    }
}
