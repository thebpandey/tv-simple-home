package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteRevealTest {
    private val lines = intArrayOf(40, 30)
    private val author = 12

    @Test
    fun `the pen writes line by line at one speed, so a short line takes less time than a long one`() {
        assertEquals(0 to 0f, QuoteReveal.penAt(lines, author, 0f))
        assertEquals(0 to 0.5f, QuoteReveal.penAt(lines, author, 20f))
        assertEquals(1 to 0f, QuoteReveal.penAt(lines, author, 40f))
        assertEquals(1 to 0.5f, QuoteReveal.penAt(lines, author, 55f))
    }

    @Test
    fun `there is a short pause before the author is written, then the author, then nothing is left`() {
        val pauseStart = 70f
        assertEquals(2 to 0f, QuoteReveal.penAt(lines, author, pauseStart))
        assertEquals(2 to 0f, QuoteReveal.penAt(lines, author, pauseStart + QuoteReveal.AUTHOR_PAUSE_CHARS - 0.5f))
        assertEquals(2 to 0.5f, QuoteReveal.penAt(lines, author, pauseStart + QuoteReveal.AUTHOR_PAUSE_CHARS + 6f))
        val total = QuoteReveal.totalUnits(lines, author).toFloat()
        assertEquals(3 to 0f, QuoteReveal.penAt(lines, author, total))
    }

    @Test
    fun `writing is slow enough to read along but never drags past 10 seconds, and erasing is quicker`() {
        val typical = QuoteReveal.totalUnits(intArrayOf(45, 45), 18)
        val write = QuoteReveal.writeMs(typical)
        assertTrue("typical write $write", write in 4_000L..8_000L)
        assertEquals(QuoteReveal.MAX_WRITE_MS, QuoteReveal.writeMs(1_000))
        assertEquals(QuoteReveal.MIN_WRITE_MS, QuoteReveal.writeMs(5))
        assertTrue(QuoteReveal.eraseMs(typical) < write)
    }

    @Test
    fun `the whole change takes a small share of the 2 minute slot, so most of the time the quote just sits there`() {
        val worst = QuoteReveal.MAX_WRITE_MS + QuoteReveal.MAX_ERASE_MS
        assertTrue(worst * 8 <= Quotes.SLOT_MINUTES * 60_000L)
    }
}
