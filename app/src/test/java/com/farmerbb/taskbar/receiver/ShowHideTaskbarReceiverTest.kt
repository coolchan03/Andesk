package com.farmerbb.taskbar.receiver

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.Constants.UNSUPPORTED
import com.farmerbb.taskbar.helper.LauncherHelper
import com.farmerbb.taskbar.service.TaskbarService
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
class ShowHideTaskbarReceiverTest {
    private lateinit var showHideTaskbarReceiver: ShowHideTaskbarReceiver
    private lateinit var application: Application
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var taskbarIntent: Intent

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        taskbarIntent = Intent(context, TaskbarService::class.java)
        prefs = U.getSharedPreferences(context)
        prefs.edit()
            .putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true)
            .apply()
        LauncherHelper.getInstance().setDesktopLauncherOpen(true)
        showHideTaskbarReceiver = ShowHideTaskbarReceiver()
    }

    @After
    fun tearDown() {
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
        prefs.edit()
            .remove(Constants.PREF_DESKTOP_SESSION_ACTIVE)
            .remove(Constants.PREF_TASKBAR_ACTIVE)
            .apply()
    }

    @Test
    fun testSkipShowHideTaskbar() {
        ShadowSettings.setCanDrawOverlays(true)
        Shadows.shadowOf(application).clearStartedServices()
        prefs.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, false).apply()
        showHideTaskbarReceiver.onReceive(context, null)
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
        val intent = Intent()
        showHideTaskbarReceiver.onReceive(context, intent)
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
        intent.action = Constants.ACTION_SHOW_HIDE_TASKBAR + UNSUPPORTED
        showHideTaskbarReceiver.onReceive(context, intent)
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
        intent.action = Constants.ACTION_SHOW_HIDE_TASKBAR
        showHideTaskbarReceiver.onReceive(context, intent)
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
        prefs.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, true).apply()
        showHideTaskbarReceiver.onReceive(context, intent)
        val startedServiceIntent = Shadows.shadowOf(application).peekNextStartedService()
        Assert.assertNotNull(startedServiceIntent)
        Assert.assertEquals(taskbarIntent.component, startedServiceIntent.component)
    }

    @Test
    fun testStaleDesktopSessionCannotRestartTaskbar() {
        val intent = Intent(Constants.ACTION_SHOW_HIDE_TASKBAR)
        prefs.edit()
            .putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true)
            .putBoolean(Constants.PREF_TASKBAR_ACTIVE, true)
            .apply()
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
        Shadows.shadowOf(application).clearStartedServices()

        showHideTaskbarReceiver.onReceive(context, intent)

        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true))
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_TASKBAR_ACTIVE, true))
    }

    @Test
    fun testShowHideCompatibilityActionKeepsDesktopVisible() {
        val intent = Intent(Constants.ACTION_SHOW_HIDE_TASKBAR)
        ShadowSettings.setCanDrawOverlays(true)
        prefs.edit()
            .putBoolean(Constants.PREF_TASKBAR_ACTIVE, true)
            .putBoolean(Constants.PREF_IS_HIDDEN, true)
            .putBoolean(Constants.PREF_COLLAPSED, true)
            .apply()
        Shadows.shadowOf(application).clearStartedServices()

        showHideTaskbarReceiver.onReceive(context, intent)

        val startedServiceIntent = Shadows.shadowOf(application).peekNextStartedService()
        Assert.assertNotNull(startedServiceIntent)
        Assert.assertEquals(taskbarIntent.component, startedServiceIntent.component)
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_IS_HIDDEN, true))
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_COLLAPSED, true))
    }
}
