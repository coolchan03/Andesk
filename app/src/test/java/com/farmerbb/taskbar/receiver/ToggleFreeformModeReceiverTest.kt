package com.farmerbb.taskbar.receiver

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.helper.LauncherHelper
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class ToggleFreeformModeReceiverTest {
    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var receiver: ToggleFreeformModeReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        receiver = ToggleFreeformModeReceiver()
        U.getSharedPreferences(context).edit()
                .putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true)
                .putBoolean(Constants.PREF_TASKBAR_ACTIVE, true)
                .apply()
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
        Shadows.shadowOf(application).clearStartedServices()
        Shadows.shadowOf(application).clearNextStartedActivities()
    }

    @After
    fun tearDown() {
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
        U.getSharedPreferences(context).edit()
                .remove(Constants.PREF_DESKTOP_SESSION_ACTIVE)
                .remove(Constants.PREF_TASKBAR_ACTIVE)
                .apply()
    }

    @Test
    fun testStaleSessionCannotToggleFreeform() {
        receiver.onReceive(context, Intent(Constants.ACTION_TOGGLE_FREEFORM_MODE))

        Assert.assertFalse(
                U.getSharedPreferences(context)
                        .getBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true))
        Assert.assertFalse(
                U.getSharedPreferences(context)
                        .getBoolean(Constants.PREF_TASKBAR_ACTIVE, true))
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedActivity())
    }

    @Test
    fun testUnrelatedBroadcastIsIgnored() {
        receiver.onReceive(context, Intent("com.farmerbb.taskbar.UNRELATED"))

        Assert.assertTrue(
                U.getSharedPreferences(context)
                        .getBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, false))
        Assert.assertTrue(
                U.getSharedPreferences(context)
                        .getBoolean(Constants.PREF_TASKBAR_ACTIVE, false))
    }
}
