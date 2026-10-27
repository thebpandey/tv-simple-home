package tv.simplehome.launcher

/** Pure favorites logic (no Android types) so it can be unit tested on the JVM. All ops return new lists. */
object Favorites {
    val DEFAULTS = listOf(
        "com.netflix.ninja",
        "com.google.android.youtube.tv",
        "com.amazon.amazonvideo.livingroom",
        "com.debridstream.tv",
        "com.wbd.stream",
        "com.phlox.tvwebbrowser",
    )

    fun seed(installed: Set<String>, defaults: List<String> = DEFAULTS): List<String> =
        defaults.filter { it in installed }

    /** Saved list wins (minus uninstalled apps); null means nothing was ever saved, so use defaults. */
    fun resolve(saved: List<String>?, installed: Set<String>): List<String> =
        saved?.filter { it in installed }?.distinct() ?: seed(installed)

    fun add(list: List<String>, pkg: String): List<String> =
        if (pkg in list) list else list + pkg

    fun remove(list: List<String>, pkg: String): List<String> = list - pkg

    fun moveLeft(list: List<String>, pkg: String): List<String> = swap(list, pkg, -1)

    fun moveRight(list: List<String>, pkg: String): List<String> = swap(list, pkg, +1)

    private fun swap(list: List<String>, pkg: String, delta: Int): List<String> {
        val from = list.indexOf(pkg)
        val to = from + delta
        if (from < 0 || to !in list.indices) return list
        return list.toMutableList().also { it[from] = it[to]; it[to] = pkg }
    }

    // One package per line; package names can never contain a newline.
    fun serialize(list: List<String>): String = list.joinToString("\n")

    /** null in means "never saved"; an empty string is a deliberately emptied list. */
    fun deserialize(raw: String?): List<String>? =
        raw?.let { r -> r.split('\n').filter { it.isNotBlank() } }
}
