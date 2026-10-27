package tv.simplehome.launcher

/** Where focus is inside the dock. Indices are positions in the pills / favorites rows. */
sealed interface DockFocus {
    data object MenuButton : DockFocus
    data class Pill(val index: Int) : DockFocus
    data class Favorite(val index: Int) : DockFocus
}

enum class DockKey { LEFT, RIGHT }

data class DockState(val menuOpen: Boolean, val focus: DockFocus)

/**
 * Pure LEFT/RIGHT navigation along the dock: [menu button] [pills, only when open] [favorites].
 * Index-based so it does not depend on geometry (the rows are scaled, pushed apart and slid sideways).
 * Returns null when the key is not handled here.
 */
object DockNav {
    fun next(state: DockState, key: DockKey, pillCount: Int, favCount: Int): DockState {
        val open = state.menuOpen
        return when (val f = state.focus) {
            DockFocus.MenuButton -> when (key) {
                DockKey.LEFT -> state // nothing is left of the button
                DockKey.RIGHT -> when {
                    open && pillCount > 0 -> state.copy(focus = DockFocus.Pill(0))
                    favCount > 0 -> state.copy(menuOpen = false, focus = DockFocus.Favorite(0))
                    else -> state
                }
            }
            is DockFocus.Pill -> when (key) {
                DockKey.LEFT -> state.copy(focus = if (f.index <= 0) DockFocus.MenuButton else DockFocus.Pill(f.index - 1))
                DockKey.RIGHT -> when {
                    f.index < pillCount - 1 -> state.copy(focus = DockFocus.Pill(f.index + 1))
                    // Only the LAST pill rolls the menu up and hands focus to the first favorite.
                    favCount > 0 -> DockState(menuOpen = false, focus = DockFocus.Favorite(0))
                    else -> DockState(menuOpen = false, focus = DockFocus.MenuButton)
                }
            }
            is DockFocus.Favorite -> when (key) {
                DockKey.LEFT -> state.copy(focus = if (f.index <= 0) DockFocus.MenuButton else DockFocus.Favorite(f.index - 1))
                DockKey.RIGHT -> state.copy(focus = if (f.index >= favCount - 1) f else DockFocus.Favorite(f.index + 1))
            }
        }
    }
}
