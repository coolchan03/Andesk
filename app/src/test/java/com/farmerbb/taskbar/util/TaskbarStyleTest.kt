package com.farmerbb.taskbar.util

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskbarStyleTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        U.getSharedPreferences(context).edit()
            .remove(Constants.PREF_TASKBAR_STYLE)
            .remove(Constants.PREF_TASKBAR_SIZE)
            .remove(Constants.PREF_FULL_LENGTH)
            .remove(Constants.PREF_CENTERED_ICONS)
            .remove(Constants.PREF_BACKGROUND_TINT)
            .remove(Constants.PREF_ACCENT_COLOR)
            .apply()
    }

    @Test
    fun testClassicUsesCustomLayoutAndColors() {
        val background = Color.argb(120, 1, 2, 3)
        val accent = Color.rgb(4, 5, 6)
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_TASKBAR_STYLE, Constants.PREF_TASKBAR_STYLE_CLASSIC)
            .putBoolean(Constants.PREF_FULL_LENGTH, false)
            .putBoolean(Constants.PREF_CENTERED_ICONS, true)
            .putInt(Constants.PREF_BACKGROUND_TINT, background)
            .putInt(Constants.PREF_ACCENT_COLOR, accent)
            .apply()

        Assert.assertFalse(U.isTaskbarFullLength(context))
        Assert.assertTrue(U.isTaskbarCentered(context))
        Assert.assertEquals(background, U.getTaskbarBackgroundColor(context))
        Assert.assertEquals(accent, U.getTaskbarAccentColor(context))
    }

    @Test
    fun testWindows10Preset() {
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_TASKBAR_STYLE, Constants.PREF_TASKBAR_STYLE_WINDOWS_10)
            .putBoolean(Constants.PREF_FULL_LENGTH, false)
            .putBoolean(Constants.PREF_CENTERED_ICONS, true)
            .apply()

        Assert.assertTrue(U.isTaskbarFullLength(context))
        Assert.assertFalse(U.isTaskbarCentered(context))
        Assert.assertEquals(Color.rgb(32, 32, 32), U.getTaskbarBackgroundColor(context))
        Assert.assertEquals(Color.WHITE, U.getTaskbarAccentColor(context))
    }

    @Test
    fun testWindows11Preset() {
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_TASKBAR_STYLE, Constants.PREF_TASKBAR_STYLE_WINDOWS_11)
            .putBoolean(Constants.PREF_FULL_LENGTH, false)
            .putBoolean(Constants.PREF_CENTERED_ICONS, false)
            .apply()

        Assert.assertTrue(U.isTaskbarFullLength(context))
        Assert.assertTrue(U.isTaskbarCentered(context))
        Assert.assertEquals(Color.argb(232, 32, 32, 32), U.getTaskbarBackgroundColor(context))
        Assert.assertEquals(Color.WHITE, U.getTaskbarAccentColor(context))
    }

    @Test
    fun testLegacyWindowsStyleMapsToWindows11() {
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_TASKBAR_STYLE, Constants.PREF_TASKBAR_STYLE_WINDOWS)
            .apply()

        Assert.assertEquals(
            Constants.PREF_TASKBAR_STYLE_WINDOWS_11,
            U.getTaskbarStyle(context),
        )
        Assert.assertTrue(U.isTaskbarCentered(context))
    }

    @Test
    fun testTaskbarSizes() {
        val density = context.resources.displayMetrics.density
        val prefs = U.getSharedPreferences(context)

        prefs.edit().putString(
            Constants.PREF_TASKBAR_SIZE,
            Constants.PREF_TASKBAR_SIZE_COMPACT,
        ).apply()
        Assert.assertEquals(Math.round(48 * density), U.getTaskbarIconSize(context))

        prefs.edit().putString(
            Constants.PREF_TASKBAR_SIZE,
            Constants.PREF_TASKBAR_SIZE_STANDARD,
        ).apply()
        Assert.assertEquals(Math.round(60 * density), U.getTaskbarIconSize(context))

        prefs.edit().putString(
            Constants.PREF_TASKBAR_SIZE,
            Constants.PREF_TASKBAR_SIZE_LARGE,
        ).apply()
        Assert.assertEquals(Math.round(72 * density), U.getTaskbarIconSize(context))
    }

    @Test
    fun testMacosDockIsAFloatingCenteredRoundedDock() {
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_TASKBAR_STYLE, Constants.PREF_TASKBAR_STYLE_MACOS)
            .putBoolean(Constants.PREF_FULL_LENGTH, true)
            .apply()

        Assert.assertTrue(U.isDockTaskbarStyle(context))
        Assert.assertFalse("A dock only spans its icons", U.isTaskbarFullLength(context))
        Assert.assertTrue(U.isTaskbarCentered(context))
        Assert.assertTrue(U.getTaskbarCornerRadius(context) > 0)
        Assert.assertTrue(U.getTaskbarExtraThickness(context) > 0)
        Assert.assertTrue(
            "The macOS dock is translucent",
            Color.alpha(U.getTaskbarBackgroundColor(context)) in 1..254,
        )
    }

    @Test
    fun testGnomeDockIsTranslucentAndDark() {
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_TASKBAR_STYLE, Constants.PREF_TASKBAR_STYLE_GNOME)
            .apply()

        Assert.assertTrue(U.isDockTaskbarStyle(context))
        Assert.assertTrue(U.isDarkBackground(U.getTaskbarBackgroundColor(context)))
        Assert.assertEquals(Color.WHITE, U.getTaskbarAccentColor(context))
    }

    @Test
    fun testChromeOsShelfAndKdePanelAreFullLengthWithoutADock() {
        for (style in listOf(
            Constants.PREF_TASKBAR_STYLE_CHROMEOS,
            Constants.PREF_TASKBAR_STYLE_KDE,
        )) {
            U.getSharedPreferences(context).edit()
                .putString(Constants.PREF_TASKBAR_STYLE, style)
                .putBoolean(Constants.PREF_FULL_LENGTH, false)
                .apply()

            Assert.assertFalse(U.isDockTaskbarStyle(context))
            Assert.assertTrue(style, U.isTaskbarFullLength(context))
            Assert.assertEquals(0, U.getTaskbarExtraThickness(context).toLong())
        }

        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_TASKBAR_STYLE, Constants.PREF_TASKBAR_STYLE_CHROMEOS)
            .apply()
        Assert.assertTrue(U.isTaskbarCentered(context))

        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_TASKBAR_STYLE, Constants.PREF_TASKBAR_STYLE_KDE)
            .apply()
        Assert.assertFalse(U.isTaskbarCentered(context))
    }

    @Test
    fun testExplicitColorsStillOverrideNewPresets() {
        val background = Color.argb(99, 9, 8, 7)
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_TASKBAR_STYLE, Constants.PREF_TASKBAR_STYLE_MACOS)
            .putInt(Constants.PREF_BACKGROUND_TINT, background)
            .apply()

        Assert.assertEquals(background, U.getTaskbarBackgroundColor(context))
    }
}
