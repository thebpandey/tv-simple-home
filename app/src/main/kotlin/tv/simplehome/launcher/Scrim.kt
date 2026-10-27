package tv.simplehome.launcher

import kotlin.math.pow

/**
 * Targeted scrims over the wallpaper (pure, so contrast can be unit tested): a top band behind the clocks, weather and banner, and a
 * soft rounded veil behind the centred quote. Nothing is tinted behind the dock. Night uses a dark veil with white text; day a light frost
 * with near-black text. The quote veil has a long smoothstep falloff on every side: the old full-width band with 18dp ramps showed as
 * a visible rectangle with hard top and bottom edges on bright paintings.
 */
object ScrimSpec {
    // About 60% of the previous strength (0.55 night / 0.50 day): the wallpaper is the point; only text areas get a veil.
    const val NIGHT_ALPHA = 0.33f
    const val DAY_ALPHA = 0.30f

    // Top band: full strength through the header text (27..175dp, the banner's last line included), then a smooth ease-out to nothing at 40% of the height (216dp).
    val TOP_PLATEAU_DP = LayoutBudget.SCRIM_HEADER_END_DP.toFloat()
    const val TOP_FADE_END_FRACTION = 0.40f
    val TOP_FADE_END_DP = 540f * TOP_FADE_END_FRACTION

    // Quote veil on the 960dp-wide layout: a rounded rectangle (superellipse, n = 4) centred on the quote box, at least 90% strength over
    // the widest quote column (64% of the width) and the tallest block, then a smoothstep to nothing at the rim, which stays above the shelf.
    val QUOTE_CY_DP = (LayoutBudget.QUOTE_TOP_DP + LayoutBudget.QUOTE_BOTTOM_DP) / 2f
    const val QUOTE_PLATEAU = 0.6f
    const val QUOTE_RX_DP = 560f
    const val QUOTE_RY_DP = 158f
    val QUOTE_STOPS = floatArrayOf(0f, QUOTE_PLATEAU, 0.7f, 0.8f, 0.9f, 1f)
    val QUOTE_EASE = floatArrayOf(1f, 1f, 0.844f, 0.5f, 0.156f, 0f)

