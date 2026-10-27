package tv.simplehome.launcher

enum class SourceKind(val id: String) { PHOTOS("photos"), PAINTINGS("paintings"), SCENES("scenes") }

/** Which wallpaper sources Auto-rotate may draw from. At least one is always on. */
data class SourceSet(val photos: Boolean, val paintings: Boolean, val scenes: Boolean) {
    fun has(kind: SourceKind): Boolean = when (kind) {
        SourceKind.PHOTOS -> photos
        SourceKind.PAINTINGS -> paintings
        SourceKind.SCENES -> scenes
    }

    /** Switches [kind] to [on]; switching off the last enabled source is refused (the set comes back unchanged). */
    fun with(kind: SourceKind, on: Boolean): SourceSet {
        val next = when (kind) {
            SourceKind.PHOTOS -> copy(photos = on)
            SourceKind.PAINTINGS -> copy(paintings = on)
            SourceKind.SCENES -> copy(scenes = on)
        }
        return if (next.count() == 0) this else next
    }

    fun count(): Int = listOf(photos, paintings, scenes).count { it }

    fun serialize(): String = SourceKind.entries.filter { has(it) }.joinToString(",") { it.id }

    companion object {
        val ALL = SourceSet(photos = true, paintings = true, scenes = true)

        /** Unknown tokens are ignored; nothing recognisable (or an empty list) means null so the caller can fall back. */
        fun parse(raw: String?): SourceSet? {
            if (raw == null) return null
            val ids = raw.split(',').map { it.trim() }.toSet()
            val set = SourceSet(SourceKind.PHOTOS.id in ids, SourceKind.PAINTINGS.id in ids, SourceKind.SCENES.id in ids)
            return if (set.count() == 0) null else set
        }
    }
}

/** Settings migration for the wallpaper source: the old single "Source" radio becomes the three switches without dropping the choice. */
object WallpaperSources {
    /** Old values: both = Scenes + Photos, scenes = Scenes only, photos = Photos only. "Both" gains Paintings (new, on by default). */
    fun fromLegacy(raw: String?): SourceSet? = when (raw) {
        "both" -> SourceSet.ALL
        "scenes" -> SourceSet(photos = false, paintings = false, scenes = true)
        "photos" -> SourceSet(photos = true, paintings = false, scenes = false)
        else -> null
    }

    /** The new setting wins; else the migrated old one; else everything on. */
    fun resolve(newRaw: String?, legacyRaw: String?): SourceSet = SourceSet.parse(newRaw) ?: fromLegacy(legacyRaw) ?: SourceSet.ALL
}

/** Pure choice of which source Auto-rotate shows in a 30-minute [slot]. */
object SourcePicker {
    /**
     * Enabled sources that actually have something to show (scenes always do). If every enabled source is empty (e.g. only Photos is on
     * and the folder is empty) the procedural scenes stand in, as before.
     */
    fun available(set: SourceSet, hasPhotos: Boolean, hasPaintings: Boolean): List<SourceKind> {
        val out = ArrayList<SourceKind>(3)
        if (set.photos && hasPhotos) out.add(SourceKind.PHOTOS)
        if (set.paintings && hasPaintings) out.add(SourceKind.PAINTINGS)
        if (set.scenes) out.add(SourceKind.SCENES)
        return if (out.isEmpty()) listOf(SourceKind.SCENES) else out
    }

    /** Sources take turns slot by slot (photo, painting, scene, photo, ...). */
    fun kindAt(kinds: List<SourceKind>, slot: Long): SourceKind = kinds[Math.floorMod(slot, kinds.size.toLong()).toInt()]

    /** The slot counter inside one source: each source advances once per full turn, so it shows every item once per cycle. */
    fun innerSlot(kinds: List<SourceKind>, slot: Long): Long = Math.floorDiv(slot, kinds.size.toLong())
}
