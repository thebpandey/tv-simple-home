package tv.simplehome.launcher

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder

data class Place(val name: String, val lat: Double, val lon: Double, val input: String) {
    companion object {
        /** First-run location (ZIP centroid). Changeable on the TV, then persisted. */
        val DEFAULT = Place("Houston, TX", 29.7604, -95.3698, "")
    }
}

sealed interface LookupResult {
    data class Found(val name: String, val lat: Double, val lon: Double) : LookupResult
    data object NotFound : LookupResult
    data object NetworkError : LookupResult
}

sealed interface InputKind {
    data class Zip(val zip: String) : InputKind
    data class City(val name: String) : InputKind
    data object Empty : InputKind
}

/** Pure location-lookup rules: input classification, URLs, defensive parsing of both geocoder responses. */
object Geocoding {
    private val ZIP = Regex("""^(\d{5})(-\d{4})?$""")

    fun classify(raw: String): InputKind {
        val t = raw.trim()
        if (t.isEmpty()) return InputKind.Empty
        // ZIP+4 is reduced to its 5-digit ZIP because the ZIP service only knows 5-digit codes.
        return ZIP.matchEntire(t)?.let { InputKind.Zip(it.groupValues[1]) } ?: InputKind.City(t)
    }

    fun zipUrl(zip: String) = "https://api.zippopotam.us/us/$zip"

    fun cityUrl(name: String) =
        "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(name, "UTF-8")}&count=1&format=json"

    fun validCoords(lat: Double, lon: Double) =
        lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0

    /** {"places":[{"place name","state abbreviation","latitude":"29.6","longitude":"-95.5"}]} (coordinates are strings). */
    fun parseZip(body: String?): LookupResult {
        val first = firstOf(root(body)?.opt("places")) ?: return LookupResult.NotFound
        val place = str(first.opt("place name")) ?: return LookupResult.NotFound
        val st = str(first.opt("state abbreviation"))
        return found(if (st != null) "$place, $st" else place, first.opt("latitude"), first.opt("longitude"))
    }

    /** {"results":[{"name","admin1","country","latitude","longitude"}]}; the key is absent when nothing matches. */
    fun parseCity(body: String?): LookupResult {
        val first = firstOf(root(body)?.opt("results")) ?: return LookupResult.NotFound
        val name = str(first.opt("name")) ?: return LookupResult.NotFound
        val region = str(first.opt("admin1")) ?: str(first.opt("country"))
        return found(if (region != null) "$name, $region" else name, first.opt("latitude"), first.opt("longitude"))
    }

    /** A failed lookup must never clobber the saved location. */
    fun applyLookup(current: Place, result: LookupResult, input: String): Place =
        if (result is LookupResult.Found) Place(result.name, result.lat, result.lon, input) else current

    /** Rebuild a stored place; anything missing or invalid falls back to the default instead of breaking the widget. */
    fun placeFromStored(name: String?, lat: String?, lon: String?, input: String?): Place {
        val la = lat?.toDoubleOrNull()
        val lo = lon?.toDoubleOrNull()
        if (name.isNullOrBlank() || la == null || lo == null || !validCoords(la, lo)) return Place.DEFAULT
        return Place(name, la, lo, input ?: "")
    }

    private fun found(name: String, lat: Any?, lon: Any?): LookupResult {
        val la = coord(lat)
        val lo = coord(lon)
        return if (la != null && lo != null && validCoords(la, lo)) LookupResult.Found(name, la, lo) else LookupResult.NotFound
    }

    private fun coord(v: Any?): Double? = when (v) {
        is Number -> v.toDouble()
        is String -> v.trim().toDoubleOrNull()
        else -> null
    }

    private fun str(v: Any?): String? = (v as? String)?.trim()?.takeIf { it.isNotEmpty() }

    private fun root(body: String?): JSONObject? {
        if (body.isNullOrBlank()) return null
        return try { JSONObject(body) } catch (e: JSONException) { null }
    }

    private fun firstOf(v: Any?): JSONObject? = (v as? JSONArray)?.opt(0) as? JSONObject
}
