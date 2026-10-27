package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleAndFetchPlanTest {
    @Test
    fun `no input for 45 seconds puts the UI to sleep, 44 seconds does not`() {
        val t = IdleTracker(startMs = 0)
        assertFalse(t.tick(44_999))
        assertTrue(t.tick(45_000))
        assertTrue(t.idle)
    }

    @Test
    fun `any key restarts the countdown so an active user never sees the UI disappear`() {
        val t = IdleTracker(startMs = 0)
        assertFalse(t.onKey(30_000))
        assertFalse(t.tick(60_000)) // only 30s since the key
        assertTrue(t.tick(75_000))
    }

    @Test
    fun `the first key after idle only wakes the UI and is consumed, so it cannot trigger an invisible action`() {
        val t = IdleTracker(startMs = 0)
        t.tick(50_000)
        assertTrue("first key is consumed", t.onKey(51_000))
        assertFalse(t.idle)
        assertFalse("the next key acts normally", t.onKey(52_000))
    }

    @Test
    fun `a disabled tracker never sleeps and schedules nothing`() {
        val t = IdleTracker(enabled = false, startMs = 0)
        assertFalse(t.tick(10_000_000))
        assertNull(t.untilIdleMs(0))
    }

    @Test
    fun `the remaining time to idle shrinks as time passes so the timer can be scheduled exactly`() {
        val t = IdleTracker(startMs = 1_000)
        assertEquals(45_000L, t.untilIdleMs(1_000))
        assertEquals(15_000L, t.untilIdleMs(31_000))
    }

    @Test
    fun `fresh weather with a missing air quality cache still schedules the air quality fetch`() {
        val plan = Weather.fetchPlan(weatherAgeMs = 5 * 60_000L, airAgeMs = null)
        assertFalse(plan.weather)
        assertTrue(plan.air)
    }

    @Test
    fun `each cache goes stale on its own at 15 minutes`() {
        assertEquals(Weather.FetchPlan(weather = true, air = false), Weather.fetchPlan(16 * 60_000L, 2 * 60_000L))
        assertEquals(Weather.FetchPlan(weather = false, air = false), Weather.fetchPlan(1_000L, 1_000L))
        assertEquals(Weather.FetchPlan(weather = true, air = true), Weather.fetchPlan(null, null))
    }

    @Test
    fun `the next periodic fetch waits for the earlier cache to go stale`() {
        assertEquals(10 * 60_000L, Weather.nextTickDelayMs(5 * 60_000L, 8 * 60_000L).let { Weather.REFRESH_MS - 5 * 60_000L })
        assertEquals(Weather.REFRESH_MS - 8 * 60_000L, Weather.nextTickDelayMs(5 * 60_000L, 8 * 60_000L).let { Weather.REFRESH_MS - 8 * 60_000L })
        assertEquals(Weather.REFRESH_MS, Weather.nextTickDelayMs(null, 1_000L))
    }
}
