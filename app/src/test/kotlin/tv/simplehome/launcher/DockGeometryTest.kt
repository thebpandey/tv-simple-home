package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockGeometryTest {
    private val dockW = DockGeometry.safeWidthDp(960f)
    private val specs = listOf(LayoutBudget.ICONS_SPEC, LayoutBudget.BANNERS_SPEC)

    @Test
    fun `unfocused cards sit exactly one shelf padding inside the shelf at top and bottom, equal, for every tile size`() {
        for (spec in specs) for (k in listOf(0.7f, 0.8f, 0.9f, 1f)) {
            val card = k * spec.cardHeightDp(spec.maxBaseDp)
            assertEquals("top ${spec.style} $card", spec.shelfPadDp, DockGeometry.cardTopDp(spec, card) - DockGeometry.shelfTopDp(spec, card), 0.01f)
            assertEquals("bottom ${spec.style} $card", spec.shelfPadDp, DockGeometry.shelfBottomDp(spec) - DockGeometry.cardBottomDp(spec, card), 0.01f)
        }
    }

    @Test
    fun `the icons shelf hugs the icons with 10dp above and below`() {
        assertEquals(10f, LayoutBudget.ICONS_SPEC.shelfPadDp, 0f)
    }

    @Test
    fun `the row, tooltip strip included, never starts above the dock top so nothing is clipped`() {
        for (spec in specs) for (card in listOf(0.7f * spec.cardHeightDp(spec.maxBaseDp), spec.cardHeightDp(spec.maxBaseDp))) {
            assertTrue(DockGeometry.rowTopPadDp(spec, card) >= 0f)
        }
    }

    private fun contentWidth(spec: DockSpec, n: Int, focus: Int): Float {
        val fit = DockFit.fit(spec, DockGeometry.availableDp(spec, 960f), n)
        if (fit.scrolls) return dockW
        return DockGeometry.restWidthDp(spec, n, fit.baseDp) + DockGeometry.growthDp(spec, n, fit.baseDp, focus)
    }

    @Test
    fun `the dock stays centred, left and right margins equal within 1dp, for 1 to 8 favorites focused or not`() {
        for (spec in specs) for (n in 1..8) for (focus in -1 until n) {
            val content = contentWidth(spec, n, focus)
            val left = DockGeometry.offsetDp(dockW, content)
            val right = dockW - left - content
            assertEquals("${spec.style} n=$n focus=$focus", left, right, 1f)
        }
    }

    @Test
    fun `even fully magnified, the dock fits inside the safe width`() {
        for (spec in specs) for (n in 1..8) for (focus in -1 until n) {
            val content = contentWidth(spec, n, focus)
            assertTrue("${spec.style} n=$n focus=$focus width=$content", content <= dockW + 0.01f)
        }
    }

    @Test
    fun `magnifying a tile moves the left edge left by half the growth, so the dock grows evenly on both sides`() {
        for (spec in specs) {
            val n = 5
            val base = DockFit.fit(spec, DockGeometry.availableDp(spec, 960f), n).baseDp
            val rest = DockGeometry.restWidthDp(spec, n, base)
            val grown = rest + DockGeometry.growthDp(spec, n, base, 2)
            val shift = DockGeometry.offsetDp(dockW, rest) - DockGeometry.offsetDp(dockW, grown)
            assertEquals((grown - rest) / 2f, shift, 0.01f)
        }
    }

    @Test
    fun `a dock wider than the screen stays anchored at the left edge instead of pushing the menu button off screen`() {
        assertEquals(0f, DockGeometry.offsetDp(dockW, dockW + 400f), 0f)
    }

    @Test
    fun `in the icons dock the menu button is the same size as an icon and the first icon starts one gap after the divider`() {
        val spec = LayoutBudget.ICONS_SPEC
        assertEquals(spec.maxBaseDp, spec.buttonDp, 0.001f)
        assertEquals(spec.dividerXDp + 1f + spec.gapDp, spec.favStartDp + spec.hPadDp + spec.rowHPadDp, 0.01f)
        assertEquals("button inset equals the shelf padding", spec.shelfPadDp, spec.menuInsetDp, 0f)
    }
}
