package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class FestivalsTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private fun id(date: LocalDate, dates: Map<String, List<FestivalOccurrence>> = festivalDates) = Festivals.activeOn(date, dates, Festivals.enabled(false))?.festival?.id // festival rules only; holidays have HolidaysTest

    private val none = emptyMap<String, List<FestivalOccurrence>>()
    private fun occ(y: Int, m: Int, day: Int, im: Int = m, iday: Int = day) = FestivalOccurrence(d(y, m, day), d(y, im, iday))
    private fun banner(date: LocalDate, dates: Map<String, List<FestivalOccurrence>> = festivalDates) =
        Festivals.bannerFor(Festivals.activeOn(date, dates)!!, date, dates, Locale.US)

    @Test
    fun `Thanksgiving is the fourth Thursday across several years`() {
        assertEquals(d(2023, 11, 23), Festivals.thanksgiving(2023))
        assertEquals(d(2024, 11, 28), Festivals.thanksgiving(2024))
        assertEquals(d(2026, 11, 26), Festivals.thanksgiving(2026))
    }

    @Test
    fun `a November starting on a Thursday still gives the 22nd because the 1st is the first Thursday not the fourth`() {
        assertEquals(java.time.DayOfWeek.THURSDAY, d(2018, 11, 1).dayOfWeek)
        assertEquals(d(2018, 11, 22), Festivals.thanksgiving(2018))
    }

    @Test
    fun `a November starting on a Friday gives the 28th because the first Thursday is the 7th`() {
        assertEquals(java.time.DayOfWeek.FRIDAY, d(2019, 11, 1).dayOfWeek)
        assertEquals(d(2019, 11, 28), Festivals.thanksgiving(2019))
    }

    @Test
    fun `Thanksgiving is always a Thursday between the 22nd and the 28th`() {
        for (y in 2000..2100) {
            val t = Festivals.thanksgiving(y)
            assertEquals(java.time.DayOfWeek.THURSDAY, t.dayOfWeek)
            assertTrue(t.dayOfMonth in 22..28)
        }
    }

    @Test
    fun `Halloween window includes the week before and ends after the day itself`() {
        assertNull(id(d(2026, 10, 23), none))
        assertEquals("halloween", id(d(2026, 10, 24), none))
        assertEquals("halloween", id(d(2026, 10, 31), none))
        assertNull(id(d(2026, 11, 1), none))
    }

    @Test
    fun `Christmas window runs a week before to the day after`() {
        assertNull(id(d(2026, 12, 17), none))
        assertEquals("christmas", id(d(2026, 12, 18), none))
        assertEquals("christmas", id(d(2026, 12, 26), none))
        assertNull(id(d(2026, 12, 27), none))
    }

    @Test
    fun `New Year covers Dec 31 and Jan 1 even though they are in different years`() {
        assertEquals("new_year", id(d(2026, 12, 31), none))
        assertEquals("new_year", id(d(2027, 1, 1), none))
        assertNull(id(d(2027, 1, 2), none))
        assertNull(id(d(2026, 12, 30), none))
    }

    @Test
    fun `the Thanksgiving window is the three days before it so the scene is not shown for weeks`() {
        assertEquals("thanksgiving", id(d(2026, 11, 23), none))
        assertEquals("thanksgiving", id(d(2026, 11, 26), none))
        assertNull(id(d(2026, 11, 22), none))
        assertNull(id(d(2026, 11, 27), none))
    }

    @Test
    fun `without lunar data only Western festivals can appear so an unfilled year never shows a false festival`() {
        assertNull(id(d(2026, 11, 8), none))
        assertNull(id(d(2026, 3, 4), none))
    }

    @Test
    fun `past 2028 lunar festivals simply do not appear while Western ones keep working`() {
        assertNull(id(d(2029, 3, 10)))
        assertNull(id(d(2030, 11, 8)))
        assertEquals("christmas", id(d(2030, 12, 25)))
    }

    @Test
    fun `every lunar festival and helper entry has all three years of verified data`() {
        val keys = listOf(
            "lohri", "makar_sankranti", "vasant_panchami", "maha_shivaratri", "holika_dahan", "holi", "chaitra_navratri",
            "ram_navami", "hanuman_jayanti", "raksha_bandhan", "janmashtami", "ganesh_chaturthi", "sharad_navratri",
            "durga_ashtami", "dussehra", "karwa_chauth", "dhanteras", "diwali", "govardhan_puja", "bhai_dooj", "chhath",
        )
        for (k in keys) {
            val list = festivalDates[k]
            assertNotNull("missing $k", list)
            assertEquals("$k years", listOf(2026, 2027, 2028), list!!.map { it.primary.year })
            // India is never more than a day away from Houston; a bigger gap means a typo.
            for (o in list) assertTrue("$k $o", kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(o.primary, o.india)) <= 1)
        }
    }

    @Test
    fun `the window of every listed occurrence contains its own main day or it could never activate`() {
        for (f in Festivals.ALL) for (o in festivalDates[f.id].orEmpty()) {
            val (start, end) = Festivals.windowFor(f, o)
            assertTrue("${f.id} $o", !o.primary.isBefore(start) && !o.primary.isAfter(end))
        }
    }

    @Test
    fun `every festival maps to a scene that exists so a data entry can never select a missing wallpaper`() {
        for (f in Festivals.ALL.filter { !it.holiday || it.sceneId.isNotEmpty() }) assertNotNull("no scene for ${f.id}", WallpaperSchedule.festivalSceneKey(f.sceneId, f.variant))
    }

    @Test
    fun `Dussehra wins on its own day because the main day beats the longer Navratri window`() {
        assertEquals("dussehra", id(d(2026, 10, 20)))
    }

    @Test
    fun `Durga Ashtami is its own main day inside the Navratri window`() {
        assertEquals("durga_ashtami", id(d(2026, 10, 18)))
    }

    @Test
    fun `inside the Navratri window but not on a main day the shorter window wins, so Dussehra eve shows Dussehra`() {
        assertEquals("dussehra", id(d(2026, 10, 19)))
        assertEquals("sharad_navratri", id(d(2026, 10, 14)))
    }

    @Test
    fun `Sharad Navratri runs from day one through Dussehra and not a day longer`() {
        assertNull(id(d(2026, 10, 10)))
        assertEquals("sharad_navratri", id(d(2026, 10, 11)))
        assertNull(id(d(2026, 10, 21)))
    }

    @Test
    fun `Diwali spans Houston Dhanteras through Houston Bhai Dooj`() {
        assertNull(id(d(2026, 11, 5)))
        assertEquals("diwali", id(d(2026, 11, 6)))
        assertEquals("diwali", id(d(2026, 11, 10)))
        assertNull(id(d(2026, 11, 11)))
    }

    @Test
    fun `Diwali window length follows the data, so 2027 starting a day earlier than a fixed offset would is honoured`() {
        assertEquals("diwali", id(d(2027, 10, 26)))
        assertEquals("diwali", id(d(2027, 10, 30)))
        assertEquals("halloween", id(d(2027, 10, 25))) // the Diwali window has not started; only the Halloween week is active
    }

    @Test
    fun `Holi window starts on Holika Dahan and ends on Holi`() {
        assertNull(id(d(2026, 3, 1)))
        assertEquals("holi", id(d(2026, 3, 2)))
        assertEquals("holi", id(d(2026, 3, 3)))
        assertNull(id(d(2026, 3, 4)))
    }

    @Test
    fun `a window that starts in the previous year is still found on Dec 31`() {
        val fake = mapOf("holi" to listOf(occ(2032, 1, 1)))
        val active = Festivals.activeOn(d(2031, 12, 31), fake)
        assertNotNull(active)
        assertEquals(d(2032, 1, 1), active!!.mainDay)
    }

    @Test
    fun `banner shows Today on the day and Tomorrow the day before`() {
        assertEquals("Today", Festivals.relativeText(d(2026, 11, 8), d(2026, 11, 8)))
        assertEquals("Tomorrow", Festivals.relativeText(d(2026, 11, 7), d(2026, 11, 8)))
    }

    @Test
    fun `banner counts days before the main day and says nothing after it`() {
        assertEquals("in 5 days", Festivals.relativeText(d(2026, 11, 3), d(2026, 11, 8)))
        assertNull(Festivals.relativeText(d(2026, 11, 9), d(2026, 11, 8)))
    }

    @Test
    fun `banner date line is spelled out in full so it is readable from the couch`() {
        assertEquals("Sunday, November 8, 2026", Festivals.dateText(d(2026, 11, 8), Locale.US))
        assertEquals("Sunday, November 8, 2026  \u00B7  in 5 days", Festivals.bannerDateLine(d(2026, 11, 3), d(2026, 11, 8), Locale.US))
        assertEquals("Sunday, November 8, 2026", Festivals.bannerDateLine(d(2026, 11, 9), d(2026, 11, 8), Locale.US))
    }

    @Test
    fun `the India line appears when New Delhi's date differs, as for Holi 2026`() {
        val b = banner(d(2026, 3, 2))
        assertEquals("Holi", b.title)
        assertEquals("Tuesday, March 3, 2026  \u00B7  Tomorrow", b.dateLine)
        assertEquals("India: Wednesday, March 4", b.indiaLine)
    }

    @Test
    fun `the India line is absent when both cities agree, as for Diwali 2026`() {
        assertNull(banner(d(2026, 11, 8)).indiaLine)
        assertNull(banner(d(2026, 11, 7)).indiaLine)
    }

    @Test
    fun `Western festivals never show an India line because the date is the same everywhere`() {
        assertNull(banner(d(2026, 12, 25), none).indiaLine)
    }

    @Test
    fun `inside the Diwali window the banner names the specific day when it is one`() {
        assertEquals("Dhanteras", banner(d(2026, 11, 6)).title)
        assertEquals("Diwali", banner(d(2026, 11, 8)).title)
        assertEquals("Govardhan Puja", banner(d(2026, 11, 9)).title)
        assertEquals("Bhai Dooj", banner(d(2026, 11, 10)).title)
    }

    @Test
    fun `on an unnamed day inside the Diwali window the banner counts down to Diwali`() {
        val b = banner(d(2026, 11, 7))
        assertEquals("Diwali", b.title)
        assertEquals("Sunday, November 8, 2026  \u00B7  Tomorrow", b.dateLine)
    }

    @Test
    fun `a named Diwali day shows its own India date when it differs`() {
        assertEquals("India: Tuesday, November 10", banner(d(2026, 11, 9)).indiaLine)
        assertEquals("India: Wednesday, November 11", banner(d(2026, 11, 10)).indiaLine)
    }

    @Test
    fun `a festival overrides Auto rotation so the season is visible`() {
        val key = WallpaperSchedule.festivalSceneKey("diwali", 0)
        assertEquals(key, WallpaperSchedule.keyAt(WallpaperMode.Auto, 0, key))
    }

    @Test
    fun `a pinned scene is not overridden by a festival because the user chose it explicitly`() {
        val key = WallpaperSchedule.festivalSceneKey("diwali", 0)
        assertEquals(2 * WallpaperSchedule.KEY_MULT, WallpaperSchedule.keyAt(WallpaperMode.Pinned(2), 0, key))
    }

    @Test
    fun `Off stays off during a festival so the idle screen still costs nothing`() {
        assertNull(WallpaperSchedule.keyAt(WallpaperMode.Off, 0, WallpaperSchedule.festivalSceneKey("holi", 0)))
    }

    @Test
    fun `without a festival Auto falls back to the 30 minute rotation`() {
        val now = 3 * WallpaperSchedule.PERIOD_MS
        assertEquals(WallpaperSchedule.sceneAt(WallpaperMode.Auto, now)!! * WallpaperSchedule.KEY_MULT, WallpaperSchedule.keyAt(WallpaperMode.Auto, now, null))
    }

    @Test
    fun `festival keys never collide with regular scene keys`() {
        val regular = (0 until WallpaperSchedule.SCENE_COUNT).map { it * WallpaperSchedule.KEY_MULT }
        for (f in Festivals.ALL.filter { it.sceneId.isNotEmpty() }) assertTrue(WallpaperSchedule.festivalSceneKey(f.sceneId, f.variant)!! !in regular)
    }
}
