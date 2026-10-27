package tv.simplehome.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/*
 * Festival scenes: same recipe as Scenes.kt (static layers painted once into `base`, a few tiny animated
 * overlays, no per-frame allocation). Several festivals share a scene through `variant`.
 */

fun createFestivalScene(id: String, variant: Int, day: Boolean): Scene = when (id) {
    "diwali" -> DiwaliScene(day)
    "holi" -> HoliScene(day)
    "navratri_dussehra" -> NavratriScene(day, effigy = variant == 1)
    "janmashtami" -> JanmashtamiScene(day)
    "ganesh" -> GaneshScene(day)
    "raksha_bandhan" -> RakshaScene(day)
    "shivaratri" -> ShivaratriScene(day)
    "sankranti_lohri" -> if (variant == 1) LohriScene(day) else KitesScene(day)
    "vasant" -> VasantScene(day)
    "karwa_chauth" -> KarwaScene(day)
    "chhath" -> ChhathScene(day)
    "halloween" -> HalloweenScene(day)
    "thanksgiving" -> ThanksgivingScene(day)
    "christmas" -> ChristmasScene(day)
    else -> NewYearScene(day, patriotic = variant == 1)
}

// ---------- shared overlay pieces ----------

internal class Falling(
    private val n: Int, private val colors: IntArray, seed: Int,
    private val halfW: Float, private val halfH: Float, vMin: Float, vMax: Float,
) {
    private val x0 = FloatArray(n)
    private val y0 = FloatArray(n)
    private val ph = FloatArray(n)
    private val v = FloatArray(n)
    private val col = IntArray(n)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val oval = RectF()

    init {
        val r = Random(seed.toLong())
        for (i in 0 until n) {
            x0[i] = r.nextFloat() * W; y0[i] = r.nextFloat() * H; ph[i] = r.nextFloat() * TAU
            v[i] = vMin + r.nextFloat() * (vMax - vMin); col[i] = colors[r.nextInt(colors.size)]
        }
    }

    fun draw(c: Canvas, t: Float) {
        for (i in 0 until n) {
            val cls = i % 3 // 3 depth classes: far (small, slow, dim) to near (big, fast, bright)
            val k = 0.7f + 0.35f * cls
            val y = wrap(y0[i] + v[i] * k * t, H + 20f) - 10f
            val x = wrap(x0[i] + 26f * sin(0.35f * t + ph[i]) + 5f * t, W)
            paint.color = withAlpha(col[i], 150 + cls * 45)
            c.save()
            c.translate(x, y)
            c.rotate(50f * sin(0.5f * t + ph[i]))
            oval.set(-halfW * k, -halfH * k, halfW * k, halfH * k)
            c.drawOval(oval, paint)
            c.restore()
        }
    }
}

internal class Snowfall(private val n: Int, seed: Int) {
    private val fx = FloatArray(n)
    private val fy = FloatArray(n)
    private val fph = FloatArray(n)
    private val near = BooleanArray(n)
    private val paint = Paint()

    init {
        val r = Random(seed.toLong())
        for (i in 0 until n) { fx[i] = r.nextFloat() * W; fy[i] = r.nextFloat() * H; fph[i] = r.nextFloat() * TAU; near[i] = i % 5 < 2 }
    }

    fun draw(c: Canvas, t: Float) {
        for (i in 0 until n) {
            val cls = i % 3
            val y = wrap(fy[i] + (8f + cls * 5f) * t, H + 4f) - 2f
            val x = wrap(fx[i] + 6f * sin(0.4f * t + fph[i]) + 2f * t, W)
            val s = 1f + cls * 0.5f
            paint.color = withAlpha(0xECEFF4, 150 + cls * 45)
            c.drawRect(x, y, x + s, y + s, paint)
        }
    }
}

private class Bulbs(private val xs: FloatArray, private val ys: FloatArray, private val rgb: IntArray, private val r: Float) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(c: Canvas, t: Float) {
        for (i in xs.indices) {
            val a = (90f + 165f * (0.5f + 0.5f * sin(1.7f * t + i * 1.9f))).toInt()
            paint.color = withAlpha(rgb[i % rgb.size], a / 4)
            c.drawCircle(xs[i], ys[i], r * 2.4f, paint)
            paint.color = withAlpha(rgb[i % rgb.size], a)
            c.drawCircle(xs[i], ys[i], r, paint)
        }
    }
}

/** Distant firework bursts, each repeating on its own period; positions vary slightly per cycle. */
internal class Fireworks(
    private val bx: FloatArray, private val by: FloatArray, private val period: FloatArray,
    private val rgb: IntArray, private val maxR: Float, private val alphaScale: Float = 1f,
) {
    private val sparks = 16
    private val ca = FloatArray(sparks) { cos(it * TAU / sparks) }
    private val sa = FloatArray(sparks) { sin(it * TAU / sparks) }
    private val paint = Paint()

    fun draw(c: Canvas, t: Float) {
        for (b in bx.indices) {
            val tt = t + b * 3.7f
            val cycle = floor(tt / period[b]).toInt()
            val local = tt - cycle * period[b]
            if (local >= DURATION) continue
            val p = local / DURATION
            val r = maxR * (1f - (1f - p) * (1f - p))
            paint.color = withAlpha(rgb[b], (230f * alphaScale * (1f - p) * (1f - p)).toInt())
            val ox = bx[b] + (hash(cycle, b) - 0.5f) * 90f
            val oy = by[b] + (hash(cycle, b + 9) - 0.5f) * 30f
            for (s in 0 until sparks) {
                val x = ox + ca[s] * r
                val y = oy + sa[s] * r + 18f * p * p // slight gravity
                c.drawRect(x, y, x + 2f, y + 2f, paint)
            }
        }
    }

    private companion object { const val DURATION = 2.4f }
}

/** Catenary-style swag of round beads between two points. */
private fun swag(c: Canvas, x0: Float, x1: Float, y: Float, sag: Float, colors: IntArray, r: Float, step: Float) {
    var x = x0
    var i = 0
    while (x <= x1) {
        val u = (x - x0) / (x1 - x0)
        c.drawCircle(x, y + sag * 4f * u * (1f - u), r, fill(colors[i % colors.size]))
        x += step; i++
    }
}

/** A soft round point of light (core plus a faint halo) instead of a "+" cross. [p] supplies the colour and alpha. */
internal fun sparkle(c: Canvas, x: Float, y: Float, s: Float, p: Paint) {
    val a = p.alpha
    c.drawCircle(x, y, s * 0.28f, p)
    p.alpha = a / 4
    c.drawCircle(x, y, s * 0.8f, p)
    p.alpha = a
}

private fun crescentPath(cx: Float, cy: Float, r: Float): Path {
    val moon = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
    moon.op(Path().apply { addCircle(cx + r * 0.5f, cy - r * 0.25f, r * 0.9f, Path.Direction.CW) }, Path.Op.DIFFERENCE)
    return moon
}

private fun glowPaint(r: Float, rgb: Int, alpha: Int) = Paint(Paint.DITHER_FLAG).apply {
    shader = RadialGradient(0f, 0f, r, intArrayOf(withAlpha(rgb, alpha), withAlpha(rgb, 0)), null, Shader.TileMode.CLAMP)
}

private fun Canvas.drawGlowAt(x: Float, y: Float, scale: Float, r: Float, p: Paint) {
    save(); translate(x, y); scale(scale, scale); drawCircle(0f, 0f, r, p); restore()
}

