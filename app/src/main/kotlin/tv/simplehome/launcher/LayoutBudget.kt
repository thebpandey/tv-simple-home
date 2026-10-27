package tv.simplehome.launcher

/**
 * 540dp-tall screen layout budget, as a pure function so it can be checked in a unit test.
 * Safe area: 27dp top and bottom (486dp of content), 48dp left and right.
 *
 * Home, top to bottom: FIXED header (top bar + optional banner) / flexible CENTER holding the quote, centred both ways /
 * FIXED dock anchored to the bottom safe edge (menu button, divider, magnifying favorites).
 */
object LayoutBudget {
    const val SCREEN_DP = 540
    const val SAFE_TOP_BOTTOM_DP = 27
    const val SAFE_HEIGHT_DP = SCREEN_DP - 2 * SAFE_TOP_BOTTOM_DP // 486

    // Header: clock 40sp, date 18sp, New Delhi 16sp, weather widget; plus a festival banner of up to 3 lines.
    // Card-less header: the clock group (64sp clock, date, New Delhi) on the left and the weather group on the right sit directly on the
    // wallpaper, kept readable by a soft text shadow and the top scrim. The festival banner (16/13/12sp) sits under the clock group.
    const val TOP_BAR_DP = 88 // clock 64sp light, date 17sp, weather 44sp
    const val BANNER_MAX_DP = 60
    const val CLOCK_GROUP_RIGHT_DP = 450f // estimates of the text groups' extents at 960dp (Inter at these sizes), kept slightly generous
    const val BANNER_RIGHT_DP = 380f
    const val WEATHER_LEFT_DP = 540f

    // Quote block, worst case: 4 lines of 19.5sp Inter Light at 1.3 line height + the 15sp author + paddings (see QuoteFit.maxBlockDp); the centre never shrinks below it.
    const val QUOTE_MIN_DP = 164

    // ---- Dock. Every dock dimension lives in DockSpec (one per style) and is the design value times that style's scale. ----
    const val DOCK_SCALE = 0.88f // icon dock: 64dp x 0.88 = 56.3dp resting icons; the single knob for the whole icon dock
    const val LABEL_SP = 13f // dock tooltip text
    const val BANNER_DOCK_SCALE = 0.68f // banner dock keeps its v2.6 size

    val ICONS_SPEC = DockSpec(DockStyle.ICONS)
    val BANNERS_SPEC = DockSpec(DockStyle.BANNERS)
    fun spec(style: DockStyle): DockSpec = if (style == DockStyle.ICONS) ICONS_SPEC else BANNERS_SPEC

    /** Both styles share one dock height (the taller one, bottom-aligned) so the quote never moves when the style changes. */
    val DOCK_DP = maxOf(ICONS_SPEC.ownDockDp, BANNERS_SPEC.ownDockDp)

    fun header(withBanner: Boolean) = TOP_BAR_DP + if (withBanner) BANNER_MAX_DP else 0

    /** Height the centre gets once header and dock are fixed. */
    fun centerDp(withBanner: Boolean) = SAFE_HEIGHT_DP - header(withBanner) - DOCK_DP

    fun homeFits() = centerDp(withBanner = true) >= QUOTE_MIN_DP

    // Spacing shared with All apps and the settings panel.
    const val ROW_PADDING_DP = 12
    const val FOCUS_SCALE = 1.06f

    // ---- All apps (Launchpad): 6 columns of 140 x 150dp cells holding 72dp squircle icons, focus 1.15x; no title, no quote. ----
    const val ALL_COLS = 6
    const val ALL_CELL_W_DP = 140
    const val ALL_ROW_DP = 150
    const val ALL_ICON_DP = 72
    const val ALL_FOCUS_SCALE = 1.15f
    const val ALL_CELL_PAD_DP = 12 // >= 12% of the icon on every side (8.64dp); covers the focus growth and the shadow
    const val ALL_SHADOW_DP = 18 // elevation of the focused icon's shadow; it reaches about this far past the icon

    /** True when [count] apps fit the viewport without scrolling, so the grid is centred vertically (otherwise it scrolls from the top). */
    fun allGridFits(count: Int): Boolean = allGridRows(count) * ALL_ROW_DP <= SAFE_HEIGHT_DP - 2 * ROW_PADDING_DP

    fun allGridRows(count: Int): Int = (count + ALL_COLS - 1) / ALL_COLS

    fun allAppsRegionDp() = SAFE_HEIGHT_DP
    fun allGridWidthDp() = ALL_COLS * ALL_CELL_W_DP

    /** How far a focused item of the given size grows past its own bounds, per side, at [scale]. */
    fun focusOverflowDp(sizeDp: Float, scale: Float): Float = (scale - 1f) / 2f * sizeDp

