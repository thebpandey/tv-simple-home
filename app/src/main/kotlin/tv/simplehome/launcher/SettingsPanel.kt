package tv.simplehome.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Pure scroll rule for the settings list: keep [top]..[bottom] (content coordinates) inside the viewport with [margin] to spare. */
object PanelScroll {
    fun target(scrollY: Int, viewportH: Int, top: Int, bottom: Int, margin: Int): Int {
        val lo = top - margin
        val hi = bottom + margin
        return when {
            lo < scrollY -> lo.coerceAtLeast(0)
            hi > scrollY + viewportH -> (hi - viewportH).coerceAtLeast(0)
            else -> scrollY
        }
    }
}

enum class OptionKind { RADIO, SWITCH, ACTION }

sealed interface PanelItem {
    data class Header(val text: String) : PanelItem
    /** [checked] is re-evaluated after every selection so checkmarks and switches always show the real state. */
    class Option(val label: String, val kind: OptionKind, val checked: () -> Boolean = { false }, val onSelect: () -> Unit) : PanelItem
}

private const val SWITCH_GREEN = 0xFF34C759.toInt()

/** A checkmark (selection) or an iOS-style switch, drawn on a canvas; allocation-free in onDraw. */
private class Indicator(context: Context, private val kind: OptionKind) : View(context) {
    var checked = false
        set(value) { field = value; invalidate() }
    /** True while the row has the accent highlight, so the glyph switches to white for contrast. */
    var onAccent = false
        set(value) { field = value; invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val tick = Path()

    override fun onMeasure(w: Int, h: Int) {
        if (kind == OptionKind.SWITCH) setMeasuredDimension(context.dp(46), context.dp(28)) else setMeasuredDimension(context.dp(22), context.dp(22))
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (kind == OptionKind.SWITCH) {
            paint.style = Paint.Style.FILL
            paint.color = if (checked) SWITCH_GREEN else if (Palette.day) 0x29000000 else 0x4DFFFFFF
            rect.set(0f, 0f, w, h)
            canvas.drawRoundRect(rect, h / 2f, h / 2f, paint)
            val r = h / 2f - context.dp(2)
            val cx = if (checked) w - h / 2f else h / 2f
            paint.color = 0x33000000
            canvas.drawCircle(cx, h / 2f + context.dp(1), r, paint) // soft shadow under the knob
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(cx, h / 2f, r, paint)
        } else if (checked) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = context.dp(2).toFloat()
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            paint.color = if (onAccent) 0xFFFFFFFF.toInt() else Palette.accent
            tick.reset()
            tick.moveTo(w * 0.14f, h * 0.55f); tick.lineTo(w * 0.40f, h * 0.80f); tick.lineTo(w * 0.88f, h * 0.22f)
            canvas.drawPath(tick, paint)
        }
    }
}

private class PanelRow(context: Context, val item: PanelItem.Option) : FocusItem(context) {
    var onFocused: ((PanelRow) -> Unit)? = null
    private val indicator: Indicator? = if (item.kind == OptionKind.ACTION) null else Indicator(context, item.kind)
    private val label = TextView(context)
    private val focusBackground = GradientDrawable().apply {
        cornerRadius = context.dp(10).toFloat()
        setColor(Palette.accent) // the system-blue selection pill, as in macOS menus
    }

    init {
        id = generateViewId()
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        setPadding(context.dp(14), 0, context.dp(14), 0)
        label.style(400, 15f)
        label.setTextColor(Palette.text)
        label.setSingleLine()
        label.ellipsize = android.text.TextUtils.TruncateAt.END
        addView(label, LayoutParams(0, -2, 1f))
        indicator?.let { addView(it, LayoutParams(-2, -2)) }
        label.text = item.label
        nextFocusLeftId = id // LEFT/RIGHT do nothing: focus must not escape to the tiles behind the panel
        nextFocusRightId = id
        setOnFocusChangeListener { _, focused ->
            background = if (focused) focusBackground else null
            label.setTextColor(if (focused) 0xFFFFFFFF.toInt() else Palette.text)
            indicator?.onAccent = focused
            if (focused) onFocused?.invoke(this)
        }
        refresh()
    }

    fun refresh() {
        indicator?.checked = item.checked()
    }
}

/**
 * Right-side settings sheet (400dp wide, inside the overscan-safe area) used instead of AlertDialogs, which are too tall for a
 * 540dp TV screen. Glass card, inset rounded groups with hairline separators, checkmarks and iOS-style switches. Selecting an
 * option applies it immediately and keeps the sheet open; BACK closes it.
 */
