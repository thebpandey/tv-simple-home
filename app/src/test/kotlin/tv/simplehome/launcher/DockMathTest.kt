package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockMathTest {
    private val spec = LayoutBudget.ICONS_SPEC
    private val base = spec.maxBaseDp
    private val gap = spec.gapDp

    private fun config(n: Int, center: Int, p: Float): Pair<FloatArray, FloatArray> {
        // p interpolates from rest (1.0x, no shift) to the full magnification, the way the 180ms animation does.
        val full = FloatArray(n) { DockMath.scaleAt(if (center < 0) 99 else kotlin.math.abs(it - center), spec) }
        val scales = FloatArray(n) { 1f + p * (full[it] - 1f) }
        return scales to DockMath.shifts(scales, base)
    }

    @Test
    fun `a magnified tile never overlaps its neighbours at any point of the animation`() {
        for (n in listOf(1, 3, 5, 8)) for (center in 0 until n) for (step in 0..10) {
            val p = step / 10f
            val (scales, shifts) = config(n, center, p)
            for (i in 0 until n - 1) {
                val rightOfI = DockMath.visualLeft(i * (base + gap), shifts[i], base, scales[i]) + base * scales[i]
                val leftOfNext = DockMath.visualLeft((i + 1) * (base + gap), shifts[i + 1], base, scales[i + 1])
                assertEquals("n=$n center=$center p=$p gap between $i and ${i + 1}", gap, leftOfNext - rightOfI, 0.01f)
            }
        }
    }

    @Test
    fun `the row grows only to the right so the first tile's left edge and the menu button stay put`() {
        val (scales, shifts) = config(5, center = 0, p = 1f)
        assertEquals(0f, DockMath.visualLeft(0f, shifts[0], base, scales[0]), 0.01f)
    }

    @Test
    fun `total pushed-out width fits the invisible end padding so the scroll range covers it`() {
        val (scales, _) = config(7, center = 3, p = 1f)
        val extra = scales.sumOf { (spec.maxBaseDp * (it - 1f)).toDouble() }.toFloat() // widest allowed tile
        assertTrue(extra <= spec.extraRightDp)
        assertTrue(spec.extraGrowthDp <= spec.extraRightDp)
    }

    @Test
    fun `with nothing focused every tile rests at 1x with no shift`() {
        val (scales, shifts) = config(4, center = -1, p = 1f)
        assertTrue(scales.all { it == 1f })
        assertTrue(shifts.all { it == 0f })
    }

    @Test
    fun `the focused tile is the biggest and lifts the most`() {
        assertTrue(DockMath.scaleAt(0, spec) > DockMath.scaleAt(1, spec) && DockMath.scaleAt(1, spec) > DockMath.scaleAt(2, spec) && DockMath.scaleAt(2, spec) > DockMath.scaleAt(3, spec))
        assertTrue(DockMath.liftAt(0, spec) > DockMath.liftAt(1, spec) && DockMath.liftAt(1, spec) > DockMath.liftAt(2, spec))
    }

    @Test
    fun `focused menu pills never overlap either, at 1_12x with the same slot-push`() {
        val pillBase = 120f
        val pillGap = 12f
        for (center in 0 until 4) for (step in 0..10) {
            val p = step / 10f
            val scales = FloatArray(4) { 1f + p * ((if (it == center) 1.12f else 1f) - 1f) }
            val shifts = DockMath.shifts(scales, pillBase)
            for (i in 0 until 3) {
                val rightOfI = DockMath.visualLeft(i * (pillBase + pillGap), shifts[i], pillBase, scales[i]) + pillBase * scales[i]
                val leftOfNext = DockMath.visualLeft((i + 1) * (pillBase + pillGap), shifts[i + 1], pillBase, scales[i + 1])
                assertEquals("pill center=$center p=$p", pillGap, leftOfNext - rightOfI, 0.01f)
            }
        }
    }
}
