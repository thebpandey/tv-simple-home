package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.simplehome.launcher.WeatherResult.Failure
import tv.simplehome.launcher.WeatherResult.Success

class WeatherTest {
    private fun body(temp: String = "71.6", code: String = "2", hi: String = "[80.4]", lo: String = "[60.1]") =
        """{"current":{"temperature_2m":$temp,"weather_code":$code},"daily":{"temperature_2m_max":$hi,"temperature_2m_min":$lo}}"""

    private fun reason(r: WeatherResult) = (r as Failure).reason

    @Test
    fun `valid response is rounded to whole degrees because the display has no room for decimals`() {
        assertEquals(Success(WeatherData(72, 80, 60, Condition.PARTLY_CLOUDY)), Weather.parse(body()))
    }

    @Test
    fun `integer temperatures are accepted since the API may omit the decimal`() {
        assertTrue(Weather.parse(body(temp = "70", hi = "[81]", lo = "[59]")) is Success)
    }

    @Test
    fun `empty or blank body is a failure not a crash so the cache keeps being shown`() {
        assertEquals(FailureReason.EMPTY_BODY, reason(Weather.parse("")))
        assertEquals(FailureReason.EMPTY_BODY, reason(Weather.parse("   ")))
        assertEquals(FailureReason.EMPTY_BODY, reason(Weather.parse(null)))
    }

    @Test
    fun `non JSON such as a captive portal HTML page is rejected instead of shown`() {
        assertEquals(FailureReason.BAD_JSON, reason(Weather.parse("<html>Login</html>")))
    }

    @Test
    fun `missing sections are failures so a partial response never shows zeros`() {
        assertEquals(FailureReason.MISSING_FIELD, reason(Weather.parse("{}")))
        assertEquals(FailureReason.MISSING_FIELD, reason(Weather.parse(body(hi = "[]"))))
        assertEquals(FailureReason.MISSING_FIELD, reason(Weather.parse("""{"current":{"temperature_2m":1,"weather_code":1},"daily":{}}""")))
    }

    @Test
    fun `wrong types are rejected rather than coerced so garbage cannot display as a temperature`() {
        assertEquals(FailureReason.WRONG_TYPE, reason(Weather.parse(body(temp = "\"warm\""))))
        assertEquals(FailureReason.WRONG_TYPE, reason(Weather.parse(body(temp = "null"))))
        assertEquals(FailureReason.WRONG_TYPE, reason(Weather.parse(body(hi = "\"80\""))))
        assertEquals(FailureReason.WRONG_TYPE, reason(Weather.parse(body(hi = "80"))))
    }

    @Test
    fun `weather code must be a real integer so a string or fraction is not guessed at`() {
        assertEquals(FailureReason.WRONG_TYPE, reason(Weather.parse(body(code = "\"3\""))))
        assertEquals(FailureReason.WRONG_TYPE, reason(Weather.parse(body(code = "3.5"))))
        assertEquals(FailureReason.MISSING_FIELD, reason(Weather.parse("""{"current":{"temperature_2m":1},"daily":{"temperature_2m_max":[1],"temperature_2m_min":[1]}}""")))
    }

    @Test
    fun `WMO codes map to the eight conditions we can draw`() {
        assertEquals(Condition.CLEAR, Weather.conditionFor(0))
        assertEquals(Condition.CLEAR, Weather.conditionFor(1))
        assertEquals(Condition.PARTLY_CLOUDY, Weather.conditionFor(2))
        assertEquals(Condition.CLOUDY, Weather.conditionFor(3))
        assertEquals(Condition.FOG, Weather.conditionFor(48))
        assertEquals(Condition.DRIZZLE, Weather.conditionFor(53))
        assertEquals(Condition.RAIN, Weather.conditionFor(63))
        assertEquals(Condition.RAIN, Weather.conditionFor(81))
        assertEquals(Condition.SNOW, Weather.conditionFor(73))
        assertEquals(Condition.SNOW, Weather.conditionFor(86))
        assertEquals(Condition.THUNDERSTORM, Weather.conditionFor(95))
    }

    @Test
    fun `an unknown future code degrades to cloudy instead of crashing or showing a blank icon`() {
        assertEquals(Condition.CLOUDY, Weather.conditionFor(1234))
        assertEquals(Condition.CLOUDY, Weather.conditionFor(-1))
    }

    @Test
    fun `data younger than 15 minutes is fresh so we do not hammer the API on every app start`() {
        assertEquals(Freshness.FRESH, Weather.freshness(0))
        assertEquals(Freshness.FRESH, Weather.freshness(Weather.REFRESH_MS - 1))
        assertEquals(15 * 60 * 1000L, Weather.REFRESH_MS)
    }

    @Test
    fun `between 15 minutes and 3 hours we still show the data but refresh it`() {
        assertEquals(Freshness.NEEDS_REFRESH, Weather.freshness(Weather.REFRESH_MS))
        assertEquals(Freshness.NEEDS_REFRESH, Weather.freshness(Weather.HIDE_AFTER_MS - 1))
    }

    @Test
    fun `after 3 hours the reading is too old to be honest and is hidden`() {
        assertEquals(Freshness.TOO_STALE, Weather.freshness(Weather.HIDE_AFTER_MS))
    }

    @Test
    fun `a clock that went backwards forces a refresh rather than trusting a negative age`() {
        assertEquals(Freshness.NEEDS_REFRESH, Weather.freshness(-1))
    }

    @Test
    fun `forecast URL carries the saved coordinates and fahrenheit`() {
        val url = Weather.buildUrl(29.7604, -95.3698)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?latitude=29.7604&longitude=-95.3698"))
        assertTrue("temperature_unit=fahrenheit" in url && "forecast_days=1" in url)
    }
}
