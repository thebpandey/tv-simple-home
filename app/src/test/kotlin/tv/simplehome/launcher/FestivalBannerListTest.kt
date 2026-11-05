package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class FestivalBannerListTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private fun ids(date: LocalDate, usHolidays: Boolean = true) =
        Festivals.bannerList(date, festivals = Festivals.enabled(usHolidays)).map { it.festival.id }

    @Test
    fun `Diwali is announced a week before Dhanteras so the household can prepare, not on the first day`() {
        assertTrue("diwali" in ids(d(2026, 10, 30))) // Dhanteras 2026-11-06 minus 7 days
        assertFalse("diwali" in ids(d(2026, 10, 29)))
    }

    @Test
    fun `the lead week shows only the banner, the festival wallpaper still waits for the festival itself`() {
        assertTrue("diwali" in ids(d(2026, 11, 5)))
        assertNull(Festivals.activeOn(d(2026, 11, 5), festivals = Festivals.enabled(false)))
    }

    @Test
    fun `one-day holidays also get the full week of notice, not the old three days`() {
        assertTrue("us_veterans" in ids(d(2026, 11, 4)))
        assertFalse("us_veterans" in ids(d(2026, 11, 3)))
    }

    @Test
    fun `festivals within a week of each other are all shown, the one on screen today first, the rest by date`() {
        assertEquals(listOf("diwali", "us_veterans", "chhath"), ids(d(2026, 11, 8)))
        assertEquals(listOf("diwali", "us_veterans"), ids(d(2026, 11, 5)))
    }

    @Test
    fun `turning US holidays off removes them from the list but keeps the festivals`() {
        assertEquals(listOf("diwali", "chhath"), ids(d(2026, 11, 8), usHolidays = false))
    }

    @Test
    fun `a festival is listed once even when two years of it are in range`() {
        val list = ids(d(2026, 12, 31))
        assertEquals(list.distinct(), list)
        assertTrue("new_year" in list)
    }

    @Test
    fun `each extra festival gets a short date with its own countdown`() {
        assertEquals("Wed, Nov 11  ·  in 6 days", Festivals.compactDateLine(d(2026, 11, 5), d(2026, 11, 11), Locale.US))
        assertEquals("Wed, Nov 11  ·  Today", Festivals.compactDateLine(d(2026, 11, 11), d(2026, 11, 11), Locale.US))
        assertEquals("Sun, Nov 8", Festivals.compactDateLine(d(2026, 11, 9), d(2026, 11, 8), Locale.US))
    }

    @Test
    fun `the banner reports which day it is about so the short line counts down to the named day`() {
        val diwali = Festivals.bannerList(d(2026, 11, 6)).first { it.festival.id == "diwali" }
        val b = Festivals.bannerFor(diwali, d(2026, 11, 6), locale = Locale.US)
        assertEquals("Dhanteras", b.title)
        assertEquals(d(2026, 11, 6), b.day)
    }

    @Test
    fun `before the window the Diwali banner counts down to Diwali itself`() {
        val diwali = Festivals.bannerList(d(2026, 11, 1)).first { it.festival.id == "diwali" }
        val b = Festivals.bannerFor(diwali, d(2026, 11, 1), locale = Locale.US)
        assertEquals("Diwali", b.title)
        assertEquals("Sunday, November 8, 2026  ·  in 7 days", b.dateLine)
    }

    private fun titles(date: LocalDate, usHolidays: Boolean = true) =
        Festivals.bannerItems(date, festivals = Festivals.enabled(usHolidays)).map { it.title }

    @Test
    fun `Dhanteras and Diwali each get their own entry when both are near, so tomorrow's Dhanteras is not hidden behind Diwali`() {
        assertEquals(listOf("Dhanteras", "Diwali", "Govardhan Puja", "Bhai Dooj", "Veterans Day"), titles(d(2026, 11, 5)))
    }

    @Test
    fun `each named day follows the same one-week notice as any festival`() {
        val oct30 = titles(d(2026, 10, 30)) // Dhanteras is 7 days away, Diwali 9
        assertTrue("Dhanteras" in oct30)
        assertFalse("Diwali" in oct30)
        assertTrue("Diwali" in titles(d(2026, 11, 1)))
    }

    @Test
    fun `a named day drops off once it has passed, and the festival in progress stays first`() {
        assertEquals(listOf("Diwali", "Govardhan Puja", "Bhai Dooj", "Veterans Day"), titles(d(2026, 11, 7)))
        assertEquals(listOf("Diwali", "Govardhan Puja", "Bhai Dooj", "Veterans Day", "Chhath Puja"), titles(d(2026, 11, 8)))
    }

    @Test
    fun `the first entry keeps the India note when the India date differs`() {
        val bhaiDooj = Festivals.bannerItems(d(2026, 11, 10)).first()
        val b = Festivals.bannerForItem(bhaiDooj, d(2026, 11, 10), Locale.US)
        assertEquals("Bhai Dooj", b.title)
        assertEquals("India: Wednesday, November 11", b.indiaLine)
    }

    @Test
    fun `festivals without named days still show as a single entry`() {
        assertEquals(listOf("Thanksgiving"), titles(d(2026, 11, 20)).filter { it == "Thanksgiving" })
    }

    @Test
    fun `stacked rows show just the countdown so it is never cut off, and a passed day shows its date`() {
        assertEquals("in 4 days", Festivals.rowWhen(d(2026, 11, 5), d(2026, 11, 9), Locale.US))
        assertEquals("Tomorrow", Festivals.rowWhen(d(2026, 11, 5), d(2026, 11, 6), Locale.US))
        assertEquals("Fri, Dec 25", Festivals.rowWhen(d(2026, 12, 26), d(2026, 12, 25), Locale.US))
    }
}
