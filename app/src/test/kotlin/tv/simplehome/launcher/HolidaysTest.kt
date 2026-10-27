package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class HolidaysTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private val none = emptyMap<String, List<FestivalOccurrence>>()
    private fun f(id: String) = Festivals.ALL.first { it.id == id }
    private fun main(id: String, year: Int) = Festivals.mainDays(f(id), year, none).single().primary
    private fun activeId(date: LocalDate, dates: Map<String, List<FestivalOccurrence>> = none, festivals: List<Festival> = Festivals.ALL) =
        Festivals.activeOn(date, dates, festivals)?.festival?.id

    @Test
    fun `the floating Monday holidays follow their rules for 2026 to 2030`() {
        val expected = mapOf(
            "us_mlk" to listOf(d(2026, 1, 19), d(2027, 1, 18), d(2028, 1, 17), d(2029, 1, 15), d(2030, 1, 21)),
            "us_presidents" to listOf(d(2026, 2, 16), d(2027, 2, 15), d(2028, 2, 21), d(2029, 2, 19), d(2030, 2, 18)),
            "us_memorial" to listOf(d(2026, 5, 25), d(2027, 5, 31), d(2028, 5, 29), d(2029, 5, 28), d(2030, 5, 27)),
            "us_labor" to listOf(d(2026, 9, 7), d(2027, 9, 6), d(2028, 9, 4), d(2029, 9, 3), d(2030, 9, 2)),
            "us_columbus" to listOf(d(2026, 10, 12), d(2027, 10, 11), d(2028, 10, 9), d(2029, 10, 8), d(2030, 10, 14)),
        )
        for ((id, dates) in expected) for ((i, date) in dates.withIndex()) {
            assertEquals("$id ${2026 + i}", date, main(id, 2026 + i))
            assertEquals("$id is a Monday", java.time.DayOfWeek.MONDAY, date.dayOfWeek)
        }
    }

    @Test
    fun `Memorial Day is the LAST Monday, so a May with five Mondays picks the fifth`() {
        assertEquals(d(2027, 5, 31), main("us_memorial", 2027)) // May 2027 has Mondays on 3, 10, 17, 24, 31
        assertEquals(d(2026, 5, 25), main("us_memorial", 2026)) // only four Mondays; 25 is the last
    }

    @Test
    fun `fixed-date holidays keep their calendar date`() {
        for (y in 2026..2030) {
            assertEquals(d(y, 6, 19), main("us_juneteenth", y))
            assertEquals(d(y, 7, 4), main("us_independence", y))
            assertEquals(d(y, 11, 11), main("us_veterans", y))
        }
    }

    @Test
    fun `a Saturday holiday is observed the Friday before and a Sunday one the Monday after`() {
        assertEquals(d(2026, 7, 3), Festivals.observedDate(d(2026, 7, 4))) // Sat
        assertEquals(d(2027, 6, 18), Festivals.observedDate(d(2027, 6, 19))) // Juneteenth 2027 is a Saturday
        assertEquals(d(2027, 7, 5), Festivals.observedDate(d(2027, 7, 4))) // Sun
        assertEquals(d(2029, 11, 12), Festivals.observedDate(d(2029, 11, 11))) // Sun
        assertEquals(d(2028, 11, 10), Festivals.observedDate(d(2028, 11, 11))) // Sat
        assertNull("a weekday holiday has no observed line", Festivals.observedDate(d(2028, 7, 4)))
    }

    @Test
    fun `the banner for a weekend holiday carries the Observed line, a weekday one does not`() {
        val sat = d(2026, 7, 4)
        val b = Festivals.bannerFor(Festivals.activeOn(sat, none)!!, sat, none, Locale.US)
        assertEquals("Independence Day", b.title)
        assertEquals("Observed Friday, July 3", b.observedLine)
        val wk = d(2028, 7, 4)
        assertNull(Festivals.bannerFor(Festivals.activeOn(wk, none)!!, wk, none, Locale.US).observedLine)
        val j = d(2027, 6, 19)
        assertEquals("Observed Friday, June 18", Festivals.bannerFor(Festivals.activeOn(j, none)!!, j, none, Locale.US).observedLine)
    }

    @Test
    fun `Christmas and New Year, which already exist as festivals, also get the Observed line on a weekend`() {
        val xmas = d(2027, 12, 25) // Saturday
        assertEquals("Observed Friday, December 24", Festivals.bannerFor(Festivals.activeOn(xmas, none)!!, xmas, none, Locale.US).observedLine)
        val ny = d(2028, 1, 1) // Saturday
        assertEquals("Observed Friday, December 31", Festivals.bannerFor(Festivals.activeOn(ny, none)!!, ny, none, Locale.US).observedLine)
    }

    @Test
    fun `a holiday banner runs from 3 days before through the day and not after`() {
        assertNull(activeId(d(2026, 9, 3)))
        assertEquals("us_labor", activeId(d(2026, 9, 4)))
        assertEquals("us_labor", activeId(d(2026, 9, 7)))
        assertNull(activeId(d(2026, 9, 8)))
    }

    @Test
    fun `the countdown text reuses the festival banner format`() {
        val day = d(2026, 9, 4)
        val b = Festivals.bannerFor(Festivals.activeOn(day, none)!!, day, none, Locale.US)
        assertEquals("Monday, September 7, 2026  ·  in 3 days", b.dateLine)
    }

    @Test
    fun `Independence Day uses the fireworks scene in its red white and blue variant, other holidays have no wallpaper`() {
        val i = f("us_independence")
        assertEquals("new_year", i.sceneId)
        assertEquals(1, i.variant)
        assertNotNull(WallpaperSchedule.festivalSceneKey(i.sceneId, i.variant))
        assertTrue(WallpaperSchedule.festivalSceneKey(i.sceneId, i.variant) != WallpaperSchedule.festivalSceneKey("new_year", 0))
        for (id in listOf("us_mlk", "us_presidents", "us_memorial", "us_juneteenth", "us_labor", "us_columbus", "us_veterans")) {
            assertNull(id, WallpaperSchedule.festivalSceneKey(f(id).sceneId, f(id).variant))
        }
    }

    @Test
    fun `a festival's main day beats a holiday's main day on the same date`() {
        val dates = mapOf("makar_sankranti" to listOf(FestivalOccurrence(d(2026, 7, 4), d(2026, 7, 4))))
        assertEquals("makar_sankranti", activeId(d(2026, 7, 4), dates))
    }

    @Test
    fun `any main day beats a window day, so a holiday's day wins over a festival's lead-up and vice versa`() {
        // Festival main day 2026-07-06 has a 1-day lead-up (Jul 5). Independence Day (Jul 4, Sat) window covers Jul 1-4.
        val dates = mapOf("makar_sankranti" to listOf(FestivalOccurrence(d(2026, 7, 5), d(2026, 7, 5))))
        // Jul 4 is the holiday's main day and the festival's lead-up day: the holiday wins.
        assertEquals("us_independence", activeId(d(2026, 7, 4), dates))
        // Jul 5 is the festival's main day and nothing else: festival.
        assertEquals("makar_sankranti", activeId(d(2026, 7, 5), dates))
        // A holiday's lead-up day loses to a festival's main day: Veterans window Nov 8-11; make a festival main day on Nov 9.
        val nov = mapOf("makar_sankranti" to listOf(FestivalOccurrence(d(2026, 11, 9), d(2026, 11, 9))))
        assertEquals("makar_sankranti", activeId(d(2026, 11, 9), nov))
    }

    @Test
    fun `existing festival dates keep their winners with holidays added, Diwali 2026 and Thanksgiving 2026`() {
        assertEquals("diwali", Festivals.activeOn(d(2026, 11, 8), festivalDates)?.festival?.id)
        assertEquals("thanksgiving", Festivals.activeOn(d(2026, 11, 26), festivalDates)?.festival?.id)
    }

    @Test
    fun `the US holidays toggle removes federal holidays but keeps Christmas, Thanksgiving and New Year`() {
        val off = Festivals.enabled(false)
        assertFalse(off.any { it.holiday })
        assertTrue(off.any { it.id == "christmas" } && off.any { it.id == "thanksgiving" } && off.any { it.id == "new_year" })
        assertNull(activeId(d(2026, 9, 7), festivals = off))
        assertEquals("us_labor", activeId(d(2026, 9, 7), festivals = Festivals.enabled(true)))
        assertEquals("christmas", activeId(d(2026, 12, 25), festivals = off))
    }

    @Test
    fun `holiday display names match the spec`() {
        assertEquals("Columbus Day / Indigenous Peoples' Day", f("us_columbus").displayName)
        assertEquals("Presidents' Day", f("us_presidents").displayName)
        assertEquals("Independence Day", f("us_independence").displayName)
        assertEquals("Martin Luther King Jr. Day", f("us_mlk").displayName)
    }
}
