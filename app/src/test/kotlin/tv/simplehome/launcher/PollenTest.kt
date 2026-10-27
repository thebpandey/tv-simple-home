package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tv.simplehome.launcher.PollenResult.Failure
import tv.simplehome.launcher.PollenResult.Success
import java.io.File

class PollenTest {
    @get:Rule val tmp = TemporaryFolder()

    private val hour = 60L * 60 * 1000

    private fun type(code: String, value: String?) =
        if (value == null) """{"code":"$code","displayName":"x","inSeason":false}"""
        else """{"code":"$code","displayName":"x","inSeason":true,"indexInfo":{"code":"UPI","value":$value,"category":"x"}}"""

    private fun body(vararg types: String) =
        """{"regionCode":"US","dailyInfo":[{"date":{"year":2026,"month":10,"day":27},"pollenTypeInfo":[${types.joinToString(",")}]}]}"""

    private fun reason(r: PollenResult) = (r as Failure).reason

    @Test
    fun `out-of-season types have no index and count as zero, so autumn shows only the weed that is actually up`() {
        val r = PollenRules.parse(body(type("GRASS", "1"), type("TREE", null), type("WEED", "3")))
        val d = (r as Success).data
        assertEquals(listOf(PollenLevel(PollenKind.GRASS, 1), PollenLevel(PollenKind.TREE, 0), PollenLevel(PollenKind.WEED, 3)), d.levels)
        assertEquals("Pollen · Weed Moderate", PollenRules.line(d))
        assertEquals(3, PollenRules.peak(d))
    }

    @Test
    fun `the live Houston response of 2026-10-27 parses, including out-of-season types that still carry an index`() {
        val live = """{"regionCode":"US","dailyInfo":[{"date":{"year":2026,"month":10,"day":27},"pollenTypeInfo":[""" +
            """{"code":"GRASS","displayName":"Grass","inSeason":false,"indexInfo":{"code":"UPI","displayName":"Universal Pollen Index",""" +
            """"value":1,"category":"Very Low","indexDescription":"x","color":{"green":0.61960787,"blue":0.22745098}}},""" +
            """{"code":"TREE","displayName":"Tree","inSeason":false,"indexInfo":{"code":"UPI","value":1,"category":"Very Low"}},""" +
            """{"code":"WEED","displayName":"Weed","inSeason":true,"indexInfo":{"code":"UPI","value":1,"category":"Very Low"},""" +
            """"healthRecommendations":["x"]}]}]}"""
        val d = (PollenRules.parse(live) as Success).data
        assertEquals("Pollen · Very Low", PollenRules.line(d)) // all three tie, so no type is singled out
    }

    @Test
    fun `a replaced key retries at once instead of waiting out the hourly backoff from the old key's failure`() {
        assertFalse(PollenRules.isDue(null, 5 * 60 * 1000L, keyChanged = false))
        assertTrue(PollenRules.isDue(null, 5 * 60 * 1000L, keyChanged = true))
        assertFalse(PollenRules.isDue(hour, 5 * 60 * 1000L, keyChanged = true)) // a fresh forecast is not refetched just for a new key
    }

    @Test
    fun `types tied at the peak are all named, in tree, grass, weed order, so the line does not reshuffle between refreshes`() {
        val d = (PollenRules.parse(body(type("WEED", "4"), type("TREE", "4"), type("GRASS", "2"))) as Success).data
        assertEquals("Pollen · Tree, Weed High", PollenRules.line(d))
    }

    @Test
    fun `the longest possible line stays shorter than the air-quality line, so the card never grows over the clock`() {
        val worst = PollenData(listOf(PollenLevel(PollenKind.TREE, 5), PollenLevel(PollenKind.GRASS, 5), PollenLevel(PollenKind.WEED, 4)))
        assertEquals("Pollen · Tree, Grass Very High", PollenRules.line(worst))
        assertTrue(PollenRules.line(worst).length <= AirQualityRules.line(AirQuality(54, 15, 0)).length)
    }

    @Test
    fun `a day with nothing in the air says None instead of hiding, which is useful news for allergy sufferers`() {
        val d = (PollenRules.parse(body(type("GRASS", "0"), type("TREE", null), type("WEED", null))) as Success).data
        assertEquals("Pollen · None", PollenRules.line(d))
    }

    @Test
    fun `no pollen types at all (unsupported location) is a failure, so the line hides rather than claiming None`() {
        assertEquals(FailureReason.MISSING_FIELD, reason(PollenRules.parse("""{"dailyInfo":[{"date":{}}]}""")))
        assertEquals(FailureReason.MISSING_FIELD, reason(PollenRules.parse(body())))
        assertEquals(FailureReason.MISSING_FIELD, reason(PollenRules.parse(body(type("MOSS", "3")))))
        assertEquals(FailureReason.MISSING_FIELD, reason(PollenRules.parse("""{"dailyInfo":[]}""")))
    }

