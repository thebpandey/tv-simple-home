package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotosTest {
    private fun entry(id: String = "a", file: String = "a.jpg", variant: String = "night", extra: String = "") =
        """{"id":"$id","file":"$file","variant":"$variant","photographer":"Ann","photographerUrl":"u","photoUrl":"p","description":"d"$extra}"""

    private fun manifest(vararg entries: String, version: String = "1") = """{"version":$version,"photos":[${entries.joinToString(",")}]}"""

    private fun ok(r: ManifestResult) = (r as ManifestResult.Success)

    @Test
    fun `a valid manifest yields its photos in order`() {
        val r = ok(PhotoManifest.parse(manifest(entry("a", "a.jpg", "night"), entry("b", "b.png", "day"))))
        assertEquals(listOf("a", "b"), r.photos.map { it.id })
        assertEquals(listOf(true, false), r.photos.map { it.night })
        assertEquals(0, r.skipped)
    }

    @Test
    fun `empty, non-JSON and wrong-version manifests fail instead of crashing the wallpaper`() {
        assertEquals(FailureReason.EMPTY_BODY, (PhotoManifest.parse("") as ManifestResult.Failure).reason)
        assertEquals(FailureReason.BAD_JSON, (PhotoManifest.parse("<html>") as ManifestResult.Failure).reason)
        assertEquals(FailureReason.WRONG_TYPE, (PhotoManifest.parse(manifest(version = "2")) as ManifestResult.Failure).reason)
        assertEquals(FailureReason.MISSING_FIELD, (PhotoManifest.parse("""{"version":1}""") as ManifestResult.Failure).reason)
    }

    @Test
    fun `entries with missing fields or a bad variant are skipped, not trusted`() {
        val r = ok(PhotoManifest.parse(manifest(
            entry("a"), """{"id":"x","file":"x.jpg","variant":"night"}""", entry("c", variant = "dusk"), "42", entry("d", "d.jpg", "day"),
        )))
        assertEquals(listOf("a", "d"), r.photos.map { it.id })
        assertEquals(3, r.skipped)
    }

    @Test
    fun `file names with directories or traversal are rejected so a manifest can't point outside the photos folder`() {
        for (bad in listOf("../secret.jpg", "a/b.jpg", "..\\\\x.jpg", "/etc/passwd.jpg", ".hidden.jpg", "a..b.jpg", "noext", "x.exe", ""))
            assertFalse("should reject '$bad'", PhotoManifest.isSafeFileName(bad))
        for (good in listOf("a.jpg", "Night_Sky-3.JPEG", "p.png", "q.webp")) assertTrue(good, PhotoManifest.isSafeFileName(good))
        assertEquals(0, ok(PhotoManifest.parse(manifest(entry("t", "../x.jpg")))).photos.size)
    }

    @Test
    fun `an oversized manifest is capped at 200 entries so it can't exhaust memory`() {
        val many = (0 until 500).map { entry("id$it", "f$it.jpg") }.toTypedArray()
        val r = ok(PhotoManifest.parse(manifest(*many)))
        assertEquals(PhotoManifest.MAX_ENTRIES, r.photos.size)
        assertEquals(300, r.skipped)
    }

    private val flags = listOf(true, true, false, true, false, true) // night, night, day, night, day, night

    @Test
    fun `photos matching the sun state are chosen, so daytime shows day photos`() {
        for (slot in 0L until 40L) {
            assertFalse(flags[WallpaperPicker.pickPhotoIndex(flags, night = false, slot = slot)!!])
            assertTrue(flags[WallpaperPicker.pickPhotoIndex(flags, night = true, slot = slot)!!])
        }
    }

    @Test
    fun `when no photo matches the sun state any photo is used rather than none`() {
        val onlyNight = listOf(true, true)
        assertNotNull(WallpaperPicker.pickPhotoIndex(onlyNight, night = false, slot = 3))
        assertNull(WallpaperPicker.pickPhotoIndex(emptyList(), night = true, slot = 3))
    }

    @Test
    fun `a photo choice is deterministic per 30 minute slot and cycles through every matching photo`() {
        val first = WallpaperPicker.pickPhotoIndex(flags, true, 7)
        assertEquals(first, WallpaperPicker.pickPhotoIndex(flags, true, 7))
        val night = flags.indices.count { flags[it] }
        val seen = (0 until night).map { WallpaperPicker.pickPhotoIndex(flags, true, it.toLong() + night * 5L) }.toSet()
        assertEquals("each night photo exactly once per cycle", night, seen.size)
    }

    private fun mixed(slot: Long, source: PhotoSource = PhotoSource.SCENES_AND_PHOTOS, fest: Int? = null, mode: WallpaperMode = WallpaperMode.Auto, photos: List<Boolean> = flags) =
        WallpaperPicker.pick(mode, WallpaperSources.fromLegacy(source.id)!!.copy(paintings = false), slot * WallpaperSchedule.PERIOD_MS + 5, fest, photos, night = true)

    @Test
    fun `mixed mode alternates photo, scene, photo, scene every 30 minutes (sources take turns, photo first)`() {
        for (slot in 0L until 12L) {
            val p = mixed(slot)
            if (slot % 2 == 0L) assertTrue("slot $slot should be a photo", p is Pick.Photo) else assertTrue("slot $slot should be a scene", p is Pick.SceneKey)
        }
    }

    @Test
    fun `mixed mode visits every scene, not just the even-numbered ones`() {
        val scenes = (1L until 25L step 2).map { (mixed(it) as Pick.SceneKey).key }.toSet()
        assertEquals(WallpaperSchedule.SCENE_COUNT, scenes.size)
    }

    @Test
    fun `scenes only and photos only sources never mix`() {
        for (slot in 0L until 10L) {
            assertTrue(mixed(slot, PhotoSource.SCENES) is Pick.SceneKey)
            assertTrue(mixed(slot, PhotoSource.PHOTOS) is Pick.Photo)
        }
    }

    @Test
    fun `without any photos every source falls back to scenes`() {
        assertTrue(mixed(1, PhotoSource.PHOTOS, photos = emptyList()) is Pick.SceneKey)
        assertTrue(mixed(1, mode = WallpaperMode.Photos, photos = emptyList()) is Pick.SceneKey)
    }

    @Test
    fun `a festival overrides Auto in every source but never a pinned scene or Off`() {
        val fest = WallpaperSchedule.festivalSceneKey("diwali", 0)!!
        assertEquals(Pick.SceneKey(fest), mixed(3, PhotoSource.PHOTOS, fest))
        assertEquals(Pick.SceneKey(2 * WallpaperSchedule.KEY_MULT), mixed(3, fest = fest, mode = WallpaperMode.Pinned(2)))
        assertNull(mixed(3, fest = fest, mode = WallpaperMode.Off))
    }

    private fun assertCovers(photoW: Float, photoH: Float, viewW: Float, viewH: Float, t: Float) {
        val tr = KenBurns.transform(t, photoW, photoH, viewW, viewH)
        val w = photoW * tr.scale
        val h = photoH * tr.scale
        val left = viewW / 2f + tr.tx - w / 2f
        val top = viewH / 2f + tr.ty - h / 2f
        val eps = 0.001f
        assertTrue("left edge exposed at t=$t ($photoW x $photoH): $left", left <= eps)
        assertTrue("top edge exposed at t=$t: $top", top <= eps)
        assertTrue("right edge exposed at t=$t", left + w >= viewW - eps)
        assertTrue("bottom edge exposed at t=$t", top + h >= viewH - eps)
    }

    @Test
    fun `the Ken Burns pan and zoom never exposes an edge for any time or photo shape`() {
        for ((pw, ph) in listOf(1920f to 1080f, 4000f to 3000f, 3000f to 4000f, 1000f to 1000f, 6000f to 1000f, 1280f to 720f)) {
            var t = 0f
            while (t <= 4000f) { assertCovers(pw, ph, 480f, 270f, t); t += 7.3f }
        }
    }

    @Test
    fun `the zoom grows smoothly by about 8 percent over the 30 minutes and then holds`() {
        val start = KenBurns.transform(0f, 1920f, 1080f, 480f, 270f).scale
        val mid = KenBurns.transform(900f, 1920f, 1080f, 480f, 270f).scale
        val end = KenBurns.transform(1800f, 1920f, 1080f, 480f, 270f).scale
        val later = KenBurns.transform(3600f, 1920f, 1080f, 480f, 270f).scale
        assertEquals(1.08f, end / start, 0.005f)
        assertTrue(start < mid && mid < end)
        assertEquals(end, later, 1e-6f)
    }

    @Test
    fun `the drift moves slowly, never more than a few view units per second`() {
        var prev = KenBurns.transform(0f, 1920f, 1080f, 480f, 270f)
        var maxStep = 0f
        var t = 0.5f
        while (t < 600f) {
            val cur = KenBurns.transform(t, 1920f, 1080f, 480f, 270f)
            maxStep = maxOf(maxStep, kotlin.math.abs(cur.tx - prev.tx), kotlin.math.abs(cur.ty - prev.ty))
            prev = cur; t += 0.5f
        }
        assertTrue("max step per 0.5s = $maxStep", maxStep < 0.5f)
    }

    @Test
    fun `the credit line names the photographer and Unsplash`() {
        val e = ok(PhotoManifest.parse(manifest(entry()))).photos.first()
        assertEquals("Photo: Ann / Unsplash", KenBurns.credit(e))
        assertNotEquals("", KenBurns.credit(e))
    }

    @Test
    fun `photo wallpaper keys never collide with scene keys`() {
        assertTrue(WallpaperSchedule.isPhotoKey(WallpaperSchedule.PHOTO_BASE))
        assertFalse(WallpaperSchedule.isPhotoKey(WallpaperSchedule.PHOTO_BASE - 1))
        assertFalse(WallpaperSchedule.isDayKey(WallpaperSchedule.PHOTO_BASE + 3))
        assertTrue(WallpaperSchedule.PHOTO_BASE > WallpaperSchedule.DAY_OFFSET + (WallpaperSchedule.SCENE_COUNT + WallpaperSchedule.FESTIVAL_SCENE_IDS.size) * WallpaperSchedule.KEY_MULT)
    }

    @Test
    fun `the real manifest shape with an extra luma field and numeric ids parses and keeps every entry`() {
        val json = """{"version":1,"photos":[
            {"id":"abc123","file":"unsplash-abc123.jpg","variant":"night","photographer":"Jane Doe","photographerUrl":"https://unsplash.com/@jane","photoUrl":"https://unsplash.com/photos/abc123","description":"Milky way over a lake","luma":42},
            {"id":777,"file":"unsplash-777.jpg","variant":"day","photographer":"John Roe","photographerUrl":"u","photoUrl":"p","description":"","luma":201,"future":{"x":1}}
        ]}"""
        val r = ok(PhotoManifest.parse(json))
        assertEquals(2, r.photos.size)
        assertEquals(0, r.skipped)
        assertEquals(42, r.photos[0].luma)
        assertEquals("777", r.photos[1].id)
        assertTrue(r.photos[0].night && !r.photos[1].night)
    }

    @Test
    fun `a bright photo gets a stronger scrim than a dark one`() {
        assertTrue(ScrimSpec.photoBoost(230) > ScrimSpec.photoBoost(20))
        assertEquals(1f, ScrimSpec.photoBoost(0), 0.001f)
    }

    @Test
    fun `an explicit Photos choice is not hijacked by a festival`() {
        val fest = WallpaperSchedule.festivalSceneKey("karwa_chauth", 0)!!
        for (slot in 0L until 8L) {
            assertTrue(WallpaperPicker.pick(WallpaperMode.Photos, WallpaperSources.fromLegacy("scenes")!!, slot * WallpaperSchedule.PERIOD_MS, fest, flags, night = true) is Pick.Photo)
        }
    }
}
