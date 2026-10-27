package tv.simplehome.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/*
 * Layered-landscape wallpapers. Each scene is built once per (scene, variant, day/night) on a background thread:
 * its static layers (sky, ridgelines, trees) are painted into `base`, a 1280x720 bitmap, using the logical
 * 480x270 coordinate system (staticCanvas() scales it). draw() receives a canvas that the view has scaled to the
 * same logical system but at full screen resolution, blits `base` with filtering and then draws a few animated
 * overlay elements straight onto the hardware canvas (anti-aliased, full resolution). Overlay code allocates
 * nothing; construction may allocate freely.
 */

const val WP_W = 480
const val WP_H = 270
const val BASE_W = 1280
const val BASE_H = 720
internal const val W = WP_W.toFloat()
internal const val H = WP_H.toFloat()
internal const val TAU = (2.0 * PI).toFloat()

interface Scene {
    fun draw(c: Canvas, t: Float)
    fun release()
}

fun createScene(index: Int, day: Boolean): Scene = when (index) {
    0 -> ForestScene(day)
    1 -> JungleScene(day)
    2 -> BeachScene(day)
    3 -> MountainsScene(day)
    4 -> SnowPeaksScene(day)
    else -> AutumnScene(day)
}

/** Read only while a scene is being constructed (single builder thread), never while drawing. */
internal object Style {
    @Volatile var day = false
}

internal fun vivid(color: Int): Int {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color, hsv)
    hsv[1] = (hsv[1] * 1.28f + 0.04f).coerceAtMost(1f)
    var v = (hsv[2] - 0.45f) * 1.12f + 0.465f
    if (Style.day) v = v * 0.85f + 0.2f
    hsv[2] = v.coerceIn(0f, 1f)
    return android.graphics.Color.HSVToColor(android.graphics.Color.alpha(color), hsv)
}

// ---------- helpers (build time unless noted) ----------

internal fun hash(i: Int, seed: Int): Float {
    var h = i * 374761393 + seed * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFF) / 65535f
}

internal fun vnoise(x: Float, seed: Int): Float {
    val i = floor(x).toInt()
    val f = x - i
    val u = f * f * (3f - 2f * f)
    return hash(i, seed) * (1f - u) + hash(i + 1, seed) * u
}

internal fun fbm(x: Float, seed: Int): Float =
    0.55f * vnoise(x, seed) + 0.3f * vnoise(x * 2.3f, seed + 1) + 0.15f * vnoise(x * 5.1f, seed + 2)

internal fun wrap(v: Float, m: Float): Float {
    val r = v % m
    return if (r < 0f) r + m else r
}

internal fun withAlpha(rgb: Int, a: Int): Int = (rgb and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

internal fun lerpColor(a: Int, b: Int, f: Float): Int {
    fun ch(shift: Int) = (((a shr shift) and 255) + ((((b shr shift) and 255) - ((a shr shift) and 255)) * f)).toInt()
    return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

internal fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = vivid(color) }

internal fun gradientRect(c: Canvas, y0: Float, y1: Float, colors: IntArray, stops: FloatArray? = null) {
    val p = Paint(Paint.DITHER_FLAG).apply { shader = LinearGradient(0f, y0, 0f, y1, IntArray(colors.size) { vivid(colors[it]) }, stops, Shader.TileMode.CLAMP) }
    c.drawRect(-MARGIN_X - 8f, if (y0 <= 0f) y0 - MARGIN_Y - 4f else y0, W + MARGIN_X + 8f, if (y1 >= H) y1 + MARGIN_Y + 4f else y1, p)
}

internal fun glow(c: Canvas, cx: Float, cy: Float, r: Float, rgb: Int, alpha: Int) {
    val p = Paint(Paint.DITHER_FLAG).apply {
        shader = RadialGradient(cx, cy, r, intArrayOf(vivid(withAlpha(rgb or 0xFF000000.toInt(), alpha)), withAlpha(rgb, 0)), null, Shader.TileMode.CLAMP)
    }
    c.drawCircle(cx, cy, r, p)
}

/** Wide, flat glow hugging the horizon (an ellipse made by squashing a radial gradient). */
internal fun horizonGlow(c: Canvas, y: Float, rgb: Int, alpha: Int) {
    val p = Paint(Paint.DITHER_FLAG).apply {
        shader = RadialGradient(0f, 0f, W * 0.55f, intArrayOf(vivid(withAlpha(rgb or 0xFF000000.toInt(), alpha)), withAlpha(rgb, 0)), null, Shader.TileMode.CLAMP)
    }
    c.save(); c.translate(W / 2f, y); c.scale(1f, 0.28f); c.drawCircle(0f, 0f, W * 0.55f, p); c.restore()
}

/** Thin light streaks (water reflections) between two y values. */
internal fun streaks(c: Canvas, y0: Float, y1: Float, rgb: Int, n: Int, seed: Int, alpha: Int, xMin: Float = 0f, xMax: Float = W) {
    val r = Random(seed.toLong())
    val p = fill(withAlpha(rgb, alpha))
    repeat(n) {
        val y = y0 + r.nextFloat() * (y1 - y0)
        val len = 6f + r.nextFloat() * 26f * (0.4f + (y - y0) / (y1 - y0))
        val x = xMin + r.nextFloat() * (xMax - xMin)
        c.drawRect(x, y, x + len, y + 0.5f, p)
    }
}

/** Ridge height function: occupies [base-amp, base]. sharp = pointed crests (mountains). */
internal fun ridge(base: Float, amp: Float, freq: Float, seed: Int, sharp: Boolean = false): (Float) -> Float =
    { x ->
        val n = fbm(x * freq, seed)
        base - amp * (if (sharp) 1f - abs(2f * n - 1f) else n)
    }

internal fun fillRidge(
    c: Canvas, color: Int, y: (Float) -> Float, down: (Float) -> Float = { 0f }, detail: Boolean = true,
) {
    fun outline(extra: (Float) -> Float, closeToBottom: Boolean): Path {
        val path = Path()
        val x0 = -MARGIN_X - 8f
        val x1 = W + MARGIN_X + 8f
        if (closeToBottom) path.moveTo(x0, H + MARGIN_Y + 4f) else path.moveTo(x0, y(x0) + down(x0) + extra(x0))
        var x = x0
        while (x <= x1) { path.lineTo(x, y(x) + down(x) + extra(x)); x += 1f }
        if (closeToBottom) { path.lineTo(x1, H + MARGIN_Y + 4f); path.close() }
        return path
    }
    c.drawPath(outline({ 0f }, true), fill(color))
    if (!detail) return
    val seed = color and 0xFFFF
    // Soft noise-shaded bands give the hill some texture instead of one flat tone.
    c.drawPath(outline({ x -> 7f + 12f * fbm(x * 0.045f, seed) }, true), fill(withAlpha(0x000000, 0x1C)))
    c.drawPath(outline({ x -> 20f + 16f * fbm(x * 0.03f + 7f, seed + 5) }, true), fill(withAlpha(0xFFFFFF, 0x0E)))
    // Rim light: a thin lighter edge along the crest (about 1.5px at 1280 width).
    val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.56f
        this.color = withAlpha(lerpColor(vivid(color), 0xFFFFF0D0.toInt(), 0.55f), 0xB0)
    }
    c.drawPath(outline({ 0f }, false), rim)
}

