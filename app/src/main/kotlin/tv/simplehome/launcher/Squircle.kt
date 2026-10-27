package tv.simplehome.launcher

import android.graphics.Path
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** Continuous-corner "squircle": the superellipse |x|^n + |y|^n = 1 (n = 5 is close to the iOS / macOS icon shape). */
object Squircle {
    const val N = 5f
    private const val STEPS = 96

    /** Points of the unit superellipse as x0,y0,x1,y1,... in [-1, 1]; pure so the shape can be unit tested. */
    fun unitPoints(n: Float = N, steps: Int = STEPS): FloatArray {
        val out = FloatArray(steps * 2)
        val e = 2.0 / n
        for (i in 0 until steps) {
            val t = 2.0 * Math.PI * i / steps
            val c = cos(t)
            val s = sin(t)
            out[2 * i] = (sign(c) * abs(c).pow(e)).toFloat()
            out[2 * i + 1] = (sign(s) * abs(s).pow(e)).toFloat()
        }
        return out
    }

    /** Squircle filling the box (left, top, right, bottom). */
    fun path(left: Float, top: Float, right: Float, bottom: Float, n: Float = N): Path {
        val p = unitPoints(n)
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f
        val rx = (right - left) / 2f
        val ry = (bottom - top) / 2f
        return Path().apply {
            for (i in 0 until p.size / 2) {
                val x = cx + p[2 * i] * rx
                val y = cy + p[2 * i + 1] * ry
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }
}
