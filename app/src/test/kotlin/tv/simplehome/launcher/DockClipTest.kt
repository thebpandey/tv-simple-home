package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockClipTest {
    private val specs = listOf(LayoutBudget.ICONS_SPEC, LayoutBudget.BANNERS_SPEC)
    private val dockW = DockGeometry.safeWidthDp(960f) // the dock's clip rect is the 48dp safe area: 0..864 in dock coordinates
    private val ring = 2f

    private fun fit(spec: DockSpec, n: Int) = DockFit.fit(spec, DockGeometry.availableDp(spec, 960f), n)

    @Test
    fun `the focused menu button, magnified, stays inside the dock clip rect and the safe area for 1 to 16 favorites`() {
        for (spec in specs) for (n in 1..16) {
            val f = fit(spec, n)
            val e = DockExtents.focused(spec, n, f.baseDp, f.scrolls, dockW, DockExtents.MENU)
            assertTrue("${spec.style} n=$n menu left ${e.leftDp}", e.leftDp - ring >= -0.01f)
            assertTrue("${spec.style} n=$n menu right ${e.rightDp}", e.rightDp + ring <= dockW + 0.01f)
        }
    }

    @Test
    fun `every focused favorite, magnified, stays inside the dock clip rect and the safe area`() {
        for (spec in specs) for (n in 1..8) {
            val f = fit(spec, n)
            for (k in 0 until n) {
                val e = DockExtents.focused(spec, n, f.baseDp, f.scrolls, dockW, k)
                assertTrue("${spec.style} n=$n fav $k left ${e.leftDp}", e.leftDp >= -0.01f)
                assertTrue("${spec.style} n=$n fav $k right ${e.rightDp}", e.rightDp <= dockW + 0.01f)
            }
        }
    }

    @Test
    fun `the whole dock including the magnified menu button fits the safe width, so the shelf can enclose it, for every item`() {
        for (spec in specs) for (n in 1..8) {
            val f = fit(spec, n)
            val rest = DockGeometry.restWidthDp(spec, n, f.baseDp)
            for (focus in -1 until n) assertTrue("${spec.style} n=$n focus=$focus", rest + DockExtents.growthDp(spec, n, f.baseDp, focus) <= dockW + 0.01f)
        }
    }

    @Test
    fun `focusing the menu button re-centres the dock, growth split evenly on both sides, like a favorite`() {
        for (spec in specs) {
            val n = 5
            val base = fit(spec, n).baseDp
            val rest = DockGeometry.restWidthDp(spec, n, base)
            val grown = rest + DockExtents.growthDp(spec, n, base, DockExtents.MENU)
            val left = DockGeometry.offsetDp(dockW, grown)
            val right = dockW - left - grown
            assertEquals(left, right, 1f)
            assertEquals((grown - rest) / 2f, DockGeometry.offsetDp(dockW, rest) - left, 0.01f)
        }
    }

    @Test
    fun `the magnified menu button keeps its left edge at its slot, so its growth pushes the neighbours instead of leaving the shelf`() {
        val spec = LayoutBudget.ICONS_SPEC
        val n = 6
        val base = fit(spec, n).baseDp
        val e = DockExtents.focused(spec, n, base, false, dockW, DockExtents.MENU)
        val content = DockGeometry.restWidthDp(spec, n, base) + DockExtents.growthDp(spec, n, base, DockExtents.MENU)
        assertEquals(DockGeometry.offsetDp(dockW, content) + spec.menuInsetDp, e.leftDp, 0.01f)
        assertEquals(spec.buttonDp * spec.magFocus, e.rightDp - e.leftDp, 0.01f)
    }

    @Test
    fun `the first and last pill of the open menu, focused with lift and ring, lie inside the shelf and the dock for both styles`() {
        for (spec in specs) {
            val first = DockExtents.pill(spec, 0)
            val last = DockExtents.pill(spec, 3)
            assertTrue("${spec.style} first pill left ${first.leftDp}", first.leftDp >= spec.menuInsetDp + spec.buttonDp) // clear of the chevron
            assertTrue("${spec.style} last pill right ${last.rightDp}", last.rightDp <= spec.expandedShelfDp)
            val offset = DockGeometry.offsetDp(dockW, spec.expandedShelfDp)
            assertTrue(offset + first.leftDp >= 0f && offset + last.rightDp <= dockW)
        }
    }

    @Test
    fun `the pill row padding covers half the focus growth, the lift, the 2dp ring and 4dp spare on every side`() {
        for (spec in specs) {
            val horizontalNeed = spec.pillWDp * (spec.pillMag - 1f) / 2f + 2f + 4f
            val verticalNeed = spec.pillHDp * (spec.pillMag - 1f) / 2f + spec.neighbourLiftDp + 2f + 4f
            assertTrue("${spec.style} horizontal", spec.pillRowPadDp >= horizontalNeed)
            assertTrue("${spec.style} vertical", spec.pillRowPadDp >= verticalNeed)
        }
    }

    @Test
    fun `the dock height still reserves headroom above the resting button for its growth, lift and tooltip`() {
        for (spec in specs) {
            val headroom = LayoutBudget.DOCK_DP - (spec.bottomGapDp + spec.shelfPadDp + spec.buttonDp) // dock top to the resting button top
            val needed = spec.buttonDp * (spec.magFocus - 1f) + spec.liftDp + 4f + spec.labelDp
            assertTrue("${spec.style} needs $needed above the button, has $headroom", needed <= headroom)
        }
    }
}
