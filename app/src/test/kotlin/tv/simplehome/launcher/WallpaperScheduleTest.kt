package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.simplehome.launcher.WallpaperSchedule.FADE_MS
import tv.simplehome.launcher.WallpaperSchedule.PERIOD_MS

class WallpaperScheduleTest {
    private val auto = WallpaperMode.Auto

    @Test
    fun `rotation is based on wall-clock so a restart mid-period shows the same scene, not scene 1`() {
        val t = 7 * PERIOD_MS + 12_345
        assertEquals(WallpaperSchedule.sceneAt(auto, t), WallpaperSchedule.sceneAt(auto, t + 20 * 60_000))
        assertEquals(7 % WallpaperSchedule.SCENE_COUNT, WallpaperSchedule.sceneAt(auto, t))
    }

    @Test
    fun `scene advances exactly at each 30 minute boundary`() {
        val before = WallpaperSchedule.sceneAt(auto, 3 * PERIOD_MS - 1)
        val after = WallpaperSchedule.sceneAt(auto, 3 * PERIOD_MS)
        assertEquals(2, before)
        assertEquals(3, after)
    }

    @Test
    fun `rotation wraps after the last scene so the cycle repeats forever`() {
        val n = WallpaperSchedule.SCENE_COUNT
        assertEquals(n - 1, WallpaperSchedule.sceneAt(auto, (n - 1) * PERIOD_MS))
        assertEquals(0, WallpaperSchedule.sceneAt(auto, n * PERIOD_MS))
    }

    @Test
    fun `pinned mode ignores the clock so the user's choice never changes on its own`() {
        val pinned = WallpaperMode.Pinned(3)
        assertEquals(3, WallpaperSchedule.sceneAt(pinned, 0))
        assertEquals(3, WallpaperSchedule.sceneAt(pinned, 99 * PERIOD_MS))
    }

    @Test
    fun `an out of range pin is clamped instead of crashing the scene lookup`() {
        assertEquals(WallpaperSchedule.SCENE_COUNT - 1, WallpaperSchedule.sceneAt(WallpaperMode.Pinned(42), 0))
    }

    @Test
    fun `Off mode has no scene and never requests frames so an idle home screen costs nothing`() {
        assertNull(WallpaperSchedule.sceneAt(WallpaperMode.Off, 123))
        assertFalse(WallpaperSchedule.wantsFrames(WallpaperMode.Off, 1f))
    }

    @Test
    fun `animator scale zero renders a single still frame because the user disabled animations`() {
        assertFalse(WallpaperSchedule.wantsFrames(auto, 0f))
        assertTrue(WallpaperSchedule.wantsFrames(auto, 1f))
        assertTrue(WallpaperSchedule.wantsFrames(WallpaperMode.Pinned(1), 0.5f))
    }

    @Test
    fun `crossfade is active for exactly three seconds then hands over fully to the new scene`() {
        assertTrue(WallpaperSchedule.isFading(0))
        assertTrue(WallpaperSchedule.isFading(FADE_MS - 1))
        assertFalse(WallpaperSchedule.isFading(FADE_MS))
        assertEquals(1f, WallpaperSchedule.fadeProgress(FADE_MS), 0f)
    }

    @Test
    fun `fade progress is linear and clamped so a late frame cannot overshoot the alpha`() {
        assertEquals(0f, WallpaperSchedule.fadeProgress(0), 0f)
        assertEquals(0.5f, WallpaperSchedule.fadeProgress(FADE_MS / 2), 0.001f)
        assertEquals(1f, WallpaperSchedule.fadeProgress(FADE_MS * 10), 0f)
        assertEquals(0f, WallpaperSchedule.fadeProgress(-5), 0f)
    }

    @Test
    fun `frame cap skips every other 60Hz vsync so the wallpaper renders at 30fps`() {
        assertFalse("first frame always renders", WallpaperSchedule.shouldSkipFrame(1000, -1))
        assertTrue(WallpaperSchedule.shouldSkipFrame(1017, 1000))
        assertFalse(WallpaperSchedule.shouldSkipFrame(1033, 1000))
    }

    @Test
    fun `serialization round trips every mode so the choice survives a restart`() {
        val modes = listOf(WallpaperMode.Auto, WallpaperMode.Off) + (0 until WallpaperSchedule.SCENE_COUNT).map { WallpaperMode.Pinned(it) }
        for (m in modes) assertEquals(m, WallpaperSchedule.parse(WallpaperSchedule.serialize(m)))
    }

    @Test
    fun `scene names match the scene count so the dialog and the renderer cannot drift apart`() {
        assertEquals(listOf("Forest", "Jungle", "Beach", "Mountains", "Snow Peaks", "Autumn Valley"), WallpaperSchedule.SCENE_NAMES)
    }

    @Test
    fun `missing or corrupt stored values fall back to Auto, the default`() {
        assertEquals(auto, WallpaperSchedule.parse(null))
        assertEquals(auto, WallpaperSchedule.parse("garbage"))
        assertEquals(auto, WallpaperSchedule.parse("scene:99"))
        assertEquals(auto, WallpaperSchedule.parse("scene:x"))
    }
}