/** Fog that thickens toward the base of a layer; the next, nearer layer covers its lower part. */
internal fun haze(c: Canvas, y0: Float, y1: Float, rgb: Int, alpha: Int) =
    gradientRect(c, y0, y1, intArrayOf(withAlpha(rgb, 0), withAlpha(rgb, alpha)))

internal fun pines(c: Canvas, color: Int, y: (Float) -> Float, count: Int, hMin: Float, hMax: Float, seed: Int) {
    val rnd = Random(seed.toLong())
    val p = fill(color)
    repeat(count) {
        val x = rnd.nextFloat() * W
        val h = hMin + rnd.nextFloat() * (hMax - hMin)
        val w = h * (0.40f + 0.18f * rnd.nextFloat())
        val by = y(x) + 2f
        val tiers = 3 + rnd.nextInt(3)
        val lean = (rnd.nextFloat() - 0.5f) * 0.12f * h
        val xs = FloatArray(tiers * 2)
        val ys = FloatArray(tiers * 2)
        for (i in 1..tiers) { // each tier: outer branch tip, then a notch back toward the trunk
            val f = i / tiers.toFloat()
            val ty = by - h * (1f - f) - h * 0.02f * rnd.nextFloat()
            val cx = x + lean * (1f - f)
            val outer = w * 0.5f * f * (0.88f + 0.24f * rnd.nextFloat())
            xs[(i - 1) * 2] = cx + outer; ys[(i - 1) * 2] = ty
            xs[(i - 1) * 2 + 1] = cx + outer * 0.45f; ys[(i - 1) * 2 + 1] = ty - h * 0.015f
        }
        val path = Path()
        path.moveTo(x + lean, by - h)
        for (i in 0 until tiers * 2) path.lineTo(xs[i], ys[i])
        path.lineTo(x + 1.5f, by + 3f); path.lineTo(x - 1.5f, by + 3f)
        for (i in tiers * 2 - 1 downTo 0) path.lineTo(2f * (x + lean * (1f - (i / 2 + 1) / tiers.toFloat())) - xs[i], ys[i])
        path.close()
        c.drawPath(path, p)
    }
}

internal fun palm(c: Canvas, color: Int, bx: Float, by: Float, tx: Float, ty: Float, frond: Float) {
    val trunk = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = 3.2f }
    val path = Path()
    path.moveTo(bx, by)
    path.quadTo(bx + (tx - bx) * 0.2f, by + (ty - by) * 0.6f, tx, ty)
    c.drawPath(path, trunk)
    val leaf = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = 1.2f }
    val spine = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = 2f }
    for (k in 0 until 8) {
        val ang = (-170f + k * 20f) * (PI.toFloat() / 180f)
        val dx = cos(ang)
        val dy = sin(ang)
        var px = tx
        var py = ty
        for (s in 1..10) {
            val f = s / 10f
            val nx = tx + dx * frond * f
            val ny = ty + dy * frond * f + frond * 0.55f * f * f // droop
            c.drawLine(px, py, nx, ny, spine)
            if (s >= 2) {
                c.drawLine(nx, ny, nx - dy * 5f * (1f - f * 0.5f) + dx * 2f, ny + dx * 5f * (1f - f * 0.5f) + 3f, leaf)
                c.drawLine(nx, ny, nx + dy * 5f * (1f - f * 0.5f) + dx * 2f, ny - dx * 5f * (1f - f * 0.5f) + 3f, leaf)
            }
            px = nx; py = ny
        }
    }
}

internal enum class Depth(val scale: Float, val topFrac: Float, val panAmp: Float) {
    // scale = bitmap pixels per logical unit (the screen is 4 px per unit): the sky holds only smooth gradients, hazy far layers
    // are 62% resolution, the foreground 88%. Discs, crescents, glows and all particles are drawn as vectors at full resolution.
    SKY(1.5f, 0f, 0.004f), FAR(2.5f, 0.2f, 0.012f), MID(2.5f, 0.35f, 0.02f), NEAR(3.5f, 0.5f, 0.03f),
}

internal const val MARGIN_X = 24f // each layer is drawn 10% wider and taller than the screen so the camera never exposes an edge
internal const val MARGIN_Y = 12f

private val LAYER_PAINT = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

private class Layer(val depth: Depth) {
    val yTop = depth.topFrac * H - MARGIN_Y
    val dst = RectF(-MARGIN_X, yTop, W + MARGIN_X, H + MARGIN_Y)
    val bitmap: Bitmap = Bitmap.createBitmap(
        kotlin.math.ceil(dst.width() * depth.scale).toInt(), kotlin.math.ceil(dst.height() * depth.scale).toInt(), Bitmap.Config.ARGB_8888,
    )
}

/**
 * A scene is 4 depth layers (sky, far, mid, near; the last three are transparent bitmaps cropped to their band) that a
 * slow camera moves by different amounts (parallax), with sky, fog and particle effects drawn between and over them.
 * Painting code works in the 480x270 logical system; layer(depth) retargets the shared canvas to that layer.
 */
internal abstract class LayeredScene(protected val day: Boolean, fogStrength: Float = 0.35f) : Scene {
    private val layers = Array(4) { Layer(Depth.entries[it]) }
    private val canvas = Canvas()
    private val skyFx = SkyFx(day)
    private val fog = Fog(day, fogStrength)

    private var celX = -1f
    private var celY = 0f
    private var celGlowR = 0f
    private var discR = 0f
    private val discPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val craterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var celPath: Path? = null
    private val celPathPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val celGlow = Paint(Paint.DITHER_FLAG)
    private val rayPaint = Paint(Paint.DITHER_FLAG)
    private val rayPath = Path().apply { moveTo(0f, 0f); lineTo(330f, -11f); lineTo(330f, 11f); close() }

    private var pan = 0f
    private var bob = 0f
    private var zoom = 1f

    init { Style.day = day }

    protected abstract fun overlay(c: Canvas, t: Float)

    /** Retargets the shared canvas to [d]'s bitmap, in logical coordinates. */
    protected fun layer(d: Depth): Canvas {
        val l = layers[d.ordinal]
        canvas.setBitmap(l.bitmap)
        canvas.scale(d.scale, d.scale)
        canvas.translate(MARGIN_X, -l.yTop)
        return canvas
    }

