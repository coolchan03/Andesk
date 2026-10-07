package com.farmerbb.taskbar.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.helper.LauncherHelper
import com.farmerbb.taskbar.ui.DashboardController
import com.farmerbb.taskbar.ui.StartMenuController
import com.farmerbb.taskbar.ui.TaskbarController
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UIHostServiceNewControllerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        U.getSharedPreferences(context).edit()
                .putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true)
                .apply()
        LauncherHelper.getInstance().setDesktopLauncherOpen(true)
    }

    @After
    fun tearDown() {
        U.getSharedPreferences(context).edit()
                .remove(Constants.PREF_DESKTOP_SESSION_ACTIVE)
                .apply()
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
    }

    @Test
    fun testDashboardService() {
        val dashboardService = Robolectric.setupService(DashboardService::class.java)
        Assert.assertTrue(dashboardService.newController() is DashboardController)
    }

    @Test
    fun testStartMenuService() {
        val startMenuService = Robolectric.setupService(StartMenuService::class.java)
        Assert.assertTrue(startMenuService.newController() is StartMenuController)
    }

    @Test
    fun testTaskbarService() {
        val taskbarService = Robolectric.setupService(TaskbarService::class.java)
        Assert.assertTrue(taskbarService.newController() is TaskbarController)
    }
}
