package tv.simplehome.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Focusable weather readout for the top bar. Hidden (GONE, so unfocusable) until it has valid data. */
class WeatherWidget(context: Context) : LinearLayout(context) {
    private val icon = ImageView(context)
    private val temp = text(44f, COLOR_TEXT, weight = 300)
    private val condition = text(14f, COLOR_TEXT)
    private val place = text(14f, COLOR_MUTED)
    private val air = text(13f, COLOR_MUTED).apply { visibility = GONE }
    private val border = GradientDrawable().apply {
        cornerRadius = context.dp(LayoutBudget.CARD_RADIUS_DP).toFloat()
        setColor(Color.TRANSPARENT)
        setStroke(context.dp(4), COLOR_ACCENT)
    }

    private val borderInset = android.graphics.drawable.InsetDrawable(border, context.dp(2)) // keep the whole stroke inside the bounds

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isClickable = true
        defaultFocusHighlightEnabled = false // the platform highlight looked like a permanent outline
        visibility = GONE
        // 4dp of stroke sits inside the bounds, so the content gets 8dp more on every side to stay fully inside the focus ring.
        setPadding(context.dp(LayoutBudget.CARD_PAD_H_DP), context.dp(LayoutBudget.CARD_PAD_V_DP), context.dp(LayoutBudget.CARD_PAD_H_DP), context.dp(LayoutBudget.CARD_PAD_V_DP))
        elevation = context.dp(6).toFloat()
        addView(icon, LayoutParams(context.dp(36), context.dp(36)))
        temp.setPadding(context.dp(6), 0, 0, 0)
        addView(temp, LayoutParams(-2, -2))
        val col = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(context.dp(10), 0, 0, 0)
            addView(condition, LayoutParams(-2, -2))
            addView(place, LayoutParams(-2, -2))
            addView(air, LayoutParams(-2, -2))
        }
        addView(col, LayoutParams(-2, -2))
        setOnFocusChangeListener { _, focused ->
            // Same focus style as the tiles: accent border + 1.06x, and nothing at all when unfocused.
            foreground = if (focused) borderInset else null
            val s = if (focused) 1.06f else 1f
            animate().scaleX(s).scaleY(s).setDuration(120).start()
        }
    }

    /** Re-colours text and the focus border for the current day/night UI mode. */
    fun applyTheme() {
        temp.setTextColor(Palette.text)
        condition.setTextColor(Palette.text70)
        place.setTextColor(Palette.text70)
        border.setStroke(context.dp(2), Palette.accent) // system-blue ring over the always-on glass card
        border.setColor(Color.TRANSPARENT)
        background = Glass.drawable(context, context.dp(LayoutBudget.CARD_RADIUS_DP).toFloat())
    }

    fun show(data: WeatherData, placeName: String) {
        icon.setImageResource(iconFor(data.condition))
        temp.text = "${data.tempF}°"
        // Condition and high/low share one line so the widget (with the air-quality line) fits the 64dp header.
        condition.text = "${data.condition.label}  \u00B7  H ${data.highF}\u00B0  L ${data.lowF}\u00B0"
        place.text = placeName
        visibility = VISIBLE
    }

    /** One compact line under the place name; hidden on its own when air quality is missing or stale. */
    fun showAir(data: AirQuality) {
        val cat = AirQualityRules.category(data.usAqi)
        val sp = android.text.SpannableString("\u25CF ${AirQualityRules.line(data)}")
        sp.setSpan(android.text.style.ForegroundColorSpan(AirQualityRules.dotColor(cat, Palette.day)), 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        air.text = sp
        air.setTextColor(Palette.text70)
        air.visibility = VISIBLE
    }

    fun hideAir() {
        air.visibility = GONE
    }

    fun hide() {
        visibility = GONE
    }

    private fun text(sp: Float, color: Int, weight: Int = 400) = TextView(context).apply {
        style(weight, sp, tabular = sp >= 40f)
        setTextColor(color)
        setSingleLine()
    }

    private fun iconFor(c: Condition): Int = when (c) {
        Condition.CLEAR -> R.drawable.wx_clear
        Condition.PARTLY_CLOUDY -> R.drawable.wx_partly
        Condition.CLOUDY -> R.drawable.wx_cloudy
        Condition.FOG -> R.drawable.wx_fog
        Condition.DRIZZLE -> R.drawable.wx_drizzle
        Condition.RAIN -> R.drawable.wx_rain
        Condition.SNOW -> R.drawable.wx_snow
        Condition.THUNDERSTORM -> R.drawable.wx_thunder
    }
}
