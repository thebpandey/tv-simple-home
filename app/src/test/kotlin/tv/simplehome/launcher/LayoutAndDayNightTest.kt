package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutAndDayNightTest {
    @Test
    fun `header, centred quote and dock fit the 486dp safe height even with a 3-line banner`() {
        assertEquals(486, LayoutBudget.SAFE_HEIGHT_DP)
        assertEquals(0.88f, LayoutBudget.DOCK_SCALE, 0f)
        assertTrue(LayoutBudget.homeFits())
        assertTrue(LayoutBudget.header(true) + LayoutBudget.QUOTE_MIN_DP + LayoutBudget.DOCK_DP <= LayoutBudget.SAFE_HEIGHT_DP)
    }

    @Test
    fun `without a banner the centre only grows, so collapsing the banner can never clip the quote`() {
        assertTrue(LayoutBudget.centerDp(false) > LayoutBudget.centerDp(true))
        assertTrue(LayoutBudget.centerDp(true) >= LayoutBudget.QUOTE_MIN_DP)
    }

    @Test
    fun `each dock style reserves room above the tiles for the magnification, the lift and the label so nothing clips`() {
        for (spec in listOf(LayoutBudget.ICONS_SPEC, LayoutBudget.BANNERS_SPEC)) {
            assertTrue(spec.topPadDp >= spec.overflowUpDp + 4f)
            assertTrue(spec.sidePadDp >= spec.overflowSideDp + 4f)
            assertTrue("own dock height ${spec.ownDockDp} must fit the shared ${LayoutBudget.DOCK_DP}", spec.ownDockDp <= LayoutBudget.DOCK_DP + 0.01f)
            assertTrue(spec.shelfPadDp + spec.bottomGapDp >= 8f)
        }
    }

    @Test
    fun `All apps shows at least three full rows and the 6 column grid fits inside the safe width`() {
        assertTrue(LayoutBudget.allAppsRegionDp() >= 3 * LayoutBudget.ALL_ROW_DP)
        assertTrue(LayoutBudget.allGridWidthDp() <= 960 - 96)
    }

    @Test
    fun `a focused Launchpad icon plus its shadow never reaches the cell edge so nothing is cropped`() {
        val icon = LayoutBudget.ALL_ICON_DP.toFloat()
        assertTrue("cell padding must be at least 12% of the icon", LayoutBudget.ALL_CELL_PAD_DP >= 0.12f * icon)
        val growth = LayoutBudget.focusOverflowDp(icon, LayoutBudget.ALL_FOCUS_SCALE)
        assertTrue(LayoutBudget.ALL_CELL_PAD_DP >= growth + 4f)
        // icon + label + gap must leave the vertical padding on top and bottom of a 150dp cell
        assertTrue(icon + 10f + 20f + 2 * LayoutBudget.ALL_CELL_PAD_DP <= LayoutBudget.ALL_ROW_DP)
        assertTrue(LayoutBudget.ALL_CELL_W_DP >= icon + 2 * LayoutBudget.ALL_CELL_PAD_DP)
    }

    @Test
    fun `the weather widget with its air quality line still fits inside the 64dp top bar`() {
        assertTrue(LayoutBudget.WEATHER_WIDGET_DP <= LayoutBudget.TOP_BAR_DP)
    }

    @Test
    fun `a sunrise or sunset crossfade is 30 seconds but a scene rotation stays 3 seconds`() {
        val n = WallpaperSchedule.withDay(WallpaperSchedule.festivalSceneKey("diwali", 0)!!, false)
        val d = WallpaperSchedule.withDay(WallpaperSchedule.festivalSceneKey("diwali", 0)!!, true)
        assertEquals(WallpaperSchedule.DAY_NIGHT_FADE_MS, WallpaperSchedule.fadeDurationMs(n, d))
        assertEquals(WallpaperSchedule.FADE_MS, WallpaperSchedule.fadeDurationMs(0, 10))
    }

    @Test
    fun `day and night variants of the same scene are different cache keys so only the active one is held`() {
        assertTrue(WallpaperSchedule.isDayKey(WallpaperSchedule.withDay(30, true)))
        assertFalse(WallpaperSchedule.isDayKey(30))
        assertEquals(30, WallpaperSchedule.baseKey(WallpaperSchedule.withDay(30, true)))
        assertTrue(WallpaperSchedule.withDay(30, true) < WallpaperSchedule.KEY_SPACE)
    }

    @Test
    fun `the festival override and scene rotation do not depend on day or night`() {
        val key = WallpaperSchedule.festivalSceneKey("holi", 0)
        assertEquals(key, WallpaperSchedule.keyAt(WallpaperMode.Auto, 0, key))
        assertEquals(2 * WallpaperSchedule.KEY_MULT, WallpaperSchedule.keyAt(WallpaperMode.Pinned(2), 0, key))
    }

    @Test
    fun `day-night mode Auto follows the sun while Always day and Always night ignore it`() {
        assertTrue(DayNightMode.AUTO.resolve(true))
        assertFalse(DayNightMode.AUTO.resolve(false))
        assertTrue(DayNightMode.DAY.resolve(false))
        assertFalse(DayNightMode.NIGHT.resolve(true))
    }

    @Test
    fun `an unknown stored day-night value falls back to Auto, the default`() {
        assertEquals(DayNightMode.AUTO, DayNightMode.parse(null))
        assertEquals(DayNightMode.AUTO, DayNightMode.parse("sunset"))
        assertEquals(DayNightMode.NIGHT, DayNightMode.parse("night"))
    }

    @Test
    fun `the glass header cards fit their rows, the weather card stays within its keep-out height, the banner card within its slot`() {
        val clockLine = 56f * 1.21f // Inter's line height at 56sp
        assertTrue(clockLine + 2 * LayoutBudget.CARD_PAD_V_DP <= LayoutBudget.TOP_BAR_DP)
        assertTrue(LayoutBudget.WEATHER_WIDGET_DP <= LayoutBudget.WEATHER_CARD_H_DP)
        assertTrue(LayoutBudget.WEATHER_CARD_H_DP <= LayoutBudget.TOP_BAR_DP)
        val bannerWorst = 24 + 17 + 16 + 2 * 12 + 8 // 20sp name + 14sp date + 13sp note + card padding + gap
        assertTrue(bannerWorst <= LayoutBudget.BANNER_MAX_DP)
        assertTrue("quote keeps its 140dp", LayoutBudget.homeFits())
    }
}
