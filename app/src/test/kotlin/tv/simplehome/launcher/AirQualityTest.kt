package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.simplehome.launcher.AirQualityResult.Failure
import tv.simplehome.launcher.AirQualityResult.Success

class AirQualityTest {
    private fun body(aqi: String = "60", pm: String = "11.6", dust: String = "0.0") =
        """{"current":{"us_aqi":$aqi,"pm2_5":$pm,"dust":$dust}}"""

    private fun reason(r: AirQualityResult) = (r as Failure).reason

    @Test
    fun `category boundaries follow the EPA table exactly, 50 vs 51`() {
        assertEquals(AqiCategory.GOOD, AirQualityRules.category(0))
        assertEquals(AqiCategory.GOOD, AirQualityRules.category(50))
        assertEquals(AqiCategory.MODERATE, AirQualityRules.category(51))
    }

    @Test
    fun `category boundaries 100 vs 101 and 150 vs 151`() {
        assertEquals(AqiCategory.MODERATE, AirQualityRules.category(100))
        assertEquals(AqiCategory.SENSITIVE, AirQualityRules.category(101))
        assertEquals(AqiCategory.SENSITIVE, AirQualityRules.category(150))
        assertEquals(AqiCategory.UNHEALTHY, AirQualityRules.category(151))
    }

    @Test
    fun `category boundaries 200 vs 201 and 300 vs 301`() {
        assertEquals(AqiCategory.UNHEALTHY, AirQualityRules.category(200))
        assertEquals(AqiCategory.VERY_UNHEALTHY, AirQualityRules.category(201))
        assertEquals(AqiCategory.VERY_UNHEALTHY, AirQualityRules.category(300))
        assertEquals(AqiCategory.HAZARDOUS, AirQualityRules.category(301))
        assertEquals(AqiCategory.HAZARDOUS, AirQualityRules.category(500))
    }

    @Test
    fun `the live Houston reading parses and rounds PM2_5 and dust to whole numbers for the compact line`() {
        val r = AirQualityRules.parse(body())
        assertEquals(Success(AirQuality(60, 12, 0)), r)
        assertEquals("AQI 60 · Moderate · PM2.5 12 · Dust 0", AirQualityRules.line((r as Success).data))
    }

    @Test
    fun `Unhealthy for Sensitive Groups is shortened to Sensitive so the line stays compact`() {
        assertTrue(AirQualityRules.line(AirQuality(120, 40, 5)).contains("Sensitive"))
    }

    @Test
    fun `empty, non-JSON and missing sections are failures so a bad response never shows a made-up reading`() {
        assertEquals(FailureReason.EMPTY_BODY, reason(AirQualityRules.parse("")))
        assertEquals(FailureReason.BAD_JSON, reason(AirQualityRules.parse("<html>")))
        assertEquals(FailureReason.MISSING_FIELD, reason(AirQualityRules.parse("{}")))
        assertEquals(FailureReason.MISSING_FIELD, reason(AirQualityRules.parse("""{"current":{"pm2_5":1,"dust":1}}""")))
    }

    @Test
    fun `null or non-numeric values are rejected rather than coerced to zero`() {
        assertEquals(FailureReason.WRONG_TYPE, reason(AirQualityRules.parse(body(aqi = "null"))))
        assertEquals(FailureReason.WRONG_TYPE, reason(AirQualityRules.parse(body(aqi = "\"60\""))))
        assertEquals(FailureReason.WRONG_TYPE, reason(AirQualityRules.parse(body(pm = "null"))))
        assertEquals(FailureReason.WRONG_TYPE, reason(AirQualityRules.parse(body(dust = "true"))))
    }

    @Test
    fun `us_aqi outside 0 to 500 is a failure because the scale ends at 500`() {
        assertEquals(FailureReason.OUT_OF_RANGE, reason(AirQualityRules.parse(body(aqi = "501"))))
        assertEquals(FailureReason.OUT_OF_RANGE, reason(AirQualityRules.parse(body(aqi = "-1"))))
        assertTrue(AirQualityRules.parse(body(aqi = "500")) is Success)
        assertTrue(AirQualityRules.parse(body(aqi = "0")) is Success)
    }

    @Test
    fun `negative particulate readings are impossible and rejected`() {
        assertEquals(FailureReason.OUT_OF_RANGE, reason(AirQualityRules.parse(body(pm = "-3"))))
    }

    @Test
    fun `pollen is never requested because it is null in the US`() {
        assertFalse(AirQualityRules.buildUrl(29.7, -95.5).contains("pollen"))
        assertTrue(AirQualityRules.buildUrl(29.7, -95.5).contains("current=us_aqi,pm2_5,dust"))
    }

    @Test
    fun `dot colours are darkened by day for contrast but unchanged at night`() {
        val yellow = AqiCategory.MODERATE
        assertEquals(yellow.color, AirQualityRules.dotColor(yellow, day = false))
        assertNotEquals(yellow.color, AirQualityRules.dotColor(yellow, day = true))
        assertTrue(Contrast.luminance(AirQualityRules.dotColor(yellow, true)) < Contrast.luminance(yellow.color))
    }

    @Test
    fun `an air quality failure does not hide the weather because each cache has its own staleness`() {
        val freshWeather: Long? = 5 * 60_000L
        val missingAir: Long? = null
        val staleAir: Long? = Weather.HIDE_AFTER_MS + 1
        assertTrue(Weather.shouldShow(freshWeather))
        assertFalse(Weather.shouldShow(missingAir))
        assertFalse(Weather.shouldShow(staleAir))
    }

    @Test
    fun `stale weather hides itself while a fresh air reading stays independently valid`() {
        assertFalse(Weather.shouldShow(Weather.HIDE_AFTER_MS))
        assertTrue(Weather.shouldShow(60_000L))
    }
}