private fun blocks(c: Canvas, base: Float, hMin: Float, hMax: Float, color: Int, seed: Int, windowRgb: Int, lit: Boolean = true) {
    val r = Random(seed.toLong())
    val p = fill(color)
    val win = fill(withAlpha(windowRgb, if (lit) 200 else 0))
    var x = -4f
    while (x < W) {
        val w = 18f + r.nextFloat() * 30f
        val h = hMin + r.nextFloat() * (hMax - hMin)
        c.drawRect(x, base - h, x + w, H, p)
        var wy = base - h + 6f
        while (wy < base - 4f) {
            var wx = x + 4f
            while (wx < x + w - 5f) { if (r.nextInt(3) == 0) c.drawRect(wx, wy, wx + 2f, wy + 3f, win); wx += 7f }
            wy += 9f
        }
        x += w - 0.4f // buildings overlap slightly: a gap would show the horizon glow as thin vertical lines
    }
}

// ---------- Diwali ----------

private class DiwaliScene(day: Boolean) : LayeredScene(day) {
    private val dx = FloatArray(13) { 24f + it * 36f }
    private val dy = 0.745f * H
    private val glow = glowPaint(18f, 0xFFB43C, 0x90)
    private val flame = fill(0xFFFFD27A.toInt())
    private val flameCore = fill(0xFFFFFFFF.toInt())
    private val fireworks = Fireworks(floatArrayOf(90f, 250f, 400f), floatArrayOf(50f, 35f, 62f), floatArrayOf(9f, 11f, 13f), intArrayOf(0xFFD27A, 0xFF8FB8, 0xFFE9A0), 34f)
    private val oval = RectF()

    init {
        val c = staticCanvas()
        sky(
            c, 0.8f * H,
            intArrayOf(0xFF0F0A2B.toInt(), 0xFF3B0F3A.toInt(), 0xFF7A2A22.toInt()),
            intArrayOf(0xFF3A8AD8.toInt(), 0xFF8AC8F0.toInt(), 0xFFF6E0B0.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
        )
        layer(Depth.FAR)
        blocks(c, 0.7f * H, 18f, 70f, if (day) 0xFF6A4A5A.toInt() else 0xFF150A1E.toInt(), 3, 0xFFC25A, lit = !day)
        layer(Depth.NEAR)
        gradientRect(c, 0.78f * H, H, intArrayOf(0xFF2A0E1C.toInt(), 0xFF120610.toInt()))
        c.drawRect(0f, 0.75f * H, W, 0.8f * H, fill(0xFF3A1626.toInt())) // parapet
        c.drawRect(0f, 0.75f * H, W, 0.755f * H, fill(0xFF5A2638.toInt()))
        if (day) { // marigold garlands draped along the parapet
            val mari = intArrayOf(0xFFF2A11A.toInt(), 0xFFE8761C.toInt(), 0xFFFFC83D.toInt())
            for (k in 0..2) swag(c, k * 160f, k * 160f + 160f, 0.70f * H, 16f, mari, 3.2f, 5.5f)
        }
        // Rangoli: concentric rings and petals, squashed to read as lying on the floor.
        c.save()
        c.translate(240f, 0.915f * H)
        c.scale(1f, 0.24f)
        val ringColors = intArrayOf(0xFFE0A030.toInt(), 0xFFC02A6A.toInt(), 0xFF1FA39A.toInt(), 0xFFE8742A.toInt(), 0xFFF3E2B8.toInt())
        for (i in 0..4) c.drawCircle(0f, 0f, 96f - i * 17f, fill(ringColors[i]))
        for (k in 0 until 12) {
            c.save(); c.rotate(k * 30f)
            oval.set(-6f, -96f, 6f, -66f)
            c.drawOval(oval, fill(ringColors[(k + 1) % 5])); c.restore()
        }
        c.restore()
        for (x in dx) { // diya bowls
            oval.set(x - 8f, dy - 5f, x + 8f, dy + 3f)
            c.drawOval(oval, fill(0xFFB5521B.toInt()))
            oval.set(x - 6f, dy - 5f, x + 6f, dy - 2f)
            c.drawOval(oval, fill(0xFF3A1208.toInt()))
        }
    }

    override fun overlay(c: Canvas, t: Float) {
        if (day) return // diyas stay unlit by day
        fireworks.draw(c, t)
        for (i in dx.indices) {
            val f = 0.85f + 0.15f * sin(7f * t + i * 2.3f) + 0.08f * sin(13f * t + i * 1.1f)
            c.drawGlowAt(dx[i], dy - 7f, f, 18f, glow)
            oval.set(dx[i] - 2.4f * f, dy - 13f * f, dx[i] + 2.4f * f, dy - 4f)
            c.drawOval(oval, flame)
            oval.set(dx[i] - 1f, dy - 8f * f, dx[i] + 1f, dy - 4f)
            c.drawOval(oval, flameCore)
        }
    }
}

// ---------- Holi ----------

private class HoliScene(day: Boolean) : LayeredScene(day) {
    private val rgb = intArrayOf(0xFF4FA3, 0xFFD23C, 0x4CD08A, 0x4FA8FF)
    private val puff = Array(4) { glowPaint(80f, rgb[it], 0x88) }
    private val px = floatArrayOf(60f, 150f, 250f, 340f, 430f, 200f, 380f)
    private val py = floatArrayOf(90f, 150f, 80f, 140f, 100f, 200f, 210f)
    private val pw = floatArrayOf(0.1f, 0.13f, 0.08f, 0.12f, 0.09f, 0.11f, 0.07f)
    private val stars = Stars(40, 0.4f * H, 41)

    init {
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF140E40.toInt(), 0xFF4A2A78.toInt(), 0xFF9A4A8A.toInt()),
            intArrayOf(0xFF5AAEF0.toInt(), 0xFFFFD0E8.toInt(), 0xFFFFF0C8.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
        )
        celestial(c, Focals.HOLI)
        val tints = if (day) intArrayOf(0xFFD9C8F2.toInt(), 0xFFBFE8D2.toInt(), 0xFFF8CDB8.toInt()) else intArrayOf(0xFF4A3A8A.toInt(), 0xFF2A4A6A.toInt(), 0xFF5A3A6A.toInt())
        for (i in 0..2) {
            layer(if (i == 0) Depth.FAR else if (i == 1) Depth.MID else Depth.NEAR)
            fillRidge(c, tints[i], ridge((0.55f + 0.14f * i) * H, 40f, 0.008f + 0.002f * i, 200 + i))
            haze(c, (0.5f + 0.14f * i) * H, (0.62f + 0.14f * i) * H, 0xFFFFFF, 0x60)
        }
layer(Depth.NEAR)
                val r = Random(8)
        repeat(14) { // powder splashes on the ground
            val x = r.nextFloat() * W
            glow(c, x, 0.9f * H + r.nextFloat() * 20f, 40f + r.nextFloat() * 30f, rgb[r.nextInt(4)], 0x70)
        }
    }

    override fun overlay(c: Canvas, t: Float) {
        if (!day) stars.draw(c, t)
        for (i in px.indices) {
            val x = px[i] + 30f * sin(pw[i] * t + i)
            val y = py[i] + 16f * sin(pw[i] * 1.3f * t + i * 2f) - 6f * sin(0.05f * t)
            c.drawGlowAt(x, y, 1f + 0.12f * sin(0.2f * t + i), 80f, puff[i % 4])
        }
    }
}

// ---------- Navratri / Dussehra ----------

private class NavratriScene(day: Boolean, effigy: Boolean) : LayeredScene(day) {
    private val bulbs: Bulbs

