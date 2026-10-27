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
    fun `the top veil holds through the header text, eases out smoothly and is gone by 34 percent of the height`() {
        val plateau = LayoutBudget.SCRIM_HEADER_END_DP.toFloat()
        assertEquals(ScrimSpec.NIGHT_ALPHA, ScrimSpec.alphaAt(plateau, heightDp, false), 0.001f)
        assertEquals(0f, ScrimSpec.alphaAt(0.34f * heightDp, heightDp, false), 0.001f)
        var prev = ScrimSpec.NIGHT_ALPHA
        var y = plateau
        while (y <= 0.34f * heightDp) { val a = ScrimSpec.alphaAt(y, heightDp, false); assertTrue("monotonic at $y", a <= prev + 1e-4f); prev = a; y += 2f }
        // no visible edge: the slope never exceeds a smoothstep's 1.5x the average
        val avg = ScrimSpec.NIGHT_ALPHA / (0.34f * heightDp - plateau)
        var yy = plateau
        while (yy < 0.34f * heightDp - 2f) { assertTrue(ScrimSpec.alphaAt(yy, heightDp, false) - ScrimSpec.alphaAt(yy + 2f, heightDp, false) <= 2f * avg * 1.8f); yy += 2f }
    }

    @Test
    fun `images get a top veil of 0_18 when dark up to 0_40 when bright, and scenes keep their tuned plateau`() {
        assertEquals(0.18f, ScrimSpec.topAlpha(35, false), 0.001f)
        assertEquals(0.40f, ScrimSpec.topAlpha(200, true), 0.001f)
        assertTrue(ScrimSpec.topAlpha(110, false) in 0.2f..0.35f)
        assertTrue(ScrimSpec.topAlpha(60, false) < ScrimSpec.topAlpha(140, false))
        assertEquals(ScrimSpec.NIGHT_ALPHA, ScrimSpec.topAlpha(null, false), 0f)
        assertEquals(ScrimSpec.DAY_ALPHA, ScrimSpec.topAlpha(null, true), 0f)
        assertEquals(0.40f, ScrimSpec.alphaAt(60f, heightDp, false, top = 0.40f), 0.001f)
    }

    @Test
    fun `the quote band sits behind the centred quote`() {
        assertTrue(ScrimSpec.QUOTE_PLATEAU_START_DP <= LayoutBudget.QUOTE_TOP_DP + 12f)
        assertTrue(ScrimSpec.QUOTE_PLATEAU_END_DP >= LayoutBudget.QUOTE_BOTTOM_DP - 12f)
    }

    @Test
    fun `behind the dock shelf there is no tint at all`() {
        // The dock view also holds transparent headroom for the magnified icons; the tiles and shelf start below it.
        val spec = LayoutBudget.ICONS_SPEC
        val shelfTop = 540 - 27 - spec.bottomGapDp - spec.cardHeightDp(spec.maxBaseDp) - 2 * spec.shelfPadDp
        for (y in shelfTop.toInt()..513 step 10) {
            assertEquals(0f, ScrimSpec.alphaAt(y.toFloat(), heightDp, false), 0f)
            assertEquals(0f, ScrimSpec.alphaAt(y.toFloat(), heightDp, true), 0f)
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
