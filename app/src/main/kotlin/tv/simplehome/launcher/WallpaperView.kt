package tv.simplehome.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.Choreographer
import android.view.View
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Draws the active scene straight onto the hardware canvas: a cached 1280x720 static layer (scaled with filtering)
 * plus animated overlays at full resolution, then a targeted scrim. Scenes are built on a background thread, only
 * the active one (plus one during a crossfade) is kept, and frame callbacks run only between start() and stop()
 * and never in Off mode. onDraw and the frame path allocate nothing.
 */
class WallpaperView(context: Context) : View(context) {
    private val choreographer = Choreographer.getInstance()
    private val frameCallback = Choreographer.FrameCallback { onFrame(it) }

    private val scenes = arrayOfNulls<Scene>(WallpaperSchedule.KEY_SPACE)
    private val building = BooleanArray(WallpaperSchedule.KEY_SPACE)
    /** Measured legibility of each built photo or painting (null for procedural scenes), see [Legibility]. */
    private val needs = arrayOfNulls<Legibility.Needs>(WallpaperSchedule.KEY_SPACE)
    private var executor: ExecutorService? = null
    private var generation = 0

    private val fadePaint = Paint()
    private val scrimPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val dimPaint = Paint().apply { color = 0xFF000000.toInt() }
    private var dimLevel = 0f // 0..1, animated: the wallpaper dims behind All apps

    private var mode: WallpaperMode = WallpaperMode.Auto
    private var photos: List<PhotoItem> = emptyList()
    private var photoIsNight: List<Boolean> = emptyList()
    private var stills: List<StillItem> = emptyList()
    private var stillEntries: List<StillEntry> = emptyList()
    private var natureStills: IntArray = IntArray(0) // indices into [stills] of the nature paintings
    private var natureNight: List<Boolean> = emptyList()
    private var sources = SourceSet.ALL
    // targetKey() is asked every frame; the answer only changes with the 30-minute slot or a setter, so it is memoised (no per-frame allocation).
    private var memoSlot = Long.MIN_VALUE
    private var memoVersion = -1
    private var version = 0
    private var memoKey: Int? = null
    /** Told what to credit whenever the shown wallpaper changes: "Photo: X / Unsplash" or null for non-photos. */
    var onWallpaperChanged: ((String?) -> Unit)? = null
    /**
     * Brightness (0..255, quote band, from the manifest) and measured legibility of the photo or painting now shown, both null for a
     * procedural scene; they drive the light/dark UI. For an image it fires once the image is decoded and measured.
     */
    var onToneChanged: ((Int?, Legibility.Needs?) -> Unit)? = null
    private var festivalKey: Int? = null
    private var festivalTheme: String? = null
    private var dayVariant = false
    /** Light or dark UI (decides the scrim colour); differs from [dayVariant] when an image's brightness, not the sun, picks the UI mode. */
    private var uiDay = false
    private var started = false
    private var animatorScale = 1f
    private var current = -1
    private var fromKey = -1
    private var fading = false
    private var fadeMs = WallpaperSchedule.FADE_MS
    private var fadeStartMs = 0L
    private var fadeAlpha = 0f
    private var frameMs = 0L
    private var lastFrameMs = -1L
    private val baseMs = System.nanoTime() / 1_000_000

    private var perfSumNs = 0L
    private var perfFrames = 0
    private var perfWindowStartMs = 0L

    init {
        visibility = GONE
    }

    fun setMode(newMode: WallpaperMode) {
        mode = newMode
        version++
        if (started) apply()
    }

    fun photoCount(): Int = photos.size
    fun paintingCount(): Int = natureStills.size

    private var zoom = 1f
    private var dimAnimator: android.animation.ValueAnimator? = null

