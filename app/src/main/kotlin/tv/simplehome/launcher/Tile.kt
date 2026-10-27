package tv.simplehome.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

const val COLOR_BG = 0xFF0E1116.toInt()
const val COLOR_CARD = 0xFF1B2029.toInt()
const val COLOR_TEXT = 0xFFFFFFFF.toInt()
const val COLOR_MUTED = 0xFFA9B3C1.toInt()
const val COLOR_ACCENT = 0xFF0A84FF.toInt() // system blue (dark); light mode uses #007AFF

/** Foreground colours for the current day/night UI mode (main thread only). Day = light frosted scrim + dark text. */
object Palette {
    var day = false
    val text get() = if (day) ScrimSpec.DAY_TEXT else ScrimSpec.NIGHT_TEXT
    val muted get() = if (day) 0xFF12161E.toInt() else COLOR_MUTED
    val accent get() = if (day) 0xFF007AFF.toInt() else COLOR_ACCENT
    val text70 get() = (text and 0x00FFFFFF) or (0xB2 shl 24)
    val text60 get() = (text and 0x00FFFFFF) or (0x99 shl 24)
    val highlight get() = if (day) 0x14000000 else 0x24FFFFFF
    val shadow get() = if (day) 0xCCFFFFFF.toInt() else 0xCC000000.toInt()
    val headerShadow get() = if (day) 0x80FFFFFF.toInt() else 0x73000000
    val quickCard get() = if (day) 0xD9FFFFFF.toInt() else COLOR_CARD
    /** Deeper version of a festival accent so it stays readable on the light scrim. */
    fun festival(accent: Int): Int =
        if (!day) accent else ((((accent shr 16) and 255) * 0.5f).toInt() shl 16) or ((((accent shr 8) and 255) * 0.5f).toInt() shl 8) or ((accent and 255) * 0.5f).toInt() or (0xFF shl 24)
}

/** A TextView whose alpha is applied to its own draw ops instead of through an offscreen layer (cheaper and no extra buffer). */
class FlatTextView(context: Context) : TextView(context) {
    override fun hasOverlappingRendering(): Boolean = false
}

/** Keyed focusable item (home tile or All apps cell) so focus can be restored after a rebuild. */
open class FocusItem(context: Context) : LinearLayout(context) {
    var key: String = ""
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun Context.dpf(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

/** A focusable card with an optional label underneath. Focus = 1.08x scale + accent border, 120ms. */
/** BORDER = classic 1.06x with an accent border; DOCK = macOS-dock magnification driven by the row; PILL = gentle 1.12x lift. */
enum class TileStyle { BORDER, DOCK, PILL }

class Tile(
    context: Context,
    val widthDp: Float,
    cardHdp: Float,
    labelBelow: Boolean,
    labelSp: Float = 18f,
    labelTopDp: Int = 8,
    private val textSp: Float = 24f,
    private val quick: Boolean = false,
    private val style: TileStyle = TileStyle.BORDER,
    private val spec: DockSpec = LayoutBudget.BANNERS_SPEC,
    /** DOCK only: a square squircle icon with no card behind it (the "Icons" dock style). */
    private val squircle: Boolean = false,
) : FocusItem(context) {
    /** Called on focus changes of a DOCK tile so the row can magnify this tile and its neighbours. */
    var onDockFocus: ((Tile, Boolean) -> Unit)? = null
    private val card = FrameLayout(context)
    private val image = ImageView(context)
    private val label = FlatTextView(context)
    private val cornerDp = if (style == TileStyle.BORDER) CORNER_DP.toFloat() else spec.tileCornerDp
    private val borderDp = if (style == TileStyle.PILL) 2 else 4
    private val border = GradientDrawable().apply {
        cornerRadius = context.dpf(cornerDp).toFloat()
        setColor(Color.TRANSPARENT)
        setStroke(context.dp(borderDp), Palette.accent)
    }
    private val cardColor = if (style == TileStyle.PILL) Glass.fill else if (quick) Palette.quickCard else COLOR_CARD
    /** Focused pill fill: lighter (white 22% at night) or darker (black 14% by day) than the resting card. */
    private val pillFocusFill = if (Palette.day) 0xCCF2F2F7.toInt() else 0xB32C2C2E.toInt()
    private val cardBackground = GradientDrawable()

    // The card clips its content to its rounded outline and a stroke is centred on its shape's edge, so a plain border
    // lost its outer half (it looked "cut off"). Inset by half the stroke so all 4dp sit inside the card.
    private val borderInset = android.graphics.drawable.InsetDrawable(border, context.dp(borderDp / 2))

    init {
        orientation = VERTICAL
        id = generateViewId() // so edge tiles can point their nextFocusLeft/Right at themselves
        isFocusable = true
        isFocusableInTouchMode = true // initial focus must work even if the TV starts the app in touch mode
        isClickable = true
        clipChildren = false
        // No layoutParams here: the parent assigns its own type via addTile(), so no casts are ever needed.

        if (!squircle) card.background = cardBackground.apply {
            // Pills are capsules (corner = half the height); everything else uses the tile corner.
            cornerRadius = if (style == TileStyle.PILL) context.dpf(cardHdp / 2f).toFloat() else context.dpf(cornerDp).toFloat()
            setColor(cardColor)
            if (style == TileStyle.PILL) setStroke(1, Glass.hairline)
        }
        card.clipToOutline = !squircle
        if (squircle) { // the bitmap carries its own squircle alpha; the outline only exists to cast the focused icon's shadow
            card.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, view.width * 0.24f)
                }
            }
        }
        card.addView(image, FrameLayout.LayoutParams(-1, -1))
        addView(card, LayoutParams(-1, context.dpf(cardHdp)))

