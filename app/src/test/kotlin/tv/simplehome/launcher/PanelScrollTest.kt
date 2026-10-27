package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class PanelScrollTest {
    // Viewport 400 tall; a section header at y=300 and its first row at y=340..384.
    @Test
    fun `scrolling up to the first row of a section reveals the header above it, not just the row`() {
        val target = PanelScroll.target(scrollY = 330, viewportH = 400, top = 300, bottom = 384, margin = 8)
        assertEquals("header top minus margin must be the new top edge", 292, target)
    }

    @Test
    fun `focusing only the row (the old behaviour) would have left the header hidden, which is the bug`() {
        val rowOnly = PanelScroll.target(scrollY = 330, viewportH = 400, top = 340, bottom = 384, margin = 8)
        assertEquals(330, rowOnly)
        assert(rowOnly > 300) { "the header at y=300 stays above the viewport" }
    }

    @Test
    fun `scrolling down to an item below the viewport brings its bottom edge into view`() {
        assertEquals(300, PanelScroll.target(scrollY = 0, viewportH = 400, top = 650, bottom = 692, margin = 8))
    }

    @Test
    fun `an item already fully visible does not scroll, and the first header never scrolls past the top`() {
        assertEquals(100, PanelScroll.target(100, 400, 200, 244, 8))
        assertEquals(0, PanelScroll.target(50, 400, 4, 40, 8))
    }
}
