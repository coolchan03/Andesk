package com.farmerbb.taskbar.receiver

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.activity.DesktopLauncherActivity
import com.farmerbb.taskbar.util.Constants
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
class StartReceiverTest {
    private lateinit var receiver: StartReceiver
    private lateinit var application: Application
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        receiver = StartReceiver()
        Shadows.shadowOf(application).clearStartedServices()
    }

    @Test
    fun testStartLaunchesDesktopOwner() {
        ShadowSettings.setCanDrawOverlays(true)

        receiver.onReceive(context, Intent(Constants.ACTION_START))

        val activityIntent = Shadows.shadowOf(application).peekNextStartedActivity()
        Assert.assertNotNull(activityIntent)
        Assert.assertEquals(
                DesktopLauncherActivity::class.java.name,
                activityIntent.component?.className)
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
    }

    @Test
    fun testOtherActionsAreIgnored() {
        ShadowSettings.setCanDrawOverlays(true)

        receiver.onReceive(context, Intent("com.farmerbb.taskbar.UNSUPPORTED"))

        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedActivity())
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
    }
}