class SettingsPanel(context: Context) : FrameLayout(context) {
    private val dim = View(context).apply { setBackgroundColor(0x80000000.toInt()) }
    private val card = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val titleView = TextView(context).apply { style(600, 20f) }
    private val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; clipChildren = false }
    private val scroller = ColumnScroller(context, 12f).apply {
        clipToPadding = false
        isVerticalScrollBarEnabled = false
        val pad = context.dp(12)
        setPadding(pad, pad, pad, pad) // 12dp of breathing room above the first and below the last row
        addView(list)
    }
    private val rows = ArrayList<PanelRow>()
    private val headerOf = HashMap<PanelRow, View>() // first row of a section -> its section header

    var isOpen = false
        private set
    var onClosed: (() -> Unit)? = null

    init {
        visibility = GONE
        addView(dim, LayoutParams(-1, -1))
        card.addView(titleView, LinearLayout.LayoutParams(-1, -2).apply { setMargins(context.dp(26), context.dp(22), context.dp(24), context.dp(4)) })
        card.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        card.elevation = context.dp(24).toFloat() // soft shadow under the sheet
        addView(
            card,
            LayoutParams(context.dp(400), -1, Gravity.END).apply { setMargins(0, context.dp(27), context.dp(48), context.dp(27)) },
        )
    }

    fun open(title: String, items: List<PanelItem>) {
        applyTheme()
        titleView.text = title
        list.removeAllViews()
        rows.clear()
        headerOf.clear()
        var group: LinearLayout? = null
        var pendingHeader: View? = null
        fun newGroup(): LinearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = context.dp(12).toFloat()
                setColor(if (Palette.day) 0x0D000000 else 0x14FFFFFF) // inset group
                setStroke(1, Glass.hairline)
            }
            setPadding(context.dp(4), context.dp(4), context.dp(4), context.dp(4))
        }.also { list.addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = context.dp(4) }) }
        for (item in items) {
            when (item) {
                is PanelItem.Header -> {
                    val headerView = TextView(context).apply {
                            text = item.text
                            style(600, 12f, 0.06f)
                            isAllCaps = true
                            setTextColor(Palette.text60)
                            setPadding(context.dp(14), context.dp(14), 0, context.dp(6))
                    }
                    list.addView(headerView, LinearLayout.LayoutParams(-1, -2))
                    pendingHeader = headerView
                    group = newGroup()
                }
                is PanelItem.Option -> {
                    val g = group ?: newGroup().also { group = it }
                    if (g.childCount > 0) { // hairline separator between rows of a group
                        g.addView(View(context).apply { setBackgroundColor(Glass.hairline) }, LinearLayout.LayoutParams(-1, 1).apply { setMargins(context.dp(14), 0, context.dp(14), 0) })
                    }
                    val row = PanelRow(context, item)
                    pendingHeader?.let { headerOf[row] = it; pendingHeader = null }
                    row.onFocused = { revealRow(it) }
                    row.setOnClickListener { item.onSelect(); if (isOpen) rows.forEach { it.refresh() } }
                    rows.add(row)
                    g.addView(row, LinearLayout.LayoutParams(-1, context.dp(44)))
                }
            }
        }
        isOpen = true
        visibility = VISIBLE
        scroller.scrollTo(0, 0)
        val first = rows.firstOrNull { it.item.kind != OptionKind.ACTION && it.item.checked() } ?: rows.firstOrNull()
        first?.post { first.requestFocus() }
    }

    /** Scrolls so the focused row AND, for the first row of a section, the section header above it are fully visible. */
    private fun revealRow(row: PanelRow) {
        val header = headerOf[row]
        val topView: View = header ?: row
        fun yIn(v: View): Int { // v's top in the scroller's content coordinates
            var y = 0
            var cur: View = v
            while (cur !== scroller && cur.parent is View) { y += cur.top; cur = cur.parent as View }
            return y
        }
        val rowTop = yIn(row)
        val target = PanelScroll.target(scroller.scrollY, scroller.height, yIn(topView), rowTop + row.height, context.dp(8))
        if (target != scroller.scrollY) scroller.post { scroller.smoothScrollTo(0, target) }
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        visibility = GONE
        list.removeAllViews()
        rows.clear()
        headerOf.clear()
        onClosed?.invoke()
    }

    private fun applyTheme() {
        // Glass: dark #1C1C1E at ~55% at night, #F2F2F7 at ~60% by day, with a 1px hairline. The backdrop blur is not applied.
        card.background = Glass.drawable(context, context.dp(22).toFloat(), if (Palette.day) 0xF0F2F2F7.toInt() else 0xEB1C1C1E.toInt())
        titleView.setTextColor(Palette.text)
    }
}
