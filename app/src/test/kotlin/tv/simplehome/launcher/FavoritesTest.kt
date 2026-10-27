package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class FavoritesTest {
    private val abc = listOf("a", "b", "c")

    @Test
    fun `uninstalled default apps are skipped so the user never sees dead tiles`() {
        val installed = setOf("com.google.android.youtube.tv", "com.wbd.stream", "unrelated.app")
        assertEquals(listOf("com.google.android.youtube.tv", "com.wbd.stream"), Favorites.seed(installed))
    }

    @Test
    fun `default order is fixed with the browser last and the stock Hisense browser never seeded`() {
        assertEquals(
            listOf(
                "com.netflix.ninja", "com.google.android.youtube.tv", "com.amazon.amazonvideo.livingroom",
                "com.debridstream.tv", "com.wbd.stream", "com.phlox.tvwebbrowser",
            ),
            Favorites.DEFAULTS,
        )
        assertFalse("com.hisense.odinbrowser" in Favorites.DEFAULTS)
    }

    @Test
    fun `defaults keep their curated order regardless of install order`() {
        val installed = setOf("com.wbd.stream", "com.netflix.ninja")
        assertEquals(listOf("com.netflix.ninja", "com.wbd.stream"), Favorites.seed(installed))
    }

    @Test
    fun `a saved empty list stays empty so a user who cleared favorites does not get defaults back`() {
        val saved = Favorites.deserialize(Favorites.serialize(emptyList()))
        assertEquals(emptyList<String>(), Favorites.resolve(saved, Favorites.DEFAULTS.toSet()))
    }

    @Test
    fun `nothing saved falls back to defaults on first launch`() {
        assertNull(Favorites.deserialize(null))
        assertEquals(listOf("com.netflix.ninja"), Favorites.resolve(null, setOf("com.netflix.ninja")))
    }

    @Test
    fun `saved favorites that were uninstalled are dropped but order of the rest is kept`() {
        assertEquals(listOf("c", "a"), Favorites.resolve(listOf("c", "gone", "a"), setOf("a", "c")))
    }

    @Test
    fun `adding an existing favorite is a no-op so tiles are never duplicated`() {
        assertEquals(abc, Favorites.add(abc, "b"))
    }

    @Test
    fun `new favorites are appended at the end so existing positions do not shift under the user`() {
        assertEquals(listOf("a", "b", "c", "d"), Favorites.add(abc, "d"))
    }

    @Test
    fun `removing keeps the order of the remaining favorites`() {
        assertEquals(listOf("a", "c"), Favorites.remove(abc, "b"))
    }

    @Test
    fun `removing an unknown package changes nothing`() {
        assertEquals(abc, Favorites.remove(abc, "zzz"))
    }

    @Test
    fun `moving the first favorite left is a no-op rather than wrapping, so the order stays predictable`() {
        assertSame(abc, Favorites.moveLeft(abc, "a"))
    }

    @Test
    fun `moving the last favorite right is a no-op rather than wrapping, so the order stays predictable`() {
        assertSame(abc, Favorites.moveRight(abc, "c"))
    }

    @Test
    fun `moving swaps with the neighbour so one key press moves exactly one slot`() {
        assertEquals(listOf("b", "a", "c"), Favorites.moveLeft(abc, "b"))
        assertEquals(listOf("a", "c", "b"), Favorites.moveRight(abc, "b"))
    }

    @Test
    fun `moving a package that is not a favorite changes nothing`() {
        assertEquals(abc, Favorites.moveLeft(abc, "zzz"))
        assertEquals(abc, Favorites.moveRight(abc, "zzz"))
    }

    @Test
    fun `serialization round trips so the order survives an app restart`() {
        val list = listOf("com.z.last", "com.a.first", "com.m.mid")
        assertEquals(list, Favorites.deserialize(Favorites.serialize(list)))
    }

    @Test
    fun `corrupt blank lines in storage are ignored instead of producing blank tiles`() {
        assertEquals(listOf("a", "b"), Favorites.deserialize("a\n\n  \nb\n"))
    }
}
