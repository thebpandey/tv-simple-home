package tv.simplehome.launcher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Pure pacing and geometry of the quote's "written by hand" reveal: text appears (and later disappears) left to right, line by line,
 * then the author. Progress is counted in characters so every line moves at the same writing speed whatever its width.
 */
object QuoteReveal {
    const val WRITE_MS_PER_CHAR = 55L // about 18 characters a second: unhurried, like someone writing it out
    const val ERASE_MS_PER_CHAR = 18L // leaving is quicker than arriving
    const val AUTHOR_PAUSE_CHARS = 8 // a short breath (about 0.4s) before the author's name is written
    const val MIN_WRITE_MS = 2_500L
    const val MAX_WRITE_MS = 10_000L
    const val MIN_ERASE_MS = 1_200L
    const val MAX_ERASE_MS = 3_500L

    /** Total progress units for a quote with these line lengths and an author line of [authorChars]. */
    fun totalUnits(lineChars: IntArray, authorChars: Int): Int = lineChars.sum() + AUTHOR_PAUSE_CHARS + authorChars

    fun writeMs(units: Int): Long = (units * WRITE_MS_PER_CHAR).coerceIn(MIN_WRITE_MS, MAX_WRITE_MS)
    fun eraseMs(units: Int): Long = (units * ERASE_MS_PER_CHAR).coerceIn(MIN_ERASE_MS, MAX_ERASE_MS)

    /**
     * Where the pen is after [progress] units: (row, fraction of that row written). Rows are the quote lines, then the author as the
     * last row; during the pause before the author the pen waits at the author row's start. Past the end: (rows, 0) = everything done.
     */
    fun penAt(lineChars: IntArray, authorChars: Int, progress: Float): Pair<Int, Float> {
        var left = progress
        for (i in lineChars.indices) {
            val n = lineChars[i].coerceAtLeast(1)
            if (left < n) return i to (left / n).coerceAtLeast(0f)
            left -= n
        }
        left -= AUTHOR_PAUSE_CHARS
        if (left < 0f) return lineChars.size to 0f
        val n = authorChars.coerceAtLeast(1)
        return if (left < n) lineChars.size to left / n else lineChars.size + 1 to 0f
    }
}

/**
 * Vertical quote block (quote text, then author) that can write itself in or wipe itself out left to right with a soft pen edge.
 * Outside an animation it draws normally, with no offscreen layer.
 */
@android.annotation.SuppressLint("ViewConstructor") // built in code only, never inflated from XML
class QuoteRevealLayout(context: Context, private val text: TextView, private val author: TextView) : LinearLayout(context) {
    private enum class Phase { SHOWN, WRITING, ERASING, HIDDEN }

    private var phase = Phase.SHOWN
    private var progress = 0f
    private var animator: ValueAnimator? = null
    private val feather = context.dp(28).toFloat() // the soft edge, about two letters wide
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }
    private val edgePaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }

    init {
        orientation = VERTICAL
        setWillNotDraw(false)
        addView(text, LayoutParams(-2, -2))
        addView(author, LayoutParams(-2, -2))
    }

    /** Wipes the current quote out, runs [swap] while nothing is visible, then writes the new quote in. */
    fun replace(swap: () -> Unit) {
        if (phase == Phase.HIDDEN) { swap(); write(); return }
        animate(Phase.ERASING, QuoteReveal::eraseMs) { swap(); write() }
    }

    fun write() = animate(Phase.WRITING, QuoteReveal::writeMs) { phase = Phase.SHOWN; invalidate() }

    private fun animate(next: Phase, duration: (Int) -> Long, done: () -> Unit) {
        animator?.cancel()
        phase = next
        progress = 0f
        // Line lengths are known only after layout; measure once laid out (the swap may have changed the text).
        post {
            val units = QuoteReveal.totalUnits(lineChars(), authorChars()).toFloat()
            animator = ValueAnimator.ofFloat(0f, units).apply {
                this.duration = duration(units.toInt())
                interpolator = LinearInterpolator()
                addUpdateListener { progress = it.animatedValue as Float; invalidate() }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    private var cancelled = false
                    override fun onAnimationCancel(a: android.animation.Animator) { cancelled = true }
                    override fun onAnimationEnd(a: android.animation.Animator) {
                        if (cancelled) return
                        if (next == Phase.ERASING) phase = Phase.HIDDEN
                        done()
                    }
                })
                start()
            }
        }
        invalidate()
    }

    private fun lineChars(): IntArray {
        val l = text.layout ?: return IntArray(0)
        return IntArray(l.lineCount) { l.getLineEnd(it) - l.getLineStart(it) }
    }

    private fun authorChars(): Int = author.text?.length ?: 0

    override fun dispatchDraw(canvas: Canvas) {
        if (phase == Phase.SHOWN) { super.dispatchDraw(canvas); return }
        if (phase == Phase.HIDDEN) return
        val w = width.toFloat()
        val h = height.toFloat()
        val layer = canvas.saveLayer(0f, 0f, w, h, null)
        super.dispatchDraw(canvas)
        val lines = lineChars()
        val (row, frac) = QuoteReveal.penAt(lines, authorChars(), progress)
        val rows = lines.size + 1
        for (r in 0 until rows) {
            val top = rowTop(r, lines.size)
            val bottom = rowBottom(r, lines.size, h)
            val hidden = if (phase == Phase.WRITING) r > row else r < row
            if (hidden) { canvas.drawRect(0f, top, w, bottom, clearPaint); continue }
            if (r != row) continue
            val (left, right) = rowSpan(r, lines.size)
            val pen = left + (right - left) * frac
            // WRITING: everything right of the pen is cleared, fading in over the feather. ERASING: everything left of it is cleared.
            if (phase == Phase.WRITING) {
                edgePaint.shader = LinearGradient(pen - feather, 0f, pen, 0f, 0x00000000, 0xFF000000.toInt(), Shader.TileMode.CLAMP)
                canvas.drawRect(pen - feather, top, pen, bottom, edgePaint)
                canvas.drawRect(pen, top, w, bottom, clearPaint)
            } else {
                edgePaint.shader = LinearGradient(pen, 0f, pen + feather, 0f, 0xFF000000.toInt(), 0x00000000, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, top, pen, bottom, clearPaint)
                canvas.drawRect(pen, top, pen + feather, bottom, edgePaint)
            }
        }
        canvas.restoreToCount(layer)
    }

    // Rows tile the whole block vertically (shadows included): quote lines from the top, the author row takes the rest.
    private fun rowTop(r: Int, quoteLines: Int): Float {
        val l = text.layout ?: return 0f
        return when {
            r == 0 -> 0f
            r < quoteLines -> text.top + text.totalPaddingTop + l.getLineTop(r).toFloat()
            else -> text.bottom.toFloat()
        }
    }

    private fun rowBottom(r: Int, quoteLines: Int, h: Float): Float = when {
        r < quoteLines - 1 -> rowTop(r + 1, quoteLines)
        r == quoteLines - 1 -> text.bottom.toFloat()
        else -> h
    }

    /** Horizontal extent of a row's ink in this layout's coordinates; the pen travels from its left to its right. */
    private fun rowSpan(r: Int, quoteLines: Int): Pair<Float, Float> {
        val (view, line) = if (r < quoteLines) text to r else author to 0
        val l = view.layout ?: return 0f to width.toFloat()
        val x0 = view.left + view.totalPaddingLeft
        return (x0 + l.getLineLeft(line)) to (x0 + l.getLineRight(line))
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
