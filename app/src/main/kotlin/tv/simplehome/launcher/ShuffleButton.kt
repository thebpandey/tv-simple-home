package tv.simplehome.launcher

import android.content.Context
import android.graphics.Paint
import android.graphics.Path

/**
 * Small round "next wallpaper" button in the top bar, between the clock group and the weather. Deliberately quiet: a faint fill and a
 * 60% glyph at rest, so it does not compete with the clock; focused it gets the accent ring and grows 1.15x like other focus targets.
 */
class ShuffleButton(context: Context) : FocusItem(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glyph = Path()

    init {
        setWillNotDraw(false)
        id = generateViewId()
        key = "shuffle"
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        contentDescription = "Next wallpaper"
        setOnFocusChangeListener { _, focused ->
            val s = if (focused) 1.15f else 1f
            animate().scaleX(s).scaleY(s).setDuration(150).setInterpolator(Motion.EASE).start()
            invalidate()
        }
    }

    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(context.dp(SIZE_DP), context.dp(SIZE_DP))

    override fun onDraw(canvas: android.graphics.Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = w / 2f
        val ring = context.dp(1).toFloat()
        paint.style = Paint.Style.FILL
        paint.color = if (Palette.day) 0x14000000 else 0x1FFFFFFF
        canvas.drawCircle(r, h / 2f, r - ring, paint)
        if (isFocused) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2 * ring
            paint.color = Palette.accent
            canvas.drawCircle(r, h / 2f, r - ring, paint)
        }
        // SF-style "shuffle": two crossing strokes, each ending in an arrowhead on the right.
        val l = w * 0.28f; val rt = w * 0.72f; val top = h * 0.36f; val bot = h * 0.64f; val head = w * 0.09f
        glyph.reset()
        glyph.moveTo(l, top); glyph.cubicTo(w * 0.48f, top, w * 0.52f, bot, rt, bot)
        glyph.moveTo(l, bot); glyph.cubicTo(w * 0.48f, bot, w * 0.52f, top, rt, top)
        glyph.moveTo(rt - head, top - head); glyph.lineTo(rt, top); glyph.lineTo(rt - head, top + head)
        glyph.moveTo(rt - head, bot - head); glyph.lineTo(rt, bot); glyph.lineTo(rt - head, bot + head)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = context.dpf(1.5f).toFloat().coerceAtLeast(1.5f)
        paint.color = if (isFocused) Palette.text else (Palette.text and 0x00FFFFFF) or (0x99 shl 24)
        canvas.drawPath(glyph, paint)
    }

    companion object { const val SIZE_DP = 32 }
}
