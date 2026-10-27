package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusRestoreTest {
    private val favs = listOf("com.netflix.ninja", "com.google.android.youtube.tv", "com.wbd.stream")

    @Test
    fun `returning from a favorite puts focus back on the tile that launched it`() {
        assertEquals(RestoreTarget.Favorite(1), FocusRestore.target("com.google.android.youtube.tv", favs))
        assertEquals(RestoreTarget.Favorite(2), FocusRestore.target("com.wbd.stream", favs))
    }

    @Test
    fun `an app launched from All apps or from outside falls back to the first favorite`() {
        assertEquals(RestoreTarget.Favorite(0), FocusRestore.target("com.some.other.app", favs))
        assertEquals(RestoreTarget.Favorite(0), FocusRestore.target(null, favs))
    }

    @Test
    fun `with no favorites the menu button gets focus, never the weather widget`() {
        assertEquals(RestoreTarget.Menu, FocusRestore.target("com.x", emptyList()))
        assertEquals(RestoreTarget.Menu, FocusRestore.target(null, emptyList()))
    }

    @Test
    fun `a favorite that was removed while the app ran no longer matches`() {
        assertEquals(RestoreTarget.Favorite(0), FocusRestore.target("com.removed", favs))
    }

    @Test
    fun `waking the TV rebuilds the dock, and the weather widget that inherits focus gives it back to the dock`() {
        assertEquals(false, FocusRestore.keepCurrent(currentShown = true, currentIsWeather = true, weatherWasFocused = false))
    }

    @Test
    fun `a rebuild while the user is on the weather widget leaves them there`() {
        assertEquals(true, FocusRestore.keepCurrent(currentShown = true, currentIsWeather = true, weatherWasFocused = true))
    }

    @Test
    fun `a visible dock tile keeps focus, and a hidden view never does`() {
        assertEquals(true, FocusRestore.keepCurrent(currentShown = true, currentIsWeather = false, weatherWasFocused = false))
        assertEquals(false, FocusRestore.keepCurrent(currentShown = false, currentIsWeather = false, weatherWasFocused = false))
    }
}
