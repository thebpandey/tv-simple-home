package tv.simplehome.launcher

import java.time.LocalDate

/*
 * Source: Drik Panchang, Houston (geoname 4699066) and New Delhi, verified 2026-10-27.
 * Covers 2026–2028; add later years from the same source. Never compute these.
 *
 * Each occurrence has a PRIMARY date (Houston: drives windows, scene and countdown) and an INDIA date (New Delhi).
 * Conventions:
 *   - chaitra_navratri / sharad_navratri: FIRST day of the nine nights.
 *   - diwali: Lakshmi Puja. dhanteras, govardhan_puja, bhai_dooj, holika_dahan are helper entries: they are not
 *     festivals or scenes of their own, they only define the parent festival's window and banner day names.
 *   - Past 2028 lunar festivals simply do not appear (Western ones are rule-based and keep working).
 */
class FestivalOccurrence(val primary: LocalDate, val india: LocalDate) {
    override fun equals(other: Any?) = other is FestivalOccurrence && other.primary == primary && other.india == india
    override fun hashCode() = primary.hashCode() * 31 + india.hashCode()
    override fun toString() = "FestivalOccurrence($primary, india=$india)"
}

/** year + "MM-DD" for Houston and for India. */
private fun occ(year: Int, primary: String, india: String) =
    FestivalOccurrence(LocalDate.parse("$year-$primary"), LocalDate.parse("$year-$india"))

val festivalDates: Map<String, List<FestivalOccurrence>> = mapOf(
    "lohri" to listOf(
        occ(2026, "01-13", "01-13"),
        occ(2027, "01-13", "01-14"),
        occ(2028, "01-13", "01-14"),
    ),
    "makar_sankranti" to listOf(
        occ(2026, "01-14", "01-14"),
        occ(2027, "01-14", "01-15"),
        occ(2028, "01-14", "01-15"),
    ),
    "vasant_panchami" to listOf(
        occ(2026, "01-23", "01-23"),
        occ(2027, "02-11", "02-11"),
        occ(2028, "01-31", "01-31"),
    ),
    "maha_shivaratri" to listOf(
        occ(2026, "02-15", "02-15"),
        occ(2027, "03-06", "03-06"),
        occ(2028, "02-23", "02-23"),
    ),
    "holika_dahan" to listOf(
        occ(2026, "03-02", "03-03"),
        occ(2027, "03-21", "03-21"),
        occ(2028, "03-10", "03-10"),
    ),
    "holi" to listOf(
        occ(2026, "03-03", "03-04"),
        occ(2027, "03-22", "03-22"),
        occ(2028, "03-11", "03-11"),
    ),
    "chaitra_navratri" to listOf(
        occ(2026, "03-19", "03-19"),
        occ(2027, "04-07", "04-07"),
        occ(2028, "03-26", "03-27"),
    ),
    "ram_navami" to listOf(
        occ(2026, "03-26", "03-26"),
        occ(2027, "04-14", "04-15"),
        occ(2028, "04-03", "04-03"),
    ),
    "hanuman_jayanti" to listOf(
        occ(2026, "04-01", "04-02"),
        occ(2027, "04-20", "04-20"),
        occ(2028, "04-08", "04-09"),
    ),
    "raksha_bandhan" to listOf(
        occ(2026, "08-27", "08-28"),
        occ(2027, "08-16", "08-17"),
        occ(2028, "08-04", "08-05"),
    ),
    "janmashtami" to listOf(
        occ(2026, "09-03", "09-04"),
        occ(2027, "08-25", "08-25"),
        occ(2028, "08-12", "08-13"),
    ),
    "ganesh_chaturthi" to listOf(
        occ(2026, "09-14", "09-14"),
        occ(2027, "09-03", "09-04"),
        occ(2028, "08-23", "08-23"),
    ),
    "sharad_navratri" to listOf(
        occ(2026, "10-11", "10-11"),
        occ(2027, "09-30", "09-30"),
        occ(2028, "09-19", "09-19"),
    ),
    "durga_ashtami" to listOf(
        occ(2026, "10-18", "10-19"),
        occ(2027, "10-07", "10-07"),
        occ(2028, "09-25", "09-26"),
    ),
    "dussehra" to listOf(
        occ(2026, "10-20", "10-20"),
        occ(2027, "10-09", "10-09"),
        occ(2028, "09-27", "09-27"),
    ),
    "karwa_chauth" to listOf(
        occ(2026, "10-28", "10-29"),
        occ(2027, "10-18", "10-18"),
        occ(2028, "10-06", "10-07"),
    ),
    "dhanteras" to listOf(
        occ(2026, "11-06", "11-06"),
        occ(2027, "10-26", "10-27"),
        occ(2028, "10-15", "10-15"),
    ),
    "diwali" to listOf(
        occ(2026, "11-08", "11-08"),
        occ(2027, "10-28", "10-29"),
        occ(2028, "10-17", "10-17"),
    ),
    "govardhan_puja" to listOf(
        occ(2026, "11-09", "11-10"),
        occ(2027, "10-29", "10-30"),
        occ(2028, "10-18", "10-18"),
    ),
    "bhai_dooj" to listOf(
        occ(2026, "11-10", "11-11"),
        occ(2027, "10-30", "10-31"),
        occ(2028, "10-19", "10-19"),
    ),
    "chhath" to listOf(
        occ(2026, "11-15", "11-15"),
        occ(2027, "11-04", "11-04"),
        occ(2028, "10-23", "10-23"),
    ),
)
