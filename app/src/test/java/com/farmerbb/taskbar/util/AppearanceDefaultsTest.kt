package com.farmerbb.taskbar.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppearanceDefaultsTest {
    @Test
    fun testResetRemovesAppearanceSettingsOnly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pref = U.getSharedPreferences(context)
        pref.edit()
            .putString(Constants.PREF_TASKBAR_STYLE, "macos")
            .putString(Constants.PREF_TASKBAR_TEXTURE, "dots")
            .putString(Constants.PREF_POSITION, "top_right")
            .putBoolean(Constants.PREF_SHORTCUT_ICON, false)
            .putBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, true)
            .commit()

        AppearanceDefaults.reset(context)

        Assert.assertFalse(pref.contains(Constants.PREF_TASKBAR_STYLE))
        Assert.assertFalse(pref.contains(Constants.PREF_TASKBAR_TEXTURE))
        Assert.assertFalse(pref.contains(Constants.PREF_POSITION))
        Assert.assertFalse(pref.contains(Constants.PREF_SHORTCUT_ICON))
        Assert.assertTrue(pref.getBoolean(Constants.PREF_DESKTOP_SESSION_ACTIVE, false))
    }
}