    // The weather widget text column: condition+high/low 16sp (19) + place 14sp (17) + air quality and pollen 13sp (15 each), plus 10dp
    // padding top and bottom.
    const val WEATHER_WIDGET_DP = 19 + 17 + 15 + 15 + 20

    // Fixed screen positions (dp from the top) of the content the scrim has to protect.
    val SCRIM_HEADER_END_DP = SAFE_TOP_BOTTOM_DP + header(true) // 175: the top scrim holds its full strength through the whole header, banner lines included
    val HEADER_END_DP = SAFE_TOP_BOTTOM_DP + header(true) // 151
    val QUOTE_TOP_DP = HEADER_END_DP + (centerDp(true) - QUOTE_MIN_DP) / 2f // top of the quote box, centred in the free centre
    val QUOTE_BOTTOM_DP = QUOTE_TOP_DP + QUOTE_MIN_DP
}

enum class DockStyle(val id: String, val label: String) {
    ICONS("icons", "Icons"),
    BANNERS("banners", "Banners");

    companion object {
        /** Missing or unknown values fall back to ICONS, the default. */
        fun parse(raw: String?): DockStyle = entries.firstOrNull { it.id == raw } ?: ICONS
    }
}

/** Every dock dimension for one style, in dp. Pure data so the geometry tests can run on the JVM. */
class DockSpec(val style: DockStyle) {
    val icons = style == DockStyle.ICONS
    val scale = if (icons) LayoutBudget.DOCK_SCALE else LayoutBudget.BANNER_DOCK_SCALE
    private fun s(x: Float) = x * scale

    /** Height / width of a resting tile: square icons or 16:9 banners. */
    val aspect = if (icons) 1f else 16f / 9f
    val maxBaseDp = if (icons) s(64f) else s(132f)
    val minBaseDp = if (icons) s(40f) else s(96f)
    val gapDp = if (icons) s(14f) else s(12f)

    // Magnification ratios are not scaled.
    val magFocus = if (icons) 1.45f else 1.35f
    val magNear = if (icons) 1.18f else 1.12f
    val magFar = if (icons) 1.06f else 1.04f
    val liftDp = if (icons) s(14f) else s(12f)
    val neighbourLiftDp = s(4f)

    val shelfPadDp = if (icons) 10f else s(12f)
    val bottomGapDp = s(8f) // between the shelf and the safe edge
    val labelDp = if (icons) 24f else 28f // tooltip strip ABOVE each tile (inside the tile view); not scaled, it must stay readable
    val shelfCornerDp = if (icons) 26f else s(16f)
    val tileCornerDp = s(12f)
    val buttonDp = if (icons) maxBaseDp else s(48f)
    val menuInsetDp = if (icons) shelfPadDp else s(8f)
    val hPadDp = s(16f) // scroller side padding
    val rowHPadDp = if (icons) shelfPadDp else s(12f) // favorites row side padding
    val dividerXDp = if (icons) menuInsetDp + buttonDp + gapDp else s(68f)
    val dividerHDp = if (icons) buttonDp * 0.8f else s(40f)
    val pillXDp = if (icons) dividerXDp + 1f + gapDp else s(76f) // where the first pill starts, just right of the divider
    val favStartDp = if (icons) pillXDp - hPadDp - rowHPadDp else s(81f) // where the favorites scroller starts (menu area)
    val pillWDp = s(120f)
    val pillHDp = s(40f)
    val pillGapDp = s(12f)
    val pillSp = 15f * scale
    val extraRightDp = s(90f) // invisible end padding so the scroll range covers the pushed-out tiles

    /** Menu pills grow to this much when focused (they are a row, not a magnifying dock: one pill at a time). */
    val pillMag = 1.06f
    /** Pill row padding on every side: half the pill's growth + the lift + the 2dp ring + 4dp spare, so a focused pill is never clipped. */
    val pillRowPadDp = pillWDp * (pillMag - 1f) / 2f + neighbourLiftDp + 2f + 4f
    /** Shelf width while the menu is open: [back chevron | four pills] only. */
    val expandedShelfDp = pillXDp + 4 * pillWDp + 3 * pillGapDp + shelfPadDp
    /** How far the focused menu button grows (its slot widens by this and the neighbours are pushed). */
    val menuGrowthDp = buttonDp * (magFocus - 1f)
    /** Room the budget keeps because the menu button is fixed-size while shrunk favorites are not: the menu-focused growth beyond the favorites-focused one. */
    val menuExtraAllowanceDp: Float get() = maxOf(0f, menuGrowthDp + ((magNear - 1f) + (magFar - 1f)) * minBaseDp - magExtra * minBaseDp)

    fun cardHeightDp(baseDp: Float) = baseDp / aspect
    fun tileHeightDp(baseDp: Float) = labelDp + cardHeightDp(baseDp)

