package com.farmerbb.taskbar.ui

import android.app.Service
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.helper.LauncherHelper
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.android.controller.ServiceController

@RunWith(RobolectricTestRunner::class)
class UIHostServiceTest {
    private lateinit var controller: ServiceController<TestUIHostService>
    private lateinit var hostService: TestUIHostService
    private lateinit var uiController: TestUIController
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        U.getSharedPreferences(context).edit()
                .putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true)
                .apply()
        LauncherHelper.getInstance().setDesktopLauncherOpen(true)

        controller = Robolectric.buildService(TestUIHostService::class.java)
        hostService = controller.create().get()
        uiController = hostService.controller
    }

    @After
    fun tearDown() {
        uiController.onCreateHost = null
        uiController.onRecreateHost = null
        uiController.onDestroyHost = null
        U.getSharedPreferences(context).edit()
                .remove(Constants.PREF_DESKTOP_SESSION_ACTIVE)
                .apply()
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
    }

    @Test
    fun testOnCreate() {
        Assert.assertEquals(uiController.onCreateHost, hostService)
    }

    @Test
    fun testOnConfigurationChanged() {
        Assert.assertNull(uiController.onRecreateHost)

        val newConfig = Configuration().apply {
            smallestScreenWidthDp = 123
            screenHeightDp = 456
            screenWidthDp = 789
        }

        val config = uiController.context.resources.configuration.apply {
            updateFrom(newConfig)
        }

        hostService.onConfigurationChanged(config)
        Assert.assertEquals(hostService, uiController.onRecreateHost)
    }

    @Test
    fun testOnDestroy() {
        Assert.assertNull(uiController.onDestroyHost)
        controller.destroy()
        Assert.assertEquals(hostService, uiController.onDestroyHost)
    }

    @Test
    fun testServiceIsNeverSticky() {
        val prefs = U.getSharedPreferences(hostService)

        prefs.edit().putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true).apply()
        Assert.assertEquals(Service.START_NOT_STICKY, hostService.onStartCommand(null, 0, 0))

        prefs.edit().putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, false).apply()
        Assert.assertEquals(Service.START_NOT_STICKY, hostService.onStartCommand(null, 0, 0))
        Assert.assertTrue(Shadows.shadowOf(hostService).isStoppedBySelf)
    }

    @Test
    fun testNoDesktopSessionStopsBeforeCreatingUi() {
        U.getSharedPreferences(context).edit()
                .putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, false)
                .apply()

        val rejectedController = Robolectric.buildService(TestUIHostService::class.java)
        val rejectedService = rejectedController.create().get()

        Assert.assertTrue(Shadows.shadowOf(rejectedService).isStoppedBySelf)
        rejectedController.destroy()
    }

    @Test
    fun testStaleSessionFlagCannotCreateUi() {
        U.getSharedPreferences(context).edit()
                .putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true)
                .putBoolean(Constants.PREF_TASKBAR_ACTIVE, true)
                .apply()
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)

        val rejectedController = Robolectric.buildService(TestUIHostService::class.java)
        val rejectedService = rejectedController.create().get()

        Assert.assertTrue(Shadows.shadowOf(rejectedService).isStoppedBySelf)
        Assert.assertFalse(
                U.getSharedPreferences(context)
                        .getBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true))
        Assert.assertFalse(
                U.getSharedPreferences(context)
                        .getBoolean(Constants.PREF_TASKBAR_ACTIVE, true))
        rejectedController.destroy()
    }

    @Test
    fun testTerminate() {
        val shadowService = Shadows.shadowOf(hostService)
        Assert.assertFalse(shadowService.isStoppedBySelf)
        hostService.terminate()
        Assert.assertTrue(shadowService.isStoppedBySelf)
    }
}