    protected fun staticCanvas(): Canvas = layer(Depth.SKY)

    /** Two-variant sky: dark palette at night, bright one by day. */
    protected fun sky(c: Canvas, y1: Float, night: IntArray, daySky: IntArray, stops: FloatArray?) =
        gradientRect(c, 0f, y1, if (day) daySky else night, stops)

    /** Sun by day, moon at night, same spot so both variants read as the same place. The glow itself breathes in the overlay. */
    protected fun celestialGlow(f: Focal) = celestialGlow(f.x, f.y, f.r)

    private fun celestialGlow(x: Float, y: Float, r: Float) {
        celX = x; celY = y
        celGlowR = if (day) r * 6f else r * 4.5f
        val rgb = if (day) 0xFFE9A8 else 0xBFD8FF
        celGlow.shader = RadialGradient(0f, 0f, celGlowR, intArrayOf(vivid(0xFF000000.toInt() or rgb), withAlpha(rgb, 0)), null, Shader.TileMode.CLAMP)
        rayPaint.shader = RadialGradient(0f, 0f, 330f, intArrayOf(vivid(0xFFFFF2C0.toInt()), withAlpha(0xFFF2C0, 0)), null, Shader.TileMode.CLAMP)
    }

    /** The disc itself is drawn as a vector every frame (crisp at any resolution); [c] is unused but kept for call sites. */
    @Suppress("UNUSED_PARAMETER")
    protected fun celestial(c: Canvas, f: Focal) {
        celestialGlow(f.x, f.y, f.r)
        val r = f.r
        discR = if (day) r * 1.1f else r
        discPaint.color = vivid(if (day) 0xFFFFF6D8.toInt() else 0xFFEEF3FF.toInt())
        craterPaint.color = withAlpha(0x8A94B8, 0x50)
    }

    /** A crescent or other custom moon shape, drawn as a vector in the sky pass. */
    protected fun celestialPath(path: Path, color: Int) {
        celPath = path
        celPathPaint.color = vivid(color)
    }

    /** Camera: pan = sum of two sines (periods ~70s and ~113s), a small vertical bob and a slow 1.00-1.04 zoom. */
    private fun updateCamera(t: Float) {
        pan = 0.6f * sin(TAU * t / 70f) + 0.4f * sin(TAU * t / 113f + 1.3f)
        bob = sin(TAU * t / 53f + 0.7f) * 0.006f * H
        zoom = 1.02f + 0.02f * sin(TAU * t / 90f)
    }

    private fun camera(c: Canvas, panAmp: Float, bobK: Float) {
        c.save()
        c.translate(W / 2f, H / 2f)
        c.scale(zoom, zoom)
        c.translate(-W / 2f + pan * panAmp * W, -H / 2f + bob * bobK)
    }

    private fun drawLayer(c: Canvas, d: Depth, bobK: Float) {
        camera(c, d.panAmp, bobK)
        val l = layers[d.ordinal]
        c.drawBitmap(l.bitmap, null, l.dst, LAYER_PAINT)
        c.restore()
    }

    final override fun draw(c: Canvas, t: Float) {
        updateCamera(t)
        drawLayer(c, Depth.SKY, 0.3f)
        // Living sky under the sky camera: breathing sun/moon glow, slow god rays by day, clouds and birds.
        camera(c, Depth.SKY.panAmp, 0.3f)
        if (celX >= 0f) {
            val breathe = sin(TAU * t / 8f)
            c.save()
            c.translate(celX, celY)
            val k = 1f + 0.06f * breathe
            c.scale(k, k)
            celGlow.alpha = (if (day) 190f else 150f).times(1f + 0.15f * breathe).toInt().coerceIn(0, 255)
            c.drawCircle(0f, 0f, celGlowR, celGlow)
            if (day) {
                val base = t * (360f / 240f) // one revolution in 4 minutes
                for (i in 0 until 7) {
                    c.save()
                    c.rotate(base + i * (360f / 7f))
                    rayPaint.alpha = (70f + 40f * sin(0.7f * t + i * 1.9f)).toInt().coerceIn(0, 255)
                    c.drawPath(rayPath, rayPaint)
                    c.restore()
                }
            }
            c.restore()
        }
        if (discR > 0f) {
            c.drawCircle(celX, celY, discR, discPaint)
            if (!day) {
                c.drawCircle(celX - discR * 0.3f, celY - discR * 0.2f, discR * 0.22f, craterPaint)
                c.drawCircle(celX + discR * 0.35f, celY + discR * 0.3f, discR * 0.16f, craterPaint)
            }
        }
        celPath?.let { c.drawPath(it, celPathPaint) }
        skyFx.draw(c, t)
        c.restore()
        drawLayer(c, Depth.FAR, 1f)
        camera(c, Depth.FAR.panAmp, 1f); fog.draw(c, t, 0); c.restore()
        drawLayer(c, Depth.MID, 1f)
        camera(c, Depth.MID.panAmp, 1f); fog.draw(c, t, 1); c.restore()
        drawLayer(c, Depth.NEAR, 1f)
        // Scene overlays (flames, particles, water glints) sit on the near layer, so they use its camera.
        camera(c, Depth.NEAR.panAmp, 1f)
        overlay(c, t)
        c.restore()
    }

    /** Called by the view well after the scene stopped being drawn, so recycling cannot race a queued frame. */
    override fun release() {
        for (l in layers) if (!l.bitmap.isRecycled) l.bitmap.recycle()
        skyFx.release()
    }
}