    /** Growth above the resting tile top of the focused tile scaled from its bottom edge, plus the lift. */
    val overflowUpDp = tileHeightDp(maxBaseDp) * (magFocus - 1f) + liftDp
    val overflowSideDp = maxBaseDp * (magFocus - 1f) / 2f
    val sidePadDp = kotlin.math.ceil(overflowSideDp + 4f)
    val topPadDp = kotlin.math.ceil(overflowUpDp + 4f)
    val ownDockDp = topPadDp + tileHeightDp(maxBaseDp) + shelfPadDp + bottomGapDp

    /** Extra row width when the focused tile and its neighbours grow and push the rest apart. */
    val magExtra = (magFocus - 1f) + 2 * (magNear - 1f) + 2 * (magFar - 1f)
    val extraGrowthDp = maxBaseDp * magExtra
}

/** Pure dock-magnification geometry: slide amounts that keep every neighbour exactly one gap away from the grown tile. */
object DockMath {
    /** Scale for a tile [distance] slots from the focused one (99 = nothing focused). */
    fun scaleAt(distance: Int, spec: DockSpec): Float = when (distance) { 0 -> spec.magFocus; 1 -> spec.magNear; 2 -> spec.magFar; else -> 1f }

    fun liftAt(distance: Int, spec: DockSpec): Float = when (distance) { 0 -> spec.liftDp; 1 -> spec.neighbourLiftDp; else -> 0f }

    /**
     * translationX per tile: each tile owns a slot of (baseWidth x its scale); scaled about its centre it sits centred in
     * its slot when shifted by (growth of every tile to its left) + (half its own growth).
     */
    fun shifts(scales: FloatArray, baseWidth: Float): FloatArray {
        val out = FloatArray(scales.size)
        var cumulative = 0f
        for (i in scales.indices) {
            val growth = baseWidth * (scales[i] - 1f)
            out[i] = cumulative + growth / 2f
            cumulative += growth
        }
        return out
    }

    /** Visual left edge of tile [i] (layout left + shift - half its growth). */
    fun visualLeft(layoutLeft: Float, shift: Float, baseWidth: Float, scale: Float): Float = layoutLeft + shift - baseWidth * (scale - 1f) / 2f
}

/** macOS-style auto-fit: pick the tile width so every favorite fits, magnification headroom included. */
object DockFit {
    /** [scrolls] is the overflow fallback: too many favorites for the minimum width, so the row scrolls (with a soft edge). */
    data class Fit(val baseDp: Float, val cardHeightDp: Float, val scrolls: Boolean)

    fun fit(spec: DockSpec, availableDp: Float, count: Int): Fit {
        val n = count.coerceAtLeast(1)
        val ideal = (availableDp - spec.menuExtraAllowanceDp - spec.gapDp * (n - 1)) / (n + spec.magExtra)
        val base = if (ideal >= spec.minBaseDp) ideal.coerceAtMost(spec.maxBaseDp) else spec.minBaseDp
        return Fit(base, spec.cardHeightDp(base), scrolls = ideal < spec.minBaseDp)
    }

    /** Width the tiles need at full magnification for [count] tiles of [baseDp]. */
    fun neededDp(spec: DockSpec, baseDp: Float, count: Int): Float =
        baseDp * (count + spec.magExtra) + spec.gapDp * (count - 1).coerceAtLeast(0) + spec.menuExtraAllowanceDp
}

/** Pure dock geometry in dp: where the unfocused cards sit relative to the shelf, and how the dock is centred. */
object DockGeometry {
    /** Top padding of the favorites scroller so the row sits at the BOTTOM of the dock (the shelf is bottom-aligned too). */
    fun rowTopPadDp(spec: DockSpec, cardHeightDp: Float): Float =
        LayoutBudget.DOCK_DP - spec.bottomGapDp - (spec.labelDp + cardHeightDp + spec.shelfPadDp)

    fun cardTopDp(spec: DockSpec, cardHeightDp: Float): Float = rowTopPadDp(spec, cardHeightDp) + spec.labelDp
    fun cardBottomDp(spec: DockSpec, cardHeightDp: Float): Float = cardTopDp(spec, cardHeightDp) + cardHeightDp

    /** The shelf is bottom-aligned with its own gap and is cardHeight + 2 x shelfPad tall. */
    fun shelfBottomDp(spec: DockSpec): Float = LayoutBudget.DOCK_DP - spec.bottomGapDp
    fun shelfTopDp(spec: DockSpec, cardHeightDp: Float): Float = shelfBottomDp(spec) - (cardHeightDp + 2 * spec.shelfPadDp)

