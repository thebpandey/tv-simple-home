package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteFitTest {
    /** Greedy word wrap with every glyph [em] x size wide (Inter Light averages about 0.5em; 0.52 plus a 6% safety margin is deliberately pessimistic). */
    private fun model(em: Float = 0.52f, safety: Float = 1.06f) = LineCounter { text, sp, widthDp ->
        val glyph = em * sp * safety
        var lines = 1
        var used = 0f
        for (word in text.split(' ')) {
            val w = word.length * glyph
            val space = glyph
            if (used == 0f) used = w
            else if (used + space + w <= widthDp) used += space + w
            else { lines++; used = w }
        }
        lines
    }

    private val width = 960f * QuoteFit.MAX_WIDTH_FRACTION
    private val all = Quotes.DAY + Quotes.EVENING

    @Test
    fun `every quote in both sets fits in at most 4 lines at 19_5sp or larger, with the author whole beneath it`() {
        assertTrue(all.size > 300)
        for (q in all) {
            val shown = "“${q.text}”"
            val r = QuoteFit.fit(shown, width, model())
            assertTrue("'${q.text.take(40)}' needs ${r.lines} lines at ${r.sp}sp", r.lines <= 4 && r.sp >= 19.5f)
            assertTrue("a quote needing more than 3 lines must already be at the smallest size", r.lines <= 3 || r.sp == 19.5f)
            // the author line: one line of 15sp at the same width model
            val author = "— ${q.author}"
            assertTrue("author '${q.author}' must fit one line", author.length * 0.52f * 15f * 1.06f <= width)
            assertTrue("block ${QuoteFit.blockHeightDp(r.lines, r.sp)}dp must fit the centre", QuoteFit.blockHeightDp(r.lines, r.sp) <= LayoutBudget.centerDp(true))
        }
    }

    @Test
    fun `a short quote keeps the full 23sp and a medium one steps down before ever using a fourth line`() {
        assertEquals(23f, QuoteFit.fit("Be here now.", width, model()).sp, 0f)
        val medium = "word ".repeat(32).trim() // too long for 3 lines at 23sp, fine at 21
        val r = QuoteFit.fit(medium, width, model())
        assertTrue(r.sp < 23f)
        assertEquals(3, r.maxLines)
    }

    @Test
    fun `when even 19_5sp needs a fourth line the quote gets four lines at 19_5sp, never a clipped third`() {
        val long = "word ".repeat(40).trim() // 199 characters: more than 3 lines even at 19.5sp
        val r = QuoteFit.fit(long, width, model())
        assertEquals(19.5f, r.sp, 0f)
        assertEquals(4, r.maxLines)
        assertEquals(4, r.lines)
    }

    @Test
    fun `the fitting really depends on the measurement, so a model that is too optimistic would be caught`() {
        val tight = LineCounter { _, _, _ -> 5 }
        assertEquals(5, QuoteFit.fit("x", width, tight).maxLines) // beyond 4 lines the block still grows rather than clipping
    }

    @Test
    fun `the block height grows with every extra line and the tallest block is four lines at 19_5sp`() {
        assertTrue(QuoteFit.blockHeightDp(4, 19.5f) > QuoteFit.blockHeightDp(3, 19.5f))
        assertEquals(QuoteFit.blockHeightDp(4, 19.5f), QuoteFit.maxBlockDp(), 0f)
        assertTrue(QuoteFit.blockHeightDp(3, 23f) <= QuoteFit.maxBlockDp() + 0.01f)
        assertTrue(QuoteFit.blockHeightDp(3, 21f) <= QuoteFit.maxBlockDp() + 0.01f)
    }
}
