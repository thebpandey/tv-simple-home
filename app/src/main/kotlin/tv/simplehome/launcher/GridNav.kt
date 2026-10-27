package tv.simplehome.launcher

enum class GridKey { LEFT, RIGHT, UP, DOWN }

/** Pure D-pad navigation for the Launchpad grid: a flat list of [count] cells laid out in [cols] columns. */
object GridNav {
    /**
     * LEFT/RIGHT stop at the ends of their row (they never wrap to another row). UP/DOWN keep the column; into a shorter last row
     * they land on its last cell (the nearest column), and at the first or last row they stay put.
     */
    fun next(index: Int, key: GridKey, cols: Int, count: Int): Int {
        if (count <= 0 || index !in 0 until count) return index
        val col = index % cols
        return when (key) {
            GridKey.LEFT -> if (col == 0) index else index - 1
            GridKey.RIGHT -> if (col == cols - 1 || index == count - 1) index else index + 1
            GridKey.UP -> if (index < cols) index else index - cols
            GridKey.DOWN -> {
                val below = index + cols
                when {
                    below < count -> below
                    index / cols == (count - 1) / cols -> index // already on the last row
                    else -> count - 1
                }
            }
        }
    }
}