    /**
     * Launchpad backdrop: dims the wallpaper to 50% and zooms it to 1.04 over 250ms. Both are drawn in onDraw (a canvas scale and the
     * scrim alpha), so there is no view scale, no hardware layer and no offscreen buffer. A real blur is not used: it would re-run on
     * every 30fps wallpaper frame on a Mali-G52.
     */
    fun setDimmed(dimmed: Boolean) {
        val toDim = if (dimmed) 0.5f else 0f
        val toZoom = if (dimmed) 1.04f else 1f
        val fromDim = dimLevel
        val fromZoom = zoom
        dimAnimator?.cancel()
        dimAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250
            interpolator = Motion.EASE
            addUpdateListener {
                val f = it.animatedValue as Float
                dimLevel = fromDim + (toDim - fromDim) * f
                zoom = fromZoom + (toZoom - fromZoom) * f
                invalidate()
            }
            start()
        }
    }

    fun setStills(list: List<StillItem>) {
        stills = list
        stillEntries = list.map { it.entry }
        val nature = list.indices.filter { StillMapping.isNature(list[it].entry.theme) }
            .sortedWith(compareBy({ list[it].entry.theme }, { list[it].entry.index }, { list[it].entry.night }))
        natureStills = nature.toIntArray()
        natureNight = nature.map { list[it].entry.night }
        version++
        if (started) apply()
    }

    fun setPhotos(list: List<PhotoItem>) {
        photos = list
        photoIsNight = list.map { it.entry.night }
        version++
        if (started) apply()
    }

    fun setSources(newSources: SourceSet) {
        sources = newSources
        version++
        if (started) apply()
    }

    /** Set by the activity from today's date; only takes effect in Auto mode (see WallpaperSchedule.keyAt). */
    fun setFestival(key: Int?, theme: String?) {
        if (festivalKey == key && festivalTheme == theme) return
        festivalKey = key
        festivalTheme = theme
        version++
        if (started && !WallpaperSchedule.wantsFrames(mode, animatorScale)) apply()
    }

    fun setUiDay(day: Boolean) {
        if (uiDay == day) return
        uiDay = day
        invalidate()
    }

    /** Day or night variant. With frames running this crossfades over 30 seconds; otherwise it swaps the still frame. */
    fun setDay(day: Boolean) {
        if (dayVariant == day) return
        dayVariant = day
        version++
        if (started && !WallpaperSchedule.wantsFrames(mode, animatorScale)) apply()
    }

    fun start(animationScale: Float) {
        if (started) return
        started = true
        animatorScale = animationScale
        apply()
    }

    fun stop() {
        started = false
        generation++ // discards scenes still being built
        choreographer.removeFrameCallback(frameCallback)
        fading = false
        dropScenes(keep = -1)
        executor?.shutdown()
        executor = null
    }

    private fun targetKey(): Int? {
        val slot = System.currentTimeMillis() / WallpaperSchedule.PERIOD_MS
        if (slot == memoSlot && version == memoVersion) return memoKey
        memoSlot = slot
        memoVersion = version
        memoKey = computeTargetKey(slot)
        return memoKey
    }

    private fun computeTargetKey(slot: Long): Int? {
        val night = !dayVariant
        val pick = WallpaperPicker.pick(mode, sources, slot * WallpaperSchedule.PERIOD_MS, festivalKey, photoIsNight, night, natureNight)
        return when (pick) {
            null -> null
            is Pick.SceneKey -> festivalPaintingKey(pick.key, night, slot) ?: WallpaperSchedule.withDay(pick.key, dayVariant)
            is Pick.Photo -> WallpaperSchedule.PHOTO_BASE + pick.index
            is Pick.Painting -> WallpaperSchedule.STILL_BASE + natureStills[pick.index]
        }
    }

    /** Inside a festival window the festival's own painted designs rotate (per slot, matching the sun state); none present = procedural scene. */
    private fun festivalPaintingKey(baseKey: Int, night: Boolean, slot: Long): Int? {
        val theme = festivalTheme
        if (theme == null || baseKey != festivalKey) return null
        return StillPools.pick(stillEntries, theme, night, slot)?.let { WallpaperSchedule.STILL_BASE + it }
    }

    private fun lumaFor(key: Int): Int? = when {
        WallpaperSchedule.isStillKey(key) -> stills.getOrNull(key - WallpaperSchedule.STILL_BASE)?.entry?.luma
        WallpaperSchedule.isPhotoKey(key) -> photos.getOrNull(key - WallpaperSchedule.PHOTO_BASE)?.entry?.luma
        else -> null
    }

    /** Photos get a stronger veil: scaled by the manifest's luma when known (brighter photo, stronger scrim), a fixed boost otherwise. */
    private fun scrimBoostFor(key: Int): Float {
        if (!WallpaperSchedule.isPhotoKey(key)) return 1f
        val luma = lumaFor(key) ?: return ScrimSpec.PHOTO_BOOST
        return ScrimSpec.photoBoost(luma)
    }

    private fun creditFor(key: Int): String? =
        if (WallpaperSchedule.isPhotoKey(key) && !WallpaperSchedule.isStillKey(key)) photos.getOrNull(key - WallpaperSchedule.PHOTO_BASE)?.let { KenBurns.credit(it.entry) } else null

    /** Show the current target immediately (no fade) and start frames if the mode wants them. */
    private fun apply() {
        choreographer.removeFrameCallback(frameCallback)
        fading = false
        val key = targetKey()
        if (key == null) {
            visibility = GONE
            current = -1
            onToneChanged?.invoke(null, null)
            dropScenes(keep = -1)
            return
        }
        visibility = VISIBLE
        current = key
        onWallpaperChanged?.invoke(creditFor(key))
        dropScenes(keep = key)
        // An image's tone is reported when it has been measured (in the build callback if it is not built yet), so the UI mode is
        // decided once, from the measurement, instead of flipping when the measurement arrives.
        if (ensureBuilt(key) || !isImageKey(key)) notifyTone(key)
        frameMs = System.nanoTime() / 1_000_000
        lastFrameMs = -1
        invalidate()
        if (WallpaperSchedule.wantsFrames(mode, animatorScale)) choreographer.postFrameCallback(frameCallback)
    }

    /** True when the scene is ready; otherwise starts building it off the UI thread and returns false. */
    private fun ensureBuilt(key: Int): Boolean {
        if (scenes[key] != null) return true
        if (building[key]) return false
        building[key] = true
        val ex = executor ?: Executors.newSingleThreadExecutor().also { executor = it }
        val gen = generation
        ex.execute {
            val (scene, measured) = createSceneForKey(key)
            post {
                building[key] = false
                if (gen == generation && started) {
                    scenes[key] = scene
                    needs[key] = measured
                    if (key == current) { notifyTone(key); rebuildScrim(width, height) }
                    invalidate()
                }
            }
        }
        return false
    }

    private fun isImageKey(key: Int) = WallpaperSchedule.isStillKey(key) || WallpaperSchedule.isPhotoKey(key)

    private fun notifyTone(key: Int) = onToneChanged?.invoke(lumaFor(key), needs.getOrNull(key))

    private fun createSceneForKey(key: Int): Pair<Scene, Legibility.Needs?> {
        if (WallpaperSchedule.isStillKey(key)) {
            val item = stills.getOrNull(key - WallpaperSchedule.STILL_BASE) ?: return EmptyScene to null
            val bmp = PhotoStore.decodeFile(item.file, item.width, item.height, width.coerceAtLeast(1920), height.coerceAtLeast(1080)) ?: return EmptyScene to null
            return StillScene(bmp, StillMapping.overlayFor(item.entry)) to measure(bmp)
        }
        if (WallpaperSchedule.isPhotoKey(key)) {
            val item = photos.getOrNull(key - WallpaperSchedule.PHOTO_BASE) ?: return EmptyScene to null
            // One photo at a time, decoded here on the builder thread at about the display size.
            val bmp = PhotoStore.decode(item, width.coerceAtLeast(1920), height.coerceAtLeast(1080)) ?: return EmptyScene to null
            return PhotoScene(bmp, item.entry.night) to measure(bmp)
        }
        val day = WallpaperSchedule.isDayKey(key)
        val base = WallpaperSchedule.baseKey(key)
        val index = base / WallpaperSchedule.KEY_MULT
        val variant = base % WallpaperSchedule.KEY_MULT
        val scene = if (index < WallpaperSchedule.SCENE_COUNT) createScene(index, day)
        else createFestivalScene(WallpaperSchedule.FESTIVAL_SCENE_IDS[index - WallpaperSchedule.SCENE_COUNT], variant, day)
        return scene to null
    }

    /** Grey histograms of the header text areas and the quote column (every 4th pixel both ways), turned into veil needs. Builder thread. */
    private fun measure(bmp: Bitmap): Legibility.Needs {
        val header = IntArray(256)
        val quote = IntArray(256)
        val row = IntArray(bmp.width)
        val sx = bmp.width / 960f
        val sy = bmp.height / 540f
        fun scan(hist: IntArray, xs: ClosedFloatingPointRange<Float>, ys: ClosedFloatingPointRange<Float>) {
            val x0 = (xs.start * sx).toInt().coerceIn(0, bmp.width)
            val x1 = (xs.endInclusive * sx).toInt().coerceIn(0, bmp.width)
            var y = (ys.start * sy).toInt().coerceIn(0, bmp.height)
            val y1 = (ys.endInclusive * sy).toInt().coerceIn(0, bmp.height)
            while (y < y1) {
                bmp.getPixels(row, 0, bmp.width, 0, y, bmp.width, 1)
                var x = x0
                while (x < x1) { hist[Legibility.luma(row[x])]++; x += 4 }
                y += 4
            }
        }
        scan(header, Legibility.HEADER_LEFT_X_DP, Legibility.HEADER_Y_DP)
        scan(header, Legibility.HEADER_RIGHT_X_DP, Legibility.HEADER_Y_DP)
        scan(quote, Legibility.QUOTE_X_DP, Legibility.QUOTE_Y_DP)
        return Legibility.needs(header, quote)
    }

    /**
     * Drops every cached scene except [keep] and frees their bitmaps shortly after: recycling right away could race a frame
     * still queued with the old display list, so release is delayed past the last frame that could reference them.
     */
    private fun dropScenes(keep: Int) {
        for (i in scenes.indices) {
            val old = scenes[i] ?: continue
            if (i == keep) continue
            scenes[i] = null
            needs[i] = null
            postDelayed({ old.release() }, 1500)
        }
    }

    private fun onFrame(frameTimeNanos: Long) {
        val now = frameTimeNanos / 1_000_000
        if (!WallpaperSchedule.shouldSkipFrame(now, lastFrameMs)) {
            frameMs = now
            lastFrameMs = now
            val key = targetKey()
            if (key != null && key != current && !fading && ensureBuilt(key)) beginFade(key, now)
            if (fading && !WallpaperSchedule.isFading(now - fadeStartMs, fadeMs)) endFade()
            invalidate()
        }
        choreographer.postFrameCallback(frameCallback)
    }

    private fun beginFade(next: Int, now: Long) {
        fromKey = current
        fadeMs = WallpaperSchedule.fadeDurationMs(fromKey, next)
        current = next
        onWallpaperChanged?.invoke(creditFor(next))
        notifyTone(next)
        fading = true
        fadeStartMs = now
    }

    private fun endFade() {
        fading = false
        fadeAlpha = 0f
        dropScenes(keep = current)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = rebuildScrim(w, h)

    /** What the current scrim mask was built for; the mask is rebuilt only when this changes. */
    private data class ScrimKey(val day: Boolean, val boost: Float, val top: Float?, val quote: Float?)
    private var scrimKey: ScrimKey? = null
    private var scrimFor = Int.MIN_VALUE
    private var scrimForDay = false
    private var scrimForNeeds: Legibility.Needs? = null
    private var scrimMask: Bitmap? = null
    private val scrimSrc = Rect()
    private val scrimDst = Rect()

    /**
     * Measured images get exactly the veil their pixels need for the current UI mode (see [Legibility]); an image still being measured
     * and procedural scenes keep the brightness-scaled and tuned plateaus.
     */
    private fun wantedScrim(): ScrimKey {
        val n = needs.getOrNull(current)
        if (n != null) return ScrimKey(uiDay, 1f, n.header(uiDay), n.quote(uiDay))
        val top = if (!WallpaperSchedule.isPhotoKey(current)) null else ScrimSpec.topAlpha(lumaFor(current) ?: 150, uiDay)
        return ScrimKey(uiDay, scrimBoostFor(current), top, null)
    }

    /** Builds the scrim as a quarter-resolution ALPHA_8 mask of [ScrimSpec.mask], drawn in white (light UI) or black (dark UI). */
    private fun rebuildScrim(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val key = wantedScrim()
        if (key == scrimKey && scrimMask != null) { scrimFor = current; scrimForDay = uiDay; scrimForNeeds = needs.getOrNull(current); return }
        val mw = (w / 4).coerceAtLeast(1)
        val mh = (h / 4).coerceAtLeast(1)
        val bytes = ScrimSpec.mask(mw, mh, key.day, key.boost, key.top, key.quote)
        val bmp = scrimMask?.takeIf { it.width == mw && it.height == mh } ?: Bitmap.createBitmap(mw, mh, Bitmap.Config.ALPHA_8).also { scrimMask = it }
        bmp.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(bytes))
        bmp.prepareToDraw()
        scrimSrc.set(0, 0, mw, mh)
        scrimDst.set(0, 0, w, h)
        scrimPaint.color = if (key.day) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        scrimKey = key
        scrimFor = current
        scrimForDay = uiDay
        scrimForNeeds = needs.getOrNull(current)
    }

    override fun onDraw(canvas: Canvas) {
        val t0 = if (BuildConfig.DEBUG) System.nanoTime() else 0L
        // Allocation-free staleness check; the mask itself is rebuilt only when the image, its measurement or the UI mode changed.
        if (scrimFor != current || scrimForDay != uiDay || scrimForNeeds !== needs.getOrNull(current)) rebuildScrim(width, height)
        val tSec = (frameMs - baseMs) / 1000f
        val w = width.toFloat()
        val h = height.toFloat()
        val zoomed = zoom != 1f
        if (zoomed) { canvas.save(); canvas.scale(zoom, zoom, w / 2f, h / 2f) }
        if (fading) {
            drawScene(canvas, fromKey, tSec)
            fadeAlpha = WallpaperSchedule.fadeProgress(frameMs - fadeStartMs, fadeMs)
            val layer = canvas.saveLayerAlpha(0f, 0f, w, h, (fadeAlpha * 255f).toInt())
            drawScene(canvas, current, tSec)
            canvas.restoreToCount(layer)
        } else if (current >= 0) {
            drawScene(canvas, current, tSec)
        }
        scrimMask?.let { canvas.drawBitmap(it, scrimSrc, scrimDst, scrimPaint) }
        if (zoomed) canvas.restore()
        if (dimLevel > 0f) {
            dimPaint.alpha = (dimLevel * 255f).toInt()
            canvas.drawRect(0f, 0f, w, h, dimPaint)
        }
        if (BuildConfig.DEBUG) logFrameTime(System.nanoTime() - t0)
    }

    private fun drawScene(canvas: Canvas, key: Int, tSec: Float) {
        val scene = scenes[key] ?: return
        canvas.save()
        canvas.scale(width / WP_W.toFloat(), height / WP_H.toFloat())
        scene.draw(canvas, tSec)
        canvas.restore()
    }

    // Debug only: average UI-thread cost of recording one frame, logged every 5s.
    private fun logFrameTime(costNs: Long) {
        if (perfWindowStartMs == 0L) perfWindowStartMs = frameMs
        perfSumNs += costNs
        perfFrames++
        if (frameMs - perfWindowStartMs >= 5000) {
            val avgMs = perfSumNs / perfFrames / 1e6
            val fps = perfFrames * 1000.0 / (frameMs - perfWindowStartMs)
            Log.d("WallpaperPerf", "scene=$current avgDrawMs=${"%.2f".format(avgMs)} fps=${"%.1f".format(fps)}")
            perfSumNs = 0
            perfFrames = 0
            perfWindowStartMs = frameMs
        }
    }

    override fun onDetachedFromWindow() {
        choreographer.removeFrameCallback(frameCallback)
        executor?.shutdownNow()
        executor = null
        super.onDetachedFromWindow()
    }
}
