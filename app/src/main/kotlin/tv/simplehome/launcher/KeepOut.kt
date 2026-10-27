package tv.simplehome.launcher

/** Rectangle in dp on the 960x540dp screen. */
data class DpRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    /** True when a circle of [radius] around ([cx], [cy]) touches this rectangle. */
    fun intersectsCircle(cx: Float, cy: Float, radius: Float): Boolean {
        val nx = cx.coerceIn(left, right)
        val ny = cy.coerceIn(top, bottom)
        val dx = cx - nx
        val dy = cy - ny
        return dx * dx + dy * dy < radius * radius
    }
}

/**
 * Regions that carry text or controls. No scene's sun, moon or other focal motif may sit on them: the header text groups (clock
 * group with its banner, weather group), the quote band and the dock band. Extents of text are estimates (Inter at the
 * given sizes), kept slightly generous.
 */
object KeepOut {
    private const val SAFE_SIDE = 48f
    const val SCREEN_W = 960f
    private val top = LayoutBudget.SAFE_TOP_BOTTOM_DP.toFloat()

    val CLOCK_GROUP = DpRect(SAFE_SIDE, top, LayoutBudget.CLOCK_GROUP_RIGHT_DP, top + LayoutBudget.TOP_BAR_DP)
    val BANNER = DpRect(SAFE_SIDE, top + LayoutBudget.TOP_BAR_DP, LayoutBudget.BANNER_RIGHT_DP, top + LayoutBudget.header(true))
    val WEATHER_GROUP = DpRect(LayoutBudget.WEATHER_LEFT_DP, top, SCREEN_W - SAFE_SIDE, top + LayoutBudget.TOP_BAR_DP)

    /** The quote is at most 64% of the width, centred; it sits lower with a banner and higher without, so the band is the union. */
    val QUOTE_BAND: DpRect get() {
        val half = SCREEN_W * 0.64f / 2f
        val noBanner = top + LayoutBudget.header(false) + (LayoutBudget.centerDp(false) - LayoutBudget.QUOTE_MIN_DP) / 2f
        return DpRect(SCREEN_W / 2f - half, noBanner, SCREEN_W / 2f + half, LayoutBudget.QUOTE_BOTTOM_DP)
    }

    val DOCK_BAND = DpRect(SAFE_SIDE, 540f - top - LayoutBudget.DOCK_DP, SCREEN_W - SAFE_SIDE, 540f - top)

    val ALL: Map<String, DpRect> get() = mapOf(
        "clock group" to CLOCK_GROUP, "banner" to BANNER, "weather group" to WEATHER_GROUP, "quote band" to QUOTE_BAND, "dock band" to DOCK_BAND,
    )
}

/** Where a scene's sun, moon or main motif sits, in the scenes' 480x270 logical units (2 logical units = 1dp at 960x540). */
data class Focal(val x: Float, val y: Float, val r: Float) {
    /** Visual radius in dp: the day sun is 1.1x the disc, plus 16dp for the camera's slow pan and zoom. */
    val clearanceDp: Float get() = r * 1.1f * 2f + CAMERA_MARGIN_DP
    val xDp: Float get() = x * 2f
    val yDp: Float get() = y * 2f

    private companion object { const val CAMERA_MARGIN_DP = 16f }
}

/**
 * Every scene's focal position, all in the clear right-hand column (right of the quote band, below the weather group) so
 * that nothing celestial sits behind text. Ridgelines start at about y=97, so the disc stays above them.
 */
object Focals {
    val FOREST = Focal(424f, 82f, 13f)
    val JUNGLE = Focal(430f, 83f, 12f)
    val BEACH = Focal(426f, 82f, 13f)
    val MOUNTAINS = Focal(428f, 80f, 11f)
    val SNOW_PEAKS = Focal(422f, 80f, 11f)
    val AUTUMN = Focal(430f, 84f, 12f)
    val HOLI = Focal(426f, 84f, 11f)
    val JANMASHTAMI = Focal(424f, 82f, 13f)
    val GANESH = Focal(428f, 84f, 12f)
    val SHIVARATRI = Focal(426f, 82f, 12f)
    val KITES = Focal(430f, 84f, 11f)
    val LOHRI = Focal(424f, 84f, 11f)
    val VASANT = Focal(428f, 83f, 12f)
    val KARWA = Focal(426f, 82f, 13f)
    val CHHATH = Focal(424f, 82f, 13f)
    val HALLOWEEN = Focal(428f, 82f, 13f)
    val THANKSGIVING = Focal(426f, 84f, 12f)
    val CHRISTMAS = Focal(430f, 84f, 12f)

    val ALL: Map<String, Focal> = mapOf(
        "forest" to FOREST, "jungle" to JUNGLE, "beach" to BEACH, "mountains" to MOUNTAINS, "snow_peaks" to SNOW_PEAKS, "autumn_valley" to AUTUMN,
        "holi" to HOLI, "janmashtami" to JANMASHTAMI, "ganesh" to GANESH, "shivaratri" to SHIVARATRI, "sankranti_kites" to KITES,
        "lohri" to LOHRI, "vasant" to VASANT, "karwa_chauth" to KARWA, "chhath" to CHHATH, "halloween" to HALLOWEEN,
        "thanksgiving" to THANKSGIVING, "christmas" to CHRISTMAS,
    )
}
