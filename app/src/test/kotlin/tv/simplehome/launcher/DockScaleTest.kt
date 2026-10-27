package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockScaleTest {
    @Test
    fun `every icon dock dimension follows the single DOCK_SCALE`() {
        val spec = LayoutBudget.ICONS_SPEC
        val s = LayoutBudget.DOCK_SCALE
        assertEquals(64f * s, spec.maxBaseDp, 0.01f)
        assertEquals(40f * s, spec.minBaseDp, 0.01f)
        assertEquals(14f * s, spec.gapDp, 0.01f)
        assertEquals(14f * s, spec.liftDp, 0.01f)
        assertEquals(120f * s, spec.pillWDp, 0.01f)
        assertEquals(40f * s, spec.pillHDp, 0.01f)
        assertEquals(15f * s, spec.pillSp, 0.001f)
    }

    @Test
    fun `the banner dock keeps its v2_6 size`() {
        val spec = LayoutBudget.BANNERS_SPEC
        assertEquals(132f * 0.68f, spec.maxBaseDp, 0.01f)
        assertEquals(12f * 0.68f, spec.gapDp, 0.01f)
        assertEquals(1.35f, spec.magFocus, 0f)
    }

    @Test
    fun `magnification ratios are not scaled and match the macOS dock design`() {
        val spec = LayoutBudget.ICONS_SPEC
        assertEquals(1.45f, DockMath.scaleAt(0, spec), 0f)
        assertEquals(1.18f, DockMath.scaleAt(1, spec), 0f)
        assertEquals(1.06f, DockMath.scaleAt(2, spec), 0f)
        assertEquals(1f, DockMath.scaleAt(3, spec), 0f)
    }

    @Test
    fun `the dock still fits the home layout with room for the quote`() {
        assertTrue(LayoutBudget.homeFits())
        assertTrue("dock ${LayoutBudget.DOCK_DP}", LayoutBudget.DOCK_DP < 170f)
    }

    @Test
    fun `tiles remain at least 8dp above the safe bottom edge`() {
        for (spec in listOf(LayoutBudget.ICONS_SPEC, LayoutBudget.BANNERS_SPEC)) assertTrue(spec.shelfPadDp + spec.bottomGapDp >= 8f)
    }

    @Test
    fun `dock style parsing defaults to icons for missing or unknown values`() {
        assertEquals(DockStyle.ICONS, DockStyle.parse(null))
        assertEquals(DockStyle.ICONS, DockStyle.parse("garbage"))
        assertEquals(DockStyle.BANNERS, DockStyle.parse("banners"))
    }
}
