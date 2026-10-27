package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Test
import tv.simplehome.launcher.DockFocus.Favorite
import tv.simplehome.launcher.DockFocus.MenuButton
import tv.simplehome.launcher.DockFocus.Pill

class DockNavTest {
    private fun step(open: Boolean, f: DockFocus, k: DockKey, pills: Int = 4, favs: Int = 5) =
        DockNav.next(DockState(open, f), k, pills, favs)

    @Test
    fun `RIGHT walks the pills one by one so Settings, Wallpaper and All apps are reachable`() {
        assertEquals(DockState(true, Pill(1)), step(true, Pill(0), DockKey.RIGHT))
        assertEquals(DockState(true, Pill(2)), step(true, Pill(1), DockKey.RIGHT))
        assertEquals(DockState(true, Pill(3)), step(true, Pill(2), DockKey.RIGHT))
    }

    @Test
    fun `only RIGHT on the last pill closes the menu and focuses the first favorite`() {
        assertEquals(DockState(false, Favorite(0)), step(true, Pill(3), DockKey.RIGHT))
        for (i in 0..2) assertEquals("pill $i keeps the menu open", true, step(true, Pill(i), DockKey.RIGHT).menuOpen)
    }

    @Test
    fun `LEFT on the first pill returns to the menu button and keeps the menu open`() {
        assertEquals(DockState(true, MenuButton), step(true, Pill(0), DockKey.LEFT))
        assertEquals(DockState(true, Pill(0)), step(true, Pill(1), DockKey.LEFT))
    }

    @Test
    fun `from the button RIGHT enters the pills when open and the favorites when closed`() {
        assertEquals(DockState(true, Pill(0)), step(true, MenuButton, DockKey.RIGHT))
        assertEquals(DockState(false, Favorite(0)), step(false, MenuButton, DockKey.RIGHT))
    }

    @Test
    fun `LEFT on the first favorite goes to the menu button and nothing is left of the button`() {
        assertEquals(DockState(false, MenuButton), step(false, Favorite(0), DockKey.LEFT))
        assertEquals(DockState(false, MenuButton), step(false, MenuButton, DockKey.LEFT))
    }

    @Test
    fun `favorites move one by one and RIGHT on the last favorite stays put`() {
        assertEquals(DockState(false, Favorite(3)), step(false, Favorite(2), DockKey.RIGHT))
        assertEquals(DockState(false, Favorite(4)), step(false, Favorite(4), DockKey.RIGHT))
        assertEquals(DockState(false, Favorite(1)), step(false, Favorite(2), DockKey.LEFT))
    }

    @Test
    fun `with no favorites the last pill hands focus back to the button instead of nowhere`() {
        assertEquals(DockState(false, MenuButton), step(true, Pill(3), DockKey.RIGHT, favs = 0))
        assertEquals(DockState(false, MenuButton), step(false, MenuButton, DockKey.RIGHT, favs = 0))
    }
}