/** Soft pre-rendered cloud sprites drifting at different depths (bright by day, thin and moonlit at night) plus daytime birds. */
internal class SkyFx(private val day: Boolean) {
    private val sprites = Array(4) { makeSprite(it) }
    private val cx = floatArrayOf(40f, 190f, 330f, 450f)
    private val cy = floatArrayOf(34f, 62f, 24f, 82f)
    private val cw = floatArrayOf(130f, 100f, 80f, 62f) // nearer = bigger = faster
    private val cv = floatArrayOf(8f, 5f, 3.5f, 2.5f)
    private val dst = RectF()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = if (day) 235 else 80 }
    private val bird = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC1A2030.toInt(); style = Paint.Style.STROKE; strokeWidth = 0.8f; strokeCap = Paint.Cap.ROUND }

    private fun makeSprite(seed: Int): Bitmap {
        val bmp = Bitmap.createBitmap(256, 96, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = Random(seed * 31L + 5)
        val rgb = if (day) 0xFFFFFF else 0xC8D6F4
        val p = Paint()
        repeat(7) {
            val x = 50f + r.nextFloat() * 156f
            val y = 40f + r.nextFloat() * 22f
            val rad = 22f + r.nextFloat() * 22f
            p.shader = RadialGradient(x, y, rad, intArrayOf(withAlpha(rgb, 200), withAlpha(rgb, 0)), null, Shader.TileMode.CLAMP)
            c.drawCircle(x, y, rad, p)
        }
        return bmp
    }

    fun release() { for (b in sprites) if (!b.isRecycled) b.recycle() }

    fun draw(c: Canvas, t: Float) {
        for (i in 0 until 4) {
            val x = wrap(cx[i] + cv[i] * t, W + cw[i] * 2f) - cw[i]
            dst.set(x, cy[i], x + cw[i], cy[i] + cw[i] * 0.375f)
            c.drawBitmap(sprites[i], null, dst, paint)
        }
        if (!day) return
        val period = 34f
        val fx = -30f + wrap(t, period) / period * (W + 60f)
        for (b in 0 until 5) {
            val bx = fx - b * 9f
            val by = 55f + 8f * kotlin.math.abs(b - 2) + 6f * sin(0.6f * t)
            val flap = 2.2f * sin(9f * t + b)
            c.drawLine(bx - 3.5f, by - flap, bx, by, bird)
            c.drawLine(bx, by, bx + 3.5f, by - flap, bird)
        }
    }
}

/** Wide translucent fog bands that glide sideways between depth layers. */
internal class Fog(day: Boolean, private val strength: Float) {
    private val paint = Paint().apply {
        val rgb = if (day) 0xFFFFFF else 0x9FB4D0
        shader = RadialGradient(0f, 0f, 150f, intArrayOf(withAlpha(rgb, 150), withAlpha(rgb, 0)), null, Shader.TileMode.CLAMP)
    }
    private val y = floatArrayOf(0.60f * H, 0.74f * H, 0.84f * H, 0.68f * H)
    private val x0 = floatArrayOf(40f, 280f, 150f, 400f)
    private val v = floatArrayOf(6f, -9f, 12f, -5f)

    fun draw(c: Canvas, t: Float, group: Int) {
        if (strength <= 0f) return
        paint.alpha = (255f * strength).toInt().coerceIn(0, 255)
        for (k in 0 until 2) {
            val i = group * 2 + k
            val x = wrap(x0[i] + v[i] * t, W + 600f) - 300f
            c.save()
            c.translate(x, y[i])
            c.scale(1.9f, 0.24f)
            c.drawCircle(0f, 0f, 150f, paint)
            c.restore()
        }
    }
}

/** Sliding highlight streaks on water (they glide sideways and pulse), for sea, lake and river scenes. */
internal class WaterFx(private val n: Int, private val y0: Float, private val y1: Float, private val rgb: Int, seed: Int) {
    private val x = FloatArray(n)
    private val y = FloatArray(n)
    private val len = FloatArray(n)
    private val v = FloatArray(n)
    private val ph = FloatArray(n)
    private val paint = Paint()

    init {
        val r = Random(seed.toLong())
        for (i in 0 until n) {
            y[i] = y0 + (y1 - y0) * (i + r.nextFloat()) / n
            val depth = (y[i] - y0) / (y1 - y0)
            len[i] = 5f + 22f * depth + r.nextFloat() * 8f
            v[i] = (2f + 6f * depth) * (if (i % 2 == 0) 1f else -1f)
            x[i] = r.nextFloat() * W; ph[i] = r.nextFloat() * TAU
        }
    }

    fun draw(c: Canvas, t: Float) {
        for (i in 0 until n) {
            val a = (30f + 120f * (0.5f + 0.5f * sin(0.8f * t + ph[i]))).toInt()
            paint.color = withAlpha(rgb, a)
            val px = wrap(x[i] + v[i] * t, W + 40f) - 20f
            c.drawRect(px, y[i], px + len[i], y[i] + 0.7f, paint)
        }
    }
}

/** Drifting motes in 3 depth classes (near = bigger, faster, brighter): pollen by day, blinking fireflies at night. */
internal class Motes(private val n: Int, private val rgb: Int, private val blink: Boolean, seed: Int, private val yMin: Float, private val yMax: Float) {
    private val x = FloatArray(n)
    private val y = FloatArray(n)
    private val ph = FloatArray(n)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        val r = Random(seed.toLong())
        for (i in 0 until n) { x[i] = r.nextFloat() * W; y[i] = yMin + r.nextFloat() * (yMax - yMin); ph[i] = r.nextFloat() * TAU }
    }

    fun draw(c: Canvas, t: Float) {
        for (i in 0 until n) {
            val cls = i % 3 // 0 far, 1 mid, 2 near
            val speed = 1.5f + cls * 2.2f
            val px = wrap(x[i] + speed * t + 8f * sin(0.25f * t + ph[i]), W)
            val py = yMin + wrap(y[i] - yMin - 0.5f * speed * t + 4f * sin(0.2f * t + ph[i] * 2f), yMax - yMin)
            var a = 90 + cls * 60
            if (blink) a = (a * (0.5f + 0.5f * sin(0.7f * t + ph[i])).coerceAtLeast(0f)).toInt()
            paint.color = withAlpha(rgb, a)
            c.drawCircle(px, py, 0.7f + cls * 0.5f, paint)
        }
    }
}

/** Small twinkling star set shared by night scenes. */
internal class Stars(n: Int, maxY: Float, seed: Int, private val alphaScale: Float = 1f) {
    private val count = n
    private val x = FloatArray(n)
    private val y = FloatArray(n)
    private val ph = FloatArray(n)
    private val fr = FloatArray(n)
    private val big = BooleanArray(n)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        val r = Random(seed.toLong())
        for (i in 0 until n) {
            x[i] = r.nextFloat() * W; y[i] = r.nextFloat() * maxY
            ph[i] = r.nextFloat() * TAU; fr[i] = 0.5f + r.nextFloat() * 1.2f; big[i] = r.nextInt(5) == 0
        }
    }

    fun draw(c: Canvas, t: Float) {
        for (i in 0 until count) {
            val a = ((70f + 150f * (0.5f + 0.5f * sin(fr[i] * t + ph[i]))) * alphaScale).toInt()
            val r = if (big[i]) 0.7f else 0.42f // 1.7 to 2.8 px on a 1080p screen
            paint.color = withAlpha(0xE6ECFF, a / 5)
            c.drawCircle(x[i], y[i], r * 2.6f, paint) // faint halo
            paint.color = withAlpha(0xE6ECFF, a)
            c.drawCircle(x[i], y[i], r, paint)
        }
    }
}

// ---------- 1. Forest (Everforest) ----------

