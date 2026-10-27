package tv.simplehome.launcher

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** [windowBeforeDays]/[windowAfterDays] extend the active period around the main day. [variant] picks a palette/detail of a shared scene. */
data class Festival(
    val id: String,
    val displayName: String,
    val sceneId: String,
    val windowBeforeDays: Int,
    val windowAfterDays: Int,
    val accent: Int = 0xFFFFC857.toInt(),
    val variant: Int = 0,
    /** If set, the window starts/ends at the primary date of this festivalDates entry (same year) instead of the fixed offsets. */
    val startKey: String? = null,
    val endKey: String? = null,
    /** festivalDates keys that get their own banner title when today is that day (e.g. Diwali's five days). */
    val dayNames: List<Pair<String, String>> = emptyList(),
    /** US federal holidays: computed by rule, and a festival's main day beats a holiday's main day. */
    val holiday: Boolean = false,
    /** Fixed-date holidays get an "Observed ..." line when they fall on a weekend. */
    val observedRule: Boolean = false,
)

data class ActiveFestival(val festival: Festival, val main: FestivalOccurrence) {
    val mainDay: LocalDate get() = main.primary
}

data class FestivalBanner(val title: String, val dateLine: String, val indiaLine: String?, val observedLine: String? = null)

/** Pure festival rules: rule-based Western dates, window resolution, and banner text. */
object Festivals {
    val ALL: List<Festival> = listOf(
        Festival("lohri", "Lohri", "sankranti_lohri", 1, 0, 0xFFFF9F43.toInt(), variant = 1),
        Festival("makar_sankranti", "Makar Sankranti", "sankranti_lohri", 1, 0, 0xFF6EC1FF.toInt()),
        Festival("vasant_panchami", "Vasant Panchami", "vasant", 1, 0, 0xFFF5D547.toInt()),
        Festival("maha_shivaratri", "Maha Shivaratri", "shivaratri", 1, 0, 0xFF9DB8FF.toInt()),
        Festival("holi", "Holi", "holi", 1, 0, 0xFFFF7EB6.toInt(), startKey = "holika_dahan"), // window: Holika Dahan through Holi
        Festival("chaitra_navratri", "Chaitra Navratri", "navratri_dussehra", 0, 8, 0xFFFF9F1C.toInt()), // nine nights
        Festival("ram_navami", "Ram Navami", "navratri_dussehra", 1, 0, 0xFFFFA94D.toInt()),
        Festival("hanuman_jayanti", "Hanuman Jayanti", "ganesh", 1, 0, 0xFFFF7A3D.toInt()),
        Festival("raksha_bandhan", "Raksha Bandhan", "raksha_bandhan", 1, 0, 0xFFFF9EAA.toInt()),
        Festival("janmashtami", "Janmashtami", "janmashtami", 1, 0, 0xFF5CC8C4.toInt()),
        Festival("ganesh_chaturthi", "Ganesh Chaturthi", "ganesh", 1, 9, 0xFFFF8C42.toInt()), // ten-day festival
        Festival("sharad_navratri", "Sharad Navratri", "navratri_dussehra", 0, 8, 0xFFFF9F1C.toInt(), endKey = "dussehra"), // day 1 through Dussehra
        Festival("durga_ashtami", "Durga Ashtami", "navratri_dussehra", 1, 0, 0xFFE8505B.toInt()),
        Festival("dussehra", "Dussehra", "navratri_dussehra", 1, 0, 0xFFFF8A3D.toInt(), variant = 1),
        Festival("karwa_chauth", "Karwa Chauth", "karwa_chauth", 1, 0, 0xFFFFC2D1.toInt()),
        Festival(
            "diwali", "Diwali", "diwali", 2, 2, 0xFFFFC857.toInt(), startKey = "dhanteras", endKey = "bhai_dooj", // Dhanteras through Bhai Dooj
            dayNames = listOf("dhanteras" to "Dhanteras", "diwali" to "Diwali", "govardhan_puja" to "Govardhan Puja", "bhai_dooj" to "Bhai Dooj"),
        ),
        Festival("chhath", "Chhath Puja", "chhath", 2, 1, 0xFFFFA45C.toInt()),
        Festival("halloween", "Halloween", "halloween", 7, 0, 0xFFFF8F2B.toInt()),
        Festival("thanksgiving", "Thanksgiving", "thanksgiving", 3, 0, 0xFFE9A23B.toInt()),
        Festival("christmas", "Christmas", "christmas", 7, 1, 0xFFE85D5D.toInt(), observedRule = true),
        Festival("new_year", "New Year", "new_year", 1, 0, 0xFFFFD95A.toInt(), observedRule = true), // Dec 31 to Jan 1
        // US federal holidays (Thanksgiving, Christmas and New Year's Day are the festivals above). Banner from 3 days before through the day;
        // only Independence Day has a wallpaper (the fireworks scene in red, white and blue), the rest are banner-only (empty scene id).
        Festival("us_mlk", "Martin Luther King Jr. Day", "", 3, 0, 0xFF6EA8FF.toInt(), holiday = true),
        Festival("us_presidents", "Presidents' Day", "", 3, 0, 0xFF7FB2FF.toInt(), holiday = true),
        Festival("us_memorial", "Memorial Day", "", 3, 0, 0xFFE06B6B.toInt(), holiday = true),
        Festival("us_juneteenth", "Juneteenth", "", 3, 0, 0xFFE0A030.toInt(), holiday = true, observedRule = true),
        Festival("us_independence", "Independence Day", "new_year", 3, 0, 0xFFE05A5A.toInt(), variant = 1, holiday = true, observedRule = true),
        Festival("us_labor", "Labor Day", "", 3, 0, 0xFF6EA8FF.toInt(), holiday = true),
        Festival("us_columbus", "Columbus Day / Indigenous Peoples' Day", "", 3, 0, 0xFFD9A066.toInt(), holiday = true),
        Festival("us_veterans", "Veterans Day", "", 3, 0, 0xFFE06B6B.toInt(), holiday = true, observedRule = true),
    )

