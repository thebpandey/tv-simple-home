package tv.simplehome.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import java.util.Random
import kotlin.math.abs
import kotlin.math.sin

/** Which light overlay goes over a painted still. All are subtle: the painting is the point. */
enum class StillOverlay {
    NONE, DIWALI, DIWALI_SOFT, FIREWORKS, FIREWORKS_RWB, SNOW, SNOW_LIGHT, PUFFS, BATS, LEAVES, TWINKLE,
    MIST, MIST_FIREFLIES, SPORES, GLINTS, CLOUDS,
}

/** Primitive-parameter layer so the frame loop never boxes a Float. */
internal fun interface StillLayer { fun draw(c: Canvas, t: Float) }

/**
 * A painted still with the same Ken Burns drift as a photo, plus a light animated overlay. Overlays are built once; draw
 * allocates nothing. Diya flickers are positioned in IMAGE space (they ride the same pan and zoom as the painting); the rest are screen space.
 */
internal class StillScene(private val bitmap: Bitmap, kind: StillOverlay) : Scene {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val kb = FloatArray(3)
    private var t0 = -1f
    private val imageLayer: StillLayer? = if (kind == StillOverlay.DIWALI) DiyaFlicker(bitmap.width.toFloat(), bitmap.height.toFloat()) else null
    private val screenLayers: Array<StillLayer> = StillOverlays.screenLayers(kind)

    override fun draw(c: Canvas, t: Float) {
        if (t0 < 0f) t0 = t
        KenBurns.transformInto(kb, t - t0, bitmap.width.toFloat(), bitmap.height.toFloat(), W, H)
        c.save()
        c.translate(W / 2f + kb[1], H / 2f + kb[2])
        c.scale(kb[0], kb[0])
        c.translate(-bitmap.width / 2f, -bitmap.height / 2f)
        c.drawBitmap(bitmap, 0f, 0f, paint)
        imageLayer?.draw(c, t)
        c.restore()
        for (l in screenLayers) l.draw(c, t)
    }

    override fun release() { if (!bitmap.isRecycled) bitmap.recycle() }
}

internal object StillOverlays {
    private val NO_LAYERS = emptyArray<StillLayer>()

    fun screenLayers(kind: StillOverlay): Array<StillLayer> = when (kind) {
        StillOverlay.NONE -> NO_LAYERS
        StillOverlay.DIWALI, StillOverlay.DIWALI_SOFT -> arrayOf(layer(Fireworks(
            floatArrayOf(110f, 250f, 390f), floatArrayOf(34f, 22f, 40f), floatArrayOf(13f, 17f, 21f),
            intArrayOf(0xFFC060, 0xFF7AA8, 0xFFE0A0), 26f, alphaScale = 0.55f,
        )))
        StillOverlay.FIREWORKS -> arrayOf(layer(Fireworks(
            floatArrayOf(100f, 240f, 380f, 170f), floatArrayOf(34f, 22f, 40f, 52f), floatArrayOf(9f, 12f, 11f, 15f),
            intArrayOf(0xFFD95A, 0xFF7AB8, 0x8FE0FF, 0xFFFFFF), 30f, alphaScale = 0.6f,
        )))
        StillOverlay.FIREWORKS_RWB -> arrayOf(layer(Fireworks(
            floatArrayOf(100f, 240f, 380f, 170f), floatArrayOf(34f, 22f, 40f, 52f), floatArrayOf(9f, 12f, 11f, 15f),
            intArrayOf(0xFF4B45, 0xFFFFFF, 0x4A8CFF, 0xFFFFFF), 30f, alphaScale = 0.6f,
        )))
        StillOverlay.SNOW -> { val s = Snowfall(46, 7); arrayOf(StillLayer { c, t -> s.draw(c, t) }) }
        StillOverlay.SNOW_LIGHT -> { val s = Snowfall(28, 9); arrayOf(StillLayer { c, t -> s.draw(c, t) }) }
        StillOverlay.PUFFS -> arrayOf(ColorPuffs(intArrayOf(0xFF3E8E, 0xFFB020, 0x40C8F0, 0x9B5CFF), 6, 0x30, 34f, 3f, -2.5f))
        StillOverlay.MIST -> arrayOf(ColorPuffs(intArrayOf(0xE8F0F0), 5, 0x1C, 60f, 4f, -0.4f))
        StillOverlay.MIST_FIREFLIES -> {
            val m = Motes(12, 0xD8F070, true, 52, 0.45f * H, 0.95f * H)
            arrayOf(ColorPuffs(intArrayOf(0xB8C8D8), 4, 0x16, 60f, 3f, -0.3f), StillLayer { c, t -> m.draw(c, t) })
        }
        StillOverlay.SPORES -> { val m = Motes(14, 0xE8F0A0, false, 61, 0.3f * H, 0.9f * H); arrayOf(StillLayer { c, t -> m.draw(c, t) }) }
        StillOverlay.GLINTS -> arrayOf(Glints())
        StillOverlay.CLOUDS -> arrayOf(ColorPuffs(intArrayOf(0xFFFFFF), 4, 0x1A, 70f, 5f, 0f))
        StillOverlay.BATS -> arrayOf(Bats())
        StillOverlay.LEAVES -> {
            val f = Falling(9, intArrayOf(0xFFD9822B.toInt(), 0xFFB5532A.toInt(), 0xFFE0A030.toInt()), 11, 2.4f, 1.3f, 5f, 10f)
            arrayOf(StillLayer { c, t -> f.draw(c, t) })
        }
        StillOverlay.TWINKLE -> { val s = Stars(16, 0.24f * H, 5, alphaScale = 0.4f); arrayOf(StillLayer { c, t -> s.draw(c, t) }) }
    }