private class ForestScene(day: Boolean) : LayeredScene(day, 1f) {
    private val mist = Paint(Paint.DITHER_FLAG).apply {
        shader = RadialGradient(0f, 0f, 110f, intArrayOf(0x55E8EBD0, 0x00E8EBD0), null, Shader.TileMode.CLAMP)
    }
    private val bandY = floatArrayOf(0.56f * H, 0.70f * H, 0.84f * H)
    private val bandSpeed = floatArrayOf(4f, 7f, 11f)
    private val bandX = floatArrayOf(60f, 300f, 180f)
    private val stars = Stars(60, 0.4f * H, 31)
    private val motes = Motes(24, if (day) 0xFFF0B0 else 0xD8F070, blink = !day, seed = 51, yMin = 0.35f * H, yMax = 0.9f * H)

    init {
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF050B14.toInt(), 0xFF123040.toInt(), 0xFF3A5E58.toInt()),
            intArrayOf(0xFF3C7FCB.toInt(), 0xFF9ACBE6.toInt(), 0xFFF4ECC6.toInt()),
            floatArrayOf(0f, 0.45f, 0.68f),
        )
        horizonGlow(c, 0.56f * H, if (day) 0xFFE9B0 else 0x4A8A80, 0x90)
        celestial(c, Focals.FOREST)
        val bases = floatArrayOf(0.50f, 0.58f, 0.67f, 0.77f, 0.90f)
        val amps = floatArrayOf(38f, 40f, 44f, 46f, 36f)
        val freqs = floatArrayOf(0.006f, 0.007f, 0.008f, 0.009f, 0.011f)
        for (i in 0..4) {
            layer(if (i < 2) Depth.FAR else if (i < 4) Depth.MID else Depth.NEAR)
            val y = ridge(bases[i] * H, amps[i], freqs[i], 10 + i)
            val color = lerpColor(if (day) 0xFFA8C48A.toInt() else 0xFF3A5A52.toInt(), if (day) 0xFF1E3A26.toInt() else 0xFF0A1412.toInt(), i / 4f)
            fillRidge(c, color, y)
            if (i >= 2) pines(c, color, y, 10 + i * 8, 10f + i * 3f, 22f + i * 6f, 3 + i)
            if (i < 4) haze(c, bases[i] * H - amps[i] * 0.4f, bases[i] * H + 24f, if (day) 0xDCE8C8 else 0x6A9A92, 0x55)
        }
    }

    override fun overlay(c: Canvas, t: Float) {
        if (!day) stars.draw(c, t)
        motes.draw(c, t)
        for (i in 0..2) {
            val x = wrap(bandX[i] + bandSpeed[i] * t, W + 440f) - 220f
            c.save()
            c.translate(x, bandY[i])
            c.scale(1.6f, 0.24f)
            c.drawCircle(0f, 0f, 110f, mist)
            c.restore()
        }
    }
}

// ---------- 2. Jungle (Kanagawa) ----------

private class JungleScene(day: Boolean) : LayeredScene(day, 1f) {
    private val rayPath = Path()
    private val rayPaint = Paint(Paint.DITHER_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, H * 0.8f, 0x55E6C384, 0x00E6C384, Shader.TileMode.CLAMP)
    }
    private val rayX = floatArrayOf(110f, 250f, 380f)
    private val rayPhase = floatArrayOf(0f, 2f, 4f)
    private val flyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fx = FloatArray(10)
    private val fy = FloatArray(10)
    private val fph = FloatArray(10)
    private val pollen = Motes(20, 0xFFF0B0, blink = false, seed = 52, yMin = 0.3f * H, yMax = 0.9f * H)

    init {
        val r = Random(21)
        for (i in 0 until 10) { fx[i] = 40f + r.nextFloat() * 400f; fy[i] = 110f + r.nextFloat() * 110f; fph[i] = r.nextFloat() * TAU }
        rayPath.moveTo(0f, 0f); rayPath.lineTo(34f, 0f); rayPath.lineTo(34f - 70f, H * 0.8f); rayPath.lineTo(-70f, H * 0.8f); rayPath.close()

        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF040C14.toInt(), 0xFF0C2A30.toInt(), 0xFF1E4A44.toInt()),
            intArrayOf(0xFF2E86B8.toInt(), 0xFF7CC4C0.toInt(), 0xFFD8E6A8.toInt()),
            floatArrayOf(0f, 0.5f, 0.8f),
        )
        horizonGlow(c, 0.5f * H, if (day) 0xF6E3A0 else 0x2A6A60, 0x80)
        celestial(c, Focals.JUNGLE)
        val bases = floatArrayOf(0.42f, 0.52f, 0.63f, 0.75f, 0.90f)
        val amps = floatArrayOf(40f, 46f, 52f, 56f, 50f)
        for (i in 0..4) {
            layer(if (i < 2) Depth.FAR else if (i < 4) Depth.MID else Depth.NEAR)
            val b = bases[i] * H
            val y: (Float) -> Float = { x -> ridge(b, amps[i], 0.012f + i * 0.002f, 40 + i)(x) - 6f * vnoise(x * 0.3f, 90 + i) }
            fillRidge(c, lerpColor(if (day) 0xFF6FB08A.toInt() else 0xFF2A5A50.toInt(), if (day) 0xFF16301F.toInt() else 0xFF050C0A.toInt(), i / 4f), y)
            if (i < 4) haze(c, b - amps[i] * 0.5f, b + 28f, if (day) 0xB5D8B8 else 0x4A8A7A, 0x58)
        }
        val dark = 0xFF0C1816.toInt()
        // Foreground: palms and broad leaves in the corners, plus a few hanging vines.
        palm(c, dark, 30f, H, 70f, 0.30f * H, 78f)
        palm(c, dark, 455f, H, 410f, 0.38f * H, 70f)
        broadLeaf(c, dark, 0f, H, -0.9f, 120f, 34f)
        broadLeaf(c, dark, 130f, H, -1.3f, 80f, 24f)
        broadLeaf(c, dark, W, H, -2.3f, 130f, 36f)
        broadLeaf(c, dark, 340f, H, -1.9f, 70f, 22f)
        vines(c, 0xFF1A2E26.toInt())
    }

    private fun broadLeaf(c: Canvas, color: Int, bx: Float, by: Float, ang: Float, len: Float, wid: Float) {
        val dx = cos(ang); val dy = sin(ang)
        val tipX = bx + dx * len; val tipY = by + dy * len
        val p = Path()
        p.moveTo(bx, by)
        p.quadTo(bx + dx * len * 0.5f - dy * wid, by + dy * len * 0.5f + dx * wid, tipX, tipY)
        p.quadTo(bx + dx * len * 0.5f + dy * wid, by + dy * len * 0.5f - dx * wid, bx, by)
        c.drawPath(p, fill(color))
    }

    private fun vines(c: Canvas, color: Int) {
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = 1.4f }
        val dot = fill(color)
        val xs = floatArrayOf(90f, 200f, 300f, 420f)
        val ls = floatArrayOf(90f, 60f, 110f, 75f)
        for (k in xs.indices) {
            val p = Path()
            p.moveTo(xs[k], 0f)
            p.quadTo(xs[k] + 12f, ls[k] * 0.5f, xs[k] - 4f, ls[k])
            c.drawPath(p, line)
            var d = 14f
            while (d < ls[k]) { c.drawCircle(xs[k] + 5f * sin(d * 0.2f), d, 2.6f, dot); d += 14f }
        }
    }

    override fun overlay(c: Canvas, t: Float) {
        if (day) pollen.draw(c, t)
        if (day) for (i in 0..2) {
            rayPaint.alpha = (255f * (0.35f + 0.65f * (0.5f + 0.5f * sin(0.15f * t + rayPhase[i])))).toInt()
            c.save()
            c.translate(rayX[i] + 25f * sin(0.05f * t + rayPhase[i]), 0f)
            c.drawPath(rayPath, rayPaint)
            c.restore()
        }
        if (!day) for (i in 0 until 10) { // fireflies blink at night only
            val b = sin(0.45f * t + fph[i])
            if (b <= 0f) continue
            val x = fx[i] + 14f * sin(0.11f * t + fph[i])
            val y = fy[i] + 8f * sin(0.17f * t + fph[i] * 2f)
            flyPaint.color = withAlpha(0xF2D98B, (50f * b * b).toInt())
            c.drawCircle(x, y, 4.5f, flyPaint)
            flyPaint.color = withAlpha(0xFFF0B0, (230f * b * b).toInt())
            c.drawCircle(x, y, 1.4f, flyPaint)
        }
    }
}