        if (style == TileStyle.DOCK) { // macOS-style tooltip: a small glass pill ABOVE the focused tile only
            label.setTextColor(Palette.text)
            label.style(500, labelSp)
            label.setSingleLine()
            label.ellipsize = android.text.TextUtils.TruncateAt.END
            label.gravity = Gravity.CENTER
            label.setPadding(context.dp(10), context.dp(2), context.dp(10), context.dp(2))
            label.background = Glass.drawable(context, context.dp(12).toFloat())
            label.alpha = 0f
            label.visibility = View.INVISIBLE
            addView(label, 0, LayoutParams(-2, context.dpf(spec.labelDp)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        } else if (labelBelow) {
            label.setTextColor(Palette.text)
            label.setShadowLayer(context.dp(2).toFloat(), 0f, context.dp(1).toFloat(), Palette.shadow)
            label.textSize = labelSp
            label.setSingleLine()
            label.ellipsize = android.text.TextUtils.TruncateAt.END
            label.gravity = Gravity.CENTER_HORIZONTAL
            label.setPadding(0, context.dp(labelTopDp), 0, 0)
            addView(label, LayoutParams(-1, -2))
        }

        setOnFocusChangeListener { _, focused ->
            when (style) {
                TileStyle.DOCK -> {
                    showLabel(focused)
                    onDockFocus?.invoke(this, focused)
                }
                TileStyle.PILL -> {
                    // Clear focus: lighter/darker fill plus a 2dp accent border; the row pushes neighbours apart.
                    cardBackground.setColor(if (focused) pillFocusFill else cardColor)
                    card.foreground = if (focused) borderInset else null
                    onDockFocus?.invoke(this, focused)
                }
                TileStyle.BORDER -> {
                    val s = if (focused) FOCUS_SCALE else 1f
                    card.foreground = if (focused) borderInset else null
                    animate().scaleX(s).scaleY(s).translationZ(if (focused) 8f else 0f).setDuration(FOCUS_MS).start()
                }
            }
        }
    }

    /**
     * Dock label above the focused tile. Cancels any running fade first and always ends fully visible or fully gone
     * (INVISIBLE), so a half-faded "ghost" of a previous label can never stay on screen.
     */
    fun showLabel(show: Boolean) {
        if (style != TileStyle.DOCK) return
        label.animate().cancel()
        if (show) {
            label.visibility = View.VISIBLE
            label.animate().alpha(1f).setDuration(150).withEndAction { label.alpha = 1f }.start()
        } else if (label.visibility == View.VISIBLE) {
            label.animate().alpha(0f).setDuration(150).withEndAction { label.alpha = 0f; label.visibility = View.INVISIBLE }.start()
        } else {
            label.alpha = 0f
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Dock tiles grow upward from their bottom edge, like icons in the macOS dock.
        if (style == TileStyle.DOCK) { pivotX = w / 2f; pivotY = h.toFloat() }
    }

    /**
     * Smoothly scale to [scale], lift by [liftDp] and slide by [translateXPx] (decelerating), as used by the dock effect.
     * The slide pushes neighbours apart: every property animates with the same curve and duration, so tile edges never cross.
     */
    fun animateTo(scale: Float, liftDp: Float, translateXPx: Float, durationMs: Long, focused: Boolean = false) {
        animate().scaleX(scale).scaleY(scale).translationY(-context.dpf(liftDp).toFloat()).translationX(translateXPx)
            .translationZ(if (focused) context.dp(2).toFloat() else 0f) // the magnified tile draws above its neighbours
            .setStartDelay(0).setDuration(durationMs).setInterpolator(Motion.EASE).start()
        // The soft elevation shadow comes from the card's outline; only the focused (largest) tile gets it.
        card.animate().translationZ(if (focused) context.dpf(10f * spec.scale).toFloat() else 0f).setDuration(durationMs).setInterpolator(Motion.EASE).start()
        // The tooltip keeps its reading size while the tile grows around it.
        if (style == TileStyle.DOCK) label.animate().scaleX(1f / scale).scaleY(1f / scale).setDuration(durationMs).setInterpolator(Motion.EASE).start()
    }

    fun bind(key: String, text: String): Tile {
        this.key = key
        label.text = text
        if (style == TileStyle.DOCK) sizeTooltip()
        return this
    }

    /**
     * The tooltip is wider than a dock tile, so it must not inherit the tile's width (it came out as "Net..."). It gets its measured
     * text width at the displayed size plus 10dp each side, up to 240dp.
     */
    private fun sizeTooltip() {
        val textW = label.paint.measureText(label.text.toString())
        val w = kotlin.math.ceil(textW + 2 * context.dp(10)).toInt().coerceAtMost(context.dp(240))
        label.layoutParams = LayoutParams(w, context.dpf(spec.labelDp)).apply { gravity = Gravity.CENTER_HORIZONTAL }
    }

    /** True 16:9 banners fill the card; anything else is letterboxed so its left/right edges are never cropped. */
    fun showBanner(d: Drawable) {
        val w = d.intrinsicWidth.toFloat()
        val h = d.intrinsicHeight.toFloat()
        val nearly16by9 = w > 0f && h > 0f && kotlin.math.abs(w / h - 16f / 9f) < 0.15f
        image.scaleType = if (nearly16by9) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
        image.setImageDrawable(d)
    }

    /** Pre-rendered squircle icon (see [IconRenderer]); fades in when it arrives after the tile was built. */
    fun showSquircle(bitmap: Bitmap, fade: Boolean) {
        image.scaleType = ImageView.ScaleType.FIT_CENTER
        image.setImageBitmap(bitmap)
        if (fade) { image.alpha = 0f; image.animate().alpha(1f).setDuration(150).start() } else image.alpha = 1f
    }

    fun showIcon(d: Drawable?, padDp: Int) {
        image.setPadding(context.dp(padDp), context.dp(padDp), context.dp(padDp), context.dp(padDp))
        image.scaleType = ImageView.ScaleType.FIT_CENTER
        image.setImageDrawable(d)
    }

    /** Text-only card, used for the Quick row. */
    fun showText(text: String) {
        val t = TextView(context)
        t.text = text
        t.setTextColor(if (quick) Palette.text else COLOR_TEXT)
        t.style(500, textSp)
        t.gravity = Gravity.CENTER
        card.addView(t, FrameLayout.LayoutParams(-1, -1))
    }

    private companion object {
        const val CORNER_DP = 12
        const val FOCUS_MS = 120L
        const val FOCUS_SCALE = 1.06f
    }
}

/**
 * Horizontal row container. When a child gains focus it scrolls so the whole tile PLUS its focus scale and border
 * is inside the visible area (margin 12dp > the 5.3dp a 176dp tile grows at 1.06x), so nothing is ever clipped.
 */
class RowScroller(context: Context, marginDp: Float = 12f) : HorizontalScrollView(context) {
    private val margin = context.dpf(marginDp)

    /**
     * Soft right edge for rows that overflow: the children are drawn into a layer and the last [softFadeDp] are erased with a
     * smooth DST_OUT gradient, so nothing is painted on top of the tiles (the framework fading edge rendered as a hard band).
     */
    var softFadeDp = 0
    private val maskPaint = Paint().apply { xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT) }
    private var maskFadePx = -1f

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        if (softFadeDp <= 0 || !canScrollHorizontally(1)) { super.dispatchDraw(canvas); return }
        val fade = context.dpf(softFadeDp.toFloat()).toFloat()
        if (maskFadePx != fade) {
            maskPaint.shader = android.graphics.LinearGradient(0f, 0f, fade, 0f, 0x00000000, 0xFF000000.toInt(), android.graphics.Shader.TileMode.CLAMP)
            maskFadePx = fade
        }
        val left = scrollX.toFloat()
        val layer = canvas.saveLayer(left, 0f, left + width, height.toFloat(), null)
        super.dispatchDraw(canvas)
        canvas.save()
        canvas.translate(left + width - fade, 0f)
        canvas.drawRect(0f, 0f, fade, height.toFloat(), maskPaint)
        canvas.restore()
        canvas.restoreToCount(layer)
    }

