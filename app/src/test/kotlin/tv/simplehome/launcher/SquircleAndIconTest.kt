package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class SquircleAndIconTest {
    @Test
    fun `every squircle point lies on the superellipse so the corners are continuous, not circular arcs`() {
        val p = Squircle.unitPoints()
        for (i in 0 until p.size / 2) {
            val v = abs(p[2 * i]).toDouble().pow(Squircle.N.toDouble()) + abs(p[2 * i + 1]).toDouble().pow(Squircle.N.toDouble())
            assertEquals(1.0, v, 1e-4)
        }
    }

    @Test
    fun `a squircle bulges further into the corner than a circle but stays inside the box`() {
        val p = Squircle.unitPoints(steps = 96)
        val diag = 96 / 8 // t = 45 degrees
        val r = kotlin.math.hypot(p[2 * diag], p[2 * diag + 1])
        assertTrue("corner radius $r should exceed a circle's 1.0", r > 1.05f)
        assertTrue(p.all { abs(it) <= 1.0001f })
    }

    private fun solid(color: Int, n: Int = 32) = IntArray(n * n) { color }

    @Test
    fun `an opaque square icon is full-bleed and gets no plate that would shrink it`() {
        assertTrue(IconRules.analyze(solid(0xFF3366CC.toInt()), 32, 32).fullBleed)
    }

    @Test
    fun `a transparent-cornered legacy icon is not full-bleed so it is drawn on a plate`() {
        val px = IntArray(32 * 32)
        for (y in 8 until 24) for (x in 8 until 24) px[y * 32 + x] = 0xFF3366CC.toInt()
        assertFalse(IconRules.analyze(px, 32, 32).fullBleed)
    }

    @Test
    fun `a mostly white transparent icon gets the dark plate, otherwise white, so it never vanishes`() {
        val white = IntArray(32 * 32)
        for (y in 8 until 24) for (x in 8 until 24) white[y * 32 + x] = 0xFFFFFFFF.toInt()
        val a = IconRules.analyze(white, 32, 32)
        assertTrue(a.mostlyWhite)
        assertEquals(IconRules.PLATE_DARK, IconRules.plateColor(a))
        val colour = IntArray(32 * 32)
        for (y in 8 until 24) for (x in 8 until 24) colour[y * 32 + x] = 0xFFE50914.toInt()
        assertEquals(IconRules.PLATE_LIGHT, IconRules.plateColor(IconRules.analyze(colour, 32, 32)))
    }

    @Test
    fun `a fully transparent icon is neither white nor full-bleed`() {
        val a = IconRules.analyze(IntArray(32 * 32), 32, 32)
        assertFalse(a.fullBleed)
        assertFalse(a.mostlyWhite)
    }
}
