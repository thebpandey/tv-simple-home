package tv.simplehome.launcher

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.animation.PathInterpolator
import android.widget.TextView

/** One shared easing for all UI motion (a smooth, no-overshoot decelerate). */
object Motion {
    val EASE = PathInterpolator(0.2f, 0f, 0f, 1f)
}

/** Inter (SIL OFL) as the single UI typeface, loaded as a variable font at the requested weight. */
object Fonts {
    private val cache = HashMap<Int, Typeface>()

    fun typeface(context: Context, weight: Int): Typeface = cache.getOrPut(weight) {
        Typeface.Builder(context.assets, "fonts/InterVariable.ttf").setFontVariationSettings("'wght' $weight").build() ?: Typeface.DEFAULT
    }
}

/** Applies the design system's type: Inter at [weight], [sp] size, letter-spacing in em, optional tabular numerals. */
fun TextView.style(weight: Int, sp: Float, tracking: Float = 0f, tabular: Boolean = false) {
    typeface = Fonts.typeface(context, weight)
    textSize = sp
    letterSpacing = tracking
    if (tabular) fontFeatureSettings = "tnum"
}

/** Soft shadow for the header text, which sits directly on the wallpaper: black 45% at night, white 50% on the light UI; 8dp blur, 1dp down. */
fun TextView.headerShadow() = setShadowLayer(context.dp(8).toFloat(), 0f, context.dp(1).toFloat(), Palette.headerShadow)

/** "Glass" material: translucent fill with a 1px hairline border; the blur behind it is not implemented (see the report). */
object Glass {
    val fill get() = if (Palette.day) 0x99F2F2F7.toInt() else 0x8C1C1C1E.toInt()
    val hairline get() = if (Palette.day) 0x14000000 else 0x24FFFFFF

    fun drawable(context: Context, radiusPx: Float, fillColor: Int = fill): GradientDrawable = GradientDrawable().apply {
        cornerRadius = radiusPx
        setColor(fillColor)
        setStroke(1, hairline) // 1px
    }
}
