package com.farmerbb.taskbar.activity

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class SystemTrayActivityTest {
    @Test
    fun testExitDesktopIntentUsesCentralizedShutdown() {
        val activity = Robolectric.buildActivity(SystemTrayActivity::class.java).get()
        val intent = activity.exitDesktopIntent

        Assert.assertEquals(Constants.ACTION_QUIT, intent.action)
        Assert.assertEquals(activity.packageName, intent.getPackage())
        Assert.assertTrue(intent.getBooleanExtra(Constants.EXTRA_DESKTOP_SESSION, false))
    }

    @Test
    fun testChevronExpandsCompactCalendar() {
        val activity = Robolectric.buildActivity(SystemTrayActivity::class.java).setup().get()
        val calendar = activity.findViewById<View>(R.id.control_center_calendar)
        val toggle = activity.findViewById<View>(R.id.control_center_calendar_toggle)
        Assert.assertNotNull("Control Center must offer a compact calendar", calendar)
        Assert.assertEquals(View.GONE, calendar.visibility)

        toggle.performClick()
        Assert.assertEquals(View.VISIBLE, calendar.visibility)
        toggle.performClick()
        Assert.assertEquals(View.GONE, calendar.visibility)
    }

    @Test
    fun testTappingDateOpensCalendarApp() {
        val activity = Robolectric.buildActivity(SystemTrayActivity::class.java).setup().get()
        val calendarApp = ComponentName("com.example.calendar", "com.example.calendar.Main")
        val packageManager = Shadows.shadowOf(activity.packageManager)
        packageManager.addActivityIfNotPresent(calendarApp)
        packageManager.addIntentFilterForActivity(
            calendarApp,
            IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme("content")
                addDataAuthority("com.android.calendar", null)
            },
        )

        val toggle = activity.findViewById<View>(R.id.control_center_calendar_toggle)
        val row = toggle.parent as ViewGroup
        row.getChildAt(0).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))

        val application = ApplicationProvider.getApplicationContext<Application>()
        val started = Shadows.shadowOf(application).nextStartedActivity
        Assert.assertNotNull("Tapping the date must open a calendar app", started)
        Assert.assertEquals(
            "The calendar must launch like a taskbar app (so it can be a freeform window)",
            calendarApp,
            started.component,
        )
    }

    @Test
    fun testSettingsTileOpensSettingsLikeATaskbarApp() {
        val activity = Robolectric.buildActivity(SystemTrayActivity::class.java).setup().get()
        val tiles = ArrayList<View>()
        activity.window.decorView.findViewsWithText(
            tiles,
            "Settings",
            View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION,
        )
        Assert.assertEquals(1, tiles.size.toLong())

        tiles[0].performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))

        val application = ApplicationProvider.getApplicationContext<Application>()
        val started = Shadows.shadowOf(application).nextStartedActivity
        Assert.assertNotNull("Settings must open via the normal app launch path", started)
        Assert.assertEquals(MainActivity::class.java.name, started.component?.className)
    }

    @Test
    fun testVolumeSliderChangesMediaVolume() {
        val activity = Robolectric.buildActivity(SystemTrayActivity::class.java).setup().get()
        val slider = activity.findViewById<SeekBar>(R.id.control_center_volume_slider)
        Assert.assertNotNull("Control Center must have a volume slider", slider)
        Assert.assertTrue(slider.max > 0)

        slider.progress = slider.max
        val audio = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Assert.assertEquals(
            slider.max.toLong(),
            audio.getStreamVolume(AudioManager.STREAM_MUSIC).toLong(),
        )
    }

    @Test
    fun testBrightnessSliderIsPresent() {
        val activity = Robolectric.buildActivity(SystemTrayActivity::class.java).setup().get()

        val slider = activity.findViewById<SeekBar>(R.id.control_center_brightness_slider)
        Assert.assertNotNull("Control Center must have a brightness slider", slider)
        Assert.assertEquals(254, slider.max.toLong())
    }

    private fun tile(activity: SystemTrayActivity, description: String): View {
        val tiles = ArrayList<View>()
        activity.window.decorView.findViewsWithText(
            tiles,
            description,
            View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION,
        )
        Assert.assertTrue("No tile for $description", tiles.isNotEmpty())
        return tiles[0]
    }

    @Test
    fun testFullScreenTileTogglesTheHideStatusBarSetting() {
        val pref = U.getSharedPreferences(ApplicationProvider.getApplicationContext())
        pref.edit().remove(Constants.PREF_HIDE_STATUS_BAR).apply()
        val activity = Robolectric.buildActivity(SystemTrayActivity::class.java).setup().get()

        tile(activity, "Full screen").performClick()

        Assert.assertTrue(pref.getBoolean(Constants.PREF_HIDE_STATUS_BAR, false))
        pref.edit().remove(Constants.PREF_HIDE_STATUS_BAR).apply()
    }

    @Test
    fun testLockLayoutTileTogglesTheDesktopLock() {
        val pref = U.getSharedPreferences(ApplicationProvider.getApplicationContext())
        pref.edit().remove(Constants.PREF_DESKTOP_LOCKED).apply()
        val activity = Robolectric.buildActivity(SystemTrayActivity::class.java).setup().get()

        tile(activity, "Lock layout").performClick()

        Assert.assertTrue(pref.getBoolean(Constants.PREF_DESKTOP_LOCKED, false))
        pref.edit().remove(Constants.PREF_DESKTOP_LOCKED).apply()
    }

    @Test
    fun testCloseAppsTileIsOffered() {
        val activity = Robolectric.buildActivity(SystemTrayActivity::class.java).setup().get()

        Assert.assertNotNull(tile(activity, "Close apps"))
    }
}
