package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class WorldCitiesTest {
    @Test
    fun `every city maps to a real time zone so picking a city always sets the clock correctly`() {
        for (c in WorldCities.ALL) ZoneId.of(c.zone) // throws on a typo
    }

    @Test
    fun `existing installs keep New Delhi until the user picks another city`() {
        assertEquals("Asia/Kolkata", WorldCities.resolve(null).zone)
        assertEquals("New Delhi", WorldCities.resolve("removed_city").name)
    }

    @Test
    fun `a saved city comes back with its own zone`() {
        assertEquals("Asia/Tokyo", WorldCities.resolve("tokyo").zone)
        assertEquals("America/Chicago", WorldCities.resolve("houston").zone)
    }

    @Test
    fun `ids are unique so a saved choice can never resolve to the wrong city`() {
        assertEquals(WorldCities.ALL.size, WorldCities.ALL.map { it.id }.toSet().size)
    }

    @Test
    fun `names are short enough to fit beside the clock and every city sits in a listed region`() {
        for (c in WorldCities.ALL) {
            assertTrue(c.name, c.name.length <= WorldCities.MAX_NAME_CHARS)
            assertTrue(c.region, c.region in WorldCities.REGIONS)
        }
    }
}