    @Test
    fun `malformed and out-of-range values are rejected so a bad response never shows a made-up level`() {
        assertEquals(FailureReason.EMPTY_BODY, reason(PollenRules.parse("")))
        assertEquals(FailureReason.BAD_JSON, reason(PollenRules.parse("<html>")))
        assertEquals(FailureReason.MISSING_FIELD, reason(PollenRules.parse("{}")))
        assertEquals(FailureReason.WRONG_TYPE, reason(PollenRules.parse("""{"dailyInfo":{}}""")))
        assertEquals(FailureReason.WRONG_TYPE, reason(PollenRules.parse(body(type("TREE", "\"3\"")))))
        assertEquals(FailureReason.WRONG_TYPE, reason(PollenRules.parse(body(type("TREE", "2.5")))))
        assertEquals(FailureReason.OUT_OF_RANGE, reason(PollenRules.parse(body(type("TREE", "6")))))
        assertEquals(FailureReason.OUT_OF_RANGE, reason(PollenRules.parse(body(type("TREE", "-1")))))
    }

    @Test
    fun `an error body from Google (API disabled, bad key) is a failure, not an empty forecast`() {
        val err = """{"error":{"code":403,"message":"Pollen API has not been used","status":"PERMISSION_DENIED"}}"""
        assertEquals(FailureReason.MISSING_FIELD, reason(PollenRules.parse(err)))
    }

    @Test
    fun `dot colour follows the shared scale, green through Low, then yellow, orange, red`() {
        assertEquals(AqiCategory.GOOD, PollenRules.dotCategory(0))
        assertEquals(AqiCategory.GOOD, PollenRules.dotCategory(2))
        assertEquals(AqiCategory.MODERATE, PollenRules.dotCategory(3))
        assertEquals(AqiCategory.SENSITIVE, PollenRules.dotCategory(4))
        assertEquals(AqiCategory.UNHEALTHY, PollenRules.dotCategory(5))
    }

    @Test
    fun `refresh is every 6 hours, not every 15 minutes, because the API is billed per request`() {
        assertTrue(PollenRules.needsFetch(null))
        assertFalse(PollenRules.needsFetch(15 * 60 * 1000L))
        assertFalse(PollenRules.needsFetch(6 * hour - 1))
        assertTrue(PollenRules.needsFetch(6 * hour))
        assertTrue(PollenRules.needsFetch(-1)) // clock moved back: refresh rather than trust it
    }

    @Test
    fun `a failing request is retried at most hourly, so a disabled API is not hit on every weather tick`() {
        assertTrue(PollenRules.isDue(null, null))
        assertFalse(PollenRules.isDue(null, 15 * 60 * 1000L))
        assertTrue(PollenRules.isDue(null, hour))
        assertFalse(PollenRules.isDue(hour, null)) // fresh cache wins regardless of attempts
        assertTrue(PollenRules.isDue(null, -5))
    }

    @Test
    fun `a forecast older than 12 hours is hidden, so yesterday's levels never pass for today's`() {
        assertFalse(PollenRules.shouldShow(null))
        assertTrue(PollenRules.shouldShow(11 * hour))
        assertFalse(PollenRules.shouldShow(12 * hour))
        assertFalse(PollenRules.shouldShow(-1))
    }

    @Test
    fun `the key is read from the device file and trimmed, and anything not shaped like a Google key is never sent`() {
        val dir = tmp.newFolder()
        assertNull(PollenRules.loadKey(dir)) // no file: pollen simply stays off
        assertNull(PollenRules.loadKey(null))
        val key = "AIza" + "A".repeat(35)
        File(dir, PollenRules.KEY_FILE).writeText("$key\n")
        assertEquals(key, PollenRules.loadKey(dir))
        assertNull(PollenRules.validKey("AIza\r\nX-Evil: 1" + "A".repeat(30))) // header injection
        assertNull(PollenRules.validKey("short"))
        assertNull(PollenRules.validKey("A".repeat(65)))
        File(dir, PollenRules.KEY_FILE).writeText("x".repeat(300))
        assertNull(PollenRules.loadKey(dir))
    }

    @Test
    fun `the key never appears in the URL, only the coordinates and fixed options`() {
        val url = PollenRules.buildUrl(29.76, -95.37)
        assertFalse(url.contains("key="))
        assertTrue(url.startsWith("https://pollen.googleapis.com/v1/forecast:lookup?"))
        assertTrue(url.contains("location.latitude=29.76&location.longitude=-95.37"))
        assertTrue(url.contains("days=1"))
    }
}