    init {
        val c = staticCanvas()
        if (effigy) {
            sky(
                c, H,
                intArrayOf(0xFF2A1048.toInt(), 0xFF8C2F4A.toInt(), 0xFFF08A3A.toInt()),
                intArrayOf(0xFF3A7ED0.toInt(), 0xFFF0B070.toInt(), 0xFFFFE0A0.toInt()),
                floatArrayOf(0f, 0.55f, 0.85f),
            )
            glow(c, 330f, 0.7f * H, 120f, 0xFFC060, 0xA0)
            layer(Depth.FAR)
            fillRidge(c, 0xFF4A2040.toInt(), ridge(0.78f * H, 30f, 0.008f, 31))
            layer(Depth.MID)
            drawEffigy(c, 340f, 0.8f * H)
            layer(Depth.NEAR)
            fillRidge(c, 0xFF1C0C1E.toInt(), ridge(0.93f * H, 22f, 0.01f, 32))
        } else {
            sky(
                c, H,
                intArrayOf(0xFF1B0B3A.toInt(), 0xFF5A1750.toInt(), 0xFFB5402F.toInt()),
                intArrayOf(0xFF2E78D0.toInt(), 0xFF86C0EC.toInt(), 0xFFF4D890.toInt()),
                floatArrayOf(0f, 0.6f, 1f),
            )
            layer(Depth.MID)
            fillRidge(c, 0xFF2A0F2E.toInt(), ridge(0.88f * H, 24f, 0.009f, 33))
            layer(Depth.NEAR)
            fillRidge(c, 0xFF160818.toInt(), ridge(0.97f * H, 14f, 0.012f, 34))
        }
layer(Depth.NEAR)
                // Marigold toran swags across the top.
        val mari = intArrayOf(0xFFF2A11A.toInt(), 0xFFE8761C.toInt(), 0xFFFFC83D.toInt())
        for (k in 0..3) swag(c, k * 120f, k * 120f + 120f, 14f, 26f, mari, 4.2f, 7f)
        for (k in 0..3) swag(c, k * 120f + 8f, k * 120f + 112f, 10f, 14f, intArrayOf(0xFF3D8F3A.toInt()), 2.2f, 9f)
        // Dandiya-night string lights on two lower swags.
        val xs = ArrayList<Float>(); val ys = ArrayList<Float>()
        for (k in 0..3) {
            var x = k * 120f + 10f
            while (x < k * 120f + 115f) {
                val u = (x - k * 120f) / 120f
                xs.add(x); ys.add(52f + 30f * 4f * u * (1f - u)); x += 15f
            }
        }
        bulbs = Bulbs(xs.toFloatArray(), ys.toFloatArray(), intArrayOf(0xFFD27A, 0xFF7AB8, 0x8FE0FF, 0xFFF08A), 2.2f)
    }

    // Deliberately simplified: a stylised looming figure with a fan of crowned heads, drawn as one silhouette.
    private fun drawEffigy(c: Canvas, cx: Float, base: Float) {
        val s = fill(0xFF1A0A1C.toInt())
        c.drawRect(cx - 14f, base - 70f, cx + 14f, base, s)                      // legs
        c.drawRect(cx - 26f, base - 120f, cx + 26f, base - 66f, s)               // torso
        c.drawRect(cx - 52f, base - 118f, cx - 26f, base - 104f, s)              // arms
        c.drawRect(cx + 26f, base - 118f, cx + 52f, base - 104f, s)
        for (i in -3..3) {                                                       // heads fan
            val hx = cx + i * 11f
            val hy = base - 132f - (3 - kotlin.math.abs(i)) * 5f
            c.drawCircle(hx, hy, 6f, s)
            val crown = Path().apply { moveTo(hx - 5f, hy - 4f); lineTo(hx, hy - 14f); lineTo(hx + 5f, hy - 4f); close() }
            c.drawPath(crown, s)
        }
    }

    override fun overlay(c: Canvas, t: Float) { if (!day) bulbs.draw(c, t) }
}

// ---------- Janmashtami ----------

private class JanmashtamiScene(day: Boolean) : LayeredScene(day) {
    private val stars = Stars(50, 0.5f * H, 12)
    private val feather: Bitmap
    private val fAnchorX = 70f
    private val featherPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val fAnchorY = 0.97f * H

    init {
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF081A3A.toInt(), 0xFF0F3A56.toInt(), 0xFF1B6B73.toInt()),
            intArrayOf(0xFF2A78C8.toInt(), 0xFF6EC0D8.toInt(), 0xFFC8E8E0.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
        )
        celestial(c, Focals.JANMASHTAMI)
        layer(Depth.FAR)
        fillRidge(c, 0xFF0E3447.toInt(), ridge(0.72f * H, 26f, 0.007f, 41))
        layer(Depth.MID)
        fillRidge(c, 0xFF0A2636.toInt(), ridge(0.84f * H, 22f, 0.009f, 42))
        layer(Depth.NEAR)
        fillRidge(c, 0xFF07161F.toInt(), ridge(0.95f * H, 16f, 0.011f, 43))
        // Flute silhouette across the lower right.
        c.save(); c.rotate(-18f, 380f, 0.88f * H)
        val flute = fill(0xFF04101A.toInt())
        c.drawRoundRect(RectF(300f, 0.865f * H, 470f, 0.865f * H + 7f), 3.5f, 3.5f, flute)
        for (i in 0..5) c.drawCircle(350f + i * 17f, 0.865f * H + 3.5f, 1.4f, fill(0xFF2C7F86.toInt()))
        c.restore()
        feather = Bitmap.createBitmap(210, 450, Bitmap.Config.ARGB_8888) // 3x so it stays crisp at full screen
        paintFeather(Canvas(feather).apply { scale(3f, 3f) })
    }

    private fun paintFeather(c: Canvas) {
        val stem = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9FD8C8.toInt(); strokeWidth = 1.6f }
        val barb = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x8856C0A8.toInt(); strokeWidth = 1f }
        c.drawLine(35f, 148f, 35f, 52f, stem)
        for (y in 56..130 step 3) { // barbs fan out and upward
            c.drawLine(35f, y.toFloat(), 35f - 24f, y - 14f, barb)
            c.drawLine(35f, y.toFloat(), 35f + 24f, y - 14f, barb)
        }
        val eye = RectF(17f, 8f, 53f, 62f)
        c.drawOval(eye, fill(0xFF1E8F7A.toInt()))
        eye.inset(5f, 6f); c.drawOval(eye, fill(0xFF2A55B8.toInt()))
        eye.inset(5f, 7f); c.drawOval(eye, fill(0xFFD9B04A.toInt()))
        eye.inset(4f, 6f); c.drawOval(eye, fill(0xFF0C3A5A.toInt()))
    }

    

    override fun overlay(c: Canvas, t: Float) {
        if (!day) stars.draw(c, t)
        c.save()
        c.translate(fAnchorX, fAnchorY)
        c.rotate(4f * sin(0.35f * t), 0f, 0f)
        c.translate(-35f, -148f)
        c.scale(1f / 3f, 1f / 3f)
        c.drawBitmap(feather, 0f, 0f, featherPaint)
        c.restore()
    }
}

// ---------- Ganesh ----------

private class GaneshScene(day: Boolean) : LayeredScene(day) {
    private val petals = Falling(14, intArrayOf(0xFFF2A11A.toInt(), 0xFFFFC83D.toInt(), 0xFFE8761C.toInt()), 5, 3.6f, 2.2f, 8f, 16f)

