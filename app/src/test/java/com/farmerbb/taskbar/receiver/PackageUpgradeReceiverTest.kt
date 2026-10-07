package com.farmerbb.taskbar.receiver

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class PackageUpgradeReceiverTest {
    private lateinit var receiver: PackageUpgradeReceiver
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var application: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        receiver = PackageUpgradeReceiver()
        prefs = U.getSharedPreferences(context)
        prefs.edit()
                .remove(Constants.PREF_DESKTOP_SESSION_ACTIVE)
                .remove(Constants.PREF_TASKBAR_ACTIVE)
                .apply()
        Shadows.shadowOf(application).clearStartedServices()
        Shadows.shadowOf(application).clearNextStartedActivities()
    }

    @Test
    fun testPackageReplacementClearsDesktopState() {
        prefs.edit()
                .putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true)
                .putBoolean(Constants.PREF_TASKBAR_ACTIVE, true)
                .apply()
        receiver.onReceive(context, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true))
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_TASKBAR_ACTIVE, true))
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedActivity())
    }

    @Test
    fun testLegacyActiveStateIsNotRestarted() {
        prefs.edit()
                .putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, false)
                .putBoolean(Constants.PREF_TASKBAR_ACTIVE, true)
                .apply()
        receiver.onReceive(context, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true))
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_TASKBAR_ACTIVE, true))
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedActivity())
    }

    @Test
    fun testOtherBroadcastIsIgnored() {
        prefs.edit()
                .putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true)
                .putBoolean(Constants.PREF_TASKBAR_ACTIVE, true)
                .apply()
        receiver.onReceive(context, Intent("com.farmerbb.taskbar.UNRELATED"))
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, false))
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_TASKBAR_ACTIVE, false))
    }
}