    // Images (photos, paintings) without a measurement yet: the top veil scales with brightness, 0.18 for dark ones up to 0.40 for bright ones.
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
        floatArrayOf(0f, TOP_PLATEAU_DP, TOP_PLATEAU_DP + fade * 0.25f, TOP_PLATEAU_DP + fade * 0.5f, TOP_PLATEAU_DP + fade * 0.75f, TOP_FADE_END_DP)
    }

    /** Fractions (of screen height) for the top band's gradient stops; the last stop is the bottom edge. */
    fun positions(heightDp: Float): FloatArray = FloatArray(STOPS_DP.size + 1) { if (it < STOPS_DP.size) STOPS_DP[it] / heightDp else 1f }

    /** Top band alphas; [top] overrides its strength (images), else it is the scene plateau x [boost]. */
    fun alphas(day: Boolean, boost: Float = 1f, top: Float? = null): FloatArray {
        val t = top ?: (plateau(day) * boost)
        return floatArrayOf(t, t, t * EASE[1], t * EASE[2], t * EASE[3], 0f, 0f)
    }

    /** The quote veil's full strength: [quote] when measured (images), else the scene plateau x [boost]. */
    fun quoteAlpha(day: Boolean, boost: Float = 1f, quote: Float? = null): Float = quote ?: (plateau(day) * boost)

    /** Top band opacity at [yDp], by linear interpolation between the stops (what the GPU gradient does). */
    fun topAt(yDp: Float, heightDp: Float, day: Boolean, boost: Float = 1f, top: Float? = null): Float {
        val pos = positions(heightDp)
        val a = alphas(day, boost, top)
        return interpolate(pos, a, (yDp / heightDp).coerceIn(0f, 1f))
    }

    /** Quote veil opacity at ([xDp], [yDp]) for full strength [a]. */
    fun quoteAt(xDp: Float, yDp: Float, a: Float): Float {
        val dx = (xDp - 480f) / QUOTE_RX_DP
        val dy = (yDp - QUOTE_CY_DP) / QUOTE_RY_DP
        val d = kotlin.math.sqrt(kotlin.math.sqrt(dx * dx * dx * dx + dy * dy * dy * dy))
        if (d >= 1f) return 0f
        return a * interpolate(QUOTE_STOPS, QUOTE_EASE, d)
    }

    /** Combined opacity of both veils at ([xDp], [yDp]) (they composite: 1 - (1 - top)(1 - quote)). */
    fun alphaAt(yDp: Float, heightDp: Float, day: Boolean, boost: Float = 1f, top: Float? = null, quote: Float? = null, xDp: Float = 480f): Float {
        val t = topAt(yDp, heightDp, day, boost, top)
        val q = quoteAt(xDp, yDp, quoteAlpha(day, boost, quote))
        return 1f - (1f - t) * (1f - q)
    }

    /**
     * The whole scrim as an alpha mask, [w] x [h] (a quarter of the screen size is plenty: it is smooth and drawn with filtering), so
     * what is drawn is exactly [alphaAt]. Rebuilt only when the wallpaper or the UI mode changes.
     */
    fun mask(w: Int, h: Int, day: Boolean, boost: Float = 1f, top: Float? = null, quote: Float? = null): ByteArray {
        val out = ByteArray(w * h)
        val q = quoteAlpha(day, boost, quote)
        for (y in 0 until h) {
            val yDp = (y + 0.5f) * 540f / h
            val t = topAt(yDp, 540f, day, boost, top)
            for (x in 0 until w) {
                val a = 1f - (1f - t) * (1f - quoteAt((x + 0.5f) * 960f / w, yDp, q))
                out[y * w + x] = (a * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
            }
        }
        return out
    }

    private fun interpolate(pos: FloatArray, a: FloatArray, f: Float): Float {
        for (i in 0 until pos.size - 1) {
            if (f <= pos[i + 1]) {
                val span = pos[i + 1] - pos[i]
                return if (span <= 0f) a[i + 1] else a[i] + (a[i + 1] - a[i]) * (f - pos[i]) / span
            }
        }
        return a.last()
    }

    /** Top band ARGB colors for [alphas]: black at night, white by day. */
    fun colors(day: Boolean, boost: Float = 1f, top: Float? = null): IntArray = argb(day, alphas(day, boost, top))

    private fun argb(day: Boolean, a: FloatArray): IntArray {
        val rgb = if (day) 0xFFFFFF else 0x000000
        return IntArray(a.size) { ((a[it] * 255f + 0.5f).toInt().coerceIn(0, 255) shl 24) or rgb }
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

/**
 * Per-image legibility, measured from the image's own pixels (pure, so it is unit tested): for the header area and the quote area,
 * the smallest veil that gives the UI's text [TARGET] contrast over the region's [PERCENTILE] worst pixel, for both UI modes. The UI
 * then takes the mode that needs the lighter veil, so a bright painting gets dark text and a faint frost instead of white text under a
 * heavy grey veil (the old rule judged only the quote band's average brightness, which let bright skies sit behind white header text).
 */
object Legibility {
    const val TARGET = 4.5 // WCAG AA for body text; the text shadow adds margin on top
    const val PERCENTILE = 0.08f // the darkest 8% (dark text) or brightest 8% (white text) may fall below the target; shadows cover them
    const val MIN_VEIL = 0.10f // a faint veil always, so the look is consistent from image to image
    const val MAX_VEIL = 0.60f // beyond this the wallpaper would turn grey; the shadow does the rest

    /** Veil strengths (0..1) for the header and quote areas, in light UI (white veil, dark text) and dark UI (black veil, white text). */
    data class Needs(val headerDay: Float, val headerNight: Float, val quoteDay: Float, val quoteNight: Float) {
        fun header(day: Boolean) = if (day) headerDay else headerNight
        fun quote(day: Boolean) = if (day) quoteDay else quoteNight
        fun worst(day: Boolean) = maxOf(header(day), quote(day))
    }

    fun needs(header: IntArray, quote: IntArray) =
        Needs(veil(header, true), veil(header, false), veil(quote, true), veil(quote, false))

    /** Light UI when it needs the lighter veil; within 0.03 of each other the brightness rule ([fallbackDay]) decides. */
    fun prefersDay(n: Needs, fallbackDay: Boolean): Boolean {
        val d = n.worst(true)
        val night = n.worst(false)
        return if (kotlin.math.abs(d - night) < 0.03f) fallbackDay else d < night
    }

    /** Smallest veil (0.01 steps, clamped to [MIN_VEIL]..[MAX_VEIL]) giving [TARGET] contrast over the percentile pixel of [hist] (256 grey bins). */
    fun veil(hist: IntArray, day: Boolean): Float {
        val v = percentile(hist, if (day) PERCENTILE else 1f - PERCENTILE) ?: return MIN_VEIL
        val text = if (day) ScrimSpec.DAY_TEXT else ScrimSpec.NIGHT_TEXT
        var a = 0f
        while (a < MAX_VEIL) {
            val bg = Contrast.blend(grey(v), if (day) 0xFFFFFF else 0x000000, a)
            if (Contrast.ratio(text, bg) >= TARGET) break
            a += 0.01f
        }
        return a.coerceIn(MIN_VEIL, MAX_VEIL)
    }

    /** Grey value at fraction [q] of the histogram's pixels, or null when it is empty. */
    fun percentile(hist: IntArray, q: Float): Int? {
        val total = hist.sum()
        if (total == 0) return null
        val rank = (q * (total - 1)).toInt()
        var seen = 0
        for (i in hist.indices) { seen += hist[i]; if (seen > rank) return i }
        return 255
    }

    /** Rec. 709 luma of an ARGB pixel, 0..255. */
    fun luma(argb: Int): Int = ((0.2126f * ((argb shr 16) and 255) + 0.7152f * ((argb shr 8) and 255) + 0.0722f * (argb and 255)) + 0.5f).toInt().coerceIn(0, 255)

    private fun grey(v: Int) = (0xFF shl 24) or (v shl 16) or (v shl 8) or v

    // Regions in dp on the 960 x 540 layout: the clock group + banner on the left and the weather group on the right; the quote column.
    val HEADER_Y_DP = LayoutBudget.SAFE_TOP_BOTTOM_DP.toFloat()..(LayoutBudget.SAFE_TOP_BOTTOM_DP + LayoutBudget.header(true)).toFloat()
    val HEADER_LEFT_X_DP = 48f..LayoutBudget.CLOCK_GROUP_RIGHT_DP
    val HEADER_RIGHT_X_DP = LayoutBudget.WEATHER_LEFT_DP..912f
    val QUOTE_Y_DP = LayoutBudget.QUOTE_TOP_DP..LayoutBudget.QUOTE_BOTTOM_DP
    val QUOTE_X_DP = (480f - 480f * QuoteFit.MAX_WIDTH_FRACTION)..(480f + 480f * QuoteFit.MAX_WIDTH_FRACTION)
}
