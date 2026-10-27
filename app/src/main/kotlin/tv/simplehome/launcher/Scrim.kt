package tv.simplehome.launcher

import kotlin.math.pow

/**
 * Targeted scrims over the wallpaper (pure, so contrast can be unit tested): a top band behind the clocks, weather
 * and banner, a bottom band behind the quote, and only a very light tint in the middle where the tiles are.
 * Night uses a dark scrim with white text; day uses a light frosted scrim with near-black text.
 */
object ScrimSpec {
    // About 60% of the previous strength (0.55 night / 0.50 day): the wallpaper is the point; only text areas get a veil.
    const val NIGHT_ALPHA = 0.33f
    const val DAY_ALPHA = 0.30f

    // Header band: the clocks, weather and banner (27..151dp) plus 24dp of fade. Quote band: behind the centred quote.
    // The middle (behind the dock) gets no tint at all.
    // Derived from the layout so the veils always sit where the text is (header band; quote band; nothing behind the dock).
    val TOP_PLATEAU_DP = LayoutBudget.SCRIM_HEADER_END_DP.toFloat() // the header texts sit on glass cards, so the veil only backs up the card row
    val TOP_FADE_END_DP = TOP_PLATEAU_DP + 24f
    // The quote band may not start before the header band has faded out.
    val QUOTE_RAMP_IN_START_DP = maxOf(LayoutBudget.QUOTE_TOP_DP - 28f, TOP_FADE_END_DP + 2f)
    val QUOTE_PLATEAU_START_DP = maxOf(LayoutBudget.QUOTE_TOP_DP - 10f, QUOTE_RAMP_IN_START_DP + 6f)
    val QUOTE_PLATEAU_END_DP = LayoutBudget.QUOTE_BOTTOM_DP + 10f
    val QUOTE_RAMP_OUT_END_DP = LayoutBudget.QUOTE_BOTTOM_DP + 30f

    const val PHOTO_BOOST = 1.4f

    /** Boost for a photo of average brightness [luma] (0..255): 1.0 for very dark photos up to 1.8 for very bright ones. */
    fun photoBoost(luma: Int): Float = 1f + 0.8f * (luma.coerceIn(0, 255) / 255f)

    const val NIGHT_TEXT = 0xFFFFFFFF.toInt()
    const val DAY_TEXT = 0xFF101216.toInt()

    private fun plateau(day: Boolean) = if (day) DAY_ALPHA else NIGHT_ALPHA
    private val STOPS_DP by lazy { floatArrayOf(0f, TOP_PLATEAU_DP, TOP_FADE_END_DP, QUOTE_RAMP_IN_START_DP, QUOTE_PLATEAU_START_DP, QUOTE_PLATEAU_END_DP, QUOTE_RAMP_OUT_END_DP) }

    /** Fractions (of screen height) for the gradient stops; the last stop is the bottom edge. */
    fun positions(heightDp: Float): FloatArray = FloatArray(STOPS_DP.size + 1) { if (it < STOPS_DP.size) STOPS_DP[it] / heightDp else 1f }

    /** [boost] > 1 strengthens the veil, used over arbitrary photos whose brightness is unknown. */
    fun alphas(day: Boolean, boost: Float = 1f): FloatArray {
        val p = plateau(day) * boost
        return floatArrayOf(p, p, 0f, 0f, p, p, 0f, 0f)
    }

    /** Scrim opacity at [yDp], by linear interpolation between the stops (what the GPU gradient does). */
    fun alphaAt(yDp: Float, heightDp: Float, day: Boolean, boost: Float = 1f): Float {
        val pos = positions(heightDp)
        val a = alphas(day, boost)
        val f = (yDp / heightDp).coerceIn(0f, 1f)
        for (i in 0 until pos.size - 1) {
            if (f <= pos[i + 1]) {
                val span = pos[i + 1] - pos[i]
                return if (span <= 0f) a[i + 1] else a[i] + (a[i + 1] - a[i]) * (f - pos[i]) / span
            }
        }
        return a.last()
    }

    /** Scrim ARGB colors for [alphas]: black at night, white by day. */
    fun colors(day: Boolean, boost: Float = 1f): IntArray {
        val rgb = if (day) 0xFFFFFF else 0x000000
        val a = alphas(day, boost)
        return IntArray(a.size) { ((a[it] * 255f + 0.5f).toInt() shl 24) or rgb }
    }
}

/** WCAG contrast helpers. */
object Contrast {
    private fun lin(c: Int): Double {
        val v = c / 255.0
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    fun luminance(argb: Int): Double =
        0.2126 * lin((argb shr 16) and 255) + 0.7152 * lin((argb shr 8) and 255) + 0.0722 * lin(argb and 255)

    fun ratio(fg: Int, bg: Int): Double {
        val a = luminance(fg)
        val b = luminance(bg)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    /** [overlayRgb] at [alpha] composited over [bg] (opaque result). */
    fun blend(bg: Int, overlayRgb: Int, alpha: Float): Int {
        fun ch(shift: Int) = (((bg shr shift) and 255) * (1 - alpha) + ((overlayRgb shr shift) and 255) * alpha + 0.5f).toInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
