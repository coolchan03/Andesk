package com.farmerbb.taskbar.activity

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.helper.LauncherHelper
import com.farmerbb.taskbar.util.Constants.PREF_DESKTOP_SESSION_ACTIVE
import com.farmerbb.taskbar.util.Constants.PREF_DESKTOP_WIDGETS
import com.farmerbb.taskbar.util.Constants.PREF_FIRST_RUN
import com.farmerbb.taskbar.util.U
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
class DesktopLauncherActivitySmokeTest {
    @Test
    fun desktopShowsTaskbarWithoutExternalOverlayPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pref = U.getSharedPreferences(context)
        pref.edit()
            .putBoolean(PREF_FIRST_RUN, false)
            .remove(PREF_DESKTOP_WIDGETS)
            .remove(PREF_DESKTOP_SESSION_ACTIVE)
            .apply()
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
        ShadowSettings.setCanDrawOverlays(false)

        val controller = Robolectric.buildActivity(DesktopLauncherActivity::class.java)
        try {
            val activity = controller.create().start().resume().visible().get()
            val startButton = activity.findViewById<View>(R.id.start_button)
            val appArea = activity.findViewById<View>(R.id.taskbar_scrollview)

            Assert.assertNotNull("The desktop must contain its own Start button", startButton)
            Assert.assertNotNull("The desktop must contain its own taskbar", appArea)
            Assert.assertEquals(View.VISIBLE, startButton.visibility)
            Assert.assertTrue(
                "The taskbar must not remain collapsed",
                appArea.visibility != View.GONE,
            )
        } finally {
            controller.pause().stop().destroy()
            LauncherHelper.getInstance().setDesktopLauncherOpen(false)
            ShadowSettings.setCanDrawOverlays(true)
        }
    }
}