    init {
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF0C0A28.toInt(), 0xFF2A1A58.toInt(), 0xFF7A2A58.toInt(), 0xFFC05A48.toInt()),
            intArrayOf(0xFF4A8CD8.toInt(), 0xFFFFC890.toInt(), 0xFFFFE0A0.toInt(), 0xFFFFF4D0.toInt()),
            floatArrayOf(0f, 0.4f, 0.75f, 1f),
        )
        celestial(c, Focals.GANESH)
        layer(Depth.FAR)
        fillRidge(c, 0xFFB5402F.toInt(), ridge(0.78f * H, 28f, 0.007f, 51))
        layer(Depth.MID)
        fillRidge(c, 0xFF7A2230.toInt(), ridge(0.88f * H, 22f, 0.009f, 52))
layer(Depth.NEAR)
                // Lotus: fanned petals around the bottom centre, over a dark pond edge.
        fillRidge(c, 0xFF3A1226.toInt(), ridge(0.97f * H, 10f, 0.01f, 53))
        val petalColors = intArrayOf(0xFFF7B6C8.toInt(), 0xFFEE7FA5.toInt(), 0xFFF9D3E0.toInt())
        for (ring in 0..2) for (k in -3..3) {
            c.save(); c.translate(240f, 0.95f * H); c.rotate(k * (16f - ring * 3f))
            val p = Path().apply { moveTo(0f, 0f); quadTo(-9f + ring * 2f, -28f - ring * -4f, 0f, -46f + ring * 9f); quadTo(9f - ring * 2f, -28f + ring * 4f, 0f, 0f) }
            c.drawPath(p, fill(petalColors[ring])); c.restore()
        }
        // Modak: small dumplings on either side of the lotus.
        for (mx in floatArrayOf(150f, 175f, 305f, 330f)) {
            val m = Path().apply { moveTo(mx, 0.93f * H - 14f); quadTo(mx - 9f, 0.93f * H, mx - 7f, 0.93f * H + 6f); lineTo(mx + 7f, 0.93f * H + 6f); quadTo(mx + 9f, 0.93f * H, mx, 0.93f * H - 14f) }
            c.drawPath(m, fill(0xFFF6D38A.toInt()))
        }
        swag(c, 0f, 200f, 8f, 22f, intArrayOf(0xFFF2A11A.toInt(), 0xFFE8761C.toInt()), 4f, 7f)
        swag(c, 280f, 480f, 8f, 22f, intArrayOf(0xFFF2A11A.toInt(), 0xFFFFC83D.toInt()), 4f, 7f)
    }

    override fun overlay(c: Canvas, t: Float) = petals.draw(c, t)
}

// ---------- Raksha Bandhan ----------

private class RakshaScene(day: Boolean) : LayeredScene(day) {
    private val sx = FloatArray(18)
    private val sy = FloatArray(18)
    private val sp = FloatArray(18)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.2f }

    init {
        val r = Random(6)
        for (i in 0 until 18) { sx[i] = r.nextFloat() * W; sy[i] = r.nextFloat() * H * 0.8f; sp[i] = r.nextFloat() * TAU }
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF1E1650.toInt(), 0xFF4A2A78.toInt(), 0xFF9A5A8A.toInt()),
            intArrayOf(0xFF7EB8F0.toInt(), 0xFFFFD0D8.toInt(), 0xFFFFE8D0.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
        )
        layer(Depth.FAR)
        fillRidge(c, 0xFFE9B7D0.toInt(), ridge(0.82f * H, 24f, 0.008f, 61))
        layer(Depth.MID)
        fillRidge(c, 0xFFCB94BE.toInt(), ridge(0.92f * H, 18f, 0.01f, 62))
layer(Depth.NEAR)
                // Rakhi thread: two looping strands crossing the scene, red and gold.
        val thread = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.4f; strokeCap = Paint.Cap.ROUND }
        val a = Path().apply { moveTo(0f, 70f); cubicTo(120f, 10f, 180f, 150f, 240f, 110f); cubicTo(300f, 70f, 360f, 190f, 480f, 90f) }
        val b = Path().apply { moveTo(0f, 100f); cubicTo(110f, 160f, 190f, 40f, 240f, 110f); cubicTo(290f, 170f, 370f, 40f, 480f, 130f) }
        thread.color = 0xFFC8283C.toInt(); c.drawPath(a, thread)
        thread.color = 0xFFE5B64A.toInt(); c.drawPath(b, thread)
        // Rakhi medallion at the crossing.
        glow(c, 240f, 110f, 46f, 0xFFE6A0, 0x90)
        for (k in 0 until 10) {
            c.save(); c.translate(240f, 110f); c.rotate(k * 36f)
            c.drawOval(RectF(-5f, -26f, 5f, -8f), fill(if (k % 2 == 0) 0xFFD84A5A.toInt() else 0xFFE5B64A.toInt())); c.restore()
        }
        c.drawCircle(240f, 110f, 9f, fill(0xFFF6E3A8.toInt()))
        c.drawCircle(240f, 110f, 4f, fill(0xFFC8283C.toInt()))
    }

    override fun overlay(c: Canvas, t: Float) {
        for (i in 0 until 18) {
            val a = (255f * (0.5f + 0.5f * sin(0.9f * t + sp[i]))).toInt()
            paint.color = withAlpha(0xFFFFFF, a)
            sparkle(c, sx[i], sy[i], 2f + 2f * (a / 255f), paint)
        }
    }
}

// ---------- Shivaratri ----------

private class ShivaratriScene(day: Boolean) : LayeredScene(day) {
    private val stars = Stars(60, 0.45f * H, 14)

    init {
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF05081A.toInt(), 0xFF141B45.toInt(), 0xFF2B3A78.toInt()),
            intArrayOf(0xFF2670C8.toInt(), 0xFF6EB4EC.toInt(), 0xFFD8EAF8.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
        )
        if (day) {
            celestial(c, Focals.SHIVARATRI)
        } else {
            celestialGlow(Focals.SHIVARATRI)
            celestialPath(crescentPath(Focals.SHIVARATRI.x, Focals.SHIVARATRI.y, Focals.SHIVARATRI.r + 2f), 0xFFEAF0FF.toInt())
        }
        var trishulY = 0f
        for (i in 0..3) {
            layer(if (i < 2) Depth.FAR else Depth.MID)
            val y = ridge((0.62f + 0.1f * i) * H, (0.34f - 0.05f * i) * H, 0.006f + 0.002f * i, 70 + i, sharp = true)
            val snow = lerpColor(if (day) 0xFFFFFFFF.toInt() else 0xFFC7D3F2.toInt(), if (day) 0xFFB8D0F0.toInt() else 0xFF6F7FB8.toInt(), i / 3f)
            val rock = lerpColor(if (day) 0xFF6A86C0.toInt() else 0xFF5A6BA8.toInt(), if (day) 0xFF2A3A70.toInt() else 0xFF14183A.toInt(), i / 3f)
            fillRidge(c, snow, y)
            fillRidge(c, rock, y, { x -> 8f + 22f * fbm(x * 0.03f, 80 + i) }, detail = false)
            if (i == 1) { trishulY = y(150f) }
            if (i < 3) haze(c, (0.6f + 0.1f * i) * H, (0.74f + 0.1f * i) * H, 0x9AA8E0, 0x40)
        }
        // Trishul on a peak: shaft, centre blade, two curved prongs.
        val ts = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF080B1E.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.8f; strokeCap = Paint.Cap.ROUND }
        val bx = 150f; val by = trishulY + 2f
        c.drawLine(bx, by, bx, by - 46f, ts)
        c.drawLine(bx, by - 46f, bx, by - 58f, ts)
        val prongs = Path().apply {
            moveTo(bx - 9f, by - 56f); quadTo(bx - 9f, by - 42f, bx, by - 42f); quadTo(bx + 9f, by - 42f, bx + 9f, by - 56f)
        }
        c.drawPath(prongs, ts)
        c.drawLine(bx, by - 58f, bx - 2.5f, by - 52f, ts); c.drawLine(bx, by - 58f, bx + 2.5f, by - 52f, ts)
        layer(Depth.NEAR)
        fillRidge(c, 0xFF070A1C.toInt(), ridge(0.97f * H, 12f, 0.012f, 77))
    }

    override fun overlay(c: Canvas, t: Float) { if (!day) stars.draw(c, t) }
}

