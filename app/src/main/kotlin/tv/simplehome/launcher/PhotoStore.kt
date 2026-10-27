package tv.simplehome.launcher

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import java.io.File
import java.io.IOException

class PhotoItem(val entry: PhotoEntry, val file: File, val width: Int, val height: Int)

/** Loads the photo collection from the app-specific external dir (no storage permission needed). Blocking: call off the UI thread. */
object PhotoStore {
    private const val MAX_MANIFEST_BYTES = 512 * 1024L
    private const val TAG = "PhotoStore"

    /** Creates photos/ and scenes/ under the app's external files dir so they exist (and are app-owned) before anything is pushed. */
    fun ensureDirs(base: File?): Boolean {
        if (base == null) return false
        return listOf("photos", "scenes").all { name -> val d = File(base, name); d.isDirectory || d.mkdirs() }
    }

    fun load(dir: File?): List<PhotoItem> {
        val debug = BuildConfig.DEBUG
        if (dir == null) { if (debug) Log.d(TAG, "no external files dir"); return emptyList() }
        val manifest = File(dir, "photos.json")
        if (!manifest.isFile || manifest.length() > MAX_MANIFEST_BYTES) {
            if (debug) Log.d(TAG, "manifest missing or too large: ${manifest.path} isFile=${manifest.isFile} len=${manifest.length()}")
            return emptyList()
        }
        val body = try { manifest.readText() } catch (e: IOException) { if (debug) Log.d(TAG, "manifest unreadable: ${e.message}"); return emptyList() }
        val parsed = PhotoManifest.parse(body) as? ManifestResult.Success
        if (parsed == null) { if (debug) Log.d(TAG, "manifest rejected: ${PhotoManifest.parse(body)}"); return emptyList() }
        val root = try { dir.canonicalFile } catch (e: IOException) { return emptyList() }
        val items = parsed.photos.mapNotNull { entry ->
            val file = File(dir, entry.file)
            // The name check already forbids directories; this also requires the file to sit directly inside the photos dir.
            val inside = try { file.canonicalFile.parentFile == root } catch (e: IOException) { false } || file.parentFile?.absolutePath == dir.absolutePath
            if (!inside || !file.isFile) { if (debug) Log.d(TAG, "skip ${entry.file}: inside=$inside isFile=${file.isFile}"); return@mapNotNull null }
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, opts)
            if (opts.outWidth <= 0 || opts.outHeight <= 0 || opts.outMimeType?.startsWith("image/") != true) {
                if (debug) Log.d(TAG, "skip ${entry.file}: not an image (${opts.outMimeType})")
                null
            } else PhotoItem(entry, file, opts.outWidth, opts.outHeight)
        }
        if (debug) Log.d(TAG, "loaded ${items.size} of ${parsed.photos.size} entries (skipped in parser: ${parsed.skipped}) from ${dir.path}")
        return items
    }

    /** Decodes one photo at about the display size: the largest power-of-two subsample that still covers [targetW] x [targetH]. */
    fun decode(item: PhotoItem, targetW: Int, targetH: Int): Bitmap? = decodeFile(item.file, item.width, item.height, targetW, targetH)

    fun decodeFile(file: File, width: Int, height: Int, targetW: Int, targetH: Int): Bitmap? {
        var sample = 1
        while (width / (sample * 2) >= targetW && height / (sample * 2) >= targetH) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888 // RGB_565 bands in skies
        }
        return BitmapFactory.decodeFile(file.path, opts)
    }
}

/** Draws nothing; used when a photo cannot be decoded so the wallpaper degrades to the plain background. */
internal object EmptyScene : Scene {
    override fun draw(c: Canvas, t: Float) {}
    override fun release() {}
}

/** A photo with a slow Ken Burns pan and zoom plus very subtle fog (and a few stars on night photos). */
internal class PhotoScene(private val bitmap: Bitmap, night: Boolean) : Scene {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val fog = Fog(day = !night, strength = if (night) 0.2f else 0.12f)
    private val stars = if (night) Stars(22, 0.2f * H, 77, alphaScale = 0.45f) else null
    private val kb = FloatArray(3)
    private var t0 = -1f

    override fun draw(c: Canvas, t: Float) {
        if (t0 < 0f) t0 = t
        KenBurns.transformInto(kb, t - t0, bitmap.width.toFloat(), bitmap.height.toFloat(), W, H)
        c.save()
        c.translate(W / 2f + kb[1], H / 2f + kb[2])
        c.scale(kb[0], kb[0])
        c.translate(-bitmap.width / 2f, -bitmap.height / 2f)
        c.drawBitmap(bitmap, 0f, 0f, paint)
        c.restore()
        fog.draw(c, t, 0)
        fog.draw(c, t, 1)
        stars?.draw(c, t)
    }

    override fun release() { if (!bitmap.isRecycled) bitmap.recycle() }
}
