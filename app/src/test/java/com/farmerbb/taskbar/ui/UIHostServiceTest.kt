package com.farmerbb.taskbar.ui

import android.app.Service
import android.content.res.Configuration
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

    @Before
    fun setUp() {
        controller = Robolectric.buildService(TestUIHostService::class.java)
        hostService = controller.create().get()
        uiController = hostService.controller
    }

    @After
    fun tearDown() {
        uiController.onCreateHost = null
        uiController.onRecreateHost = null
        uiController.onDestroyHost = null
        U.getSharedPreferences(hostService).edit()
                .remove(Constants.PREF_DESKTOP_SESSION_ACTIVE)
                .apply()
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
    fun testDesktopSessionIsNotSticky() {
        val prefs = U.getSharedPreferences(hostService)

        prefs.edit().putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, false).apply()
        Assert.assertEquals(Service.START_STICKY, hostService.onStartCommand(null, 0, 0))

        prefs.edit().putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true).apply()
        Assert.assertEquals(Service.START_NOT_STICKY, hostService.onStartCommand(null, 0, 0))
    }

    @Test
    fun testTerminate() {
        val shadowService = Shadows.shadowOf(hostService)
        Assert.assertFalse(shadowService.isStoppedBySelf)
        hostService.terminate()
        Assert.assertTrue(shadowService.isStoppedBySelf)
    }
}
