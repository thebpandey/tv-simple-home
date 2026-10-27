package tv.simplehome.launcher

/**
 * Brings stored settings from older app versions forward without ever dropping a user's choice: it only adds or normalises
 * values, never removes a key. Pure so it can be unit tested.
 */
object SettingsMigration {
    const val KEY_VERSION = "settings_version"
    const val CURRENT_VERSION = 3

    data class Result(val puts: Map<String, String>, val newVersion: Int)

    private val SCENE_NAMES = mapOf(
        "forest" to 0, "jungle" to 1, "beach" to 2, "mountains" to 3, "snow peaks" to 4, "snowpeaks" to 4, "autumn valley" to 5, "autumn" to 5,
    )

    /** Accepts every shape an older version (or a hand edit) may have stored; an unrecognised value is kept untouched. */
    fun normalizeWallpaper(raw: String): String {
        val t = raw.trim()
        val lower = t.lowercase()
        return when {
            lower == "auto" || lower == "off" || lower == "photos" || lower == "paintings" -> lower
            lower.startsWith("scene:") -> {
                val n = lower.removePrefix("scene:").trim().toIntOrNull()
                if (n != null && n in 0 until WallpaperSchedule.SCENE_COUNT) "scene:$n"
                else SCENE_NAMES[lower.removePrefix("scene:").trim()]?.let { "scene:$it" } ?: raw
            }
            SCENE_NAMES.containsKey(lower) -> "scene:${SCENE_NAMES.getValue(lower)}"
            lower.toIntOrNull() in 0 until WallpaperSchedule.SCENE_COUNT -> "scene:$lower"
            else -> raw
        }
    }

    fun migrate(prefs: Map<String, *>): Result {
        val stored = (prefs[KEY_VERSION] as? Int) ?: 0
        if (stored >= CURRENT_VERSION) return Result(emptyMap(), stored)
        val puts = LinkedHashMap<String, String>()
        val wallpaper = prefs["wallpaper"] as? String
        if (wallpaper != null) {
            val normal = normalizeWallpaper(wallpaper)
            if (normal != wallpaper) puts["wallpaper"] = normal
        }
        // Photos-only is the new default for people who never chose. Anyone who already picked a wallpaper (or has a legacy
        // value) keeps exactly what they saw before photos existed: scenes only.
        if (prefs["photo_source"] == null && prefs.keys.any { it == "wallpaper" || it == "favorites" || it == "festival_wallpapers" }) {
            puts["photo_source"] = PhotoSource.SCENES.id
        }
        return Result(puts, CURRENT_VERSION)
    }
}
