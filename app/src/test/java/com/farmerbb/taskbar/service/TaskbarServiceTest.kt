package com.farmerbb.taskbar.service

import android.content.ComponentName
import android.content.Intent
import com.farmerbb.taskbar.activity.DesktopLauncherActivity
import com.farmerbb.taskbar.activity.MainActivity
import org.junit.Assert
import org.junit.Test

class TaskbarServiceTest {
    @Test
    fun testDesktopLauncherTaskDetection() {
        val desktopIntent = Intent().setComponent(
            ComponentName("com.farmerbb.taskbar", DesktopLauncherActivity::class.java.name)
        )
        val settingsIntent = Intent().setComponent(
            ComponentName("com.farmerbb.taskbar", MainActivity::class.java.name)
        )

        Assert.assertTrue(TaskbarService.isDesktopLauncherTask(desktopIntent))
        Assert.assertFalse(TaskbarService.isDesktopLauncherTask(settingsIntent))
        Assert.assertFalse(TaskbarService.isDesktopLauncherTask(null))
    }
}