    /** When true the owner scrolls explicitly (the dock needs to account for magnified, shifted tiles). */
    var manualScroll = false

    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect): Int {
        if (manualScroll) return 0
        rect.left -= margin
        rect.right += margin
        return super.computeScrollDeltaToGetChildRectOnScreen(rect)
    }
}

/** Vertical scroller; keeps the focused row inside the visible area with [marginDp] to spare (scale and highlight included). */
class ColumnScroller(context: Context, marginDp: Float) : android.widget.ScrollView(context) {
    private val margin = context.dpf(marginDp)

    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect): Int {
        rect.top -= margin
        rect.bottom += margin
        return super.computeScrollDeltaToGetChildRectOnScreen(rect)
    }
}

/** A 2dp system-blue (90%) squircle outline drawn over a focused Launchpad icon. */
private class SquircleRing(private val context: Context) : android.graphics.drawable.Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = context.dp(2).toFloat() }
    private var path = android.graphics.Path()
    var visible = false
        set(value) { field = value; invalidateSelf() }

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        val inset = paint.strokeWidth / 2f
        path = Squircle.path(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset)
    }

    override fun draw(canvas: android.graphics.Canvas) {
        if (!visible) return
        paint.color = (Palette.accent and 0x00FFFFFF) or (0xE6 shl 24)
        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
    @Deprecated("Deprecated in Java") override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}

