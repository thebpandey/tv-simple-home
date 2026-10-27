package tv.simplehome.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build

/** What a probe render of an icon says about how it must be framed. */
data class IconAnalysis(val fullBleed: Boolean, val mostlyWhite: Boolean)

/** Pure pixel analysis (ARGB ints) so the plate rules are JVM-testable. */
object IconRules {
    const val PLATE_SCALE = 0.78f
    const val PLATE_LIGHT = 0xFFFFFFFF.toInt()
    const val PLATE_DARK = 0xFF3A3A3C.toInt() // lighter than the old #2C2C2E: that vanished against the dimmed Launchpad

    fun analyze(pixels: IntArray, w: Int, h: Int): IconAnalysis {
        fun alpha(x: Int, y: Int) = (pixels[y * w + x] ushr 24) and 255
        // Full-bleed = all four corners (1px in) are opaque: the artwork already fills the square, a plate would only shrink it.
        val fullBleed = alpha(1, 1) >= 230 && alpha(w - 2, 1) >= 230 && alpha(1, h - 2) >= 230 && alpha(w - 2, h - 2) >= 230
        var opaque = 0
        var white = 0
        for (p in pixels) {
            if ((p ushr 24) < 128) continue
            opaque++
            if (((p shr 16) and 255) > 0xE0 && ((p shr 8) and 255) > 0xE0 && (p and 255) > 0xE0) white++
        }
        return IconAnalysis(fullBleed, mostlyWhite = opaque > 0 && white.toFloat() / opaque >= 0.55f)
    }

    /** The plate colour for an icon that has to sit on one: white, or dark grey when the icon is itself mostly white. */
    fun plateColor(a: IconAnalysis): Int = if (a.mostlyWhite) PLATE_DARK else PLATE_LIGHT
}

/** Renders any app icon into a continuous-corner squircle bitmap. Allocates freely: call it off the UI thread. */
object IconRenderer {
    private const val PROBE = 32

    fun render(d: Drawable, px: Int): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val size = px.toFloat()
        val shape = Squircle.path(0f, 0f, size, size)
        if (Build.VERSION.SDK_INT >= 26 && d is AdaptiveIconDrawable) {
            // The layers are 108dp with the 72dp visible window in the middle: extend the bounds by a quarter each side.
            val pad = (px * 0.25f).toInt()
            d.background?.let { it.setBounds(-pad, -pad, px + pad, px + pad); it.draw(c) }
            d.foreground?.let { it.setBounds(-pad, -pad, px + pad, px + pad); it.draw(c) }
        } else {
            val analysis = analyze(d)
            if (analysis.fullBleed) {
                d.setBounds(0, 0, px, px)
                d.draw(c)
            } else {
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = IconRules.plateColor(analysis) }
                c.drawPath(shape, fill)
                val inset = (px * (1f - IconRules.PLATE_SCALE) / 2f).toInt()
                d.setBounds(inset, inset, px - inset, px - inset)
                d.draw(c)
            }
        }
        maskOutside(c, shape)
        // A faint light edge keeps a dark plate visible on the dimmed Launchpad (and any icon crisp on both backdrops).
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = 0x33FFFFFF }
        c.drawPath(Squircle.path(1f, 1f, size - 1f, size - 1f), edge)
        return bmp
    }

    private fun analyze(d: Drawable): IconAnalysis {
        val probe = Bitmap.createBitmap(PROBE, PROBE, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, PROBE, PROBE)
        d.draw(Canvas(probe))
        val pixels = IntArray(PROBE * PROBE)
        probe.getPixels(pixels, 0, PROBE, 0, 0, PROBE, PROBE)
        probe.recycle()
        return IconRules.analyze(pixels, PROBE, PROBE)
    }

    /** Clears everything outside the squircle with an anti-aliased edge (clipPath on a software canvas is not anti-aliased). */
    private fun maskOutside(c: Canvas, shape: Path) {
        val inverse = Path(shape).apply { fillType = Path.FillType.INVERSE_WINDING }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        c.drawPath(inverse, paint)
    }
}
