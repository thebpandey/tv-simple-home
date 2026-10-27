package tv.simplehome.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ActivityNotFoundException
import android.content.pm.ResolveInfo
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Package of the TV inputs app; change here if the device's input picker differs. */
const val INPUTS_PACKAGE = "com.vt.tv.tvcenter"

private const val PREFS = "home"
private const val PREF_FAVORITES = "favorites"
private const val PREF_WALLPAPER = "wallpaper"
private const val PREF_FESTIVAL_WP = "festival_wallpapers"
private const val PREF_AQ_BODY = "aq_body"
private const val PREF_AQ_TIME = "aq_time"
private const val PREF_WX_BODY = "wx_body"
private const val PREF_WX_TIME = "wx_time"
private const val PREF_WX_NAME = "wx_name"
private const val PREF_WX_LAT = "wx_lat"
private const val PREF_WX_LON = "wx_lon"
private const val PREF_WX_INPUT = "wx_input"
/*
 * Home layout (numbers checked by LayoutBudget; 960x540dp, overscan-safe area 48dp sides / 27dp top and bottom = 486dp):
 *   FIXED header   64 (+60 banner) | flexible CENTER: the quote, centred both ways (>= 104dp) | FIXED dock 163 at the bottom edge.
 *   Dock: [menu button] | [shelf with 132x74 favorites, macOS-dock magnification 1.35x, label above the focused tile].
 *   Worst case 124 + 104 + 163 = 391 of 486dp, so the centre always has >= 104dp and nothing scrolls vertically.
 */
private const val SAFE_SIDE_DP = 48
private const val SAFE_VERT_DP = 27
private const val PREF_IDLE = "idle_hide"
private const val PREF_DOCK_STYLE = "dock_style"
private const val PREF_US_HOLIDAYS = "us_holidays"
private const val PREF_PHOTO_SOURCE = "photo_source" // legacy single Source radio, migrated into PREF_SOURCES
private const val PREF_SOURCES = "wallpaper_sources"
private const val DOCK_MS = 220L
/** Largest icon any screen draws, shared by the dock and the Launchpad (one cache, one size): dock 56.3dp x 1.45 = 81.6dp, Launchpad 72dp x 1.15 = 82.8dp. */
private val ICON_MAX_DP = maxOf(LayoutBudget.ICONS_SPEC.maxBaseDp * LayoutBudget.ICONS_SPEC.magFocus, LayoutBudget.ALL_ICON_DP * LayoutBudget.ALL_FOCUS_SCALE)
private const val PREF_DAYNIGHT = "day_night_mode"