    /** Width the favorites (tiles + growth) may use: screen minus the safe margins, the menu area and the paddings. */
    fun availableDp(spec: DockSpec, screenWidthDp: Float): Float =
        screenWidthDp - 96f - spec.favStartDp - 2 * spec.hPadDp - 2 * spec.rowHPadDp

    fun safeWidthDp(screenWidthDp: Float): Float = screenWidthDp - 96f

    /** Dock width at rest: menu area, paddings, the tiles and the gaps (matches the shelf width). */
    fun restWidthDp(spec: DockSpec, count: Int, baseDp: Float): Float =
        spec.favStartDp + spec.hPadDp + spec.rowHPadDp + count * baseDp +
            (count - 1).coerceAtLeast(0) * spec.gapDp + spec.rowHPadDp

    /** Extra width while tile [focusIndex] (or none, when negative) is magnified with its neighbours. */
    fun growthDp(spec: DockSpec, count: Int, baseDp: Float, focusIndex: Int): Float =
        if (focusIndex < 0) 0f else (0 until count).sumOf { (baseDp * (DockMath.scaleAt(kotlin.math.abs(it - focusIndex), spec) - 1f)).toDouble() }.toFloat()

    /** Left offset that centres [contentDp] in [dockDp]; never negative, so a too-wide dock stays anchored at the left edge. */
    fun offsetDp(dockDp: Float, contentDp: Float): Float = maxOf(0f, (dockDp - contentDp) / 2f)
}

/** Pure placement of the photo credit (bottom-right) so it never lies on the dock shelf. */
object CreditPlacement {
    private const val RIGHT_EDGE_DP = 912f
    private const val CLEARANCE_DP = 12f
    private const val ABOVE_SHELF_GAP_DP = 6f

    /**
     * Bottom margin (dp from the screen bottom). When the dock's right end (magnified) stays left of the credit, the credit sits in the
     * safe-area corner beside the dock, level with the shelf bottom; otherwise it sits just above the shelf's right end.
     */
    fun bottomMarginDp(spec: DockSpec, dockRightDp: Float, creditWidthDp: Float, shelfHeightDp: Float): Float {
        val corner = LayoutBudget.SAFE_TOP_BOTTOM_DP + spec.bottomGapDp
        val creditLeft = RIGHT_EDGE_DP - creditWidthDp
        return if (dockRightDp + CLEARANCE_DP <= creditLeft) corner else corner + shelfHeightDp + ABOVE_SHELF_GAP_DP
    }
}

/** Pure model of where a focused dock item ends up, in dp from the dock's left clip edge (the 48dp safe edge). */
object DockExtents {
    const val NONE = -2
    const val MENU = -1

    data class Extent(val leftDp: Float, val rightDp: Float)

    /** Growth of the whole dock content with [focus] (MENU, a favorite index or NONE). */
    fun growthDp(spec: DockSpec, count: Int, baseDp: Float, focus: Int): Float = when {
        focus == MENU -> spec.menuGrowthDp + (0 until count).sumOf { (baseDp * (DockMath.scaleAt(it + 1, spec) - 1f)).toDouble() }.toFloat()
        focus >= 0 -> DockGeometry.growthDp(spec, count, baseDp, focus)
        else -> 0f
    }

    /**
     * Visual bounds (scale, slot push and the dock's re-centring included) of the focused item. The magnified menu button keeps its left
     * edge at the slot's left (its growth goes into its slot and pushes the divider and favorites), so it can never leave the shelf.
     */
    fun focused(spec: DockSpec, count: Int, baseDp: Float, scrolls: Boolean, dockW: Float, focus: Int): Extent {
        val content = if (scrolls) dockW else DockGeometry.restWidthDp(spec, count, baseDp) + growthDp(spec, count, baseDp, focus)
        val offset = DockGeometry.offsetDp(dockW, content)
        if (focus == MENU) return Extent(offset + spec.menuInsetDp, offset + spec.menuInsetDp + spec.buttonDp * spec.magFocus)
        val scales = FloatArray(count) { DockMath.scaleAt(kotlin.math.abs(it - focus), spec) }
        val shifts = DockMath.shifts(scales, baseDp)
        val layoutLeft = spec.favStartDp + spec.hPadDp + spec.rowHPadDp + focus * (baseDp + spec.gapDp)
        val left = offset + DockMath.visualLeft(layoutLeft, shifts[focus], baseDp, scales[focus])
        return Extent(left, left + baseDp * scales[focus])
    }

    /** Bounds of expanded-menu pill [index] (focused, with its ring) in dp from the shelf's left edge. */
    fun pill(spec: DockSpec, index: Int): Extent {
        val grow = spec.pillWDp * (spec.pillMag - 1f) / 2f
        val left = spec.pillXDp + index * (spec.pillWDp + spec.pillGapDp) - grow - 2f
        return Extent(left, left + spec.pillWDp + 2 * grow + 4f)
    }
}
