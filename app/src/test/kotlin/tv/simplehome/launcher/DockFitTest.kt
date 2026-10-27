package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockFitTest {
    private val specs = listOf(LayoutBudget.ICONS_SPEC, LayoutBudget.BANNERS_SPEC)
    private fun available(spec: DockSpec) = DockGeometry.availableDp(spec, 960f)

    @Test
    fun `for 1 to 8 favorites the fitted row plus its magnification never exceeds the available width unless it scrolls`() {
        for (spec in specs) for (n in 1..8) {
            val fit = DockFit.fit(spec, available(spec), n)
            assertTrue("${spec.style} n=$n base=${fit.baseDp}", fit.baseDp in spec.minBaseDp..spec.maxBaseDp)
            if (!fit.scrolls) assertTrue("${spec.style} n=$n needs ${DockFit.neededDp(spec, fit.baseDp, n)}", DockFit.neededDp(spec, fit.baseDp, n) <= available(spec) + 0.01f)
        }
    }

    @Test
    fun `the icons dock keeps full-size 56dp icons for up to 8 favorites, matching the macOS Dock look`() {
        val spec = LayoutBudget.ICONS_SPEC
        for (n in 1..8) assertEquals("n=$n", spec.maxBaseDp, DockFit.fit(spec, available(spec), n).baseDp, 0.001f)
        assertTrue("resting icon ${spec.maxBaseDp}dp should be 54-58dp", spec.maxBaseDp in 54f..58f)
    }

    @Test
    fun `many favorites shrink toward the minimum`() {
        for (spec in specs) {
            assertTrue(DockFit.fit(spec, available(spec), 14).baseDp < DockFit.fit(spec, available(spec), 9).baseDp || DockFit.fit(spec, available(spec), 14).baseDp == spec.minBaseDp)
            assertTrue(DockFit.fit(spec, available(spec), 12).baseDp >= spec.minBaseDp)
        }
        assertEquals(LayoutBudget.BANNERS_SPEC.maxBaseDp, DockFit.fit(LayoutBudget.BANNERS_SPEC, available(LayoutBudget.BANNERS_SPEC), 3).baseDp, 0.001f)
    }

    @Test
    fun `too many favorites fall back to scrolling at the minimum width instead of shrinking unreadably`() {
        for (spec in specs) {
            val fit = DockFit.fit(spec, available(spec), 24)
            assertTrue(fit.scrolls)
            assertEquals(spec.minBaseDp, fit.baseDp, 0.001f)
            assertFalse(DockFit.fit(spec, available(spec), 8).scrolls)
        }
    }

    @Test
    fun `icons are square and banners stay 16 by 9 whatever the fitted width`() {
        for (n in 1..8) {
            val i = DockFit.fit(LayoutBudget.ICONS_SPEC, available(LayoutBudget.ICONS_SPEC), n)
            assertEquals(1f, i.baseDp / i.cardHeightDp, 0.001f)
            val b = DockFit.fit(LayoutBudget.BANNERS_SPEC, available(LayoutBudget.BANNERS_SPEC), n)
            assertEquals(16f / 9f, b.baseDp / b.cardHeightDp, 0.001f)
        }
    }

    @Test
    fun `a narrower screen shrinks the tiles or scrolls rather than overflowing`() {
        for (spec in specs) {
            val fit = DockFit.fit(spec, 300f, 5)
            assertTrue(fit.scrolls || DockFit.neededDp(spec, fit.baseDp, 5) <= 300f)
        }
    }
}
