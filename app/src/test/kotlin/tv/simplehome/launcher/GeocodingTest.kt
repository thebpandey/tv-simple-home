package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocodingTest {
    private val houston = """{"post code":"77002","places":[{"place name":"Houston","longitude":"-95.3698","state":"Texas","state abbreviation":"TX","latitude":"29.7604"}]}"""

    @Test
    fun `five digits is a ZIP even with stray whitespace from the on-screen keyboard`() {
        assertEquals(InputKind.Zip("77002"), Geocoding.classify(" 77002 "))
    }

    @Test
    fun `four digits is not a ZIP so it goes to city search instead of a guaranteed 404`() {
        assertEquals(InputKind.City("7703"), Geocoding.classify("7703"))
    }

    @Test
    fun `ZIP plus 4 uses the 5 digit ZIP because the ZIP service rejects the extension`() {
        assertEquals(InputKind.Zip("77002"), Geocoding.classify("77002-1234"))
    }

    @Test
    fun `city names are trimmed and blank input is empty so no request is made`() {
        assertEquals(InputKind.City("New York"), Geocoding.classify("  New York "))
        assertEquals(InputKind.Empty, Geocoding.classify("   "))
    }

    @Test
    fun `city names are URL encoded so spaces and accents cannot break the query`() {
        assertTrue("name=S%C3%A3o+Paulo&count=1" in Geocoding.cityUrl("São Paulo"))
    }

    @Test
    fun `ZIP response with string coordinates is parsed into a display name and numbers`() {
        assertEquals(LookupResult.Found("Houston, TX", 29.7604, -95.3698), Geocoding.parseZip(houston))
    }

    @Test
    fun `unknown ZIP returns an empty object and is NotFound so we fall back to city search`() {
        assertEquals(LookupResult.NotFound, Geocoding.parseZip("{}"))
        assertEquals(LookupResult.NotFound, Geocoding.parseZip("""{"places":[]}"""))
        assertEquals(LookupResult.NotFound, Geocoding.parseZip(null))
        assertEquals(LookupResult.NotFound, Geocoding.parseZip("not json"))
    }

    @Test
    fun `ZIP coordinates that are not numbers are rejected rather than saved as zero`() {
        assertEquals(LookupResult.NotFound, Geocoding.parseZip(houston.replace("29.7604", "north")))
    }

    @Test
    fun `out of range coordinates are rejected so a bad API answer cannot save an impossible place`() {
        assertEquals(LookupResult.NotFound, Geocoding.parseZip(houston.replace("29.7604", "91.0")))
        assertEquals(LookupResult.NotFound, Geocoding.parseZip(houston.replace("-95.3698", "-181")))
        assertFalse(Geocoding.validCoords(Double.NaN, 0.0))
    }

    @Test
    fun `city result uses admin1 for the region`() {
        val b = """{"results":[{"name":"Paris","latitude":48.85341,"longitude":2.3488,"admin1":"Ile-de-France","country":"France"}]}"""
        assertEquals(LookupResult.Found("Paris, Ile-de-France", 48.85341, 2.3488), Geocoding.parseCity(b))
    }

    @Test
    fun `city result falls back to country when there is no admin1`() {
        val b = """{"results":[{"name":"Monaco","latitude":43.73,"longitude":7.42,"country":"Monaco"}]}"""
        assertEquals(LookupResult.Found("Monaco, Monaco", 43.73, 7.42), Geocoding.parseCity(b))
    }

    @Test
    fun `no matches omit the results key entirely and that means NotFound`() {
        assertEquals(LookupResult.NotFound, Geocoding.parseCity("""{"generationtime_ms":0.5}"""))
        assertEquals(LookupResult.NotFound, Geocoding.parseCity("""{"results":[]}"""))
    }

    @Test
    fun `city coordinates given as strings are still accepted but out of range ones are not`() {
        val ok = """{"results":[{"name":"X","latitude":"10.5","longitude":"20.5"}]}"""
        assertEquals(LookupResult.Found("X", 10.5, 20.5), Geocoding.parseCity(ok))
        assertEquals(LookupResult.NotFound, Geocoding.parseCity(ok.replace("10.5", "-95")))
    }

    @Test
    fun `a failed lookup keeps the previous location so a typo never wipes a working setup`() {
        val current = Place("Austin, TX", 30.27, -97.74, "78701")
        assertEquals(current, Geocoding.applyLookup(current, LookupResult.NotFound, "zzzz"))
        assertEquals(current, Geocoding.applyLookup(current, LookupResult.NetworkError, "77002"))
    }

    @Test
    fun `a successful lookup replaces the place and remembers what the user typed`() {
        val next = Geocoding.applyLookup(Place.DEFAULT, LookupResult.Found("Paris, France", 48.8, 2.3), "paris")
        assertEquals(Place("Paris, France", 48.8, 2.3, "paris"), next)
    }

    @Test
    fun `first run and corrupt storage both yield the Houston default so the widget always has a place`() {
        assertEquals(Place.DEFAULT, Geocoding.placeFromStored(null, null, null, null))
        assertEquals(Place.DEFAULT, Geocoding.placeFromStored("X", "abc", "1", null))
        assertEquals(Place.DEFAULT, Geocoding.placeFromStored("X", "95", "1", null))
        assertEquals(Place("X", 10.0, 20.0, ""), Geocoding.placeFromStored("X", "10.0", "20.0", null))
        assertEquals("Houston, TX", Place.DEFAULT.name)
    }
}
