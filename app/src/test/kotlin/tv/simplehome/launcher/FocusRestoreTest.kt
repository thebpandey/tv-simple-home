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
}