    private fun layer(f: Fireworks) = StillLayer { c, t -> f.draw(c, t) }
}

/** Faint warm flickers over the diya flames along the parapet (positions are fractions of the painting, matched to diwali-night). */
private class DiyaFlicker(private val imgW: Float, private val imgH: Float) : StillLayer {
    private val xs = floatArrayOf(189f, 232f, 332f, 421f, 575f, 700f, 878f, 1068f, 1272f, 1415f, 1600f, 1744f, 1791f, 1832f, 148f, 78f, 1665f, 1733f, 1876f)
    private val ys = floatArrayOf(645f, 648f, 647f, 646f, 646f, 646f, 646f, 646f, 646f, 646f, 646f, 646f, 646f, 646f, 735f, 680f, 750f, 715f, 733f)
    private val radius = 30f * imgW / 1920f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, radius, intArrayOf(0xB0FFB347.toInt(), 0x00FFB347), null, Shader.TileMode.CLAMP)
    }

    override fun draw(c: Canvas, t: Float) {
        for (i in xs.indices) {
            val flicker = 0.7f + 0.2f * sin(t * (4.1f + (i % 4)) + i * 1.7f) + 0.1f * sin(t * 11f + i * 0.9f)
            paint.alpha = (255f * flicker * 0.6f).toInt().coerceIn(0, 255)
            c.save()
            c.translate(xs[i] * imgW / 1920f, ys[i] * imgH / 1080f)
            c.drawCircle(0f, 0f, radius, paint)
            c.restore()
        }
    }
}

/** Soft radial puffs ([n] of them, [alpha] at the core, [radius] logical units) drifting by ([vx], [vy]) per second: gulal, mist, clouds. Very faint. */
private class ColorPuffs(colours: IntArray, private val n: Int, alpha: Int, private val radius: Float, private val vx: Float, private val vy: Float) : StillLayer {
    private val x0 = FloatArray(n)
    private val y0 = FloatArray(n)
    private val ph = FloatArray(n)
    private val paints: Array<Paint>

    init {
        val r = Random(21)
        for (i in 0 until n) { x0[i] = r.nextFloat() * W; y0[i] = r.nextFloat() * H; ph[i] = r.nextFloat() * TAU }
        paints = Array(n) {
            val rgb = colours[it % colours.size]
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
                shader = RadialGradient(0f, 0f, radius, intArrayOf(withAlpha(rgb, alpha), withAlpha(rgb, 0)), null, Shader.TileMode.CLAMP)
            }
        }
    }

    override fun draw(c: Canvas, t: Float) {
        for (i in 0 until n) {
            val x = wrap(x0[i] + vx * t + 14f * sin(0.11f * t + ph[i]), W + 2f * radius) - radius
            val y = wrap(y0[i] + vy * t + 4f * sin(0.07f * t + ph[i]), H + 2f * radius) - radius
            c.save()
            c.translate(x, y)
            c.drawCircle(0f, 0f, radius, paints[i])
            c.restore()
        }
    }
}

/** One or two bats now and then: each crosses the upper sky in about 6 seconds, then rests for a while. */
private class Bats : StillLayer {
    private val body = Path().apply {
        moveTo(0f, 0f); quadTo(-6f, -5f, -11f, -1f); quadTo(-8f, -1f, -6f, 2f); quadTo(-3f, 0f, 0f, 3f)
        quadTo(3f, 0f, 6f, 2f); quadTo(8f, -1f, 11f, -1f); quadTo(6f, -5f, 0f, 0f); close()
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC0A0812.toInt() }
    private val period = floatArrayOf(23f, 31f)
    private val y = floatArrayOf(38f, 62f)

    override fun draw(c: Canvas, t: Float) {
        for (b in period.indices) {
            val local = (t + b * 9f) % period[b]
            if (local > FLIGHT) continue
            val p = local / FLIGHT
            val flap = 0.55f + 0.45f * abs(sin(local * 9f))
            c.save()
            c.translate(-20f + p * (W + 40f), y[b] + 9f * sin(p * 9f))
            c.scale(1.3f, 1.3f * flap)
            c.drawPath(body, paint)
            c.restore()
        }
    }

    private companion object { const val FLIGHT = 6f }
}

/** Tiny sun glints that wink on the water in the lower half of a beach painting. */
private class Glints : StillLayer {
    private val n = 22
    private val x = FloatArray(n)
    private val y = FloatArray(n)
    private val ph = FloatArray(n)
    private val paint = Paint().apply { color = 0xFFFFFFFF.toInt() }

    init {
        val r = Random(77)
        for (i in 0 until n) { x[i] = r.nextFloat() * W; y[i] = (0.56f + 0.3f * r.nextFloat()) * H; ph[i] = r.nextFloat() * TAU }
    }

    override fun draw(c: Canvas, t: Float) {
        for (i in 0 until n) {
            val a = sin(1.3f * t + ph[i] * 3f)
            if (a < 0.55f) continue
            paint.alpha = ((a - 0.55f) / 0.45f * 150f).toInt()
            c.drawRect(x[i], y[i], x[i] + 3f, y[i] + 0.7f, paint)
        }
    }
}