// ---------- 3. Beach (Catppuccin sunset) ----------

private class BeachScene(day: Boolean) : LayeredScene(day) {
    private val horizon = 0.50f * H
    private val sunX = Focals.BEACH.x
    private val foam = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.6f }
    private val path = Path()
    private val glint = Paint()
    private val water = WaterFx(26, 0.5f * H + 3f, 0.74f * H, if (day) 0xFFFFFF else 0xCFE0FF, 61)

    private fun shoreY(x: Float) = 0.74f * H + 0.10f * H * (x / W) + 3f * sin(x * 0.03f)

    init {
        val c = staticCanvas()
        sky(
            c, horizon,
            intArrayOf(0xFF050818.toInt(), 0xFF14204A.toInt(), 0xFF3A3A78.toInt(), 0xFF7A6A9A.toInt()),
            intArrayOf(0xFF2A8AE0.toInt(), 0xFF6CBCF0.toInt(), 0xFFB8E4F6.toInt(), 0xFFE6F6FA.toInt()),
            floatArrayOf(0f, 0.4f, 0.75f, 1f),
        )
        horizonGlow(c, horizon, if (day) 0xFFFFFF else 0x7A7ACA, 0x90)
        celestial(c, Focals.BEACH)
        layer(Depth.FAR)
        // Sea: pale at the horizon, deepening toward the shore (turquoise by day, moonlit navy at night).
        gradientRect(
            c, horizon, H,
            if (day) intArrayOf(0xFFA6E8EC.toInt(), 0xFF3CC8CC.toInt(), 0xFF1CA2B0.toInt(), 0xFF0E7C90.toInt())
            else intArrayOf(0xFF3A4A80.toInt(), 0xFF1A2A58.toInt(), 0xFF0E1A3A.toInt(), 0xFF080F26.toInt()),
            floatArrayOf(0f, 0.2f, 0.55f, 0.9f),
        )
        streaks(c, horizon + 2f, shoreY(sunX) - 4f, if (day) 0xFFFFFF else 0xCFE0FF, 90, 3, 0x70, sunX - 70f, sunX + 70f)
        val band = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f; color = 0x30FFFFFF }
        for (k in 1..6) {
            val p = Path()
            val y0 = horizon + k * k * 3.2f + 4f
            p.moveTo(0f, y0)
            var x = 0f
            while (x <= W) { p.lineTo(x, y0 + 1.5f * sin(x * 0.04f + k)); x += 8f }
            c.drawPath(p, band)
        }
        layer(Depth.NEAR)
        // Sand
        val sand = Path()
        sand.moveTo(0f, H)
        var x = 0f
        while (x <= W) { sand.lineTo(x, shoreY(x)); x += 4f }
        sand.lineTo(W, H); sand.close()
        val sandPaint = Paint(Paint.DITHER_FLAG).apply {
            shader = if (day) LinearGradient(0f, 0.74f * H, 0f, H, vivid(0xFFF8E4B8.toInt()), vivid(0xFFD2A86E.toInt()), Shader.TileMode.CLAMP)
            else LinearGradient(0f, 0.74f * H, 0f, H, vivid(0xFF6A6A92.toInt()), vivid(0xFF30304E.toInt()), Shader.TileMode.CLAMP)
        }
        c.drawPath(sand, sandPaint)
        val dark = if (day) 0xFF14402A.toInt() else 0xFF0A0C1C.toInt()
        palm(c, dark, 34f, 0.92f * H, 62f, 0.34f * H, 72f)
        palm(c, dark, 96f, 0.97f * H, 78f, 0.52f * H, 56f)
    }

    override fun overlay(c: Canvas, t: Float) {
        // Sun glints on the water: dashes widen toward the viewer and flicker out of phase.
        water.draw(c, t)
        val shoreAtSun = shoreY(sunX)
        for (i in 0 until 14) {
            val y = horizon + 3f + i * (shoreAtSun - horizon - 12f) / 14f
            val w = (5f + i * 3.4f) * (0.55f + 0.45f * sin(0.9f * t + i * 1.7f))
            glint.color = withAlpha(if (day) 0xFFFFFF else 0xCFE0FF, (210 * (1f - i / 16f)).toInt())
            val cx = sunX + 3f * sin(0.5f * t + i)
            c.drawRect(cx - w / 2f, y, cx + w / 2f, y + 1.6f, glint)
        }
        // Foam lines roll up the sand and slide back.
        for (k in 0..2) {
            val s = 0.5f + 0.5f * sin(0.35f * t + k * 2.1f)
            path.reset()
            var x = 0f
            val lift = 3f + k * 6f + 12f * (1f - s)
            path.moveTo(0f, shoreY(0f) - lift)
            while (x <= W) { x += 8f; path.lineTo(x, shoreY(x) - lift + 1.5f * sin(x * 0.08f + t)) }
            foam.color = withAlpha(0xFFF4EA, (50f + 130f * s).toInt())
            c.drawPath(path, foam)
        }
    }
}

// ---------- 4. Mountains (Tokyo Night) ----------