// ---------- Makar Sankranti (kites) ----------

private class KitesScene(day: Boolean) : LayeredScene(day) {
    private val stars = Stars(50, 0.4f * H, 43)
    private val n = 9
    private val kx = FloatArray(n)
    private val ky = FloatArray(n)
    private val kph = FloatArray(n)
    private val kv = FloatArray(n)
    private val kc = IntArray(n)
    private val body = Path().apply { moveTo(0f, -13f); lineTo(9f, 0f); lineTo(0f, 17f); lineTo(-9f, 0f); close() }
    private val tail = Path().apply { moveTo(0f, 17f); for (i in 1..5) quadTo(if (i % 2 == 0) -5f else 5f, 17f + i * 6f - 3f, 0f, 17f + i * 6f) }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = 0xCCFFFFFF.toInt() }

    init {
        val r = Random(15)
        val palette = intArrayOf(0xFFE8472B.toInt(), 0xFFFFC83D.toInt(), 0xFFEE5FA0.toInt(), 0xFF3FB871.toInt(), 0xFFFF8A3D.toInt(), 0xFFFFFFFF.toInt())
        for (i in 0 until n) { kx[i] = r.nextFloat() * W; ky[i] = 20f + r.nextFloat() * 150f; kph[i] = r.nextFloat() * TAU; kv[i] = 3f + r.nextFloat() * 5f; kc[i] = palette[r.nextInt(palette.size)] }
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF0C1438.toInt(), 0xFF2A3A78.toInt(), 0xFF5A5A98.toInt()),
            intArrayOf(0xFF3C8CE0.toInt(), 0xFF9CCDF0.toInt(), 0xFFE4F3FB.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
        )
        celestial(c, Focals.KITES)
        layer(Depth.FAR)
        fillRidge(c, 0xFFB5D4E8.toInt(), ridge(0.7f * H, 30f, 0.007f, 91))
        layer(Depth.MID)
        fillRidge(c, 0xFF8FB8D2.toInt(), ridge(0.8f * H, 26f, 0.009f, 92))
        layer(Depth.NEAR)
        fillRidge(c, 0xFF5E86A2.toInt(), ridge(0.9f * H, 20f, 0.011f, 93))
        if (!day) for (i in 0 until 5) c.drawOval(RectF(40f + i * 95f, 24f + 14f * (i % 2), 110f + i * 95f, 36f + 14f * (i % 2)), fill(0x22FFFFFF))
    }

    override fun overlay(c: Canvas, t: Float) {
        if (!day) stars.draw(c, t)
        for (i in 0 until n) {
            val x = wrap(kx[i] + kv[i] * t, W + 60f) - 30f
            val y = ky[i] + 12f * sin(0.3f * t + kph[i])
            c.drawLine(x, y + 12f, x - 20f, H, line)
            c.save()
            c.translate(x, y)
            c.rotate(14f * sin(0.4f * t + kph[i]))
            paint.color = kc[i]
            c.drawPath(body, paint)
            c.drawPath(tail, line)
            c.restore()
        }
    }
}

// ---------- Lohri (bonfire) ----------

private class LohriScene(day: Boolean) : LayeredScene(day) {
    private val glow = glowPaint(150f, 0xFF8A2A, 0xB0)
    private val flame = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flameColors = intArrayOf(0xFFE8472B.toInt(), 0xFFFF9A2A.toInt(), 0xFFFFE08A.toInt())
    private val path = Path()
    private val ember = Paint()
    private val ex = FloatArray(10)
    private val ev = FloatArray(10)

    init {
        val r = Random(16)
        for (i in 0 until 10) { ex[i] = 215f + r.nextFloat() * 50f; ev[i] = 14f + r.nextFloat() * 14f }
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF0B1030.toInt(), 0xFF2A2A5E.toInt(), 0xFF8A4A58.toInt()),
            intArrayOf(0xFF4A90D8.toInt(), 0xFF9CCCEC.toInt(), 0xFFE8EEF4.toInt()),
            floatArrayOf(0f, 0.6f, 0.9f),
        )
        celestial(c, Focals.LOHRI)
        layer(Depth.FAR)
        fillRidge(c, 0xFF1B1E44.toInt(), ridge(0.74f * H, 30f, 0.007f, 94))
        layer(Depth.MID)
        fillRidge(c, 0xFF12132E.toInt(), ridge(0.86f * H, 22f, 0.01f, 95))
        layer(Depth.NEAR)
        gradientRect(c, 0.88f * H, H, intArrayOf(0xFF1C1210.toInt(), 0xFF0C0808.toInt()))
        val logs = fill(0xFF3A2216.toInt())
        for (k in -2..2) { c.save(); c.translate(240f, 0.93f * H); c.rotate(k * 22f); c.drawRoundRect(RectF(-30f, -4f, 30f, 4f), 3f, 3f, logs); c.restore() }
    }

    override fun overlay(c: Canvas, t: Float) {
        val f = 0.9f + 0.1f * sin(9f * t) + 0.06f * sin(17f * t)
        if (!day) c.drawGlowAt(240f, 0.9f * H, f, 150f, glow)
        for (layer in 0..2) {
            val h = (46f - layer * 13f) * (0.9f + 0.12f * sin(8f * t + layer * 2f))
            val w = 22f - layer * 6f
            path.reset()
            path.moveTo(240f - w, 0.93f * H)
            path.quadTo(240f - w * 0.6f + 3f * sin(5f * t + layer), 0.93f * H - h * 0.6f, 240f + 2f * sin(7f * t + layer), 0.93f * H - h)
            path.quadTo(240f + w * 0.6f, 0.93f * H - h * 0.6f, 240f + w, 0.93f * H)
            path.close()
            flame.color = flameColors[layer]
            c.drawPath(path, flame)
        }
        for (i in 0 until 10) {
            val y = 0.9f * H - wrap(ev[i] * t + i * 20f, 120f)
            val x = ex[i] + 8f * sin(0.9f * t + i)
            ember.color = withAlpha(0xFFB04A, (230f * (1f - (0.9f * H - y) / 120f)).toInt())
            c.drawRect(x, y, x + 1.6f, y + 1.6f, ember)
        }
    }
}

// ---------- Vasant Panchami ----------

private class VasantScene(day: Boolean) : LayeredScene(day) {
    private val n = 36
    private val fx = FloatArray(n)
    private val fy = FloatArray(n)
    private val fph = FloatArray(n)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloud = Paint().apply { color = 0x66FFFFFF }
    private val oval = RectF()
    private val pollen = Motes(24, 0xFFF0A0, blink = false, seed = 64, yMin = 0.35f * H, yMax = 0.9f * H)
    private val stars = Stars(50, 0.4f * H, 45)

