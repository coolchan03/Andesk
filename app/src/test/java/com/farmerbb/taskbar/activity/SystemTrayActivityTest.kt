package com.farmerbb.taskbar.activity

import android.view.View
import android.view.ViewGroup
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.util.Constants
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

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
    fun testDateExpandsCompactCalendar() {
        val activity = Robolectric.buildActivity(SystemTrayActivity::class.java).setup().get()
        val calendar = activity.findViewById<View>(R.id.control_center_calendar)
        Assert.assertNotNull("Control Center must offer a compact calendar", calendar)
        Assert.assertEquals(View.GONE, calendar.visibility)

        val parent = calendar.parent as ViewGroup
        val date = parent.getChildAt(parent.indexOfChild(calendar) - 1)
        date.performClick()
        Assert.assertEquals(View.VISIBLE, calendar.visibility)
        date.performClick()
        Assert.assertEquals(View.GONE, calendar.visibility)
    }
}
