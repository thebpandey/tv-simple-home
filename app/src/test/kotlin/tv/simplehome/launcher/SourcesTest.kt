package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcesTest {
    private val all = SourceSet.ALL

    @Test
    fun `the last enabled source cannot be switched off, so Auto-rotate always has something to show`() {
        val onlyScenes = all.with(SourceKind.PHOTOS, false).with(SourceKind.PAINTINGS, false)
        assertEquals(SourceSet(photos = false, paintings = false, scenes = true), onlyScenes)
        assertEquals(onlyScenes, onlyScenes.with(SourceKind.SCENES, false))
        assertEquals(1, onlyScenes.with(SourceKind.SCENES, false).count())
        assertTrue(onlyScenes.with(SourceKind.PHOTOS, true).photos)
    }

    @Test
    fun `sources serialize and parse back, unknown tokens are ignored and nothing recognisable gives null`() {
        for (s in listOf(all, SourceSet(true, false, false), SourceSet(false, true, true))) assertEquals(s, SourceSet.parse(s.serialize()))
        assertEquals(SourceSet(true, false, false), SourceSet.parse("photos,bogus"))
        assertNull(SourceSet.parse(""))
        assertNull(SourceSet.parse("nonsense"))
        assertNull(SourceSet.parse(null))
    }

    @Test
    fun `migration keeps the old Source choice, and Scenes + Photos gains Paintings`() {
        assertEquals(SourceSet(photos = true, paintings = true, scenes = true), WallpaperSources.fromLegacy("both"))
        assertEquals(SourceSet(photos = false, paintings = false, scenes = true), WallpaperSources.fromLegacy("scenes"))
        assertEquals(SourceSet(photos = true, paintings = false, scenes = false), WallpaperSources.fromLegacy("photos"))
        assertNull(WallpaperSources.fromLegacy("garbage"))
        assertNull(WallpaperSources.fromLegacy(null))
    }

    @Test
    fun `the new setting beats the old one, the old one beats the default, and a fresh install has everything on`() {
        assertEquals(SourceSet(false, true, false), WallpaperSources.resolve("paintings", "scenes"))
        assertEquals(SourceSet(false, false, true), WallpaperSources.resolve(null, "scenes"))
        assertEquals(all, WallpaperSources.resolve(null, null))
        assertEquals("a corrupt new value falls through to the old choice, never to a blank setting", SourceSet(true, false, false), WallpaperSources.resolve("zzz", "photos"))
    }

    @Test
    fun `with all three on and content present, sources take turns photo then painting then scene`() {
        val kinds = SourcePicker.available(all, hasPhotos = true, hasPaintings = true)
        assertEquals(listOf(SourceKind.PHOTOS, SourceKind.PAINTINGS, SourceKind.SCENES), kinds)
        assertEquals(listOf(SourceKind.PHOTOS, SourceKind.PAINTINGS, SourceKind.SCENES, SourceKind.PHOTOS), (0L..3L).map { SourcePicker.kindAt(kinds, it) })
        // each source advances once per full turn, so it shows every item once per cycle
        assertEquals(listOf(0L, 0L, 0L, 1L, 1L, 1L, 2L), (0L..6L).map { SourcePicker.innerSlot(kinds, it) })
    }

    @Test
    fun `a switched-off or empty source is skipped, and if nothing is left the animated scenes stand in`() {
        assertEquals(listOf(SourceKind.PAINTINGS, SourceKind.SCENES), SourcePicker.available(all.with(SourceKind.PHOTOS, false), true, true))
        assertEquals(listOf(SourceKind.SCENES), SourcePicker.available(all, hasPhotos = false, hasPaintings = false))
        assertEquals(listOf(SourceKind.SCENES), SourcePicker.available(SourceSet(true, false, false), hasPhotos = false, hasPaintings = true))
    }

    private val photoNight = listOf(true, false, true)
    private val paintNight = listOf(true, true, false, false)
    private fun pick(slot: Long, set: SourceSet, night: Boolean = true, mode: WallpaperMode = WallpaperMode.Auto, fest: Int? = null) =
        WallpaperPicker.pick(mode, set, slot * WallpaperSchedule.PERIOD_MS + 3, fest, photoNight, night, paintNight)

    @Test
    fun `Auto-rotate cycles photo, painting, scene across slots and each pick matches the sun variant`() {
        val kinds = (0L until 9L).map { pick(it, all) }
        for (slot in 0 until 9) when (slot % 3) {
            0 -> { val p = kinds[slot] as Pick.Photo; assertTrue(photoNight[p.index]) }
            1 -> { val p = kinds[slot] as Pick.Painting; assertTrue(paintNight[p.index]) }
            else -> assertTrue(kinds[slot] is Pick.SceneKey)
        }
    }

    @Test
    fun `toggling a source off removes it from the rotation`() {
        val noPaint = all.with(SourceKind.PAINTINGS, false)
        assertTrue((0L until 12L).none { pick(it, noPaint) is Pick.Painting })
        val onlyPaint = SourceSet(photos = false, paintings = true, scenes = false)
        assertTrue((0L until 12L).all { pick(it, onlyPaint) is Pick.Painting })
    }

    @Test
    fun `picks are deterministic per slot, and the paintings mode and the festival override still behave`() {
        for (slot in 0L until 10L) assertEquals(pick(slot, all), pick(slot, all))
        assertTrue(pick(4, all, mode = WallpaperMode.Paintings) is Pick.Painting)
        assertEquals(Pick.SceneKey(77), pick(4, all, fest = 77))
        assertEquals(Pick.SceneKey(2 * WallpaperSchedule.KEY_MULT), pick(4, all, mode = WallpaperMode.Pinned(2), fest = 77))
        assertNull(pick(4, all, mode = WallpaperMode.Off))
    }

    @Test
    fun `Paintings auto-rotate round-trips through the stored wallpaper mode`() {
        assertEquals("paintings", WallpaperSchedule.serialize(WallpaperMode.Paintings))
        assertEquals(WallpaperMode.Paintings, WallpaperSchedule.parse("paintings"))
        assertEquals("paintings", SettingsMigration.normalizeWallpaper("Paintings"))
        assertNotNull(WallpaperSchedule.keyAt(WallpaperMode.Paintings, 0, null))
    }

    @Test
    fun `a grid that fits the viewport is centred vertically and one that does not scrolls from the top`() {
        assertTrue(LayoutBudget.allGridFits(12))
        assertTrue(LayoutBudget.allGridFits(18)) // 3 rows
        assertFalse(LayoutBudget.allGridFits(30)) // 5 rows of 150dp > 462dp
        assertEquals(2, LayoutBudget.allGridRows(12))
        assertEquals(3, LayoutBudget.allGridRows(13))
    }
}
