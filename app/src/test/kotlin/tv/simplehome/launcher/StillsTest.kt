package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StillsTest {
    private fun v1(id: String = "diwali-night", file: String = "diwali-night.jpg", variant: String = "night", extra: String = "") =
        """{"id":"$id","file":"$file","variant":"$variant"$extra}"""

    private fun v1m(vararg entries: String, version: String = "1") = """{"version":$version,"scenes":[${entries.joinToString(",")}]}"""

    private fun v2(id: String = "diwali-night-3", file: String = "diwali-night-3.jpg", theme: String = "diwali", variant: String = "night", index: String = "3", extra: String = "") =
        """{"id":"$id","file":"$file","theme":"$theme","variant":"$variant","index":$index$extra}"""

    private fun v2m(vararg entries: String) = """{"version":2,"scenes":[${entries.joinToString(",")}]}"""
    private fun ok(r: StillManifestResult) = r as StillManifestResult.Success

    @Test
    fun `a version 2 manifest yields theme, variant, design index and the quote-band brightness`() {
        val r = ok(StillManifest.parse(v2m(v2(extra = ""","luma":35"""), v2("holi-day-1", "holi-day-1.jpg", "holi", "day", "1", ""","luma":126"""))))
        assertEquals(listOf("diwali", "holi"), r.stills.map { it.theme })
        assertEquals(listOf(3, 1), r.stills.map { it.index })
        assertEquals(listOf(true, false), r.stills.map { it.night })
        assertEquals(listOf(35, 126), r.stills.map { it.luma })
    }

    @Test
    fun `a version 1 manifest still loads, each id standing for design 1 of its theme`() {
        val r = ok(StillManifest.parse(v1m(v1(extra = ""","luma":35"""), v1("rakhi-day", "rakhi-day.jpg", "day"), v1("autumn-night", "an.jpg", "night"))))
        assertEquals(listOf("diwali", "rakhi", "autumn"), r.stills.map { it.theme })
        assertTrue(r.stills.all { it.index == 1 })
        assertEquals(0, r.skipped)
    }

    @Test
    fun `empty, non-JSON, unknown-version and listless manifests fail without crashing the wallpaper`() {
        assertEquals(FailureReason.EMPTY_BODY, (StillManifest.parse(" ") as StillManifestResult.Failure).reason)
        assertEquals(FailureReason.BAD_JSON, (StillManifest.parse("<html>") as StillManifestResult.Failure).reason)
        assertEquals(FailureReason.WRONG_TYPE, (StillManifest.parse(v1m(version = "3")) as StillManifestResult.Failure).reason)
        assertEquals(FailureReason.MISSING_FIELD, (StillManifest.parse("""{"version":2}""") as StillManifestResult.Failure).reason)
    }

    @Test
    fun `traversal file names, unknown themes, bad variants and bad ids are skipped since the file is untrusted`() {
        val r = ok(StillManifest.parse(v2m(
            v2(file = "../../etc/passwd.jpg"), v2(file = "a/b.jpg"), v2(theme = "evil"), v2(variant = "dusk"), v2(id = "Bad Id!"), v2(id = "../x"), "7",
            v2("holi-day-1", "holi-day-1.jpg", "holi", "day", "1"),
        )))
        assertEquals(listOf("holi-day-1"), r.stills.map { it.id })
        assertEquals(7, r.skipped)
    }

    @Test
    fun `the design index must be an integer from 1 to 4`() {
        val r = ok(StillManifest.parse(v2m(
            v2("a-0", "a0.jpg", index = "0"), v2("a-5", "a5.jpg", index = "5"), v2("a-s", "as.jpg", index = "\"2\""), v2("a-f", "af.jpg", index = "2.5"),
            v2("a-1", "a1.jpg", index = "1"), v2("a-4", "a4.jpg", index = "4"),
        )))
        assertEquals(listOf(1, 4), r.stills.map { it.index })
    }

    @Test
    fun `every allowed theme is accepted, nature and festival, and nothing else`() {
        assertEquals(24, StillManifest.THEMES.size)
        for (theme in StillManifest.THEMES) assertEquals(theme, 1, ok(StillManifest.parse(v2m(v2("x-$theme", "x.jpg", theme, "day", "1")))).stills.size)
        assertEquals(setOf("forest", "jungle", "beach", "mountains", "snowpeaks", "autumn"), StillManifest.NATURE_THEMES.toSet())
    }

    @Test
    fun `a duplicate id or a duplicate design slot keeps the first entry so a later line cannot swap a painting`() {
        val r = ok(StillManifest.parse(v2m(v2(file = "first.jpg"), v2(file = "second.jpg"), v2("other-id", "third.jpg"))))
        assertEquals(listOf("first.jpg"), r.stills.map { it.file })
        assertEquals(2, r.skipped)
    }

    @Test
    fun `an out-of-range or non-numeric luma is dropped to unknown rather than trusted`() {
        val r = ok(StillManifest.parse(v2m(v2(extra = ""","luma":999"""), v2("b-1", "b.jpg", index = "1", extra = ""","luma":"bright""""))))
        assertEquals(listOf<Int?>(null, null), r.stills.map { it.luma })
    }

    @Test
    fun `the manifest holds up to 192 designs and no more than 256 entries`() {
        val all = StillManifest.THEMES.flatMap { t -> listOf("day", "night").flatMap { v -> (1..4).map { i -> v2("$t-$v-$i", "$t-$v-$i.jpg", t, v, "$i") } } }
        assertEquals(192, ok(StillManifest.parse(v2m(*all.toTypedArray()))).stills.size)
    }

    @Test
    fun `every festival in the brief maps to its theme and Ram Navami and Hanuman Jayanti stay procedural`() {
        val expected = mapOf(
            "diwali" to "diwali", "chaitra_navratri" to "navratri", "sharad_navratri" to "navratri", "durga_ashtami" to "navratri",
            "dussehra" to "dussehra", "makar_sankranti" to "sankranti", "lohri" to "lohri", "karwa_chauth" to "karwa",
            "chhath" to "chhath", "janmashtami" to "janmashtami", "ganesh_chaturthi" to "ganesh", "raksha_bandhan" to "rakhi",
            "maha_shivaratri" to "shivaratri", "holi" to "holi", "vasant_panchami" to "vasant", "halloween" to "halloween",
            "thanksgiving" to "thanksgiving", "christmas" to "christmas", "new_year" to "newyear", "us_independence" to "july4",
        )
        for ((fest, theme) in expected) assertEquals(fest, theme, StillMapping.themeForFestival(fest))
        assertNull(StillMapping.themeForFestival("ram_navami"))
        assertNull(StillMapping.themeForFestival("hanuman_jayanti"))
        assertEquals(StillManifest.FESTIVAL_THEMES.toSet(), expected.values.toSet())
    }

    @Test
    fun `nature scene indices map to the six nature themes in scene order`() {
        assertEquals("autumn", StillMapping.natureTheme(5))
        assertEquals("Autumn Valley", WallpaperSchedule.SCENE_NAMES[5])
        assertEquals(StillManifest.NATURE_THEMES, (0..5).map { StillMapping.natureTheme(it) })
    }

    private fun e(theme: String, night: Boolean, index: Int, id: String = "$theme-${if (night) "n" else "d"}$index") =
        StillEntry(id, "$id.jpg", theme, night, index, null)

    @Test
    fun `overlays follow the brief, nature paintings included, and only the matched diwali painting gets the diya flickers`() {
        assertEquals(StillOverlay.DIWALI, StillMapping.overlayFor(e("diwali", true, 1, "diwali-night-1")))
        assertEquals("a v1 manifest's diwali-night is design 1 too", StillOverlay.DIWALI, StillMapping.overlayFor(e("diwali", true, 1, "diwali-night")))
        assertEquals(StillOverlay.DIWALI_SOFT, StillMapping.overlayFor(e("diwali", true, 3)))
        assertEquals("day design 1 is a different painting", StillOverlay.DIWALI_SOFT, StillMapping.overlayFor(e("diwali", false, 1)))
        assertEquals(StillOverlay.FIREWORKS, StillMapping.overlayFor(e("newyear", true, 1)))
        assertEquals(StillOverlay.FIREWORKS_RWB, StillMapping.overlayFor(e("july4", true, 1)))
        assertEquals(StillOverlay.SNOW, StillMapping.overlayFor(e("christmas", true, 1)))
        assertEquals(StillOverlay.PUFFS, StillMapping.overlayFor(e("holi", false, 1)))
        assertEquals(StillOverlay.BATS, StillMapping.overlayFor(e("halloween", true, 1)))
        assertEquals(StillOverlay.LEAVES, StillMapping.overlayFor(e("autumn", false, 1)))
        assertEquals(StillOverlay.NONE, StillMapping.overlayFor(e("sankranti", false, 1)))
        assertEquals(StillOverlay.MIST_FIREFLIES, StillMapping.overlayFor(e("forest", true, 1)))
        assertEquals(StillOverlay.MIST, StillMapping.overlayFor(e("forest", false, 1)))
        assertEquals(StillOverlay.SPORES, StillMapping.overlayFor(e("jungle", false, 2)))
        assertEquals(StillOverlay.GLINTS, StillMapping.overlayFor(e("beach", false, 1)))
        assertEquals(StillOverlay.CLOUDS, StillMapping.overlayFor(e("mountains", false, 1)))
        assertEquals(StillOverlay.SNOW_LIGHT, StillMapping.overlayFor(e("snowpeaks", true, 1)))
    }

    // ---- pools ----

    private val diwali = listOf(e("diwali", true, 2), e("holi", false, 1), e("diwali", false, 1), e("diwali", true, 1), e("diwali", true, 3), e("diwali", false, 2))

    @Test
    fun `a festival's designs are listed by design number whatever the manifest order`() {
        val designs = StillPools.designsOf(diwali, "diwali")
        assertEquals(listOf(1, 1, 2, 2, 3), designs.map { diwali[it].index })
        assertFalse(designs.any { diwali[it].theme != "diwali" })
    }

    @Test
    fun `within a festival window the designs of the current sun variant rotate once each per cycle`() {
        val nightPool = listOf(1, 2, 3).toSet()
        val seen = (0L until 3L).map { diwali[StillPools.pick(diwali, "diwali", night = true, slot = it)!!] }
        assertEquals(nightPool, seen.map { it.index }.toSet())
        assertTrue(seen.all { it.night })
        val day = (0L until 2L).map { diwali[StillPools.pick(diwali, "diwali", night = false, slot = it)!!] }
        assertTrue(day.all { !it.night })
        assertEquals(setOf(1, 2), day.map { it.index }.toSet())
    }

    @Test
    fun `the pick is deterministic per slot and changes between slots`() {
        for (slot in listOf(0L, 7L, 12345L)) assertEquals(StillPools.pick(diwali, "diwali", true, slot), StillPools.pick(diwali, "diwali", true, slot))
        assertTrue((0L until 12L).map { StillPools.pick(diwali, "diwali", true, it) }.toSet().size > 1)
    }

    @Test
    fun `a theme with no design for the current variant falls back to the other variant, and with none at all to null`() {
        val onlyNight = listOf(e("lohri", true, 1), e("lohri", true, 2))
        val dayPick = StillPools.pick(onlyNight, "lohri", night = false, slot = 4)
        assertNotNull(dayPick)
        assertTrue(onlyNight[dayPick!!].night)
        assertNull("no paintings for the theme means the procedural scene", StillPools.pick(onlyNight, "holi", night = true, slot = 4))
        assertNull(StillPools.pick(emptyList(), "diwali", false, 0))
    }

    @Test
    fun `still keys sit above photo keys, count as images, and fit the scene cache`() {
        assertTrue(WallpaperSchedule.STILL_BASE >= WallpaperSchedule.PHOTO_BASE + PhotoManifest.MAX_ENTRIES)
        assertTrue(WallpaperSchedule.isStillKey(WallpaperSchedule.STILL_BASE))
        assertFalse(WallpaperSchedule.isStillKey(WallpaperSchedule.PHOTO_BASE + 5))
        assertTrue(WallpaperSchedule.isPhotoKey(WallpaperSchedule.STILL_BASE + 3))
        assertFalse(WallpaperSchedule.isDayKey(WallpaperSchedule.STILL_BASE))
        assertTrue(WallpaperSchedule.STILL_BASE + StillManifest.MAX_ENTRIES - 1 < WallpaperSchedule.KEY_SPACE)
        assertEquals(WallpaperSchedule.FADE_MS, WallpaperSchedule.fadeDurationMs(WallpaperSchedule.STILL_BASE, WallpaperSchedule.STILL_BASE + 1))
    }

    @Test
    fun `an image at luma 110 or more gets the light UI, so bright pastel paintings never carry white text`() {
        for (luma in listOf(126, 143, 118, 110)) assertTrue("luma $luma", UiTheme.isDay(DayNightMode.AUTO, sunSaysDay = false, imageLuma = luma))
        for (luma in listOf(35, 75, 109)) assertFalse("luma $luma", UiTheme.isDay(DayNightMode.AUTO, sunSaysDay = true, imageLuma = luma))
    }

    @Test
    fun `without an image the sun decides, and an explicit Always day or Always night choice beats the image`() {
        assertTrue(UiTheme.isDay(DayNightMode.AUTO, true, null))
        assertFalse(UiTheme.isDay(DayNightMode.AUTO, false, null))
        assertFalse(UiTheme.isDay(DayNightMode.NIGHT, true, 200))
        assertTrue(UiTheme.isDay(DayNightMode.DAY, false, 10))
    }

    @Test
    fun `the real version 1 manifest from the artwork pipeline parses with all 20 stills`() {
        // Opt-in: point STILLS_MANIFEST at a real scenes.json from the artwork pipeline.
        val path = System.getenv("STILLS_MANIFEST") ?: return
        val file = java.io.File(path)
        if (!file.isFile) return
        val r = ok(StillManifest.parse(file.readText()))
        assertEquals(20, r.stills.size)
        assertEquals(0, r.skipped)
    }
}
