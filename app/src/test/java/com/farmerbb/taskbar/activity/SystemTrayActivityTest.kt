package com.farmerbb.taskbar.activity

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
}