    init {
        val r = Random(18)
        for (i in 0 until n) { fx[i] = r.nextFloat() * W; fy[i] = 0.74f * H + r.nextFloat() * 0.24f * H; fph[i] = r.nextFloat() * TAU }
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF0C1030.toInt(), 0xFF1E2C5A.toInt(), 0xFF4A5A70.toInt()),
            intArrayOf(0xFF5AAEEE.toInt(), 0xFFD6EBF2.toInt(), 0xFFF8F0B8.toInt()),
            floatArrayOf(0f, 0.5f, 0.72f),
        )
        celestial(c, Focals.VASANT)
        layer(Depth.FAR)
        fillRidge(c, 0xFFA8C88A.toInt(), ridge(0.66f * H, 26f, 0.007f, 101))
        layer(Depth.MID)
        fillRidge(c, 0xFFC9C95A.toInt(), ridge(0.74f * H, 22f, 0.009f, 102))
        layer(Depth.NEAR)
        fillRidge(c, 0xFFE3C93A.toInt(), ridge(0.82f * H, 20f, 0.011f, 103))
        val r2 = Random(19)
        val yellows = intArrayOf(0xFFF5D547.toInt(), 0xFFFFE266.toInt(), 0xFFE8BE2C.toInt(), 0xFF9AB43A.toInt())
        repeat(900) { // mustard flowers
            val x = r2.nextFloat() * W; val y = 0.8f * H + r2.nextFloat() * 0.2f * H
            c.drawCircle(x, y, 1.2f + (y / H - 0.8f) * 5f, fill(yellows[r2.nextInt(4)]))
        }
    }

    override fun overlay(c: Canvas, t: Float) {
        // Foreground blossoms nod in the breeze.
        for (i in 0 until n) {
            paint.color = if (i % 3 == 0) 0xFFFFE266.toInt() else 0xFFF5D547.toInt()
            c.drawCircle(fx[i] + 2.5f * sin(1.1f * t + fph[i]), fy[i], 1.8f + (fy[i] / H - 0.74f) * 6f, paint)
        }
        pollen.draw(c, t)
        if (!day) { stars.draw(c, t); return }
        val x = wrap(20f * t, W + 160f) - 80f
        oval.set(x, 34f, x + 80f, 50f); c.drawOval(oval, cloud)
        oval.set(x + 20f, 26f, x + 70f, 46f); c.drawOval(oval, cloud)
    }
}

// ---------- Karwa Chauth ----------

private class KarwaScene(day: Boolean) : LayeredScene(day) {
    private val stars = Stars(46, 0.32f * H, 61)

    // The moon sits in the top band, clear of the centred quote and the dock; the chhalni and diya motif was dropped because
    // the only free place for it was behind the dock, where it read as a stray arc.
    init {
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF0B0E2E.toInt(), 0xFF2A2060.toInt(), 0xFF8A4A6A.toInt(), 0xFFE0906A.toInt()),
            intArrayOf(0xFF3A80D0.toInt(), 0xFF9ACAE8.toInt(), 0xFFF8D8B8.toInt(), 0xFFFFEBD0.toInt()),
            floatArrayOf(0f, 0.5f, 0.8f, 1f),
        )
        horizonGlow(c, 0.78f * H, if (day) 0xFFE0B0 else 0xE0906A, 0x70)
        celestial(c, Focals.KARWA)
        layer(Depth.FAR)
        blocks(c, 0.8f * H, 14f, 44f, if (day) 0xFF6A4A5A.toInt() else 0xFF160C22.toInt(), 22, 0xFFC25A, lit = !day)
        layer(Depth.NEAR)
        fillRidge(c, if (day) 0xFF3A2A34.toInt() else 0xFF0C0814.toInt(), ridge(0.97f * H, 10f, 0.012f, 23))
    }

    override fun overlay(c: Canvas, t: Float) {
        if (!day) stars.draw(c, t)
    }
}

// ---------- Chhath ----------

private class ChhathScene(day: Boolean) : LayeredScene(day) {
    private val horizon = 0.52f * H
    private val glint = Paint()
    private val dgl = glowPaint(14f, 0xFFB43C, 0x90)
    private val flame = fill(0xFFFFD27A.toInt())
    private val bowl = fill(0xFF8A3A16.toInt())
    private val oval = RectF()
    private val water = WaterFx(24, 0.52f * H + 4f, 0.9f * H, if (day) 0xFFFFFF else 0xCFE0FF, 63)
    private val dx = floatArrayOf(60f, 130f, 200f, 300f, 370f, 430f, 250f, 160f)
    private val dyy = floatArrayOf(0.72f, 0.8f, 0.68f, 0.76f, 0.85f, 0.71f, 0.9f, 0.93f)

    init {
        val c = staticCanvas()
        sky(
            c, horizon,
            intArrayOf(0xFF0A0C28.toInt(), 0xFF1A2058.toInt(), 0xFF3A3A78.toInt(), 0xFF6A5A98.toInt()),
            intArrayOf(0xFF3A88D8.toInt(), 0xFF8AD0F0.toInt(), 0xFFFFE8C0.toInt(), 0xFFFFF4D8.toInt()),
            floatArrayOf(0f, 0.45f, 0.8f, 1f),
        )
        horizonGlow(c, horizon - 4f, if (day) 0xFFE8B0 else 0x7A6ACA, 0x90)
        celestial(c, Focals.CHHATH)
        layer(Depth.FAR)
        fillRidge(c, 0xFF4A2A52.toInt(), ridge(horizon + 2f, 14f, 0.012f, 111)) // far bank
layer(Depth.MID)
                gradientRect(
            c, horizon, H,
            if (day) intArrayOf(0xFFB8E0F0.toInt(), 0xFF3AA0C0.toInt(), 0xFF1A6A90.toInt(), 0xFF104A70.toInt())
            else intArrayOf(0xFF2A3A6A.toInt(), 0xFF16204A.toInt(), 0xFF0C1230.toInt(), 0xFF080C22.toInt()),
            floatArrayOf(0f, 0.3f, 0.7f, 1f),
        )
        streaks(c, horizon + 2f, 0.9f * H, if (day) 0xFFFFFF else 0xB8C8FF, 80, 14, 0x60, 150f, 330f)
layer(Depth.NEAR)
                // Offerings on the near bank: soop baskets (shallow trays) with fruit and a diya.
        val sand = fill(0xFF2A1A22.toInt())
        c.drawRect(0f, 0.94f * H, W, H, sand)
        for (bx in floatArrayOf(60f, 100f, 380f)) {
            val tray = Path().apply { moveTo(bx - 22f, 0.9f * H); lineTo(bx + 22f, 0.9f * H); lineTo(bx + 15f, 0.97f * H); lineTo(bx - 15f, 0.97f * H); close() }
            c.drawPath(tray, fill(0xFF10080E.toInt()))
            for (f in -1..1) c.drawCircle(bx + f * 9f, 0.9f * H - 3f, 4.5f, fill(0xFF10080E.toInt()))
        }
    }

    override fun overlay(c: Canvas, t: Float) {
        water.draw(c, t)
        val sunReflect = 240f
        for (i in 0 until 12) {
            val y = horizon + 4f + i * 12f
            val w = (6f + i * 4f) * (0.55f + 0.45f * sin(0.9f * t + i * 1.7f))
            glint.color = withAlpha(if (day) 0xFFFFFF else 0xCFE0FF, (210 * (1f - i / 14f)).toInt())
            val cx = sunReflect + 3f * sin(0.5f * t + i)
            c.drawRect(cx - w / 2f, y, cx + w / 2f, y + 1.6f, glint)
        }
        for (i in dx.indices) { // floating diyas bob and flicker
            val y = dyy[i] * H + 1.5f * sin(0.8f * t + i)
            val x = dx[i] + 2f * sin(0.3f * t + i)
            val f = 0.85f + 0.15f * sin(8f * t + i * 2f)
            c.drawGlowAt(x, y - 3f, f, 14f, dgl)
            oval.set(x - 5f, y - 1f, x + 5f, y + 3f); c.drawOval(oval, bowl)
            oval.set(x - 1.5f, y - 7f * f, x + 1.5f, y - 1f); c.drawOval(oval, flame)
        }
    }
}

