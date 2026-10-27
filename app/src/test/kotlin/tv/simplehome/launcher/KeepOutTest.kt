package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepOutTest {
    @Test
    fun `no scene's sun, moon or focal motif overlaps the clock group, banner, weather group, quote band or dock band at 960x540dp`() {
        for ((scene, f) in Focals.ALL) for ((name, rect) in KeepOut.ALL) {
            assertFalse("$scene focal (${f.xDp},${f.yDp}) r=${f.clearanceDp}dp overlaps $name $rect", rect.intersectsCircle(f.xDp, f.yDp, f.clearanceDp))
        }
    }

    @Test
    fun `every focal sits inside the overscan-safe area so a TV that crops its edges never hides it`() {
        for ((scene, f) in Focals.ALL) {
            assertTrue("$scene x", f.xDp - f.clearanceDp >= 48f && f.xDp + f.clearanceDp <= 960f - 48f)
            assertTrue("$scene y", f.yDp - f.clearanceDp >= 27f && f.yDp + f.clearanceDp <= 540f - 27f)
        }
    }

    @Test
    fun `focals stay above the highest ridgeline so the sun is a sky object, not buried in terrain`() {
        for ((scene, f) in Focals.ALL) assertTrue("$scene bottom ${f.y + f.r * 1.1f}", f.y + f.r * 1.1f <= 98f)
    }

    @Test
    fun `the circle test really detects an overlap, so the keep-out checks above can fail`() {
        val f = Focal(240f, 40f, 13f) // the old top-centre spot: right behind the weather card edge / clock group at 960dp
        assertTrue(KeepOut.ALL.values.any { it.intersectsCircle(f.xDp, f.yDp, f.clearanceDp) })
        assertFalse(DpRect(0f, 0f, 10f, 10f).intersectsCircle(100f, 100f, 5f))
    }

    @Test
    fun `the quote band spans the banner and no-banner positions of the quote`() {
        val q = KeepOut.QUOTE_BAND
        assertEquals(LayoutBudget.QUOTE_BOTTOM_DP, q.bottom, 0.01f)
        assertTrue(q.top < LayoutBudget.QUOTE_TOP_DP)
    }
}
