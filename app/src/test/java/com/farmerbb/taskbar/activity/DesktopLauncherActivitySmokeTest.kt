package com.farmerbb.taskbar.activity

import android.content.Context
import android.os.Build
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.helper.GlobalHelper
import com.farmerbb.taskbar.helper.LauncherHelper
import com.farmerbb.taskbar.util.Constants.PREF_DESKTOP_SESSION_ACTIVE
import com.farmerbb.taskbar.util.Constants.PREF_DESKTOP_WIDGETS
import com.farmerbb.taskbar.util.Constants.PREF_FIRST_RUN
import com.farmerbb.taskbar.util.Constants.PREF_TASKBAR_ACTIVE
import com.farmerbb.taskbar.util.DesktopSessionState
import com.farmerbb.taskbar.util.TaskbarPosition
import com.farmerbb.taskbar.util.U
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowSettings
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class DesktopLauncherActivitySmokeTest {
    @Test
    fun desktopShowsTaskbarWithoutExternalOverlayPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pref = U.getSharedPreferences(context)
        val priorRotation = ReflectionHelpers.getStaticField<Int?>(
            TaskbarPosition::class.java,
            "cachedRotation",
        )
        pref.edit()
            .putBoolean(PREF_FIRST_RUN, false)
            .remove(PREF_DESKTOP_WIDGETS)
            .remove(PREF_DESKTOP_SESSION_ACTIVE)
            .apply()
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
        LauncherHelper.getInstance().setOnPrimaryHomeScreen(false)
        LauncherHelper.getInstance().setOnSecondaryHomeScreen(false, -1)
        ShadowSettings.setCanDrawOverlays(false)

        val controller = Robolectric.buildActivity(DesktopLauncherActivity::class.java)
        try {
            val activity = controller.create().start().resume().visible().get()
            val startButton = activity.findViewById<View>(R.id.start_button)
            val appArea = activity.findViewById<View>(R.id.taskbar_scrollview)
            val alert = ShadowAlertDialog.getLatestAlertDialog()
            val alertMessage = if (alert == null) "" else Shadows.shadowOf(alert).message

            Assert.assertNotNull(
                "Desktop taskbar missing: session=" + DesktopSessionState.isActive(activity) +
                    " taskbarActive=" + pref.getBoolean(PREF_TASKBAR_ACTIVE, false) +
                    " secondary=" + LauncherHelper.getInstance().isOnSecondaryHomeScreen(activity) +
                    " dialog=" + alertMessage,
                startButton,
            )
            Assert.assertNotNull("The desktop must contain its own taskbar", appArea)
            Assert.assertEquals(View.VISIBLE, startButton.visibility)
            Assert.assertTrue(
                "The taskbar must not remain collapsed",
                appArea.visibility != View.GONE,
            )
        } finally {
            controller.pause().stop().destroy()
            LauncherHelper.getInstance().setDesktopLauncherOpen(false)
            LauncherHelper.getInstance().setOnPrimaryHomeScreen(false)
            LauncherHelper.getInstance().setOnSecondaryHomeScreen(false, -1)
            ReflectionHelpers.setStaticField(
                TaskbarPosition::class.java,
                "cachedRotation",
                priorRotation,
            )
            GlobalHelper.getInstance().setReflectionAllowed(
                Build.VERSION.SDK_INT < Build.VERSION_CODES.P,
            )
            pref.edit().clear().apply()
            ShadowSettings.setCanDrawOverlays(true)
        }
    }
}
