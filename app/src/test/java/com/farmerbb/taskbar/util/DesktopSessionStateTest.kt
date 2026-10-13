package com.farmerbb.taskbar.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.helper.LauncherHelper
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DesktopSessionStateTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        U.getSharedPreferences(context).edit()
                .remove(Constants.PREF_DESKTOP_SESSION_ACTIVE)
                .remove(Constants.PREF_TASKBAR_ACTIVE)
                .commit()
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
    }

    @After
    fun tearDown() {
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
    }

    @Test
    fun resumeBackgroundAndCloseKeepOverlayStateConsistent() {
        val prefs = U.getSharedPreferences(context)
        DesktopSessionState.foreground(context)
        assertTrue(prefs.getBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, false))
        assertTrue(prefs.getBoolean(Constants.PREF_TASKBAR_ACTIVE, false))
        DesktopSessionState.background(context)
        assertTrue(prefs.getBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, false))
        assertFalse(prefs.getBoolean(Constants.PREF_TASKBAR_ACTIVE, true))

        DesktopSessionState.foreground(context)
        assertTrue(prefs.getBoolean(Constants.PREF_TASKBAR_ACTIVE, false))

        DesktopSessionState.close(context)
        assertFalse(prefs.getBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true))
        assertFalse(prefs.getBoolean(Constants.PREF_TASKBAR_ACTIVE, true))
    }

    @Test
    fun activeRequiresLauncherAndSessionFlag() {
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true).commit()
        assertFalse(DesktopSessionState.isActive(context))

        LauncherHelper.getInstance().setDesktopLauncherOpen(true)
        assertTrue(DesktopSessionState.isActive(context))

        prefs.edit().putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, false).commit()
        assertFalse(DesktopSessionState.isActive(context))
    }

    @Test
    fun staleSessionPreferenceDoesNotMakeDesktopActiveWithoutOwner() {
        val prefs = U.getSharedPreferences(context)
        prefs.edit()
                .putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true)
                .putBoolean(Constants.PREF_TASKBAR_ACTIVE, true)
                .commit()

        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
        assertFalse(DesktopSessionState.isActive(context))

        DesktopSessionState.close(context)
        assertFalse(prefs.getBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true))
        assertFalse(prefs.getBoolean(Constants.PREF_TASKBAR_ACTIVE, true))
    }

    @Test
    fun backgroundDoesNotDisableUnrelatedTaskbarSession() {
        val pref = U.getSharedPreferences(context)
        pref.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, true).commit()
        DesktopSessionState.background(context)
        assertTrue(pref.getBoolean(Constants.PREF_TASKBAR_ACTIVE, false))
    }
}
