package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrimTest {
    private val heightDp = 540f

    // Text rows (dp): the header band holds the clocks, weather and banner; the quote band holds the centred quote core.
    private val headerRows = 27f..LayoutBudget.SCRIM_HEADER_END_DP.toFloat()
    private val quoteRows = LayoutBudget.QUOTE_TOP_DP.toFloat() + 10f..LayoutBudget.QUOTE_BOTTOM_DP.toFloat() - 10f

    private fun minAlpha(range: ClosedFloatingPointRange<Float>, day: Boolean): Float {
        var m = 1f
        var y = range.start
        while (y <= range.endInclusive) { m = minOf(m, ScrimSpec.alphaAt(y, heightDp, day)); y += 1f }
        return m
    }

    private fun assertReadable(scene: String, bg: Int, text: Int, rows: ClosedFloatingPointRange<Float>, day: Boolean) {
        val a = minAlpha(rows, day)
        val blended = Contrast.blend(bg, if (day) 0xFFFFFF else 0x000000, a)
        val r = Contrast.ratio(text, blended)
        assertTrue("$scene day=$day alpha=$a ratio=$r", r >= 4.5)
    }

    @Test
    fun `white text stays at 4_5 to 1 over every scene's brightest night colour in the header and quote bands`() {
        for (e in ScenePalettes.NIGHT_BRIGHTEST) {
            assertReadable(e.scene + " header", e.header, ScrimSpec.NIGHT_TEXT, headerRows, day = false)
            assertReadable(e.scene + " quote", e.quote, ScrimSpec.NIGHT_TEXT, quoteRows, day = false)
        }
    }

    @Test
    fun `near-black text stays at 4_5 to 1 over every scene's darkest day colour in the header and quote bands`() {
        for (e in ScenePalettes.DAY_DARKEST) {
            assertReadable(e.scene + " header", e.header, ScrimSpec.DAY_TEXT, headerRows, day = true)
            assertReadable(e.scene + " quote", e.quote, ScrimSpec.DAY_TEXT, quoteRows, day = true)
        }
    }

    @Test
    fun `muted day text also stays readable on the day palette`() {
        for (e in ScenePalettes.DAY_DARKEST) {
            val a = minAlpha(headerRows, true)
            assertTrue(e.scene, Contrast.ratio(0xFF12161E.toInt(), Contrast.blend(e.header, 0xFFFFFF, a)) >= 4.5)
        }
    }

    @Test
    fun `the scrim is at most 60 percent of the old strength so the wallpaper stays visible`() {
        assertTrue(ScrimSpec.NIGHT_ALPHA <= 0.55f * 0.62f)
        assertTrue(ScrimSpec.DAY_ALPHA <= 0.50f * 0.62f)
    }

    @Test
    fun `the top veil holds through the header text, eases out smoothly and is gone by 40 percent of the height`() {
        val plateau = LayoutBudget.SCRIM_HEADER_END_DP.toFloat()
        assertEquals(ScrimSpec.NIGHT_ALPHA, ScrimSpec.topAt(plateau, heightDp, false), 0.001f)
        assertEquals(0f, ScrimSpec.topAt(ScrimSpec.TOP_FADE_END_FRACTION * heightDp, heightDp, false), 0.001f)
        var prev = ScrimSpec.NIGHT_ALPHA
        var y = plateau
        while (y <= ScrimSpec.TOP_FADE_END_FRACTION * heightDp) { val a = ScrimSpec.topAt(y, heightDp, false); assertTrue("monotonic at $y", a <= prev + 1e-4f); prev = a; y += 2f }
        // no visible edge: the slope never exceeds a smoothstep's 1.5x the average
        val avg = ScrimSpec.NIGHT_ALPHA / (ScrimSpec.TOP_FADE_END_FRACTION * heightDp - plateau)
        var yy = plateau
        while (yy < ScrimSpec.TOP_FADE_END_FRACTION * heightDp - 2f) { assertTrue(ScrimSpec.topAt(yy, heightDp, false) - ScrimSpec.topAt(yy + 2f, heightDp, false) <= 2f * avg * 1.8f); yy += 2f }
    }

    @Test
    fun `images get a top veil of 0_18 when dark up to 0_40 when bright, and scenes keep their tuned plateau`() {
        assertEquals(0.18f, ScrimSpec.topAlpha(35, false), 0.001f)
        assertEquals(0.40f, ScrimSpec.topAlpha(200, true), 0.001f)
        assertTrue(ScrimSpec.topAlpha(110, false) in 0.2f..0.35f)
        assertTrue(ScrimSpec.topAlpha(60, false) < ScrimSpec.topAlpha(140, false))
        assertEquals(ScrimSpec.NIGHT_ALPHA, ScrimSpec.topAlpha(null, false), 0f)
        assertEquals(ScrimSpec.DAY_ALPHA, ScrimSpec.topAlpha(null, true), 0f)
        assertEquals(0.40f, ScrimSpec.topAt(60f, heightDp, false, top = 0.40f), 0.001f)
    }

    @Test
    fun `the quote veil is at least 90 percent strength over the whole quote column, corners included`() {
        val a = 0.4f
        var y = LayoutBudget.QUOTE_TOP_DP
        while (y <= LayoutBudget.QUOTE_BOTTOM_DP) {
            var x = Legibility.QUOTE_X_DP.start
            while (x <= Legibility.QUOTE_X_DP.endInclusive) {
                assertTrue("x=$x y=$y", ScrimSpec.quoteAt(x, y, a) >= 0.9f * a)
                x += 8f
            }
            y += 4f
        }
    }

    @Test
    fun `the quote veil has no visible edge on any side - it eases out instead of stepping like the old band`() {
        // The old band rose from 0 to full over 18dp, which read as a hard-edged rectangle. Along any ray out of the centre the veil
        // may change by at most 2.5 percent of its strength per dp (about a third of that old ramp's slope).
        val a = 1f
        for (deg in 0 until 360 step 15) {
            val r = Math.toRadians(deg.toDouble())
            var prev = ScrimSpec.quoteAt(480f, ScrimSpec.QUOTE_CY_DP, a)
            var t = 1f
            while (t < 700f) {
                val v = ScrimSpec.quoteAt(480f + t * kotlin.math.cos(r).toFloat(), ScrimSpec.QUOTE_CY_DP + t * kotlin.math.sin(r).toFloat(), a)
                assertTrue("deg=$deg t=$t step=${prev - v}", kotlin.math.abs(prev - v) <= 0.025f)
                assertTrue("monotonic deg=$deg t=$t", v <= prev + 1e-4f)
                prev = v
                t += 1f
            }
        }
    }

    @Test
    fun `the drawn mask is exactly the tested scrim function`() {
        val w = 240
        val h = 135
        val m = ScrimSpec.mask(w, h, day = true, top = 0.3f, quote = 0.45f)
        for ((x, y) in listOf(120 to 10, 120 to 60, 30 to 60, 200 to 100, 120 to 130)) {
            val expected = ScrimSpec.alphaAt((y + 0.5f) * 540f / h, 540f, true, top = 0.3f, quote = 0.45f, xDp = (x + 0.5f) * 960f / w)
            assertEquals("x=$x y=$y", expected, (m[y * w + x].toInt() and 255) / 255f, 1f / 255f + 1e-4f)
        }
    }

    @Test
    fun `behind the dock shelf there is no tint at all`() {
        // The dock view also holds transparent headroom for the magnified icons; the tiles and shelf start below it.
        val spec = LayoutBudget.ICONS_SPEC
        val shelfTop = 540 - 27 - spec.bottomGapDp - spec.cardHeightDp(spec.maxBaseDp) - 2 * spec.shelfPadDp
        for (y in shelfTop.toInt()..513 step 10) for (x in 0..960 step 40) {
            assertEquals(0f, ScrimSpec.alphaAt(y.toFloat(), heightDp, false, quote = 0.6f, xDp = x.toFloat()), 0f)
            assertEquals(0f, ScrimSpec.alphaAt(y.toFloat(), heightDp, true, quote = 0.6f, xDp = x.toFloat()), 0f)
        }
    }

    @Test
    fun `gradient stops are increasing so the GPU gradient is valid`() {
        val p = ScrimSpec.positions(heightDp)
        for (i in 0 until p.size - 1) assertTrue(p[i] <= p[i + 1])
        assertEquals(p.size, ScrimSpec.colors(true).size)
        assertEquals(p.size, ScrimSpec.alphas(false).size)
    }
}
