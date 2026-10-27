package tv.simplehome.launcher

sealed interface RestoreTarget {
    data class Favorite(val index: Int) : RestoreTarget
    data object Menu : RestoreTarget
}

/** Pure choice of where focus goes when Home comes back (after an app, or a HOME press). Never the weather widget. */
object FocusRestore {
    fun target(lastLaunchedPackage: String?, favoritePackages: List<String>): RestoreTarget {
        val i = if (lastLaunchedPackage == null) -1 else favoritePackages.indexOf(lastLaunchedPackage)
        return when {
            i >= 0 -> RestoreTarget.Favorite(i)
            favoritePackages.isNotEmpty() -> RestoreTarget.Favorite(0) // launched from All apps / externally / not a favorite
            else -> RestoreTarget.Menu
        }
    }
}
