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

    // Top band: full strength through the header text (27..150dp), then a smooth ease-out to nothing at 34% of the screen height (184dp).
    // Quote band: behind the centred quote. The middle (behind the dock) gets no tint at all.
    val TOP_PLATEAU_DP = LayoutBudget.SCRIM_HEADER_END_DP.toFloat()
    const val TOP_FADE_END_FRACTION = 0.34f
    val TOP_FADE_END_DP = 540f * TOP_FADE_END_FRACTION
    // The quote band may not start before the top band has faded out.
    val QUOTE_RAMP_IN_START_DP = maxOf(LayoutBudget.QUOTE_TOP_DP - 28f, TOP_FADE_END_DP + 2f)
    val QUOTE_PLATEAU_START_DP = maxOf(LayoutBudget.QUOTE_TOP_DP - 10f, QUOTE_RAMP_IN_START_DP + 6f)
    val QUOTE_PLATEAU_END_DP = LayoutBudget.QUOTE_BOTTOM_DP + 10f
    val QUOTE_RAMP_OUT_END_DP = LayoutBudget.QUOTE_BOTTOM_DP + 30f

    // Images (photos, paintings): the top veil scales with how bright the image is, 0.18 for dark ones up to 0.40 for bright ones.
    const val IMAGE_TOP_MIN = 0.18f
    const val IMAGE_TOP_MAX = 0.40f

    /** Top veil strength: the scene constant when no image [luma] is known (scenes keep their tuned plateau), else 0.18..0.40 by brightness. */
    fun topAlpha(luma: Int?, day: Boolean): Float =
        if (luma == null) plateau(day) else IMAGE_TOP_MIN + (IMAGE_TOP_MAX - IMAGE_TOP_MIN) * ((luma - 60) / 100f).coerceIn(0f, 1f)

    const val PHOTO_BOOST = 1.4f

    /** Boost for a photo of average brightness [luma] (0..255): 1.0 for very dark photos up to 1.8 for very bright ones. */
    fun photoBoost(luma: Int): Float = 1f + 0.8f * (luma.coerceIn(0, 255) / 255f)

    const val NIGHT_TEXT = 0xFFFFFFFF.toInt()
    const val DAY_TEXT = 0xFF101216.toInt()

    private fun plateau(day: Boolean) = if (day) DAY_ALPHA else NIGHT_ALPHA

    // The ease-out is four extra stops (1, 0.84, 0.5, 0.16, 0 of the strength, a smoothstep) so it has no visible edge.
    private val EASE = floatArrayOf(1f, 0.844f, 0.5f, 0.156f, 0f)
    private val STOPS_DP by lazy {
        val fade = TOP_FADE_END_DP - TOP_PLATEAU_DP
        floatArrayOf(0f, TOP_PLATEAU_DP, TOP_PLATEAU_DP + fade * 0.25f, TOP_PLATEAU_DP + fade * 0.5f, TOP_PLATEAU_DP + fade * 0.75f, TOP_FADE_END_DP,
            QUOTE_RAMP_IN_START_DP, QUOTE_PLATEAU_START_DP, QUOTE_PLATEAU_END_DP, QUOTE_RAMP_OUT_END_DP)
    }

    /** Fractions (of screen height) for the gradient stops; the last stop is the bottom edge. */
    fun positions(heightDp: Float): FloatArray = FloatArray(STOPS_DP.size + 1) { if (it < STOPS_DP.size) STOPS_DP[it] / heightDp else 1f }

    /**
     * [boost] > 1 strengthens the quote veil (photos); [top] overrides the top veil's strength (images), else it is the scene plateau x [boost].
     */
    fun alphas(day: Boolean, boost: Float = 1f, top: Float? = null): FloatArray {
        val q = plateau(day) * boost
        val t = top ?: q
        return floatArrayOf(t, t, t * EASE[1], t * EASE[2], t * EASE[3], 0f, 0f, q, q, 0f, 0f)
    }

    /** Scrim opacity at [yDp], by linear interpolation between the stops (what the GPU gradient does). */
    fun alphaAt(yDp: Float, heightDp: Float, day: Boolean, boost: Float = 1f, top: Float? = null): Float {
        val pos = positions(heightDp)
        val a = alphas(day, boost, top)
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
    fun colors(day: Boolean, boost: Float = 1f, top: Float? = null): IntArray {
        val rgb = if (day) 0xFFFFFF else 0x000000
        val a = alphas(day, boost, top)
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
