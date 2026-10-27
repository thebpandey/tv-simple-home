package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditPlacementTest {
    private val spec = LayoutBudget.ICONS_SPEC
    private val shelfH = spec.maxBaseDp + 2 * spec.shelfPadDp

    @Test
    fun `with a short dock the credit sits in the safe corner level with the shelf bottom`() {
        val m = CreditPlacement.bottomMarginDp(spec, dockRightDp = 700f, creditWidthDp = 150f, shelfHeightDp = shelfH)
        assertEquals(27f + spec.bottomGapDp, m, 0.01f)
    }

    @Test
    fun `when the dock reaches the credit's corner the credit moves above the shelf so it never overlaps it`() {
        val m = CreditPlacement.bottomMarginDp(spec, dockRightDp = 900f, creditWidthDp = 150f, shelfHeightDp = shelfH)
        assertTrue("margin $m must clear the shelf top at ${27f + spec.bottomGapDp + shelfH}", m >= 27f + spec.bottomGapDp + shelfH)
    }

    @Test
    fun `the credit width decides, the same dock overlaps a long credit but not a short one`() {
        val short = CreditPlacement.bottomMarginDp(spec, 760f, 100f, shelfH)
        val long = CreditPlacement.bottomMarginDp(spec, 760f, 220f, shelfH)
        assertTrue(long > short)
    }
}