/**
 * Launchpad cell: a squircle icon straight on the dimmed wallpaper with a one-line label under it. Focus = icon 1.15x with a
 * soft shadow beneath, a 2dp accent ring and the label at 100% / weight 500 (others 70%); there is no highlight box. The cell pads every side by more than
 * the focus growth plus the shadow, and nothing in it clips.
 */
class AppCell(context: Context, cellWidthPx: Int, cellHeightPx: Int) : FocusItem(context) {
    private val icon = ImageView(context)
    private val label = FlatTextView(context)
    private val ring = SquircleRing(context)

    init {
        id = generateViewId()
        orientation = VERTICAL
        gravity = Gravity.CENTER
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        clipChildren = false
        clipToPadding = false
        val pad = context.dp(LayoutBudget.ALL_CELL_PAD_DP)
        setPadding(pad, pad, pad, pad)
        icon.scaleType = ImageView.ScaleType.FIT_CENTER
        icon.foreground = ring
        ring.visible = false
        icon.outlineProvider = object : android.view.ViewOutlineProvider() { // casts the focus shadow in the squircle's footprint
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.width * 0.24f)
            }
        }
        addView(icon, LayoutParams(context.dp(LayoutBudget.ALL_ICON_DP), context.dp(LayoutBudget.ALL_ICON_DP)))
        label.style(400, 13f)
        label.setSingleLine()
        label.ellipsize = android.text.TextUtils.TruncateAt.END
        label.gravity = Gravity.CENTER_HORIZONTAL
        label.setTextColor(Palette.text)
        label.setShadowLayer(context.dp(2).toFloat(), 0f, context.dp(1).toFloat(), Palette.shadow)
        label.alpha = UNFOCUSED_LABEL_ALPHA
        addView(label, LayoutParams(-1, -2).apply { topMargin = context.dp(10) })
        minimumWidth = cellWidthPx
        minimumHeight = cellHeightPx
        setOnFocusChangeListener { _, focused ->
            label.alpha = if (focused) 1f else UNFOCUSED_LABEL_ALPHA
            label.typeface = Fonts.typeface(context, if (focused) 500 else 400)
            ring.visible = focused
            val s = if (focused) LayoutBudget.ALL_FOCUS_SCALE else 1f
            icon.animate().scaleX(s).scaleY(s).translationZ(if (focused) context.dp(LayoutBudget.ALL_SHADOW_DP).toFloat() else 0f).setDuration(220).setInterpolator(Motion.EASE).start()
            animate().translationZ(if (focused) context.dp(1).toFloat() else 0f).setDuration(0).start() // draw above neighbours' shadows
        }
    }

    fun bind(key: String, text: String): AppCell {
        this.key = key
        label.text = text
        return this
    }

    fun showSquircle(bitmap: Bitmap, fade: Boolean) {
        icon.setImageBitmap(bitmap)
        if (fade) { icon.alpha = 0f; icon.animate().alpha(1f).setDuration(150).start() } else icon.alpha = 1f
    }

    private companion object {
        const val UNFOCUSED_LABEL_ALPHA = 0.7f
    }
}

