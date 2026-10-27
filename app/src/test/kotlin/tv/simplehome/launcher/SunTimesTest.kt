package tv.simplehome.launcher

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SunTimesTest {
    private val chicago = ZoneId.of("America/Chicago")
    private val hLat = 29.7604
    private val hLon = -95.3698
    private val tromsoZone = ZoneId.of("Europe/Oslo")

    private fun houston(date: LocalDate) = normal(SunTimes.forDate(date, hLat, hLon, chicago))

    private fun normal(day: SunDay): SunDay.Normal =
        day as? SunDay.Normal ?: error("expected Normal but was $day")

    private fun assertWithin(expected: String, actual: ZonedDateTime, label: String) {
        val exp = actual.toLocalDate().atTime(LocalTime.parse(expected))
        val diff = abs(Duration.between(exp, actual.toLocalDateTime()).toMinutes())
        if (diff > 3) fail("$label: expected $expected +-3 min but was ${actual.toLocalTime()}")
    }

    private fun abs(n: Long) = if (n < 0) -n else n
    private fun hours(a: ZonedDateTime, b: ZonedDateTime) = Duration.between(a, b).toMinutes() / 60.0

    // Reference values below were computed with an independent Python implementation of the
    // NOAA solar-calculator equations (zenith 90.833, equation of time, refined at event time).
    // They match published almanac values for Houston to within a minute.

    @Test fun `houston summer solstice matches NOAA reference so the launcher flips theme at the right time`() {
        val d = houston(LocalDate.of(2026, 6, 21))
        assertWithin("06:22:23", d.sunrise, "sunrise")
        assertWithin("20:25:41", d.sunset, "sunset")
    }

    @Test fun `houston winter solstice matches NOAA reference in standard time`() {
        val d = houston(LocalDate.of(2026, 12, 21))
        assertWithin("07:13:13", d.sunrise, "sunrise")
        assertWithin("17:27:31", d.sunset, "sunset")
    }

    @Test fun `summer days are longer and end later than winter days in the northern hemisphere`() {
        val jun = houston(LocalDate.of(2026, 6, 21))
        val dec = houston(LocalDate.of(2026, 12, 21))
        assertTrue(jun.sunset.toLocalTime().isAfter(dec.sunset.toLocalTime()))
        assertTrue(hours(jun.sunrise, jun.sunset) > 13.5)
        assertTrue(hours(dec.sunrise, dec.sunset) < 10.5)
    }

    @Test fun `tromso polar night yields AlwaysNight and isDay agrees`() {
        val noon = ZonedDateTime.of(2026, 12, 21, 12, 0, 0, 0, tromsoZone)
        assertEquals(SunDay.AlwaysNight, SunTimes.forDate(noon.toLocalDate(), 69.6492, 18.9553, tromsoZone))
        assertFalse(SunTimes.isDay(noon, 69.6492, 18.9553))
        assertNull(SunTimes.nextTransition(noon, 69.6492, 18.9553))
    }

    @Test fun `tromso midnight sun yields AlwaysDay and isDay agrees even at midnight`() {
        val midnight = ZonedDateTime.of(2026, 6, 21, 0, 30, 0, 0, tromsoZone)
        assertEquals(SunDay.AlwaysDay, SunTimes.forDate(midnight.toLocalDate(), 69.6492, 18.9553, tromsoZone))
        assertTrue(SunTimes.isDay(midnight, 69.6492, 18.9553))
        assertNull(SunTimes.nextTransition(midnight, 69.6492, 18.9553))
    }

    @Test fun `isDay boundaries are inclusive at sunrise and exclusive at sunset`() {
        val d = houston(LocalDate.of(2026, 10, 26))
        assertTrue(SunTimes.isDay(d.sunrise, hLat, hLon))
        assertFalse(SunTimes.isDay(d.sunrise.minusSeconds(1), hLat, hLon))
        assertTrue(SunTimes.isDay(d.sunset.minusSeconds(1), hLat, hLon))
        assertFalse(SunTimes.isDay(d.sunset, hLat, hLon))
    }

    @Test fun `DST transition days still produce sane same-date times`() {
        for (date in listOf(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 11, 1))) {
            val d = houston(date)
            assertTrue("$date order", d.sunrise.isBefore(d.sunset))
            assertEquals(date, d.sunrise.toLocalDate())
            assertEquals(date, d.sunset.toLocalDate())
            val len = hours(d.sunrise, d.sunset)
            assertTrue("$date length $len", len in 10.0..13.0)
        }
    }

    @Test fun `DST day uses the post-change offset so clock times match reference`() {
        // 2026-03-08 is spring-forward; sunrise must read CDT (UTC-5), not CST.
        val d = houston(LocalDate.of(2026, 3, 8))
        assertWithin("07:39:56", d.sunrise, "sunrise")
        assertWithin("19:26:10", d.sunset, "sunset")
    }

    @Test fun `nextTransition from midday is today's sunset`() {
        val d = houston(LocalDate.of(2026, 10, 26))
        val now = ZonedDateTime.of(2026, 10, 26, 12, 0, 0, 0, chicago)
        val next = assertNotNull(SunTimes.nextTransition(now, hLat, hLon))
        assertEquals(d.sunset.toInstant(), next.toInstant())
        assertTrue(next.isAfter(now))
    }

    @Test fun `nextTransition after sunset is tomorrow's sunrise`() {
        val tomorrow = houston(LocalDate.of(2026, 10, 27))
        val now = ZonedDateTime.of(2026, 10, 26, 23, 0, 0, 0, chicago)
        val next = assertNotNull(SunTimes.nextTransition(now, hLat, hLon))
        assertEquals(tomorrow.sunrise.toInstant(), next.toInstant())
    }

    @Test fun `nextTransition is strictly after now even exactly at an event`() {
        val d = houston(LocalDate.of(2026, 10, 26))
        val next = assertNotNull(SunTimes.nextTransition(d.sunset, hLat, hLon))
        assertTrue(next.isAfter(d.sunset))
    }

    @Test fun `sydney summer day is longer than winter day in the southern hemisphere`() {
        val z = ZoneId.of("Australia/Sydney")
        val dec = normal(SunTimes.forDate(LocalDate.of(2026, 12, 21), -33.8688, 151.2093, z))
        val jun = normal(SunTimes.forDate(LocalDate.of(2026, 6, 21), -33.8688, 151.2093, z))
        assertTrue(hours(dec.sunrise, dec.sunset) > hours(jun.sunrise, jun.sunset) + 3)
    }

    private fun <T : Any> assertNotNull(v: T?): T {
        assertNotNull("expected non-null", v)
        return v!!
    }
}
