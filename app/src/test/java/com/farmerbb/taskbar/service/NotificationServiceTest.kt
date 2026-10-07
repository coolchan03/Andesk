package com.farmerbb.taskbar.service

import android.app.Service
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.helper.LauncherHelper
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class NotificationServiceTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        U.getSharedPreferences(context).edit()
                .putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true)
                .putBoolean(Constants.PREF_TASKBAR_ACTIVE, true)
                .apply()
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
    }

    @Test
    fun testStaleSessionCannotStartNotificationService() {
        val controller = Robolectric.buildService(NotificationService::class.java)
        val service = controller.create().get()

        Assert.assertTrue(Shadows.shadowOf(service).isStoppedBySelf)
        Assert.assertFalse(
                U.getSharedPreferences(context)
                        .getBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true))
        Assert.assertFalse(
                U.getSharedPreferences(context)
                        .getBoolean(Constants.PREF_TASKBAR_ACTIVE, true))
        Assert.assertEquals(
                Service.START_NOT_STICKY,
                service.onStartCommand(null, 0, 0))

        controller.destroy()
    }
}