/** Dock menu button: a squircle (icons dock) or a circle (banners dock) with a thin-stroke 2x2 grid glyph, a chevron when expanded. */
class MenuButton(context: Context) : FocusItem(context) {
    var spec: DockSpec = LayoutBudget.ICONS_SPEC
        set(value) { field = value; requestLayout(); invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chevron = android.graphics.Path()
    private val cell = android.graphics.RectF()
    private var shape: android.graphics.Path? = null
    private var ring: android.graphics.Path? = null
    var expanded = false
        set(value) { field = value; invalidate() }

    init {
        setWillNotDraw(false) // a ViewGroup (LinearLayout) skips onDraw by default, which left the button invisible
        id = generateViewId()
        key = "menu"
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        setOnFocusChangeListener { _, focused ->
            onTooltip?.invoke(focused)
            onDockFocus?.invoke(focused) // the activity magnifies it as part of the dock (slot, push and re-centring included)
        }
    }

    /** Called on focus changes so the dock can lay out the magnified button like any other dock item. */
    var onDockFocus: ((Boolean) -> Unit)? = null

    /** Same curve and duration as the other dock items; pivot is the bottom centre. */
    fun animateTo(scale: Float, liftDp: Float, translateXPx: Float, durationMs: Long) {
        animate().scaleX(scale).scaleY(scale).translationY(-context.dpf(liftDp).toFloat()).translationX(translateXPx)
            .setDuration(durationMs).setInterpolator(Motion.EASE).start()
    }

    /** Shows or hides the "Menu" tooltip the activity keeps above the button. */
    var onTooltip: ((Boolean) -> Unit)? = null

    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(context.dpf(spec.buttonDp), context.dpf(spec.buttonDp))

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        pivotX = w / 2f; pivotY = h.toFloat()
        shape = Squircle.path(0f, 0f, w.toFloat(), h.toFloat())
        val inset = context.dp(1).toFloat()
        ring = Squircle.path(inset, inset, w - inset, h - inset)
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.style = Paint.Style.FILL
        paint.color = if (Palette.day) 0x1A000000 else 0x24FFFFFF // dark ~10% by day, white ~14% at night
        if (spec.icons) shape?.let { canvas.drawPath(it, paint) } else canvas.drawCircle(w / 2f, h / 2f, w / 2f, paint)
        if (isFocused) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = context.dp(2).toFloat()
            paint.color = Palette.accent
            if (spec.icons) ring?.let { canvas.drawPath(it, paint) } else canvas.drawCircle(w / 2f, h / 2f, w / 2f - context.dp(1), paint)
        }
        paint.color = Palette.text
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = context.dpf(1.5f).toFloat().coerceAtLeast(1.5f)
        if (expanded) {
            chevron.reset()
            chevron.moveTo(w * 0.58f, h * 0.30f); chevron.lineTo(w * 0.40f, h * 0.50f); chevron.lineTo(w * 0.58f, h * 0.70f)
            canvas.drawPath(chevron, paint)
        } else {
            // "square.grid.2x2": four outlined rounded squares.
            val side = w * 0.20f
            val gap = w * 0.07f
            val x0 = (w - 2 * side - gap) / 2f
            val y0 = (h - 2 * side - gap) / 2f
            val r = side * 0.28f
            for (ix in 0..1) for (iy in 0..1) {
                cell.set(x0 + ix * (side + gap), y0 + iy * (side + gap), x0 + ix * (side + gap) + side, y0 + iy * (side + gap) + side)
                canvas.drawRoundRect(cell, r, r, paint)
            }
        }
    }
}