// ---------- Halloween ----------

private class HalloweenScene(day: Boolean) : LayeredScene(day) {
    private val px = floatArrayOf(110f, 250f, 370f)
    private val py = floatArrayOf(0.92f * H, 0.95f * H, 0.9f * H)
    private val pr = floatArrayOf(22f, 28f, 20f)
    private val glow = glowPaint(46f, 0xFF8A1A, 0x90)
    private val bat = Path().apply {
        moveTo(0f, 0f); quadTo(-5f, -5f, -12f, -3f); quadTo(-9f, 0f, -7f, 3f); quadTo(-4f, 1f, 0f, 3f)
        quadTo(4f, 1f, 7f, 3f); quadTo(9f, 0f, 12f, -3f); quadTo(5f, -5f, 0f, 0f); close()
    }
    private val batPaint = fill(0xFF06040C.toInt())

    init {
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF0A0A24.toInt(), 0xFF1E1946.toInt(), 0xFF4A2A58.toInt()),
            intArrayOf(0xFF4A8CD0.toInt(), 0xFFA8D0E8.toInt(), 0xFFF8E0B0.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
        )
        celestial(c, Focals.HALLOWEEN)
        for (i in 0..2) {
            layer(if (i == 0) Depth.FAR else if (i == 1) Depth.MID else Depth.NEAR)
            val col = if (day) lerpColor(0xFFC8782C.toInt(), 0xFF5A3A1C.toInt(), i / 2f) else lerpColor(0xFF2A2650.toInt(), 0xFF0A0A18.toInt(), i / 2f)
            val y = ridge((0.62f + 0.1f * i) * H, 36f, 0.008f + 0.002f * i, 121 + i)
            fillRidge(c, col, y)
            pines(c, col, y, 14 + i * 4, 18f + i * 8f, 36f + i * 10f, 130 + i)
        }
        layer(Depth.NEAR)
        gradientRect(c, 0.86f * H, H, if (day) intArrayOf(0xFF7A5A2A.toInt(), 0xFF4A3A1C.toInt()) else intArrayOf(0xFF140E1C.toInt(), 0xFF07050C.toInt()))
        // Bare trees at the edges.
        val bark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (day) 0xFF3A2A1A.toInt() else 0xFF07050C.toInt(); style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        for (side in 0..1) {
            val x = if (side == 0) 24f else 456f
            bark.strokeWidth = 5f; c.drawLine(x, H, x, 0.45f * H, bark)
            bark.strokeWidth = 2.4f
            c.drawLine(x, 0.6f * H, x + (if (side == 0) 34f else -34f), 0.46f * H, bark)
            c.drawLine(x, 0.52f * H, x + (if (side == 0) -18f else 18f), 0.4f * H, bark)
        }
        for (i in 0..2) { // pumpkins: body, ridges, stem, carved face
            val body = RectF(px[i] - pr[i], py[i] - pr[i] * 0.8f, px[i] + pr[i], py[i] + pr[i] * 0.8f)
            c.drawOval(body, fill(0xFFD9641A.toInt()))
            val rib = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFA84410.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.2f }
            c.drawOval(RectF(px[i] - pr[i] * 0.5f, py[i] - pr[i] * 0.8f, px[i] + pr[i] * 0.5f, py[i] + pr[i] * 0.8f), rib)
            c.drawRect(px[i] - 2f, py[i] - pr[i] * 0.8f - 6f, px[i] + 2f, py[i] - pr[i] * 0.8f + 1f, fill(0xFF3A5A1E.toInt()))
            val face = fill(if (day) 0xFF2A1408.toInt() else 0xFFFFC85A.toInt()) // lit candle at night, dark carving by day
            val eyes = Path().apply {
                moveTo(px[i] - pr[i] * 0.5f, py[i] - 4f); lineTo(px[i] - pr[i] * 0.2f, py[i] - 4f); lineTo(px[i] - pr[i] * 0.35f, py[i] - 10f); close()
                moveTo(px[i] + pr[i] * 0.2f, py[i] - 4f); lineTo(px[i] + pr[i] * 0.5f, py[i] - 4f); lineTo(px[i] + pr[i] * 0.35f, py[i] - 10f); close()
            }
            c.drawPath(eyes, face)
            c.drawArc(RectF(px[i] - pr[i] * 0.5f, py[i] - 2f, px[i] + pr[i] * 0.5f, py[i] + 9f), 10f, 160f, true, face)
        }
    }

    override fun overlay(c: Canvas, t: Float) {
        if (day) return
        for (i in 0..2) {
            val f = 0.8f + 0.2f * sin(6f * t + i * 2.1f) + 0.08f * sin(11f * t + i)
            c.drawGlowAt(px[i], py[i], f, 46f, glow)
        }
        for (b in 0..1) { // two bats crossing on different periods, wings flapping
            val period = if (b == 0) 16f else 23f
            val p = wrap(t + b * 7f, period) / period
            val x = -20f + p * (W + 40f)
            val y = 50f + b * 40f + 14f * sin(2.2f * t + b)
            c.save(); c.translate(x, y); c.scale(1f, 0.55f + 0.45f * sin(14f * t + b)); c.drawPath(bat, batPaint); c.restore()
        }
    }
}

// ---------- Thanksgiving ----------

private class ThanksgivingScene(day: Boolean) : LayeredScene(day) {
    private val leaves = Falling(12, intArrayOf(0xFFD65D0E.toInt(), 0xFFFE8019.toInt(), 0xFFD79921.toInt(), 0xFFB5402F.toInt()), 24, 3.5f, 2f, 9f, 16f)
    private val stars = Stars(50, 0.4f * H, 47)

    init {
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF0E1230.toInt(), 0xFF3A2A50.toInt(), 0xFF7A4A48.toInt()),
            intArrayOf(0xFF4A90D0.toInt(), 0xFFA8D0E8.toInt(), 0xFFFCE6B0.toInt()),
            floatArrayOf(0f, 0.5f, 0.72f),
        )
        horizonGlow(c, 0.6f * H, if (day) 0xFFD27A else 0xB05A5A, 0x90)
        celestial(c, Focals.THANKSGIVING)
        layer(Depth.FAR)
        fillRidge(c, 0xFFB5793A.toInt(), ridge(0.6f * H, 26f, 0.007f, 141))
        layer(Depth.MID)
        fillRidge(c, 0xFF8E8A28.toInt(), ridge(0.68f * H, 24f, 0.009f, 142))
