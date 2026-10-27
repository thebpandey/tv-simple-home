package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMigrationTest {
    @Test
    fun `a pinned Forest survives the update unchanged`() {
        val r = SettingsMigration.migrate(mapOf("wallpaper" to "scene:0"))
        assertFalse(r.puts.containsKey("wallpaper"))
        assertEquals(WallpaperMode.Pinned(0), WallpaperSchedule.parse("scene:0"))
    }

    @Test
    fun `legacy shapes of the wallpaper choice are normalised, not reset to Auto`() {
        assertEquals("scene:0", SettingsMigration.normalizeWallpaper("Forest"))
        assertEquals("scene:4", SettingsMigration.normalizeWallpaper("scene:Snow Peaks"))
        assertEquals("scene:2", SettingsMigration.normalizeWallpaper("2"))
        assertEquals("off", SettingsMigration.normalizeWallpaper("OFF"))
        assertEquals("auto", SettingsMigration.normalizeWallpaper(" auto "))
    }

    @Test
    fun `an unrecognised value is kept exactly as stored rather than discarded`() {
        assertEquals("something-new", SettingsMigration.normalizeWallpaper("something-new"))
        assertFalse(SettingsMigration.migrate(mapOf("wallpaper" to "something-new")).puts.containsKey("wallpaper"))
    }

    @Test
    fun `people who already chose a wallpaper keep scenes only, so the new photos default does not change what they see`() {
        val r = SettingsMigration.migrate(mapOf("wallpaper" to "auto"))
        assertEquals(PhotoSource.SCENES.id, r.puts["photo_source"])
        assertEquals(SettingsMigration.CURRENT_VERSION, r.newVersion)
    }

    @Test
    fun `a fresh install gets the new default of photos only`() {
        val r = SettingsMigration.migrate(emptyMap<String, Any>())
        assertTrue(r.puts.isEmpty())
        assertEquals(PhotoSource.PHOTOS, PhotoSource.parse(null))
    }

    @Test
    fun `an explicit photo source is never overwritten`() {
        val r = SettingsMigration.migrate(mapOf("wallpaper" to "auto", "photo_source" to "both"))
        assertFalse(r.puts.containsKey("photo_source"))
    }

    @Test
    fun `migration runs once and then does nothing`() {
        val r = SettingsMigration.migrate(mapOf("wallpaper" to "Forest", SettingsMigration.KEY_VERSION to SettingsMigration.CURRENT_VERSION))
        assertTrue(r.puts.isEmpty())
    }

    @Test
    fun `migration never removes a stored setting`() {
        val prefs = mapOf("wallpaper" to "Forest", "favorites" to "a\nb", "festival_wallpapers" to false, "day_night_mode" to "night")
        val r = SettingsMigration.migrate(prefs)
        // The result only contains additions or replacements for keys that already exist; nothing is deleted by design.
        assertTrue(r.puts.keys.all { it == "wallpaper" || it == "photo_source" })
    }
}
