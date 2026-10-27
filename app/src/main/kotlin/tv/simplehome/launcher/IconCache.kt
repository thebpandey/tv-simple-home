package tv.simplehome.launcher

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Squircle icons rendered once per package at [px] (the largest size any screen draws: base x max magnification x density),
 * on one background thread. Dock icons stay cached; the All apps ones are evicted when that screen closes, and everything
 * is dropped in onStop, so the memory cost is only paid while the Launchpad is open.
 */
class IconCache(private val main: Handler, private val px: Int) {
    private val cache = HashMap<String, Bitmap>()
    private val waiting = HashMap<String, MutableList<(Bitmap) -> Unit>>()
    private var executor: ExecutorService? = null
    private var generation = 0
    private var inFlight = 0
    private var renderedInBatch = 0

    fun get(pkg: String): Bitmap? = cache[pkg]

    /** [source] runs on the worker (PackageManager icon loading is slow); [onReady] runs on the main thread. */
    fun request(pkg: String, source: () -> Drawable?, onReady: (Bitmap) -> Unit) {
        cache[pkg]?.let { onReady(it); return }
        val list = waiting[pkg]
        if (list != null) { list.add(onReady); return }
        waiting[pkg] = mutableListOf(onReady)
        inFlight++
        val ex = executor ?: Executors.newSingleThreadExecutor().also { executor = it }
        val gen = generation
        ex.execute {
            val bmp = try { source()?.let { IconRenderer.render(it, px) } } catch (e: RuntimeException) { null }
            main.post {
                if (gen != generation) return@post
                inFlight--
                renderedInBatch++
                val callbacks = waiting.remove(pkg)
                if (bmp == null) return@post
                cache[pkg] = bmp
                callbacks?.forEach { it(bmp) }
                if (inFlight == 0) settle()
            }
        }
    }

    /**
     * Loading an app icon decodes its full-size resource bitmaps (adaptive icons: two layers of up to 432px) into native memory that stays
     * until a GC happens to run; a batch of 12+ icons leaves tens of MB of garbage. Once a batch has drained, ask for one collection.
     */
    private fun settle() {
        if (renderedInBatch >= GC_BATCH) executor?.execute { Runtime.getRuntime().gc() }
        renderedInBatch = 0
    }

    fun evictExcept(keep: Set<String>) {
        cache.keys.retainAll(keep)
    }

    /** Drops everything, including work still queued (its results are discarded by the generation check). */
    fun clear() {
        generation++
        inFlight = 0
        renderedInBatch = 0
        cache.clear()
        waiting.clear()
        executor?.shutdownNow()
        executor = null
    }

    fun size(): Int = cache.size

    private companion object { const val GC_BATCH = 4 }
}