layer(Depth.NEAR)
                // Wheat field: rows of golden stalks, denser and larger toward the viewer.
        gradientRect(c, 0.72f * H, H, intArrayOf(0xFFC9A04A.toInt(), 0xFF8A5E24.toInt()))
        val stalk = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.2f }
        val r = Random(25)
        val golds = intArrayOf(0xFFE8C45A.toInt(), 0xFFD9A93A.toInt(), 0xFFF2D77A.toInt())
        repeat(520) {
            val y = 0.74f * H + r.nextFloat() * 0.26f * H
            val x = r.nextFloat() * W; val h = 8f + (y / H - 0.74f) * 60f
            stalk.color = golds[r.nextInt(3)]
            c.drawLine(x, y, x + 2f, y - h, stalk)
            c.drawOval(RectF(x - 0.5f, y - h - 4f, x + 3f, y - h + 2f), fill(stalk.color))
        }
        for (i in 0..2) { // pumpkins
            val px = 80f + i * 160f; val py = 0.9f * H + (i % 2) * 8f; val pr = 22f + (i % 2) * 6f
            c.drawOval(RectF(px - pr, py - pr * 0.8f, px + pr, py + pr * 0.8f), fill(0xFFD9641A.toInt()))
            c.drawOval(RectF(px - pr * 0.5f, py - pr * 0.8f, px + pr * 0.5f, py + pr * 0.8f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFA84410.toInt(); style = Paint.Style.STROKE })
            c.drawRect(px - 2f, py - pr * 0.8f - 6f, px + 2f, py - pr * 0.8f + 1f, fill(0xFF4A5A1E.toInt()))
        }
    }

    override fun overlay(c: Canvas, t: Float) { if (!day) stars.draw(c, t); leaves.draw(c, t) }
}

// ---------- Christmas ----------

private class ChristmasScene(day: Boolean) : LayeredScene(day) {
    private val snow = Snowfall(50, 26)
    private val lights: Bulbs
    private val windowGlow = glowPaint(26f, 0xFFB43C, 0x70)

    init {
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF081028.toInt(), 0xFF16284A.toInt(), 0xFF2E4A72.toInt()),
            intArrayOf(0xFF3A88D8.toInt(), 0xFF9AD0F0.toInt(), 0xFFF0F8FF.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
        )
        celestial(c, Focals.CHRISTMAS)
        for (i in 0..2) {
            layer(if (i == 0) Depth.FAR else if (i == 1) Depth.MID else Depth.NEAR)
            val col = lerpColor(0xFF2A4560.toInt(), 0xFF0C1A26.toInt(), i / 2f)
            val y = ridge((0.62f + 0.09f * i) * H, 30f, 0.008f + 0.002f * i, 151 + i)
            fillRidge(c, col, y)
            pines(c, col, y, 20 + i * 4, 20f + i * 8f, 38f + i * 10f, 160 + i)
        }
        layer(Depth.NEAR)
        gradientRect(c, 0.86f * H, H, intArrayOf(0xFFDCE8F4.toInt(), 0xFFB4C6DA.toInt()))
        // Cabin with lit windows and a chimney.
        c.drawRect(60f, 0.62f * H, 150f, 0.88f * H, fill(0xFF3A2216.toInt()))
        c.drawPath(Path().apply { moveTo(52f, 0.62f * H); lineTo(105f, 0.5f * H); lineTo(158f, 0.62f * H); close() }, fill(0xFF1C1420.toInt()))
        c.drawPath(Path().apply { moveTo(56f, 0.62f * H); lineTo(105f, 0.52f * H); lineTo(154f, 0.62f * H); close() }, fill(0xFFE8EEF6.toInt()))
        c.drawRect(126f, 0.47f * H, 136f, 0.55f * H, fill(0xFF2A1A18.toInt()))
        if (!day) { glow(c, 80f, 0.72f * H, 30f, 0xFFB43C, 0x90); glow(c, 128f, 0.72f * H, 30f, 0xFFB43C, 0x90) }
        val windowColor = if (day) 0xFF9CB4C8.toInt() else 0xFFFFD27A.toInt() // warm light only at night
        c.drawRect(72f, 0.69f * H, 90f, 0.76f * H, fill(windowColor))
        c.drawRect(120f, 0.69f * H, 138f, 0.76f * H, fill(windowColor))
        c.drawRect(98f, 0.74f * H, 112f, 0.88f * H, fill(0xFF1C1210.toInt()))
        // Decorated tree on the right.
        val tx = 360f; val ty = 0.88f * H
        c.drawRect(tx - 5f, ty - 4f, tx + 5f, ty + 6f, fill(0xFF4A2A16.toInt()))
        for (k in 0..3) c.drawPath(Path().apply { moveTo(tx, ty - 120f + k * 26f); lineTo(tx + 26f + k * 12f, ty - 14f - (3 - k) * 26f + 6f); lineTo(tx - 26f - k * 12f, ty - 14f - (3 - k) * 26f + 6f); close() }, fill(lerpColor(0xFF1E5A34.toInt(), 0xFF0F3A22.toInt(), k / 3f)))
        c.drawPath(Path().apply { // star on top
            moveTo(tx, ty - 138f); lineTo(tx + 4f, ty - 128f); lineTo(tx + 14f, ty - 128f); lineTo(tx + 6f, ty - 121f); lineTo(tx + 9f, ty - 111f)
            lineTo(tx, ty - 117f); lineTo(tx - 9f, ty - 111f); lineTo(tx - 6f, ty - 121f); lineTo(tx - 14f, ty - 128f); lineTo(tx - 4f, ty - 128f); close()
        }, fill(0xFFFFD95A.toInt()))
        val xs = FloatArray(14); val ys = FloatArray(14)
        val r = Random(28)
        for (i in 0 until 14) { val ky = 20f + r.nextFloat() * 90f; ys[i] = ty - 112f + ky; xs[i] = tx + (r.nextFloat() - 0.5f) * (ky * 0.7f + 10f) }
        lights = Bulbs(xs, ys, intArrayOf(0xFF5A5A, 0xFFD95A, 0x7AB8FF, 0x8AE08A), 2.4f)
    }

    override fun overlay(c: Canvas, t: Float) {
        if (!day) lights.draw(c, t)
        snow.draw(c, t)
    }
}

// ---------- New Year ----------

/** [patriotic] = the Independence Day variant: the same fireworks in red, white and blue. */
private class NewYearScene(day: Boolean, patriotic: Boolean = false) : LayeredScene(day) {
    private val stars = Stars(60, 0.55f * H, 29)
    private val confetti = Falling(
        20,
        if (patriotic) intArrayOf(0xFFE8413B.toInt(), 0xFFFFFFFF.toInt(), 0xFF3C6FE0.toInt()) else intArrayOf(0xFFFFD95A.toInt(), 0xFFFF7AB8.toInt(), 0xFF7AD0FF.toInt(), 0xFF9AFF9A.toInt()),
        30, 2f, 1.2f, 6f, 12f,
    )
    private val fireworks = Fireworks(
        floatArrayOf(90f, 210f, 330f, 430f, 150f), floatArrayOf(60f, 40f, 75f, 50f, 100f),
        floatArrayOf(7f, 9f, 8f, 11f, 13f),
        if (patriotic) intArrayOf(0xFF4B45, 0xFFFFFF, 0x4A8CFF, 0xFFFFFF, 0xFF4B45) else intArrayOf(0xFFD95A, 0xFF7AB8, 0x8FE0FF, 0xFFFFFF, 0xA0FF9A), 40f,
    )

    init {
        val c = staticCanvas()
        sky(
            c, H,
            intArrayOf(0xFF03040E.toInt(), 0xFF0E1236.toInt(), 0xFF2A2656.toInt()),
            intArrayOf(0xFF3A88D8.toInt(), 0xFF9AD0F0.toInt(), 0xFFF4F0E0.toInt()),
            floatArrayOf(0f, 0.7f, 1f),
        )
        layer(Depth.MID)
        fillRidge(c, 0xFF14143A.toInt(), ridge(0.86f * H, 26f, 0.008f, 171))
        layer(Depth.NEAR)
        fillRidge(c, 0xFF08081C.toInt(), ridge(0.95f * H, 16f, 0.011f, 172))
    }

    override fun overlay(c: Canvas, t: Float) {
        if (day) { confetti.draw(c, t); return }
        stars.draw(c, t)
        fireworks.draw(c, t)
    }
}
