package tv.simplehome.launcher

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface FetchOutcome {
    data class Body(val text: String) : FetchOutcome
    data class Error(val reason: FailureReason) : FetchOutcome
}

object WeatherFetcher {
    private const val TIMEOUT_MS = 10_000
    private const val MAX_CHARS = 64 * 1024

    /**
     * NETWORK USE: this is the ONLY network call site in the app and the sole reason for the INTERNET
     * permission. Hosts: api.open-meteo.com (forecast; sends only the saved coordinates),
     * geocoding-api.open-meteo.com and api.zippopotam.us (location lookup; sends only the text the user
     * typed). No keys, no identifiers. Blocking: call from a background thread.
     */
    fun fetch(url: String): FetchOutcome {
        var conn: HttpURLConnection? = null
        return try {
            // https URLs always yield HttpURLConnection.
            conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                FetchOutcome.Error(FailureReason.HTTP_ERROR)
            } else {
                val sb = StringBuilder()
                conn.inputStream.bufferedReader().use { r ->
                    val buf = CharArray(4096)
                    while (sb.length < MAX_CHARS) {
                        val n = r.read(buf)
                        if (n < 0) break
                        sb.append(buf, 0, n)
                    }
                }
                FetchOutcome.Body(sb.toString())
            }
        } catch (e: IOException) {
            FetchOutcome.Error(FailureReason.NETWORK)
        } finally {
            conn?.disconnect()
        }
    }
}

/** Turns what the user typed into coordinates: 5-digit ZIP first, then city search. Blocking. */
object LocationResolver {
    fun resolve(raw: String): LookupResult = when (val kind = Geocoding.classify(raw)) {
        InputKind.Empty -> LookupResult.NotFound
        is InputKind.Zip -> {
            val byZip = lookup(Geocoding.zipUrl(kind.zip), Geocoding::parseZip)
            if (byZip is LookupResult.Found) byZip else lookup(Geocoding.cityUrl(kind.zip), Geocoding::parseCity)
        }
        is InputKind.City -> lookup(Geocoding.cityUrl(kind.name), Geocoding::parseCity)
    }

    private fun lookup(url: String, parse: (String?) -> LookupResult): LookupResult =
        when (val out = WeatherFetcher.fetch(url)) {
            is FetchOutcome.Body -> parse(out.text)
            is FetchOutcome.Error ->
                if (out.reason == FailureReason.NETWORK) LookupResult.NetworkError else LookupResult.NotFound
        }
}