private class MountainsScene(day: Boolean) : LayeredScene(day, 1f) {
    private val waterline = 0.70f * H
    private val stars = Stars(46, 0.28f * H, 5)
    private val shimmer = Paint()
    private val sx = FloatArray(14)
    private val sl = FloatArray(14)
    private val sp = FloatArray(14)

    private val water = WaterFx(24, waterline + 4f, H - 4f, if (day) 0xFFFFFF else 0xA8C0FF, 62)

    init {
        val r = Random(9)
        for (i in 0 until 14) { sx[i] = r.nextFloat() * W; sl[i] = 20f + r.nextFloat() * 50f; sp[i] = r.nextFloat() * TAU }
        paintUpper(mirror = false)
        paintUpper(mirror = true) // reflect the scene into the lake
        val c = layer(Depth.NEAR) // the water tint must sit above every reflected layer
        gradientRect(c, waterline, H, if (day) intArrayOf(0x882A4A80.toInt(), 0xCC1A3058.toInt()) else intArrayOf(0x881A1B26.toInt(), 0xCC15161F.toInt()))
        streaks(c, waterline + 2f, H - 4f, if (day) 0xFFFFFF else 0xA8C0FF, 70, 11, 0x60, 120f, 400f)
    }

    /** Retargets to depth [d]; the mirrored pass flips the lower part of the layer upside down about the waterline. */
    private fun begin(d: Depth, mirror: Boolean): Canvas {
        val c = layer(d)
        if (mirror) {
            c.clipRect(-40f, waterline, W + 40f, H + 20f)
            c.translate(0f, waterline); c.scale(1f, -1f); c.translate(0f, -waterline)
        } else {
            c.clipRect(-40f, -30f, W + 40f, waterline)
        }
        return c
    }

    private fun paintUpper(mirror: Boolean) {
        var c = begin(Depth.SKY, mirror)
        sky(
            c, waterline,
            intArrayOf(0xFF080A1E.toInt(), 0xFF1E2766.toInt(), 0xFF5A3F94.toInt(), 0xFFB65C8C.toInt()),
            intArrayOf(0xFF2F6FD0.toInt(), 0xFF6FB0EC.toInt(), 0xFFB8DAF4.toInt(), 0xFFF2E8E0.toInt()),
            floatArrayOf(0f, 0.4f, 0.75f, 1f),
        )
        horizonGlow(c, waterline - 8f, if (day) 0xFFE8C8 else 0xFF7AB0, 0x90)
        if (day) {
            celestial(c, Focals.MOUNTAINS)
        } else {
            val f = Focals.MOUNTAINS
            val moon = Path().apply { addCircle(f.x, f.y, f.r, Path.Direction.CW) }
            val cut = Path().apply { addCircle(f.x + 7f, f.y - 3f, f.r - 1f, Path.Direction.CW) }
            moon.op(cut, Path.Op.DIFFERENCE)
            celestialGlow(f)
            celestialPath(moon, 0xFFE6EBFF.toInt())
        }
        val bases = floatArrayOf(0.52f, 0.58f, 0.64f, 0.68f, 0.70f)
        val amps = floatArrayOf(0.26f * H, 0.24f * H, 0.20f * H, 0.15f * H, 0.07f * H)
        for (i in 0..4) {
            c = begin(if (i < 2) Depth.FAR else if (i < 4) Depth.MID else Depth.NEAR, mirror)
            val y = ridge(bases[i] * H, amps[i], 0.006f + i * 0.0015f, 60 + i, sharp = i < 4)
            fillRidge(c, lerpColor(if (day) 0xFF8AA6D8.toInt() else 0xFF6A68B8.toInt(), if (day) 0xFF2A3A6A.toInt() else 0xFF0C0E26.toInt(), i / 4f), y)
            if (i < 4) haze(c, bases[i] * H - amps[i] * 0.3f, bases[i] * H, if (day) 0xC8DCF4 else 0x7B72B8, 0x48)
        }
    }

    override fun overlay(c: Canvas, t: Float) {
        if (!day) stars.draw(c, t)
        water.draw(c, t)
        for (i in 0 until 14) {
            val y = waterline + 5f + i * ((H - waterline - 8f) / 14f)
            val a = (20f + 50f * (0.5f + 0.5f * sin(0.7f * t + sp[i]))).toInt()
            shimmer.color = withAlpha(0x9DB8FF, a)
            val x = sx[i] + 3f * sin(0.3f * t + sp[i])
            c.drawRect(x, y, x + sl[i], y + 1.2f, shimmer)
        }
    }
}

// ---------- 5. Snow Peaks (Nord) ----------

