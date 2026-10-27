package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegibilityTest {
    /** A histogram of [n] pixels spread evenly from grey [lo] to grey [hi]. */
    private fun hist(lo: Int, hi: Int, n: Int = 1000): IntArray = IntArray(256).also { h -> for (i in 0 until n) h[lo + (hi - lo) * i / (n - 1)]++ }

    private fun ratioAfterVeil(v: Int, day: Boolean, a: Float): Double {
        val bg = Contrast.blend((0xFF shl 24) or (v shl 16) or (v shl 8) or v, if (day) 0xFFFFFF else 0x000000, a)
        return Contrast.ratio(if (day) ScrimSpec.DAY_TEXT else ScrimSpec.NIGHT_TEXT, bg)
    }

    @Test
    fun `the chosen veil gives the text 4_5 to 1 over the region's worst 8 percent pixel whenever the cap allows it`() {
        for ((lo, hi) in listOf(0 to 60, 40 to 200, 90 to 160, 150 to 250, 200 to 255)) for (day in listOf(true, false)) {
            val h = hist(lo, hi)
            val a = Legibility.veil(h, day)
            val worst = Legibility.percentile(h, if (day) Legibility.PERCENTILE else 1f - Legibility.PERCENTILE)!!
            if (a < Legibility.MAX_VEIL) assertTrue("lo=$lo hi=$hi day=$day a=$a", ratioAfterVeil(worst, day, a) >= Legibility.TARGET)
            assertTrue(a in Legibility.MIN_VEIL..Legibility.MAX_VEIL)
        }
    }

    @Test
    fun `a bright daytime painting gets dark text and a light veil, not white text under a heavy grey one`() {
        // Bright sky over the header and a pale lake under the quote: the case the user reported as hard to read.
        val n = Legibility.needs(hist(170, 250), hist(120, 230))
        assertTrue(Legibility.prefersDay(n, fallbackDay = false)) // even if the brightness rule would have said dark UI
        assertTrue("light veil ${n.worst(true)}", n.worst(true) <= 0.35f)
        assertTrue("white text would need ${n.worst(false)}", n.worst(false) >= 0.5f)
    }

    @Test
    fun `a dark night photo keeps white text with only the faint minimum veil`() {
        val n = Legibility.needs(hist(5, 60), hist(10, 80))
        assertFalse(Legibility.prefersDay(n, fallbackDay = true))
        assertEquals(Legibility.MIN_VEIL, n.worst(false), 0.001f)
    }

    @Test
    fun `brighter header pixels mean a stronger veil for white text and a weaker one for dark text`() {
        assertTrue(Legibility.veil(hist(150, 255), false) > Legibility.veil(hist(60, 180), false))
        assertTrue(Legibility.veil(hist(150, 255), true) < Legibility.veil(hist(60, 180), true))
    }

    @Test
    fun `when both modes need about the same veil the brightness rule decides`() {
        val n = Legibility.Needs(0.3f, 0.31f, 0.3f, 0.31f)
        assertTrue(Legibility.prefersDay(n, fallbackDay = true))
        assertFalse(Legibility.prefersDay(n, fallbackDay = false))
    }

    @Test
    fun `a measured image overrides the brightness rule in Auto, but an explicit mode still wins`() {
        val bright = Legibility.needs(hist(170, 250), hist(150, 240))
        assertTrue(UiTheme.isDay(DayNightMode.AUTO, sunSaysDay = false, imageLuma = 100, needs = bright))
        assertFalse(UiTheme.isDay(DayNightMode.NIGHT, sunSaysDay = true, imageLuma = 200, needs = bright))
    }

    @Test
    fun `secondary text is solid, so the wallpaper never shows through the glyphs`() {
        for (day in listOf(true, false)) {
            Palette.day = day
            assertEquals(0xFF, Palette.text70 ushr 24)
            assertEquals(0xFF, Palette.text60 ushr 24)
            assertTrue(Contrast.ratio(Palette.text70, if (day) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()) >= 12.0)
        }
        Palette.day = false
    }
}
