package com.farmerbb.taskbar.activity

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.util.Constants
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
        Assert.assertEquals(Intent.ACTION_VIEW, started.action)
    }
}