private class AppEntry(val pkg: String, val label: String, val info: ResolveInfo)

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private var apps: List<AppEntry> = emptyList()
    private var favorites: List<String> = emptyList()
    private var showingAll = false

    private lateinit var favRow: LinearLayout
    private lateinit var favScroller: RowScroller
    private lateinit var pillRow: LinearLayout
    private lateinit var menuButton: MenuButton
    private lateinit var dock: FrameLayout
    private lateinit var shelf: View
    private lateinit var dockContent: FrameLayout
    private lateinit var dockDivider: View
    private lateinit var menuTooltip: TextView
    private var dockStyle = DockStyle.ICONS
    private var spec = LayoutBudget.ICONS_SPEC
    private val pillRowPadDp get() = spec.pillRowPadDp
    private lateinit var iconCache: IconCache
    private var dockBaseDp = LayoutBudget.ICONS_SPEC.maxBaseDp
    private var dockScrolls = false
    private var shelfRestPx = 0f
    private var shelfGrowthPx = 0f // magnification growth, animated with the tiles
    private lateinit var delhiRow: View
    private val pills = ArrayList<Tile>()
    private var menuExpanded = false
    private var lastDockFocus: View? = null
    private val idleTracker = IdleTracker()
    private var lastLaunchedPackage: String? = null
    private var pendingHomeRestore = false
    private lateinit var creditView: TextView
    private val creditHide = Runnable { if (idleTracker.idle) creditView.animate().alpha(0f).setDuration(600).start() }
    private val swallowedKeys = HashSet<Int>()
    private val idleCheck = Runnable { checkIdle() }
    private lateinit var allScreen: LinearLayout
    private lateinit var allList: LinearLayout
    private val allCells = ArrayList<AppCell>()
    private lateinit var allScroller: ColumnScroller
    private lateinit var panel: SettingsPanel
    private var panelReturnKey: String? = null
    private var panelReturnView: View? = null
    private lateinit var wallpaper: WallpaperView
    private lateinit var homeColumn: LinearLayout
    private lateinit var festivalBanner: LinearLayout
    private lateinit var festivalName: TextView
    private lateinit var festivalDate: TextView
    private lateinit var festivalIndia: TextView
    private val quoteTexts = ArrayList<TextView>() // home footer and All apps footer show the same quote
    private val quoteAuthors = ArrayList<TextView>()
    private var shownQuote: Quote? = null
    private val themed = ArrayList<() -> Unit>() // re-colours static views when the day/night UI mode flips
    private var dayNightMode = DayNightMode.AUTO
    private var uiDay = false
    private var imageLuma: Int? = null // brightness of the photo or painting being shown, null for a procedural scene
    private val dayNightTick = Runnable { updateDayNight() }
    private val quoteTick = Runnable { updateQuote() }
    private val clockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateDayNight()
            updateQuote()
        }
    }
    private lateinit var weatherWidget: WeatherWidget
    private var weatherExecutor: ExecutorService? = null
    private var weatherGen = 0 // bumped when the place changes so in-flight fetches for the old place are discarded
    private val weatherTick = object : Runnable {
        override fun run() {
            fetchWeather(force = true) // the 15-minute tick refreshes both
            handler.postDelayed(this, Weather.REFRESH_MS)
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Installs fire several broadcasts in a burst; coalesce them.
            handler.removeCallbacks(refreshTask)
            handler.postDelayed(refreshTask, 400)
        }
    }
    private val refreshTask = Runnable { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        migrateSettings()
        dockStyle = DockStyle.parse(prefs.getString(PREF_DOCK_STYLE, null))
        spec = LayoutBudget.spec(dockStyle)
        iconCache = IconCache(handler, kotlin.math.ceil(ICON_MAX_DP * resources.displayMetrics.density).toInt())
        dayNightMode = DayNightMode.parse(prefs.getString(PREF_DAYNIGHT, null))
        uiDay = resolveDay()
        Palette.day = uiDay
        buildUi()
        wallpaper.setDay(dayNightMode.resolve(uiDay)) // the sun-based variant for scene and photo selection
        wallpaper.setUiDay(uiDay)
        wallpaper.setMode(WallpaperSchedule.parse(prefs.getString(PREF_WALLPAPER, null)))
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(packageReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(packageReceiver, filter)
        }
        val clockFilter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(clockReceiver, clockFilter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(clockReceiver, clockFilter)
        }
        updateDayNight()
        updateQuote()
        startWallpaper()
        startWeather()
        wallpaper.setSources(currentSources())
        loadPhotos()
        idleTracker.enabled = prefs.getBoolean(PREF_IDLE, true)
        idleTracker.reset(android.os.SystemClock.uptimeMillis())
        scheduleIdle(IdleTracker.DEFAULT_TIMEOUT_MS)
    }

    /** Brings settings from older versions forward (never dropping any) once per version. */
    private fun migrateSettings() {
        val result = SettingsMigration.migrate(prefs.all)
        if (result.puts.isEmpty() && result.newVersion == (prefs.all[SettingsMigration.KEY_VERSION] as? Int)) return
        val edit = prefs.edit()
        for ((k, v) in result.puts) edit.putString(k, v)
        edit.putInt(SettingsMigration.KEY_VERSION, result.newVersion).commit()
    }

    private fun startWallpaper() {
        val scale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        wallpaper.start(scale)
    }

    override fun onStop() {
        handler.removeCallbacks(refreshTask)
        wallpaper.stop()
        iconCache.clear()
        stopWeather()
        handler.removeCallbacks(quoteTick)
        handler.removeCallbacks(dayNightTick)
        handler.removeCallbacks(idleCheck)
        idleTracker.reset(0L)
        wakeUi()
        unregisterReceiver(clockReceiver)
        unregisterReceiver(packageReceiver)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        startWallpaper() // no-op unless a launch stopped it without this activity being stopped
        refresh()
        if (pendingHomeRestore) {
            pendingHomeRestore = false
            if (showingAll) restoreFocus("app:${lastLaunchedPackage}") else restoreHomeFocus()
        }
    }

    // HOME re-press while already open arrives here (singleTask): go back to the main screen.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            // HOME pressed while the launcher's task is alive: reset to a clean Home with focus on the right tile.
            if (panel.isOpen) panel.close()
            setMenuExpanded(false, animate = false)
            if (showingAll) showHome()
            idleTracker.reset(android.os.SystemClock.uptimeMillis())
            wakeUi()
            pendingHomeRestore = true
        } else if (showingAll) showHome()
    }

    /** After layout, focus the favorite that launched the last app, else the first favorite, else the menu. Never the weather widget. */
    private fun restoreHomeFocus() {
        val favPackages = (0 until favRow.childCount).mapNotNull { (favRow.getChildAt(it) as? FocusItem)?.key?.removePrefix("fav:") }
        val view = when (val t = FocusRestore.target(lastLaunchedPackage, favPackages)) {
            is RestoreTarget.Favorite -> favRow.getChildAt(t.index)
            RestoreTarget.Menu -> menuButton
        }
        homeColumn.post { view.requestFocus() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (panel.isOpen) { panel.close(); return }
        if (showingAll) { showHome(); return }
        if (menuExpanded) { setMenuExpanded(false); menuButton.requestFocus(); return }
        // On home, BACK deliberately does nothing so the launcher never exits.
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (panel.isOpen) return super.onKeyDown(keyCode, event) // the panel's rows handle navigation
        if (keyCode == KeyEvent.KEYCODE_MENU && event.repeatCount == 0) {
            return (currentFocus as? FocusItem)?.performLongClick() ?: false
        }
        if (showingAll) {
            val key = when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> GridKey.LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> GridKey.RIGHT
                KeyEvent.KEYCODE_DPAD_UP -> GridKey.UP
                KeyEvent.KEYCODE_DPAD_DOWN -> GridKey.DOWN
                else -> null
            }
            if (key != null && moveInGrid(currentFocus, key)) return true
        } else if (homeKey(keyCode)) return true
        return super.onKeyDown(keyCode, event)
    }

    /** All apps: the grid moves focus by index (see [GridNav]), so the ends of rows and columns are hard stops. */
    private fun moveInGrid(from: View?, key: GridKey): Boolean {
        val cell = from as? AppCell ?: return false
        val index = allCells.indexOf(cell)
        if (index < 0) return false
        val target = GridNav.next(index, key, LayoutBudget.ALL_COLS, allCells.size)
        if (target != index) allCells[target].requestFocus()
        return true
    }

    // ---------- UI construction ----------

    private fun buildUi() {
        val sideDp = dp(SAFE_SIDE_DP)
        favRow = LinearLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }
        favScroller = RowScroller(this, spec.sidePadDp).apply {
            manualScroll = true // the dock scrolls itself so the magnified, pushed-apart tile is what stays visible
            clipToPadding = false
            // clipChildren=true would clip the row to its own bounds and cut the lift, rounded top and label off; the dock
            // (below) clips this scroller to its bounds instead, which is the safe edge on the sides and has 49dp of headroom on top.
            clipChildren = false
            isHorizontalScrollBarEnabled = false
            isHorizontalFadingEdgeEnabled = false // the framework edge rendered as a hard band; RowScroller.softFadeDp is used instead
            // Room for the lift, the magnification and the label above the tiles.
            addView(favRow)
        }
        menuButton = MenuButton(this).apply { spec = this@MainActivity.spec; setOnClickListener { setMenuExpanded(!menuExpanded) } }
        pillRow = LinearLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            // Headroom for the focused pill's 4dp lift, 1.12x growth and 2dp border (the dock clips this row to its bounds).
            visibility = View.GONE
        }
        shelf = View(this)
        dockDivider = View(this)
        menuTooltip = FlatTextView(this).apply {
            text = "Menu"
            style(500, LayoutBudget.LABEL_SP)
            setSingleLine()
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(2), dp(10), dp(2))
            alpha = 0f
            visibility = View.INVISIBLE
            themed.add { setTextColor(Palette.text); background = Glass.drawable(this@MainActivity, dp(12).toFloat()) }
        }
        menuButton.onTooltip = { show -> showMenuTooltip(show) }
        menuButton.onDockFocus = { focused -> onMenuFocus(focused) }
        themed.add {
            shelf.background = Glass.drawable(this, dpf(spec.shelfCornerDp).toFloat())
            dockDivider.setBackgroundColor((Palette.text and 0x00FFFFFF) or (0x4D shl 24)) // 1px hairline at 30% opacity
        }
        // The dock's content (shelf, button, divider, pills, favorites) is one group that is centred as a whole, like the macOS Dock.
        dockContent = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            // The shelf hugs the actual tile extent (menu button to last tile + 12dp) and grows with the magnification.
            addView(shelf, FrameLayout.LayoutParams(1, 1, Gravity.BOTTOM or Gravity.START))
            addView(favScroller, FrameLayout.LayoutParams(-1, -1))
            addView(pillRow, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.START))
            addView(dockDivider, FrameLayout.LayoutParams(1, 1, Gravity.BOTTOM or Gravity.START)) // 1px wide; sized in fitDock
            addView(menuButton, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.START))
            addView(menuTooltip, FrameLayout.LayoutParams(-2, dp(24), Gravity.BOTTOM or Gravity.START))
        }
        dock = FrameLayout(this).apply {
            clipChildren = true // nothing, not even favorites slid aside by the menu, may draw past the safe edge
            clipToPadding = false
            addView(dockContent, FrameLayout.LayoutParams(-1, -1))
        }
        val center = FrameLayout(this).apply {
            clipChildren = false
            minimumHeight = dp(LayoutBudget.QUOTE_MIN_DP)
            addView(buildQuoteBlock(), FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        }
        homeColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(sideDp, dp(SAFE_VERT_DP), sideDp, dp(SAFE_VERT_DP))
            clipChildren = false
            clipToPadding = false
            addView(buildTopBar(), LinearLayout.LayoutParams(-1, dp(LayoutBudget.TOP_BAR_DP)))
            addView(buildFestivalBanner(), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })
            addView(center, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(dock, LinearLayout.LayoutParams(-1, dpf(LayoutBudget.DOCK_DP)))
        }
        window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus -> onFocusMoved(newFocus) }

        // All apps = fixed title / scrolling grid / fixed quote footer.
        allList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        allScroller = ColumnScroller(this, LayoutBudget.ALL_ROW_DP / 2f).apply { // half a row of margin keeps the focused row fully visible
            clipToPadding = false
            clipChildren = false
            isVerticalScrollBarEnabled = false
            setPadding(0, dp(LayoutBudget.ROW_PADDING_DP), 0, dp(LayoutBudget.ROW_PADDING_DP))
            addView(allList, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL))
        }
        allScreen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(sideDp, dp(SAFE_VERT_DP), sideDp, dp(SAFE_VERT_DP))
            clipChildren = false
            clipToPadding = false
            visibility = View.GONE
            addView(allScroller, LinearLayout.LayoutParams(-1, 0, 1f))
        }

        wallpaper = WallpaperView(this)
        creditView = TextView(this).apply {
            textSize = 11f
            alpha = 0.6f
            visibility = View.GONE
            themed.add { setTextColor(Palette.text); setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), Palette.shadow) }
        }
        wallpaper.onWallpaperChanged = { credit -> updateCredit(credit) }
        wallpaper.onLumaChanged = { luma -> onImageLuma(luma) }
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(COLOR_BG)
            addView(wallpaper, FrameLayout.LayoutParams(-1, -1))
            addView(homeColumn, FrameLayout.LayoutParams(-1, -1))
            addView(allScreen, FrameLayout.LayoutParams(-1, -1))
            // Unsplash attribution: bottom-right inside the safe area.
            addView(creditView, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END).apply {
                rightMargin = dp(SAFE_SIDE_DP); bottomMargin = dp(SAFE_VERT_DP)
            })
            panel = SettingsPanel(context).apply { onClosed = { panelClosed() } }
            addView(panel, FrameLayout.LayoutParams(-1, -1))
        })
        applyTheme()
    }

    /** A compact glass card (translucent fill, 1px hairline, soft shadow) that hugs its content; re-skinned when day/night flips. */
    private fun glassCard(view: View, radiusDp: Int = LayoutBudget.CARD_RADIUS_DP) {
        view.elevation = dp(6).toFloat()
        themed.add { view.background = Glass.drawable(this, dp(radiusDp).toFloat()) }
    }

    /** Top bar: the clock group card on the left and the weather card on the right, tops aligned; the banner has its own card below. */
    private fun buildTopBar(): View {
        val bar = FrameLayout(this).apply { clipChildren = false; clipToPadding = false }
        val clockCard = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            setPadding(dp(LayoutBudget.CARD_PAD_H_DP), dp(LayoutBudget.CARD_PAD_V_DP), dp(LayoutBudget.CARD_PAD_H_DP), dp(LayoutBudget.CARD_PAD_V_DP))
        }
        glassCard(clockCard)
        // TextClock follows the system 12/24h setting and ticks on its own. Inter Light 56sp, tabular numerals.
        clockCard.addView(TextClock(this).apply {
            format12Hour = "h:mm"
            format24Hour = "H:mm"
            style(300, 56f, -0.02f, tabular = true)
            includeFontPadding = false
            themed.add { setTextColor(Palette.text) }
        })
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), 0, 0)
        }
        info.addView(TextClock(this).apply {
            format12Hour = "EEEE, MMMM d"
            format24Hour = "EEEE, MMMM d"
            style(400, 17f)
            themed.add { setTextColor(Palette.text70) }
        })
        val delhi = LinearLayout(this).apply { gravity = Gravity.BOTTOM }
        delhiRow = delhi
        delhi.addView(TextView(this).apply {
            text = "New Delhi"
            style(400, 17f)
            setPadding(0, 0, dp(8), 0)
            themed.add { setTextColor(Palette.text70) }
        })
        delhi.addView(TextClock(this).apply {
            timeZone = "Asia/Kolkata"
            style(400, 17f, tabular = true)
            themed.add { setTextColor(Palette.text70) }
        })
        info.addView(delhi)
        clockCard.addView(info)
        bar.addView(clockCard, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START))
        weatherWidget = WeatherWidget(this).apply {
            setOnClickListener { openWeatherPanel() }
            isFocusedByDefault = false // reachable only by an explicit UP from the dock
        }
        themed.add { weatherWidget.applyTheme() }
        // Right margin leaves room for the focus scale inside the 48dp safe area.
        bar.addView(weatherWidget, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply { rightMargin = dp(6) })
        return bar
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        style(600, 12f, 0.06f)
        isAllCaps = true
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), 0, 0, 0)
        themed.add { setTextColor(Palette.text60) }
    }

    /**
     * Scrolling row. Padding is 12dp on ALL sides (focus scale overflow 5.3dp + 4dp spare; the border is drawn inside the
     * card), clipToPadding/clipChildren are off so nothing in the row or its scroller clips a focused tile, and the
     * scroll region above clips only at its own bounds, the 48dp safe edge. [fadeEdge] hints that more tiles are off to the right.
     */
    private fun rowScroller(row: LinearLayout, fadeEdge: Boolean) = RowScroller(this).apply {
        clipToPadding = false
        clipChildren = false
        isHorizontalScrollBarEnabled = false
        val pad = dp(LayoutBudget.ROW_PADDING_DP)
        setPadding(pad, pad, pad, pad)
        if (fadeEdge) {
            isHorizontalFadingEdgeEnabled = true
            setFadingEdgeLength(dp(32))
        }
        addView(row)
    }

    // ---------- Theme and day/night ----------

    private fun applyTheme() {
        Palette.day = uiDay
        for (t in themed) t()
        if (::festivalName.isInitialized) updateFestival()
    }

    private fun resolveDay(): Boolean {
        val place = loadPlace()
        return dayNightMode.resolve(SunTimes.isDay(ZonedDateTime.now(), place.lat, place.lon))
    }

    /**
     * Recomputes day/night and schedules the next sunrise/sunset. The wallpaper's scene variant follows the sun (or the forced mode);
     * the UI's light/dark follows the shown photo or painting's brightness in Auto mode (see [UiTheme]), the sun otherwise.
     */
    private fun updateDayNight() {
        val place = loadPlace()
        val now = ZonedDateTime.now()
        val sunDay = SunTimes.isDay(now, place.lat, place.lon)
        wallpaper.setDay(dayNightMode.resolve(sunDay))
        applyUiDay(UiTheme.isDay(dayNightMode, sunDay, imageLuma))
        handler.removeCallbacks(dayNightTick)
        if (dayNightMode == DayNightMode.AUTO) {
            val next = SunTimes.nextTransition(now, place.lat, place.lon)
            // Capped at 30 min so a DST or timezone change (no broadcast for DST) is picked up quickly.
            val delay = next?.let { java.time.Duration.between(now, it).toMillis() + 1_000 } ?: 30L * 60 * 1000
            handler.postDelayed(dayNightTick, delay.coerceIn(1_000L, 30L * 60 * 1000))
        }
    }

    private fun applyUiDay(day: Boolean) {
        if (day != uiDay) {
            uiDay = day
            applyTheme()
            rebuildHome()
            if (showingAll) rebuildAll()
        }
        wallpaper.setUiDay(uiDay)
    }

    private fun onImageLuma(luma: Int?) {
        if (imageLuma == luma) return
        imageLuma = luma
        val place = loadPlace()
        applyUiDay(UiTheme.isDay(dayNightMode, SunTimes.isDay(ZonedDateTime.now(), place.lat, place.lon), luma))
    }

    // ---------- Festival ----------

    private fun buildFestivalBanner(): View {
        festivalName = TextView(this).apply {
            style(600, 20f)
            themed.add { setShadowLayer(dp(3).toFloat(), 0f, dp(1).toFloat(), Palette.shadow) }
        }
        festivalDate = TextView(this).apply {
            style(400, 14f)
            themed.add { setTextColor(Palette.text70); setShadowLayer(dp(3).toFloat(), 0f, dp(1).toFloat(), Palette.shadow) }
        }
        festivalIndia = TextView(this).apply {
            style(400, 13f)
            visibility = View.GONE
            themed.add { setTextColor(Palette.text60); setShadowLayer(dp(3).toFloat(), 0f, dp(1).toFloat(), Palette.shadow) }
        }
        festivalBanner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(festivalName, LinearLayout.LayoutParams(-2, -2))
            addView(festivalDate, LinearLayout.LayoutParams(-2, -2))
            addView(festivalIndia, LinearLayout.LayoutParams(-2, -2))
        }
        glassCard(festivalBanner)
        return festivalBanner
    }

    /** Banner always follows the calendar; the wallpaper override additionally needs the toggle (and Auto mode, see WallpaperSchedule.keyAt). */
    private fun updateFestival() {
        val today = LocalDate.now()
        val active = Festivals.activeOn(today, festivals = Festivals.enabled(prefs.getBoolean(PREF_US_HOLIDAYS, true)))
        if (active == null) {
            festivalBanner.visibility = View.GONE
        } else {
            val banner = Festivals.bannerFor(active, today)
            festivalName.text = banner.title
            festivalName.setTextColor(Palette.festival(active.festival.accent))
            festivalDate.text = banner.dateLine
            val note = banner.indiaLine ?: banner.observedLine // the India date differs, or a weekend holiday is observed on another day
            festivalIndia.text = note.orEmpty()
            festivalIndia.visibility = if (note != null) View.VISIBLE else View.GONE
            festivalBanner.visibility = View.VISIBLE
        }
        val enabled = prefs.getBoolean(PREF_FESTIVAL_WP, true)
        wallpaper.setFestival(
            if (enabled && active != null) WallpaperSchedule.festivalSceneKey(active.festival.sceneId, active.festival.variant) else null,
            if (enabled && active != null) StillMapping.themeForFestival(active.festival.id) else null,
        )
    }

    // ---------- Quote ----------

    /** Quote footer (home and All apps each get one): Inter Light 23sp, 1.3 line height, centred, at most ~64% of the width. */
    private fun buildQuoteBlock(): View {
        val text = TextView(this).apply {
            style(300, 23f)
            setLineSpacing(0f, 1.3f)
            gravity = Gravity.CENTER
            maxLines = 3
            ellipsize = android.text.TextUtils.TruncateAt.END
            maxWidth = (resources.displayMetrics.widthPixels * 0.64f).toInt()
        }
        val author = TextView(this).apply {
            style(500, 15f, 0.02f)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        quoteTexts.add(text)
        quoteAuthors.add(author)
        themed.add {
            text.setTextColor(Palette.text)
            author.setTextColor(Palette.text60)
            text.setShadowLayer(dp(3).toFloat(), 0f, dp(1).toFloat(), Palette.shadow)
            author.setShadowLayer(dp(3).toFloat(), 0f, dp(1).toFloat(), Palette.shadow)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(2), 0, dp(10))
            isFocusable = false
            addView(text, LinearLayout.LayoutParams(-2, -2))
            addView(author, LinearLayout.LayoutParams(-2, -2))
        }
    }

    /** Shows the quote for the current 15-minute slot (400ms alpha crossfade when it changes) and schedules the next slot. */
    private fun updateQuote() {
        updateFestival()
        val now = java.time.LocalDateTime.now()
        val quote = Quotes.quoteAt(now)
        if (quote !== shownQuote) {
            val first = shownQuote == null
            shownQuote = quote
            for (i in quoteTexts.indices) setQuoteText(quoteTexts[i], quoteAuthors[i], quote, animate = !first)
            applyQuoteTint()
        }
        handler.removeCallbacks(quoteTick)
        val untilBoundary = java.time.temporal.ChronoUnit.MILLIS.between(now, Quotes.nextBoundary(now)) + 250
        handler.postDelayed(quoteTick, untilBoundary.coerceIn(1_000L, Quotes.SLOT_MINUTES * 60_000L + 1_000L))
    }

    private fun setQuoteText(text: TextView, author: TextView, quote: Quote, animate: Boolean) {
        val apply = {
            text.text = "\u201C${quote.text}\u201D"
            author.text = "\u2014 ${quote.author}"
        }
        if (!animate) { apply(); return }
        // Gentle, together: fade out 1.2s (ease-in-out); swap the text while invisible (the centre keeps the quote
        // vertically centred, so nothing visibly jumps); fade in 1.2s with a 6dp upward drift.
        val ease = android.view.animation.AccelerateDecelerateInterpolator()
        val drift = dp(6).toFloat()
        text.animate().cancel(); author.animate().cancel()
        author.animate().alpha(0f).setDuration(1200).setInterpolator(ease).start()
        text.animate().alpha(0f).setDuration(1200).setInterpolator(ease).withEndAction {
            apply()
            text.translationY = drift; author.translationY = drift
            text.animate().alpha(1f).translationY(0f).setDuration(1200).setInterpolator(ease).start()
            author.animate().alpha(1f).translationY(0f).setDuration(1200).setInterpolator(ease).start()
        }.start()
    }

    /** One quiet palette for day and evening quotes (the warm evening tint was dropped with the italic serif). */
    private fun applyQuoteTint() {
        for (i in quoteTexts.indices) {
            quoteTexts[i].setTextColor(Palette.text)
            quoteAuthors[i].setTextColor(Palette.text60)
        }
    }

    // ---------- Data ----------

    private fun scanApps(): List<AppEntry> {
        val found = LinkedHashMap<String, AppEntry>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in packageManager.queryIntentActivities(query, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName || pkg in found) continue
                found[pkg] = AppEntry(pkg, ri.loadLabel(packageManager).toString(), ri)
            }
        }
        return found.values.sortedBy { it.label.lowercase() }
    }

    private fun refresh() {
        val focusKey = (currentFocus as? FocusItem)?.key
        apps = scanApps()
        val saved = Favorites.deserialize(prefs.getString(PREF_FAVORITES, null))
        favorites = Favorites.resolve(saved, apps.map { it.pkg }.toSet())
        rebuildHome()
        if (showingAll) rebuildAll()
        restoreFocus(focusKey)
    }

    /** Auto-fit: tile width chosen so all favorites fit with the magnification headroom; otherwise scroll with a soft edge. */
    private fun fitDock(count: Int) {
        val fit = DockFit.fit(spec, DockGeometry.availableDp(spec, screenWidthDp()), count)
        dockBaseDp = fit.baseDp
        dockScrolls = fit.scrolls
        favRow.setPadding(dpf(spec.rowHPadDp), 0, dpf(spec.rowHPadDp + if (fit.scrolls) spec.extraRightDp else 0f), dpf(spec.shelfPadDp))
        favScroller.softFadeDp = if (fit.scrolls) (40 * spec.scale).toInt() else 0
        val cardH = fit.cardHeightDp
        // Sit the row at the BOTTOM of the dock so the unfocused cards are centred in the shelf (equal padding above and below).
        favScroller.setPadding(dpf(spec.hPadDp), dpf(DockGeometry.rowTopPadDp(spec, cardH)), dpf(spec.hPadDp), dpf(spec.bottomGapDp))
        // Vertically centre the menu button, divider, pills and the shelf on the (variable height) card row.
        val cardCenter = spec.bottomGapDp + spec.shelfPadDp + cardH / 2f
        fun lp(v: View) = v.layoutParams as FrameLayout.LayoutParams
        lp(favScroller).leftMargin = dpf(spec.favStartDp)
        menuButton.spec = spec
        lp(menuButton).apply { leftMargin = dpf(spec.menuInsetDp); bottomMargin = dpf(cardCenter - spec.buttonDp / 2f) }
        lp(dockDivider).apply { width = 1; height = dpf(spec.dividerHDp); leftMargin = dpf(spec.dividerXDp); bottomMargin = dpf(cardCenter - spec.dividerHDp / 2f) }
        val tipW = kotlin.math.ceil(menuTooltip.paint.measureText(menuTooltip.text.toString()) + 2 * dp(10)).toInt()
        lp(menuTooltip).apply {
            width = tipW; height = dpf(spec.labelDp)
            leftMargin = (dpf(spec.menuInsetDp + spec.buttonDp / 2f) - tipW / 2).coerceAtLeast(0)
            bottomMargin = dpf(cardCenter - spec.buttonDp / 2f + spec.buttonDp * spec.magFocus + spec.liftDp + 4f)
        }
        val pillPad = pillRowPadDp
        pillRow.setPadding(dpf(pillPad), dpf(pillPad), dpf(pillPad), dpf(pillPad))
        lp(pillRow).apply { leftMargin = dpf(spec.pillXDp - pillPad); bottomMargin = dpf(cardCenter - spec.pillHDp / 2f - pillPad) }
        val shelfHeight = cardH + 2 * spec.shelfPadDp
        val restDp = if (fit.scrolls) DockGeometry.safeWidthDp(screenWidthDp()) else DockGeometry.restWidthDp(spec, count, fit.baseDp)
        shelfRestPx = dpf(restDp).toFloat().coerceAtLeast(1f)
        lp(shelf).apply { width = if (fit.scrolls) ViewGroup.LayoutParams.MATCH_PARENT else shelfRestPx.toInt(); height = dpf(shelfHeight); bottomMargin = dpf(spec.bottomGapDp) }
        shelf.background = Glass.drawable(this, dpf(spec.shelfCornerDp).toFloat())
        dockDivider.setBackgroundColor((Palette.text and 0x00FFFFFF) or (0x4D shl 24))
        favScroller.setHorizontalFadingEdgeEnabled(false)
        shelf.pivotX = 0f
        shelf.scaleX = 1f
        shelfGrowthPx = 0f
        dockContent.translationX = dpf(DockGeometry.offsetDp(DockGeometry.safeWidthDp(screenWidthDp()), restDp)).toFloat()
        dock.requestLayout()
        placeCredit()
    }

    /** Keeps the photo credit off the dock shelf: beside the dock when the corner is free, else just above the shelf. */
    private fun placeCredit() {
        if (!::creditView.isInitialized || !::dockContent.isInitialized) return
        val d = resources.displayMetrics.density
        creditView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val widthDp = creditView.measuredWidth / d
        val rest = shelfRestPx / d
        val grown = maxOf(rest + if (dockScrolls) 0f else spec.maxBaseDp * spec.magExtra, expandedShelfDp())
        val dockW = DockGeometry.safeWidthDp(screenWidthDp())
        val dockRight = SAFE_SIDE_DP + (if (dockScrolls) dockW else (dockW + grown) / 2f)
        val shelfH = spec.cardHeightDp(dockBaseDp) + 2 * spec.shelfPadDp
        val margin = CreditPlacement.bottomMarginDp(spec, dockRight, widthDp, shelfH)
        (creditView.layoutParams as? FrameLayout.LayoutParams)?.let { it.bottomMargin = dpf(margin); creditView.layoutParams = it }
    }

    private fun screenWidthDp(): Float = resources.displayMetrics.widthPixels / resources.displayMetrics.density

    private fun showMenuTooltip(show: Boolean) {
        menuTooltip.animate().cancel()
        if (show) {
            menuTooltip.visibility = View.VISIBLE
            menuTooltip.animate().alpha(1f).setDuration(150).start()
        } else {
            menuTooltip.animate().alpha(0f).setDuration(150).withEndAction { menuTooltip.visibility = View.INVISIBLE }.start()
        }
    }

    /** Width of the shelf while the menu is open: [back chevron | four pills] only. */
    private fun expandedShelfDp(): Float = spec.expandedShelfDp

    /** Grows the shelf with the magnification / menu and re-centres the whole dock in step (growth splits evenly left and right). */
    private fun updateShelf(durationMs: Long) {
        val ease = Motion.EASE
        // Menu open: the shelf holds only [chevron | pills], centred; otherwise the favorites' rest width plus the magnification growth.
        val content = if (menuExpanded) dpf(expandedShelfDp()).toFloat() else shelfRestPx + if (dockScrolls) 0f else shelfGrowthPx
        shelf.animate().scaleX(content / shelfRestPx).setDuration(durationMs).setInterpolator(ease).start()
        val dockPx = dpf(DockGeometry.safeWidthDp(screenWidthDp())).toFloat()
        dockContent.animate().translationX(maxOf(0f, (dockPx - content) / 2f)).setDuration(durationMs).setInterpolator(ease).start()
    }

    private fun rebuildHome() {
        setMenuExpanded(false, animate = false)
        favRow.removeAllViews()
        val installedFavs = favorites.count { pkg -> apps.any { it.pkg == pkg } }
        fitDock(installedFavs)
        val tileW = dockBaseDp
        val cardH = spec.cardHeightDp(dockBaseDp)
        val squircle = spec.icons
        for (pkg in favorites) {
            val app = apps.firstOrNull { it.pkg == pkg } ?: continue
            val tile = Tile(this, tileW, cardH, labelBelow = true, labelSp = LayoutBudget.LABEL_SP, style = TileStyle.DOCK, spec = spec, squircle = squircle).bind("fav:$pkg", app.label)
            if (squircle) {
                bindSquircle(tile, app)
            } else {
                val banner: Drawable? = app.info.activityInfo.loadBanner(packageManager)
                    ?: app.info.activityInfo.applicationInfo.loadBanner(packageManager)
                if (banner != null) tile.showBanner(banner) else tile.showIcon(app.info.loadIcon(packageManager), 18)
            }
            tile.setOnClickListener { launchApp(app) }
            tile.setOnLongClickListener { editFavorite(app); true }
            tile.onDockFocus = { t, focused -> onDockFocus(t, focused) }
            favRow.addTile(tile, spec.gapDp)
        }
        pillRow.removeAllViews()
        pills.clear()
        addPill("Inputs") { launchPackage(INPUTS_PACKAGE) }
        addPill("Settings") { launchIntent(Intent(Settings.ACTION_SETTINGS)) }
        addPill("Wallpaper") { chooseWallpaper() }
        addPill("All apps") { showAll() }
        // D-pad edges: LEFT from the first favorite goes to the menu button; RIGHT on the last one stays put.
        menuButton.nextFocusLeftId = menuButton.id
        favRow.getChildAt(0)?.nextFocusLeftId = menuButton.id
        favRow.getChildAt(favRow.childCount - 1)?.let { it.nextFocusRightId = it.id }
        // Initial focus (also on the first key press if the app starts in touch mode): the first favorite.
        (favRow.getChildAt(0) ?: menuButton).isFocusedByDefault = true
    }

    /** Shows the cached squircle at once, else renders it in the background and fades it in. */
    private fun bindSquircle(tile: Tile, app: AppEntry) {
        val cached = iconCache.get(app.pkg)
        if (cached != null) { tile.showSquircle(cached, fade = false); return }
        iconCache.request(app.pkg, { app.info.loadIcon(packageManager) }) { bmp -> tile.showSquircle(bmp, fade = true) }
    }

    /** LEFT on a row's first cell and RIGHT on its last stay put instead of jumping to another row. */
    private fun pinEdges(row: LinearLayout) {
        val first = row.getChildAt(0) ?: return
        val last = row.getChildAt(row.childCount - 1)
        first.nextFocusLeftId = first.id
        last.nextFocusRightId = last.id
    }

    private fun addPill(name: String, action: () -> Unit) {
        val tile = Tile(this, spec.pillWDp, spec.pillHDp, labelBelow = false, textSp = spec.pillSp, quick = true, style = TileStyle.PILL, spec = spec).bind("quick:$name", name)
        tile.showText(name)
        tile.setOnClickListener { action() }
        tile.onDockFocus = { t, focused -> onPillFocus(t, focused) }
        pills.add(tile)
        pillRow.addTile(tile, spec.pillGapDp)
    }

    // ---------- Dock: magnification and the collapsible menu ----------

    /** macOS-dock effect: focused tile 1.35x lifted 12dp, neighbours 1.12x/4dp, second neighbours 1.04x, rest at rest. */
    private fun onDockFocus(tile: Tile, focused: Boolean) {
        if (focused) {
            lastDockFocus = tile
            for (i in 0 until favRow.childCount) (favRow.getChildAt(i) as? Tile)?.let { if (it !== tile) it.showLabel(false) } // one label at a time
            magnify(favRow.indexOfChild(tile))
        } else {
            favRow.post { if (!favRow.hasFocus() && !menuButton.isFocused) magnify(-1) }
        }
    }

    /** Focus on the menu button: it magnifies as a dock item, its slot widens and everything to its right is pushed. */
    private fun onMenuFocus(focused: Boolean) {
        if (focused) magnify(-1, menuFocused = true)
        else menuButton.post { if (!menuButton.isFocused && !favRow.hasFocus()) magnify(-1) }
    }

    /**
     * Slides and scales every dock item; see [DockMath] and [DockExtents] for the geometry (tests prove nothing overlaps or leaves the dock).
     * With [menuFocused] the button is the magnified item: its growth widens its slot, pushes the divider and favorites, and the dock re-centres.
     */
    private fun magnify(center: Int, menuFocused: Boolean = false) {
        val base = dpf(dockBaseDp).toFloat()
        val n = favRow.childCount
        val distance = { i: Int -> if (menuFocused) i + 1 else if (center < 0) 99 else kotlin.math.abs(i - center) }
        val scales = FloatArray(n) { DockMath.scaleAt(distance(it), spec) }
        val shifts = DockMath.shifts(scales, base)
        val menuPush = if (menuFocused && !menuExpanded) dpf(spec.menuGrowthDp).toFloat() else 0f
        shelfGrowthPx = menuPush + scales.sumOf { (base * (it - 1f)).toDouble() }.toFloat()
        updateShelf(DOCK_MS)
        for (i in 0 until n) {
            val t = favRow.getChildAt(i) as? Tile ?: continue
            t.animateTo(scales[i], DockMath.liftAt(distance(i), spec), shifts[i] + menuPush, DOCK_MS, focused = i == center && !menuFocused)
        }
        dockDivider.animate().translationX(menuPush).setDuration(DOCK_MS).setInterpolator(Motion.EASE).start()
        // Pivot is the bottom centre; shifting by half the growth keeps the button's left edge at its slot edge, so it never leaves the shelf.
        if (menuFocused) menuButton.animateTo(spec.magFocus, spec.liftDp, menuPush / 2f, DOCK_MS) else menuButton.animateTo(1f, 0f, 0f, DOCK_MS)
        if (menuFocused && dockScrolls) favScroller.smoothScrollTo(0, 0)
        if (center >= 0 && !menuFocused) ensureDockVisible(favRow.getChildAt(center), scales[center], shifts[center])
    }

    /** Scrolls the dock so the magnified focused tile, with 12dp spare (plus the fading edge when scrolling), is fully visible. */
    private fun ensureDockVisible(tile: View, scale: Float, shiftPx: Float) {
        val base = dpf(dockBaseDp).toFloat()
        val visualLeft = tile.left + shiftPx - base * (scale - 1f) / 2f
        val visualRight = visualLeft + base * scale
        val padLeft = favScroller.paddingLeft
        val viewWidth = favScroller.width
        if (viewWidth <= 0) return
        val margin = dpf((12f + if (dockScrolls) 40f else 0f) * spec.scale).toFloat() // room for the soft edge only when the row overflows
        val visLeft = favScroller.scrollX - padLeft.toFloat()
        val visRight = visLeft + viewWidth
        var target = favScroller.scrollX.toFloat()
        if (visualLeft - margin < visLeft) target = visualLeft - margin + padLeft
        else if (visualRight + margin > visRight) target = visualRight + margin - viewWidth + padLeft
        val maxScroll = (favRow.width + padLeft + favScroller.paddingRight - viewWidth).coerceAtLeast(0)
        favScroller.smoothScrollTo(target.toInt().coerceIn(0, maxScroll), 0)
    }

    private fun inMenuGroup(v: View?) = v === menuButton || (v is Tile && v.parent === pillRow)

    private fun onFocusMoved(newFocus: View?) {
        if (newFocus == null) return
        if (newFocus === menuButton || (newFocus is Tile && newFocus.parent === favRow) || newFocus.parent === pillRow) lastDockFocus = newFocus
        if (menuExpanded && !inMenuGroup(newFocus)) setMenuExpanded(false) // focus left the menu: roll it back up
    }

    /** Pill focus: 1.12x with a 4dp lift, neighbours slide aside (slot = width x scale + gap) so pills never overlap. */
    private fun onPillFocus(tile: Tile, focused: Boolean) {
        if (focused) { lastDockFocus = tile; magnifyPills(pills.indexOf(tile)) }
        else pillRow.post { if (!pillRow.hasFocus()) magnifyPills(-1) }
    }

    private fun magnifyPills(center: Int) {
        val scales = FloatArray(pills.size) { if (it == center) spec.pillMag else 1f }
        val shifts = DockMath.shifts(scales, dpf(spec.pillWDp).toFloat())
        pills.forEachIndexed { i, p -> p.animateTo(scales[i], if (i == center) spec.neighbourLiftDp else 0f, shifts[i], 150) }
    }

    /**
     * Opens the menu: the favorites fade out completely (200ms) and the shelf shrinks to hold only [chevron | four pills], centred, so
     * nothing can overflow the safe edge or reach the photo credit. Collapsing reverses it.
     */
    private fun setMenuExpanded(expand: Boolean, animate: Boolean = true) {
        if (menuExpanded == expand && animate) return
        menuExpanded = expand
        menuButton.expanded = expand
        val interpolator = Motion.EASE
        val duration = if (animate) 200L else 0L
        val hidden = -dpf(36f * spec.scale).toFloat()
        favScroller.animate().cancel()
        if (expand) {
            pillRow.visibility = View.VISIBLE
            pillRow.animate().cancel()
            pillRow.translationX = hidden // the whole row unrolls out from behind the button; pills fade in one after another
            pillRow.animate().translationX(0f).setDuration(260).setInterpolator(interpolator).start()
            pills.forEachIndexed { i, pill ->
                pill.animate().cancel()
                pill.alpha = 0f
                pill.animate().alpha(1f).setStartDelay(i * 40L).setDuration(260).setInterpolator(interpolator).start()
            }
            favScroller.animate().alpha(0f).setDuration(duration).setInterpolator(interpolator)
                .withEndAction { if (menuExpanded) favScroller.visibility = View.INVISIBLE }.start() // invisible = unfocusable
            updateShelf(260)
            placeCredit()
            pills.firstOrNull()?.post { pills.first().requestFocus() }
        } else {
            favScroller.visibility = View.VISIBLE // visible before any focus request; the fade-in follows
            favScroller.animate().alpha(1f).setDuration(duration).setInterpolator(interpolator).start()
            pillRow.animate().cancel()
            pillRow.animate().translationX(hidden).setDuration(duration).setInterpolator(interpolator)
                .withEndAction { if (!menuExpanded) { pillRow.visibility = View.GONE; magnifyPills(-1) } }.start()
            pills.forEach { pill -> pill.animate().cancel(); pill.animate().alpha(0f).setStartDelay(0).setDuration(duration).setInterpolator(interpolator).start() }
            if (::shelf.isInitialized && shelfRestPx > 0f) updateShelf(duration)
            if (!animate) { pillRow.visibility = View.GONE; pillRow.translationX = hidden; favScroller.alpha = 1f }
            placeCredit()
        }
    }

    /** DPAD handling on Home; returns true when the key was handled. */
    private fun homeKey(keyCode: Int): Boolean {
        val f = currentFocus
        val inDock = f === menuButton || (f is Tile && (f.parent === favRow || f.parent === pillRow))
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> if (inDock) { weatherWidget.takeIf { it.isShown }?.requestFocus(); return true }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (f === weatherWidget) {
                    (lastDockFocus?.takeIf { it.isAttachedToWindow && it.isShown } ?: favRow.getChildAt(0) ?: menuButton).requestFocus()
                    return true
                }
                if (inDock) return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> if (inDock) return dockLeftRight(f, if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) DockKey.LEFT else DockKey.RIGHT)
        }
        return false
    }

    /** Index-based LEFT/RIGHT along the dock (see [DockNav]); geometry is not trusted with scaled, pushed and slid rows. */
    private fun dockLeftRight(f: View?, key: DockKey): Boolean {
        val focus: DockFocus = when {
            f === menuButton -> DockFocus.MenuButton
            f is Tile && f.parent === pillRow -> DockFocus.Pill(pills.indexOf(f))
            f is Tile && f.parent === favRow -> DockFocus.Favorite(favRow.indexOfChild(f))
            else -> return false
        }
        val result = DockNav.next(DockState(menuExpanded, focus), key, pills.size, favRow.childCount)
        if (menuExpanded && !result.menuOpen) setMenuExpanded(false)
        val target: View? = when (val t = result.focus) {
            DockFocus.MenuButton -> menuButton
            is DockFocus.Pill -> pills.getOrNull(t.index)
            is DockFocus.Favorite -> favRow.getChildAt(t.index)
        }
        target?.requestFocus()
        return true
    }

    /** The only place tile params are made: always the parent's own LayoutParams type. */
    private fun LinearLayout.addTile(tile: Tile, gapDp: Float) {
        val lp = LinearLayout.LayoutParams(dpf(tile.widthDp), LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.rightMargin = dpf(gapDp)
        addView(tile, lp)
    }

    // ---------- Photos ----------

    private fun loadPhotos() {
        val dir = getExternalFilesDir(null)?.let { java.io.File(it, "photos") }
        Thread {
            PhotoStore.ensureDirs(getExternalFilesDir(null)) // the app owns photos/ and scenes/ so they can be pushed to even on a fresh install
            val stillItems = StillStore.load(getExternalFilesDir(null)?.let { java.io.File(it, "scenes") })
            val items = PhotoStore.load(dir) // blocking file and image-header reads: off the UI thread
            handler.post { wallpaper.setStills(stillItems); wallpaper.setPhotos(items) }
        }.start()
    }

    /** Shows "Photo: X / Unsplash"; hidden in idle mode except for 10 seconds when a photo first appears. */
    private fun updateCredit(text: String?) {
        creditView.animate().cancel()
        handler.removeCallbacks(creditHide)
        if (text == null) { creditView.visibility = View.GONE; return }
        creditView.text = text
        creditView.visibility = View.VISIBLE
        placeCredit()
        creditView.alpha = 0.6f
        if (idleTracker.idle) handler.postDelayed(creditHide, 10_000)
    }

    // ---------- Idle mode ----------

    private fun fadeTargets() = listOf(dock, weatherWidget, festivalBanner, delhiRow)

    private fun checkIdle() {
        val now = android.os.SystemClock.uptimeMillis()
        val homeActive = !showingAll && !panel.isOpen
        if (homeActive && idleTracker.tick(now)) {
            for (v in fadeTargets()) v.animate().alpha(0f).setDuration(600).start()
            creditView.animate().alpha(0f).setDuration(600).start()
        } else {
            scheduleIdle(if (homeActive) idleTracker.untilIdleMs(now) else IdleTracker.DEFAULT_TIMEOUT_MS)
        }
    }

    private fun scheduleIdle(delayMs: Long?) {
        handler.removeCallbacks(idleCheck)
        if (delayMs != null) handler.postDelayed(idleCheck, delayMs + 50)
    }

    private fun wakeUi() {
        for (v in fadeTargets()) v.animate().alpha(1f).setDuration(300).start()
        if (creditView.visibility == View.VISIBLE) creditView.animate().alpha(0.6f).setDuration(300).start()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val now = android.os.SystemClock.uptimeMillis()
        val homeActive = !showingAll && !panel.isOpen
        if (!homeActive) idleTracker.reset(now)
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode in swallowedKeys) return true // key repeats of the waking press
            if (homeActive && idleTracker.onKey(now)) {
                swallowedKeys.add(event.keyCode) // the waking key only wakes; it must not also act
                wakeUi()
                scheduleIdle(idleTracker.untilIdleMs(now))
                return true
            }
            if (!homeActive) idleTracker.reset(now) else idleTracker.touch(now)
            scheduleIdle(idleTracker.untilIdleMs(now) ?: IdleTracker.DEFAULT_TIMEOUT_MS)
        } else if (event.action == KeyEvent.ACTION_UP && swallowedKeys.remove(event.keyCode)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /** Built only while the All apps screen is open, then dropped, to keep icon bitmaps out of memory. */
    private fun rebuildAll() {
        allList.removeAllViews()
        allCells.clear()
        val cellW = dp(LayoutBudget.ALL_CELL_W_DP)
        val cellH = dp(LayoutBudget.ALL_ROW_DP)
        (allList.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.gravity = Gravity.CENTER_HORIZONTAL or if (LayoutBudget.allGridFits(apps.size)) Gravity.CENTER_VERTICAL else Gravity.TOP
            allList.layoutParams = it
        }
        var row: LinearLayout? = null
        apps.forEachIndexed { i, app ->
            if (i % LayoutBudget.ALL_COLS == 0) {
                row = LinearLayout(this).apply { clipChildren = false; clipToPadding = false }
                allList.addView(row, LinearLayout.LayoutParams(cellW * LayoutBudget.ALL_COLS, cellH))
            }
            val cell = AppCell(this, cellW, cellH).bind("app:${app.pkg}", app.label)
            val cached = iconCache.get(app.pkg)
            if (cached != null) cell.showSquircle(cached, fade = false)
            else iconCache.request(app.pkg, { app.info.loadIcon(packageManager) }) { bmp -> cell.showSquircle(bmp, fade = true) }
            cell.setOnClickListener { launchApp(app) }
            cell.setOnLongClickListener { editAllAppsEntry(app); true }
            row!!.addView(cell, LinearLayout.LayoutParams(cellW, cellH))
            allCells.add(cell)
        }
    }

    // ---------- Navigation and focus ----------

    private fun showAll() {
        showingAll = true
        homeColumn.visibility = View.GONE
        allScreen.visibility = View.VISIBLE
        wallpaper.setDimmed(true)
        rebuildAll()
        restoreFocus(null)
    }

    private fun showHome() {
        showingAll = false
        allList.removeAllViews()
        allCells.clear()
        iconCache.evictExcept(favorites.toSet()) // the Launchpad icons are only held while it is open
        allScreen.visibility = View.GONE
        homeColumn.visibility = View.VISIBLE
        wallpaper.setDimmed(false)
        focusDock() // never the weather widget: the framework would otherwise pick the first focusable view
    }

    /** Focus the last-focused dock item, else the first favorite, else the menu button. */
    private fun focusDock() {
        val remembered = lastDockFocus?.takeIf { it.isAttachedToWindow && it !== weatherWidget }
        val target = remembered ?: favRow.getChildAt(0) ?: menuButton
        homeColumn.post { if (!target.requestFocus()) (favRow.getChildAt(0) ?: menuButton).requestFocus() }
    }

    private fun restoreFocus(key: String?) {
        val root = if (showingAll) allList else homeColumn
        val tiles = ArrayList<FocusItem>()
        collectTiles(root, tiles)
        if (tiles.isEmpty()) return
        val current = currentFocus
        if (current != null && current.isShown) return
        val target = tiles.firstOrNull { it.key == key }
            ?: tiles.firstOrNull { it.key.startsWith("fav:") }
            ?: tiles.first()
        if (!target.requestFocus() && target.key.startsWith("quick:")) menuButton.requestFocus() // pill is hidden while the menu is collapsed
    }

    private fun collectTiles(v: View, out: MutableList<FocusItem>) {
        if (v is FocusItem) out.add(v) else if (v is android.view.ViewGroup) {
            for (i in 0 until v.childCount) collectTiles(v.getChildAt(i), out)
        }
    }

    // ---------- Launching ----------

    private fun launchApp(app: AppEntry) {
        lastLaunchedPackage = app.pkg
        pendingHomeRestore = true
        val ai = app.info.activityInfo
        val intent = Intent(Intent.ACTION_MAIN)
            .setComponent(ComponentName(ai.packageName, ai.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        launchIntent(intent)
    }

    private fun launchPackage(pkg: String) {
        lastLaunchedPackage = null
        pendingHomeRestore = true
        val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg)
            ?: packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) toast("App not installed") else launchIntent(intent)
    }

    private fun launchIntent(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            wallpaper.stop() // another app is taking over; stop drawing right away
        } catch (e: ActivityNotFoundException) {
            pendingHomeRestore = false; toast("App not available")
        } catch (e: SecurityException) {
            pendingHomeRestore = false; toast("App not available")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // ---------- Weather ----------

    private fun loadPlace(): Place = Geocoding.placeFromStored(
        prefs.getString(PREF_WX_NAME, null), prefs.getString(PREF_WX_LAT, null),
        prefs.getString(PREF_WX_LON, null), prefs.getString(PREF_WX_INPUT, null),
    )

    private fun cacheAgeMs(): Long? =
        if (prefs.getString(PREF_WX_BODY, null) == null) null else System.currentTimeMillis() - prefs.getLong(PREF_WX_TIME, 0L)

    private fun startWeather() {
        weatherExecutor = Executors.newSingleThreadExecutor()
        renderWeather() // cached data shows immediately
        // Each cache decides on its own: a fresh weather cache must not keep a missing air-quality cache from being fetched.
        val plan = Weather.fetchPlan(cacheAgeMs(), aqAgeMs())
        if (plan.weather || plan.air) fetchWeather(force = false)
        handler.postDelayed(weatherTick, Weather.nextTickDelayMs(cacheAgeMs(), aqAgeMs()))
    }

    private fun stopWeather() {
        handler.removeCallbacks(weatherTick)
        weatherExecutor?.shutdownNow()
        weatherExecutor = null
    }

    private fun aqAgeMs(): Long? =
        if (prefs.getString(PREF_AQ_BODY, null) == null) null else System.currentTimeMillis() - prefs.getLong(PREF_AQ_TIME, 0L)

    private fun fetchWeather(force: Boolean) {
        val ex = weatherExecutor?.takeIf { !it.isShutdown } ?: return
        val place = loadPlace()
        val gen = weatherGen
        val plan = if (force) Weather.FetchPlan(weather = true, air = true) else Weather.fetchPlan(cacheAgeMs(), aqAgeMs())
        ex.execute {
            // Two independent requests on the same background thread; either may fail without affecting the other.
            val wx = if (plan.weather) WeatherFetcher.fetch(Weather.buildUrl(place.lat, place.lon)) else null
            val wxValid = wx is FetchOutcome.Body && Weather.parse(wx.text) is WeatherResult.Success
            val aq = if (plan.air) WeatherFetcher.fetch(AirQualityRules.buildUrl(place.lat, place.lon)) else null
            val aqValid = aq is FetchOutcome.Body && AirQualityRules.parse(aq.text) is AirQualityResult.Success
            handler.post {
                if (gen == weatherGen) {
                    val edit = prefs.edit()
                    val now = System.currentTimeMillis()
                    if (wxValid && wx is FetchOutcome.Body) edit.putString(PREF_WX_BODY, wx.text).putLong(PREF_WX_TIME, now)
                    if (aqValid && aq is FetchOutcome.Body) edit.putString(PREF_AQ_BODY, aq.text).putLong(PREF_AQ_TIME, now)
                    edit.apply()
                }
                renderWeather() // failures just leave the caches as they are; never an error UI
            }
        }
    }

    private fun renderWeather() {
        val body = prefs.getString(PREF_WX_BODY, null)
        val result = if (body != null && Weather.shouldShow(cacheAgeMs())) Weather.parse(body) else null
        if (result is WeatherResult.Success) {
            weatherWidget.show(result.data, loadPlace().name)
            renderAir()
        } else {
            weatherWidget.hide()
        }
    }

    /** Air quality has its own cache and staleness: if it fails or is old, only its line disappears. */
    private fun renderAir() {
        val body = prefs.getString(PREF_AQ_BODY, null)
        val age = if (body == null) null else System.currentTimeMillis() - prefs.getLong(PREF_AQ_TIME, 0L)
        val result = if (body != null && Weather.shouldShow(age)) AirQualityRules.parse(body) else null
        if (result is AirQualityResult.Success) weatherWidget.showAir(result.data) else weatherWidget.hideAir()
    }

    private fun openWeatherPanel() {
        openPanel(
            "Weather location",
            listOf(
                PanelItem.Header("CURRENT"),
                PanelItem.Option(loadPlace().name, OptionKind.RADIO, { true }, onSelect = {}),
                PanelItem.Header("CHANGE"),
                PanelItem.Option("Change location\u2026", OptionKind.ACTION, onSelect = { closeThen { editWeatherLocation() } }),
            ),
            returnKey = null,
            returnView = weatherWidget,
        )
    }

    private fun editWeatherLocation() {
        val input = EditText(this).apply {
            hint = "ZIP code or city"
            setSingleLine()
        }
        val holder = FrameLayout(this).apply {
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(input, FrameLayout.LayoutParams(-1, -2))
        }
        AlertDialog.Builder(this)
            .setTitle("Weather location")
            .setView(holder)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ -> resolveLocation(input.text.toString()) }
            .show()
    }

    private fun resolveLocation(raw: String) {
        val ex = weatherExecutor?.takeIf { !it.isShutdown } ?: return
        ex.execute {
            val result = LocationResolver.resolve(raw)
            handler.post { applyLocation(raw.trim(), result) }
        }
    }

    private fun applyLocation(input: String, result: LookupResult) {
        val next = Geocoding.applyLookup(loadPlace(), result, input)
        if (result !is LookupResult.Found) {
            toast("Couldn't find that location")
            return
        }
        prefs.edit()
            .putString(PREF_WX_NAME, next.name).putString(PREF_WX_LAT, next.lat.toString())
            .putString(PREF_WX_LON, next.lon.toString()).putString(PREF_WX_INPUT, next.input)
            .remove(PREF_WX_BODY).remove(PREF_WX_TIME).remove(PREF_AQ_BODY).remove(PREF_AQ_TIME)
            .apply()
        weatherGen++
        updateDayNight() // sunrise/sunset depend on the saved location
        renderWeather()
        toast("Weather location: ${next.name}")
        fetchWeather(force = true)
    }

    // ---------- Settings panel ----------

    /** Opens the right-side panel; focus is blocked on the screens behind it and returns to the opener on close. */
    private fun openPanel(title: String, items: List<PanelItem>, returnKey: String?, returnView: View? = null) {
        panelReturnKey = returnKey
        panelReturnView = returnView
        homeColumn.descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
        allScreen.descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
        panel.open(title, items)
    }

    private fun panelClosed() {
        homeColumn.descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
        allScreen.descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
        val view = panelReturnView
        if (view != null && view.isShown) view.requestFocus() else restoreFocus(panelReturnKey)
    }

    /** Runs [action] after closing the panel so focus is already back on the tiles when the action rebuilds them. */
    private fun closeThen(action: () -> Unit) { panel.close(); action() }

    // ---------- Wallpaper ----------

    private fun currentSources(): SourceSet = WallpaperSources.resolve(prefs.getString(PREF_SOURCES, null), prefs.getString(PREF_PHOTO_SOURCE, null))

    private fun currentWallpaperMode() = WallpaperSchedule.parse(prefs.getString(PREF_WALLPAPER, null))

    private fun setWallpaperMode(mode: WallpaperMode) {
        prefs.edit().putString(PREF_WALLPAPER, WallpaperSchedule.serialize(mode)).apply()
        wallpaper.setMode(mode) // live preview behind the panel
    }

    /** Wallpaper settings panel. Every choice applies immediately; the panel stays open until BACK. */
    private fun chooseWallpaper() {
        val items = ArrayList<PanelItem>()
        items += PanelItem.Header("SCENE")
        items += PanelItem.Option("Auto-rotate (every 30 min)", OptionKind.RADIO, { currentWallpaperMode() == WallpaperMode.Auto }) { setWallpaperMode(WallpaperMode.Auto) }
        WallpaperSchedule.SCENE_NAMES.forEachIndexed { i, name ->
            items += PanelItem.Option(name, OptionKind.RADIO, { currentWallpaperMode() == WallpaperMode.Pinned(i) }) { setWallpaperMode(WallpaperMode.Pinned(i)) }
        }
        items += PanelItem.Option("Photos (auto-rotate)", OptionKind.RADIO, { currentWallpaperMode() == WallpaperMode.Photos }) { setWallpaperMode(WallpaperMode.Photos) }
        items += PanelItem.Option("Paintings (auto-rotate)", OptionKind.RADIO, { currentWallpaperMode() == WallpaperMode.Paintings }) { setWallpaperMode(WallpaperMode.Paintings) }
        items += PanelItem.Option("Off (plain dark)", OptionKind.RADIO, { currentWallpaperMode() == WallpaperMode.Off }) { setWallpaperMode(WallpaperMode.Off) }
        items += PanelItem.Header("SOURCES (for Auto-rotate) \u00B7 ${wallpaper.photoCount()} photos \u00B7 ${wallpaper.paintingCount()} paintings")
        for ((kind, label) in listOf(SourceKind.PHOTOS to "Photos", SourceKind.PAINTINGS to "Paintings", SourceKind.SCENES to "Animated scenes")) {
            items += PanelItem.Option(label, OptionKind.SWITCH, { currentSources().has(kind) }) {
                val cur = currentSources()
                val next = cur.with(kind, !cur.has(kind)) // the last enabled source cannot be switched off
                prefs.edit().putString(PREF_SOURCES, next.serialize()).apply()
                wallpaper.setSources(next)
            }
        }
        items += PanelItem.Header("DOCK STYLE")
        for (st in DockStyle.entries) {
            items += PanelItem.Option(st.label, OptionKind.RADIO, { dockStyle == st }) { setDockStyle(st) }
        }
        items += PanelItem.Header("DAY / NIGHT")
        for (m in DayNightMode.entries) {
            items += PanelItem.Option(m.label, OptionKind.RADIO, { dayNightMode == m }) {
                dayNightMode = m
                prefs.edit().putString(PREF_DAYNIGHT, m.id).apply()
                updateDayNight()
            }
        }
        items += PanelItem.Header("IDLE")
        items += PanelItem.Option("Hide UI when idle (45s)", OptionKind.SWITCH, { prefs.getBoolean(PREF_IDLE, true) }) {
            val on = !prefs.getBoolean(PREF_IDLE, true)
            prefs.edit().putBoolean(PREF_IDLE, on).apply()
            idleTracker.enabled = on
        }
        items += PanelItem.Header("US HOLIDAYS")
        items += PanelItem.Option("US holidays", OptionKind.SWITCH, { prefs.getBoolean(PREF_US_HOLIDAYS, true) }) {
            prefs.edit().putBoolean(PREF_US_HOLIDAYS, !prefs.getBoolean(PREF_US_HOLIDAYS, true)).apply()
            updateFestival()
        }
        items += PanelItem.Header("FESTIVALS")
        items += PanelItem.Option("Festival wallpapers", OptionKind.SWITCH, { prefs.getBoolean(PREF_FESTIVAL_WP, true) }) {
            prefs.edit().putBoolean(PREF_FESTIVAL_WP, !prefs.getBoolean(PREF_FESTIVAL_WP, true)).apply()
            updateFestival()
        }
        openPanel("Wallpaper", items, returnKey = "quick:Wallpaper")
    }

    private fun setDockStyle(style: DockStyle) {
        if (style == dockStyle) return
        dockStyle = style
        spec = LayoutBudget.spec(style)
        prefs.edit().putString(PREF_DOCK_STYLE, style.id).apply()
        lastDockFocus = null
        rebuildHome()
    }

    // ---------- Favorites editing ----------

    private fun saveFavorites(list: List<String>, focusKey: String?) {
        prefs.edit().putString(PREF_FAVORITES, Favorites.serialize(list)).apply()
        favorites = list
        rebuildHome()
        if (showingAll) rebuildAll()
        // Rebuilding detached the focused tile; put focus back on the same app.
        restoreFocus(focusKey)
    }

    private fun editFavorite(app: AppEntry) {
        val key = "fav:${app.pkg}"
        openPanel(
            app.label,
            listOf(
                PanelItem.Header("FAVORITE"),
                PanelItem.Option("Move left", OptionKind.ACTION, onSelect = { closeThen { saveFavorites(Favorites.moveLeft(favorites, app.pkg), key) } }),
                PanelItem.Option("Move right", OptionKind.ACTION, onSelect = { closeThen { saveFavorites(Favorites.moveRight(favorites, app.pkg), key) } }),
                PanelItem.Option("Remove from favorites", OptionKind.ACTION, onSelect = { closeThen { saveFavorites(Favorites.remove(favorites, app.pkg), null) } }),
            ),
            returnKey = key,
        )
    }

    private fun editAllAppsEntry(app: AppEntry) {
        val isFav = app.pkg in favorites
        val key = "app:${app.pkg}"
        openPanel(
            app.label,
            listOf(
                PanelItem.Header("FAVORITES"),
                PanelItem.Option(if (isFav) "Remove from favorites" else "Add to favorites", OptionKind.ACTION, onSelect = {
                    closeThen {
                        val updated = if (isFav) Favorites.remove(favorites, app.pkg) else Favorites.add(favorites, app.pkg)
                        saveFavorites(updated, key)
                        toast(if (isFav) "Removed from favorites" else "Added to favorites")
                    }
                }),
            ),
            returnKey = key,
        )
    }
}
