package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class QuotesTest {
    private val day = LocalDate.of(2026, 3, 14)
    private fun at(h: Int, m: Int = 0, d: LocalDate = day): LocalDateTime = d.atTime(h, m)

    @Test
    fun `17_59 is the day set and 18_00 is the evening set`() {
        assertEquals(QuoteSet.DAY, Quotes.setAt(at(17, 45)))
        assertEquals(QuoteSet.DAY, Quotes.setAt(at(17, 59)))
        assertEquals(QuoteSet.EVENING, Quotes.setAt(at(18, 0)))
        assertTrue(Quotes.quoteAt(at(17, 59)) in Quotes.DAY)
        assertTrue(Quotes.quoteAt(at(18, 0)) in Quotes.EVENING)
    }

    @Test
    fun `midnight returns to the day set`() {
        assertEquals(QuoteSet.EVENING, Quotes.setAt(at(23, 59)))
        assertEquals(QuoteSet.DAY, Quotes.setAt(day.plusDays(1).atStartOfDay()))
    }

    @Test
    fun `anywhere inside one 5 minute slot gives the same quote, so a restart mid-slot shows what was on screen`() {
        for (minute in 0 until 5) assertSame(Quotes.quoteAt(at(9, 30)), Quotes.quoteAt(at(9, 30 + minute)))
        assertSame(Quotes.quoteAt(at(9, 45)), Quotes.quoteAt(at(9, 49)))
    }

    @Test
    fun `a fresh computation after a restart gives the identical quote because nothing is stored`() {
        assertSame(Quotes.quoteAt(at(13, 20)), Quotes.quoteAt(LocalDateTime.of(2026, 3, 14, 13, 20, 59)))
    }

    @Test
    fun `the next slot shows a different quote so the quote rotates every 5 minutes`() {
        for (s in 0 until Quotes.SLOTS_PER_DAY - 1) {
            val a = day.atStartOfDay().plusMinutes(s * 5L)
            if (Quotes.setAt(a) == Quotes.setAt(a.plusMinutes(5))) assertNotEquals(Quotes.quoteAt(a), Quotes.quoteAt(a.plusMinutes(5)))
        }
    }

    @Test
    fun `the day set has 216 slots and the evening set 72`() {
        assertEquals(288, Quotes.SLOTS_PER_DAY)
        assertEquals(216, (0 until 288).count { Quotes.setAt(day.atStartOfDay().plusMinutes(it * 5L)) == QuoteSet.DAY })
    }

    @Test
    fun `every quote appears exactly once per full cycle before any repeats`() {
        val n = Quotes.DAY.size
        // Walk one whole cycle of the day set starting at a cycle boundary found by scanning forward.
        val seen = HashSet<Quote>()
        var d = day.atStartOfDay()
        var shown = 0
        val start = Math.floorMod(day.toEpochDay() * 216, n.toLong()).toInt()
        // Advance until the next cycle start, then collect n consecutive day slots.
        var skip = (n - start) % n
        while (skip > 0) { d = nextDaySlot(d); skip-- }
        while (shown < n) { assertTrue("repeat inside a cycle", seen.add(Quotes.quoteAt(d))); d = nextDaySlot(d); shown++ }
        assertEquals(n, seen.size)
    }

    private fun nextDaySlot(from: LocalDateTime): LocalDateTime {
        var x = from.plusMinutes(5)
        while (Quotes.setAt(x) != QuoteSet.DAY) x = x.plusMinutes(5)
        return x
    }

    @Test
    fun `no quote is ever shown twice in a row, including across cycle boundaries`() {
        var d = LocalDate.of(2026, 1, 1).atStartOfDay()
        var prev = Quotes.quoteAt(d)
        repeat(Quotes.DAY.size * 3) {
            d = nextDaySlot(d)
            val q = Quotes.quoteAt(d)
            assertNotEquals("immediate repeat at $d", prev, q)
            prev = q
        }
    }

    @Test
    fun `an evening shows no repeats unless it straddles a cycle boundary, because 72 slots fit in the 117 quote pool`() {
        // The evening's 72 slots are consecutive positions of one continuous cycle; a repeat is only possible when
        // the cycle restarts in the middle of the evening (quote X at the end of cycle k, again early in cycle k+1).
        val n = Quotes.EVENING.size.toLong()
        var clean = 0
        for (offset in 0L until 120L) {
            val d = day.plusDays(offset)
            val shown = (0 until 72).map { Quotes.quoteAt(d.atTime(18, 0).plusMinutes(it * 5L)) }
            val start = d.toEpochDay() * 72
            val straddles = Math.floorDiv(start, n) != Math.floorDiv(start + 71, n)
            if (!straddles) { assertEquals("evening $d", 72, shown.toSet().size); clean++ }
        }
        assertTrue("most evenings are clean", clean > 40)
    }

    @Test
    fun `the day sequence continues across midnight instead of restarting, so late and early quotes differ`() {
        val last = Quotes.quoteAt(day.atTime(17, 55))
        val first = Quotes.quoteAt(day.plusDays(1).atStartOfDay())
        assertNotEquals(last, first)
    }

    @Test
    fun `different cycles use different orders so the sequence does not loop identically`() {
        val a = Quotes.cycleOrder(Quotes.DAY.size, QuoteSet.DAY, 10).toList()
        val b = Quotes.cycleOrder(Quotes.DAY.size, QuoteSet.DAY, 11).toList()
        assertNotEquals(a, b)
        assertEquals((0 until Quotes.DAY.size).toList(), a.sorted())
    }

    @Test
    fun `dates before 1970 still map to a valid quote instead of a negative index crash`() {
        val old = LocalDate.of(1960, 1, 1)
        assertTrue(Quotes.quoteAt(at(9, 0, old)) in Quotes.DAY)
        assertTrue(Quotes.quoteAt(at(20, 0, old)) in Quotes.EVENING)
    }

    @Test
    fun `next boundary is the next 5 minutes, which includes exactly 18_00 and midnight`() {
        assertEquals(at(9, 5), Quotes.nextBoundary(at(9, 0)))
        assertEquals(at(9, 15), Quotes.nextBoundary(at(9, 14)))
        assertEquals(at(18, 0), Quotes.nextBoundary(at(17, 59)))
        assertEquals(day.plusDays(1).atStartOfDay(), Quotes.nextBoundary(at(23, 59)))
    }

    @Test
    fun `each pool is large enough for a whole window without wrapping`() {
        assertTrue(Quotes.DAY.size >= 216)
        assertTrue(Quotes.EVENING.size >= 72)
    }

    @Test
    fun `every requested day theme has at least 10 quotes so the rotation can surface it`() =
        checkThemes(Quotes.DAY, Quotes.DAY_THEMES)

    @Test
    fun `every requested evening theme has at least 10 quotes so the rotation can surface it`() =
        checkThemes(Quotes.EVENING, Quotes.EVENING_THEMES)

    @Test
    fun `day quotes only use day themes and evening quotes only evening themes so the mood matches the hour`() {
        assertTrue(Quotes.DAY.all { it.theme in Quotes.DAY_THEMES })
        assertTrue(Quotes.EVENING.all { it.theme in Quotes.EVENING_THEMES })
    }

    @Test
    fun `no quote is blank, unattributed or unsourced, and none is too long for a 540dp screen`() {
        for (q in Quotes.DAY + Quotes.EVENING) {
            assertTrue("blank text", q.text.isNotBlank())
            assertTrue("missing author for: ${q.text}", q.author.isNotBlank())
            assertTrue("missing source for: ${q.text}", q.source.isNotBlank())
            assertTrue("too long (${q.text.length}): ${q.text}", q.text.length <= Quotes.MAX_LENGTH)
        }
    }

    @Test
    fun `no text appears twice, within or across sets, so a repeat can never be mistaken for rotation`() {
        val texts = (Quotes.DAY + Quotes.EVENING).map { it.text.lowercase() }
        assertEquals(texts.size, texts.toSet().size)
    }

    @Test
    fun `a source is more specific than the author name alone`() {
        for (q in Quotes.DAY + Quotes.EVENING) assertNotEquals("source equals author: ${q.text}", q.author, q.source)
    }

    private fun assertSame(a: Any, b: Any) = org.junit.Assert.assertSame(a, b)

    private fun checkThemes(set: List<Quote>, themes: List<Theme>) {
        for (th in themes) {
            val n = set.count { it.theme == th }
            assertTrue("theme $th has only $n quotes", n >= Quotes.MIN_PER_THEME)
        }
    }

    @Test
    fun `the merged collection is all there with its new themes`() {
        assertTrue(Quotes.DAY.size >= 440)
        assertTrue(Quotes.EVENING.size >= 225)
        assertTrue(Quotes.DAY.any { it.theme == Theme.VIRTUE } && Quotes.DAY.any { it.theme == Theme.INTEGRITY } && Quotes.DAY.any { it.theme == Theme.WISDOM })
        assertTrue(Quotes.EVENING.any { it.theme == Theme.LOVE })
    }

    @Test
    fun `merge cleanup left no double hyphens or stray whitespace in any quote`() {
        for (q in Quotes.DAY + Quotes.EVENING) {
            assertTrue("double hyphen in: ${q.text}", "--" !in q.text)
            assertEquals("untrimmed: ${q.text}", q.text.trim(), q.text)
            assertTrue("double space in: ${q.text}", "  " !in q.text)
        }
    }

    @Test
    fun `a whole day cycle now spans more than one day, yet each quote still appears once per cycle`() {
        // 448 quotes against 216 day slots: a cycle lasts a little over two days; the shuffle must still be a permutation.
        val order = Quotes.cycleOrder(Quotes.DAY.size, QuoteSet.DAY, 3)
        assertEquals((0 until Quotes.DAY.size).toList(), order.sorted())
    }
}