private class SnowPeaksScene(day: Boolean) : LayeredScene(day, 0.6f) {
    private val path = Path()
    private val aurora = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flake = Paint()
    private val n = 40
    private val fx = FloatArray(n)
    private val fy = FloatArray(n)
    private val fph = FloatArray(n)
    private val near = BooleanArray(n)
    private val auroraRgb = intArrayOf(0xA3BE8C, 0x88C0D0, 0xB48EAD)
    private val sparkX = FloatArray(18) { hash(it, 7) * W }
    private val sparkY = FloatArray(18) { H * (0.5f + 0.4f * hash(it, 9)) }
    private val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 0.6f }
    private val auroraY = floatArrayOf(52f, 74f, 96f)
    private val layerScale = floatArrayOf(1f, 0.55f, 0.25f)
    private val layerAlpha = intArrayOf(22, 34, 52)

    init {
        val r = Random(33)
        for (i in 0 until n) { fx[i] = r.nextFloat() * W; fy[i] = r.nextFloat() * H; fph[i] = r.nextFloat() * TAU; near[i] = i % 5 < 2 }
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF0A1226.toInt(), 0xFF1E3A6A.toInt(), 0xFF5A86B8.toInt()),
            intArrayOf(0xFF2B7BD8.toInt(), 0xFF74B8F0.toInt(), 0xFFD2E8FA.toInt()),
            floatArrayOf(0f, 0.55f, 0.85f),
        )
        horizonGlow(c, 0.7f * H, if (day) 0xFFFFFF else 0x88C0D0, 0x80)
        if (day) celestial(c, Focals.SNOW_PEAKS)
        val bases = floatArrayOf(0.60f, 0.68f, 0.77f, 0.86f)
        val amps = floatArrayOf(0.34f * H, 0.30f * H, 0.26f * H, 0.20f * H)
        for (i in 0..3) {
            layer(if (i < 2) Depth.FAR else Depth.MID)
            val y = ridge(bases[i] * H, amps[i], 0.006f + i * 0.002f, 70 + i, sharp = true)
            val rock = lerpColor(if (day) 0xFF6A86B8.toInt() else 0xFF4A62A0.toInt(), if (day) 0xFF2A3A68.toInt() else 0xFF10183A.toInt(), i / 3f)
            val snow = lerpColor(if (day) 0xFFFFFFFF.toInt() else 0xFFC7D8F2.toInt(), if (day) 0xFFB8D2F0.toInt() else 0xFF6F86B8.toInt(), i / 3f)
            fillRidge(c, snow, y)
            // Rock shows below an irregular snow line.
            fillRidge(c, rock, y, { x -> 8f + 24f * fbm(x * 0.03f, 80 + i) }, detail = false)
            if (i < 3) haze(c, bases[i] * H - amps[i] * 0.3f, bases[i] * H + 10f, if (day) 0xD8E8FA else 0x7A9AD0, 0x50)
        }
        layer(Depth.NEAR)
        val fg = ridge(0.95f * H, 22f, 0.01f, 99)
        fillRidge(c, if (day) 0xFF3A5A7A.toInt() else 0xFF222C42.toInt(), fg)
        pines(c, if (day) 0xFF1E3A44.toInt() else 0xFF141C2C.toInt(), fg, 22, 16f, 34f, 8)
    }

    override fun overlay(c: Canvas, t: Float) {
        // Faint aurora at night; by day the snow sparkles instead.
        if (day) {
            for (i in 0 until 18) {
                glintPaint.color = withAlpha(0xFFFFFF, (255f * (0.5f + 0.5f * sin(1.3f * t + i * 2.1f))).toInt())
                sparkle(c, sparkX[i], sparkY[i], 1.6f, glintPaint)
            }
        } else for (k in 0..2) {
            for (layer in 0..2) {
                val th = 16f * layerScale[layer]
                path.reset()
                var x = 0f
                path.moveTo(0f, auroraY[k] - th)
                while (x <= W) { path.lineTo(x, curve(k, x, t) - th); x += 8f }
                x = W
                while (x >= 0f) { path.lineTo(x, curve(k, x, t) + th * 1.8f); x -= 8f }
                path.close()
                aurora.color = withAlpha(auroraRgb[k], layerAlpha[layer])
                c.drawPath(path, aurora)
            }
        }
        for (i in 0 until n) {
            val cls = i % 3 // 3 depth classes: far (small, slow, dim) to near (big, fast, bright)
            val speed = 8f + cls * 5f
            val y = wrap(fy[i] + speed * t, H + 4f) - 2f
            val x = wrap(fx[i] + 6f * sin(0.4f * t + fph[i]) + 2f * t, W)
            val s = 1f + cls * 0.5f
            flake.color = withAlpha(0xECEFF4, 150 + cls * 45)
            c.drawRect(x, y, x + s, y + s, flake)
        }
    }

    private fun curve(k: Int, x: Float, t: Float) =
        auroraY[k] + 18f * sin(0.012f * x + 0.28f * t + k * 2f) + 7f * sin(0.03f * x - 0.2f * t)
}

// ---------- 6. Autumn Valley (Gruvbox) ----------

private class AutumnScene(day: Boolean) : LayeredScene(day) {
    private val n = 12
    private val lx = FloatArray(n)
    private val ly = FloatArray(n)
    private val lph = FloatArray(n)
    private val lspeed = FloatArray(n)
    private val lcolor = IntArray(n)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val oval = RectF()
    private val stars = Stars(50, 0.35f * H, 35)

    init {
        val r = Random(5)
        val palette = intArrayOf(0xFE8019, 0xFABD2F, 0xD65D0E, 0xFB4934)
        for (i in 0 until n) {
            lx[i] = r.nextFloat() * W; ly[i] = r.nextFloat() * H; lph[i] = r.nextFloat() * TAU
            lspeed[i] = 9f + r.nextFloat() * 8f; lcolor[i] = palette[r.nextInt(palette.size)]
        }
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF070A1C.toInt(), 0xFF2A2048.toInt(), 0xFF6A3A5A.toInt()),
            intArrayOf(0xFF4A90D0.toInt(), 0xFFA8D0E8.toInt(), 0xFFFCE6B0.toInt()),
            floatArrayOf(0f, 0.5f, 0.72f),
        )
        horizonGlow(c, 0.52f * H, if (day) 0xFFD27A else 0xB05A7A, 0x90)
        celestial(c, Focals.AUTUMN)
        val bases = floatArrayOf(0.52f, 0.60f, 0.68f, 0.77f, 0.86f, 0.96f)
        val amps = floatArrayOf(30f, 34f, 36f, 40f, 40f, 34f)
        val colors = if (day) intArrayOf(0xFFE8B868.toInt(), 0xFFD08A3A.toInt(), 0xFF9A9A30.toInt(), 0xFF9A6A2C.toInt(), 0xFF6E4A24.toInt(), 0xFF3E2E20.toInt())
            else intArrayOf(0xFF6A4A5A.toInt(), 0xFF5A3A4A.toInt(), 0xFF3A3A30.toInt(), 0xFF3A2A28.toInt(), 0xFF241A1E.toInt(), 0xFF120E14.toInt())
        val rnd = Random(12)
        val treeColors = intArrayOf(0xFFD65D0E.toInt(), 0xFFFE8019.toInt(), 0xFFB8741A.toInt(), 0xFF98971A.toInt())
        for (i in 0..5) {
            layer(if (i < 2) Depth.FAR else if (i < 4) Depth.MID else Depth.NEAR)
            val y = ridge(bases[i] * H, amps[i], 0.007f + i * 0.0015f, 120 + i)
            fillRidge(c, colors[i], y)
            if (i in 2..4) { // a few small, soft patches of autumn colour (no hard polka dots)
                repeat(6) {
                    val x = rnd.nextFloat() * W
                    glow(c, x, y(x) - 2f, 3f + i * 0.6f + rnd.nextFloat() * 2f, treeColors[rnd.nextInt(4)] and 0xFFFFFF, 0x88)
                }
            }
            if (i < 5) haze(c, bases[i] * H - amps[i] * 0.4f, bases[i] * H + 20f, if (day) 0xF2C98A else 0x7A5A7A, 0x50)
        }
    }

    override fun overlay(c: Canvas, t: Float) {
        if (!day) stars.draw(c, t)
        for (i in 0 until n) {
            val y = wrap(ly[i] + lspeed[i] * t, H + 20f) - 10f
            val x = wrap(lx[i] + 26f * sin(0.35f * t + lph[i]) + 5f * t, W)
            paint.color = withAlpha(lcolor[i], 230)
            c.save()
            c.translate(x, y)
            c.rotate(50f * sin(0.5f * t + lph[i]))
            oval.set(-3.5f, -2f, 3.5f, 2f)
            c.drawOval(oval, paint)
            c.restore()
        }
    }
}
