package com.farmerbb.taskbar.activity

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.util.Constants
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

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
        val toggle = activity.findViewById<View>(R.id.control_center_calendar_toggle)
        val row = toggle.parent as ViewGroup
        row.getChildAt(0).performClick()

        val started = Shadows.shadowOf(activity).nextStartedActivity
        Assert.assertNotNull("Tapping the date must open a calendar app", started)
        Assert.assertTrue(
            started.action == Intent.ACTION_VIEW ||
                started.selector?.hasCategory(Intent.CATEGORY_APP_CALENDAR) == true,
        )
    }
}
