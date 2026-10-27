package tv.simplehome.launcher

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

sealed interface SunDay {
    data class Normal(val sunrise: ZonedDateTime, val sunset: ZonedDateTime) : SunDay
    data object AlwaysDay : SunDay
    data object AlwaysNight : SunDay
}

/** Sunrise/sunset from the NOAA solar calculator equations (Meeus based), accurate to about a minute. */
object SunTimes {
    private const val ZENITH_DEG = 90.833
    private const val UNIX_EPOCH_JD = 2440587.5
    private const val REFINEMENTS = 3

    private sealed interface Event {
        data class At(val minutesUtc: Double) : Event
        data object NeverRises : Event
        data object NeverSets : Event
    }

    fun forDate(date: LocalDate, lat: Double, lon: Double, zone: ZoneId): SunDay {
        // Polar state is decided on the requested UTC day; sunrise implies sunset exists too.
        val rise = event(date, lat, lon, rising = true)
        if (rise is Event.NeverRises) return SunDay.AlwaysNight
        if (rise is Event.NeverSets) return SunDay.AlwaysDay

        // An event for UTC day D may land on the neighbouring local day, so scan D-1..D+1.
        val rises = localMatches(date, lat, lon, zone, rising = true)
        val sets = localMatches(date, lat, lon, zone, rising = false)
        val sunrise = rises.firstOrNull() ?: return SunDay.AlwaysNight
        val sunset = sets.firstOrNull() ?: return SunDay.AlwaysDay
        return SunDay.Normal(sunrise, sunset)
    }

    fun isDay(now: ZonedDateTime, lat: Double, lon: Double): Boolean =
        when (val day = forDate(now.toLocalDate(), lat, lon, now.zone)) {
            SunDay.AlwaysDay -> true
            SunDay.AlwaysNight -> false
            is SunDay.Normal -> !now.isBefore(day.sunrise) && now.isBefore(day.sunset)
        }

    fun nextTransition(now: ZonedDateTime, lat: Double, lon: Double): ZonedDateTime? =
        (0L..3L).asSequence()
            .map { forDate(now.toLocalDate().plusDays(it), lat, lon, now.zone) }
            .filterIsInstance<SunDay.Normal>()
            .flatMap { sequenceOf(it.sunrise, it.sunset) }
            .filter { it.isAfter(now) }
            .minOrNull()

    private fun localMatches(
        date: LocalDate, lat: Double, lon: Double, zone: ZoneId, rising: Boolean,
    ): List<ZonedDateTime> =
        (-1L..1L).mapNotNull { offset ->
            val utcDay = date.plusDays(offset)
            val e = event(utcDay, lat, lon, rising) as? Event.At ?: return@mapNotNull null
            utcDay.atStartOfDay(ZoneOffset.UTC).plusSeconds(Math.round(e.minutesUtc * 60))
                .withZoneSameInstant(zone)
        }.filter { it.toLocalDate() == date }

    private fun event(utcDay: LocalDate, lat: Double, lon: Double, rising: Boolean): Event {
        val jdMidnight = utcDay.toEpochDay() + UNIX_EPOCH_JD
        var minutes = 720.0 - 4.0 * lon
        repeat(REFINEMENTS) {
            val (decl, eqTime) = solarPosition(jdMidnight + minutes / 1440.0)
            val cosHa = cos(Math.toRadians(ZENITH_DEG)) / (cos(Math.toRadians(lat)) * cos(decl)) -
                tan(Math.toRadians(lat)) * tan(decl)
            if (cosHa > 1.0) return Event.NeverRises
            if (cosHa < -1.0) return Event.NeverSets
            val haMinutes = 4.0 * Math.toDegrees(acos(cosHa))
            val noon = 720.0 - 4.0 * lon - eqTime
            minutes = if (rising) noon - haMinutes else noon + haMinutes
        }
        return Event.At(minutes)
    }

    /** Returns (declination in radians, equation of time in minutes) at the given Julian day. */
    private fun solarPosition(jd: Double): Pair<Double, Double> {
        val t = (jd - 2451545.0) / 36525.0
        val l0 = (280.46646 + t * (36000.76983 + t * 0.0003032)).mod(360.0)
        val m = Math.toRadians(357.52911 + t * (35999.05029 - 0.0001537 * t))
        val ecc = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
        val center = sin(m) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
            sin(2 * m) * (0.019993 - 0.000101 * t) + sin(3 * m) * 0.000289
        val omega = Math.toRadians(125.04 - 1934.136 * t)
        val lambda = Math.toRadians(l0 + center - 0.00569 - 0.00478 * sin(omega))
        val seconds = 21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))
        val obliq = Math.toRadians(23.0 + (26.0 + seconds / 60.0) / 60.0 + 0.00256 * cos(omega))
        val decl = asin(sin(obliq) * sin(lambda))
        val y = tan(obliq / 2).let { it * it }
        val l0r = Math.toRadians(l0)
        val eq = y * sin(2 * l0r) - 2 * ecc * sin(m) + 4 * ecc * y * sin(m) * cos(2 * l0r) -
            0.5 * y * y * sin(4 * l0r) - 1.25 * ecc * ecc * sin(2 * m)
        return decl to 4.0 * Math.toDegrees(eq)
    }
}