    /** The festival list for the "US holidays" setting: Off drops the federal holidays only (Christmas, Thanksgiving, New Year stay). */
    fun enabled(usHolidays: Boolean): List<Festival> = if (usHolidays) ALL else ALL.filter { !it.holiday }

    /** The [n]th [day] of a month (n = 1..4), or the last one when [n] is -1. */
    private fun nthWeekday(year: Int, month: Int, day: DayOfWeek, n: Int): LocalDate =
        if (n == -1) LocalDate.of(year, month, 1).with(TemporalAdjusters.lastInMonth(day))
        else LocalDate.of(year, month, 1).with(TemporalAdjusters.dayOfWeekInMonth(n, day))

    /** Saturday holidays are observed on the Friday before, Sunday ones on the Monday after; null when it falls on a weekday. */
    fun observedDate(date: LocalDate): LocalDate? = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> date.minusDays(1)
        DayOfWeek.SUNDAY -> date.plusDays(1)
        else -> null
    }

    fun observedLine(date: LocalDate, locale: Locale = Locale.getDefault()): String? =
        observedDate(date)?.let { "Observed " + it.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale)) }

    /** US Thanksgiving: the fourth Thursday of November. */
    fun thanksgiving(year: Int): LocalDate =
        LocalDate.of(year, 11, 1).with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY))

    /** Main days of [f] that fall in [year]. Western festivals are rule-based; lunar ones come only from [dates]. */
    fun mainDays(f: Festival, year: Int, dates: Map<String, List<FestivalOccurrence>> = festivalDates): List<FestivalOccurrence> = when (f.id) {
        "halloween" -> listOf(same(LocalDate.of(year, 10, 31)))
        "thanksgiving" -> listOf(same(thanksgiving(year)))
        "christmas" -> listOf(same(LocalDate.of(year, 12, 25)))
        "new_year" -> listOf(same(LocalDate.of(year, 1, 1)))
        "us_mlk" -> listOf(same(nthWeekday(year, 1, DayOfWeek.MONDAY, 3)))
        "us_presidents" -> listOf(same(nthWeekday(year, 2, DayOfWeek.MONDAY, 3)))
        "us_memorial" -> listOf(same(nthWeekday(year, 5, DayOfWeek.MONDAY, -1)))
        "us_juneteenth" -> listOf(same(LocalDate.of(year, 6, 19)))
        "us_independence" -> listOf(same(LocalDate.of(year, 7, 4)))
        "us_labor" -> listOf(same(nthWeekday(year, 9, DayOfWeek.MONDAY, 1)))
        "us_columbus" -> listOf(same(nthWeekday(year, 10, DayOfWeek.MONDAY, 2)))
        "us_veterans" -> listOf(same(LocalDate.of(year, 11, 11)))
        else -> dates[f.id].orEmpty().filter { it.primary.year == year }
    }

    private fun same(d: LocalDate) = FestivalOccurrence(d, d) // Western holidays fall on the same date everywhere

    /** Active period for one occurrence; always contains the main day. */
    fun windowFor(f: Festival, main: FestivalOccurrence, dates: Map<String, List<FestivalOccurrence>> = festivalDates): Pair<LocalDate, LocalDate> {
        val year = main.primary.year
        fun keyed(key: String?): LocalDate? = key?.let { k -> dates[k].orEmpty().firstOrNull { it.primary.year == year }?.primary }
        val start = keyed(f.startKey) ?: main.primary.minusDays(f.windowBeforeDays.toLong())
        val end = keyed(f.endKey) ?: main.primary.plusDays(f.windowAfterDays.toLong())
        return minOf(start, main.primary) to maxOf(end, main.primary)
    }

    /**
     * The festival whose window contains [date]. Overlaps resolve to: a festival whose main day is today,
     * then the shorter window (so a one-day Dussehra beats the nine-night Navratri window), then list order.
     */
    fun activeOn(
        date: LocalDate,
        dates: Map<String, List<FestivalOccurrence>> = festivalDates,
        festivals: List<Festival> = ALL,
    ): ActiveFestival? {
        var best: ActiveFestival? = null
        var bestRank = Rank(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
        festivals.forEachIndexed { index, f ->
            // A window can straddle New Year, so look at the neighbouring years' main days too.
            for (year in date.year - 1..date.year + 1) {
                for (main in mainDays(f, year, dates)) {
                    val (start, end) = windowFor(f, main, dates)
                    if (date.isBefore(start) || date.isAfter(end)) continue
                    val length = ChronoUnit.DAYS.between(start, end).toInt() + 1
                    // A main day beats a window day; between two main days a festival beats a holiday; then the shorter window, then list order.
                    val rank = Rank(if (main.primary == date) 0 else 1, if (f.holiday) 1 else 0, length, index)
                    if (compareRank(rank, bestRank) < 0) {
                        best = ActiveFestival(f, main)
                        bestRank = rank
                    }
                }
            }
        }
        return best
    }

    private data class Rank(val mainDay: Int, val holiday: Int, val windowLength: Int, val listIndex: Int)

    private fun compareRank(a: Rank, b: Rank): Int =
        compareValuesBy(a, b, { it.mainDay }, { it.holiday }, { it.windowLength }, { it.listIndex })

    /** "Today" / "Tomorrow" / "in N days" up to the main day; null once it has passed. */
    fun relativeText(today: LocalDate, mainDay: LocalDate): String? = when (val days = ChronoUnit.DAYS.between(today, mainDay)) {
        0L -> "Today"
        1L -> "Tomorrow"
        else -> if (days > 1) "in $days days" else null
    }

    fun dateText(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale))

    /**
     * Banner content. Inside a multi-day window (Diwali) the title names the specific day when today is one;
     * otherwise it names the festival and counts down to its main day. The India line appears only when
     * the New Delhi date differs from the Houston date being shown.
     */
    fun bannerFor(
        active: ActiveFestival, today: LocalDate,
        dates: Map<String, List<FestivalOccurrence>> = festivalDates, locale: Locale = Locale.getDefault(),
    ): FestivalBanner {
        var title = active.festival.displayName
        var shown = active.main
        for ((key, name) in active.festival.dayNames) {
            val o = dates[key].orEmpty().firstOrNull { it.primary == today }
            if (o != null) { title = name; shown = o; break }
        }
        val india = if (shown.india != shown.primary) "India: " + shown.india.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale)) else null
        val observed = if (active.festival.observedRule) observedLine(shown.primary, locale) else null
        return FestivalBanner(title, bannerDateLine(today, shown.primary, locale), india, observed)
    }

    /** Second banner line: the main day's date, plus the countdown when it is today or still ahead. */
    fun bannerDateLine(today: LocalDate, mainDay: LocalDate, locale: Locale = Locale.getDefault()): String {
        val rel = relativeText(today, mainDay)
        return if (rel == null) dateText(mainDay, locale) else "${dateText(mainDay, locale)}  ·  $rel"
    }
}
