package com.farmerbb.taskbar.ui

import android.app.Application
import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.activity.DesktopLauncherActivity
import com.farmerbb.taskbar.helper.LauncherHelper
import com.farmerbb.taskbar.util.Constants.PREF_DESKTOP_SESSION_ACTIVE
import com.farmerbb.taskbar.util.Constants.PREF_FIRST_RUN
import com.farmerbb.taskbar.util.DesktopSessionState
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
class ShowDesktopSliverTest {
    private class RecordingHost : UIHost {
        var added: View? = null

        override fun addView(view: View, params: ViewParams) {
            added = view
        }

        override fun removeView(view: View) {}

        override fun terminate() {}

        override fun updateViewLayout(view: View, params: ViewParams) {}
    }

    private lateinit var context: Context
    private val host = RecordingHost()
    private lateinit var controller: TaskbarController

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ShadowSettings.setCanDrawOverlays(true)
        U.getSharedPreferences(context).edit().putBoolean(PREF_FIRST_RUN, false).apply()
        LauncherHelper.getInstance().setDesktopLauncherOpen(true)
        DesktopSessionState.foreground(context)
        controller = TaskbarController(context)
        controller.onCreateHost(host)
    }

    @After
    fun tearDown() {
        controller.onDestroyHost(host)
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
        U.getSharedPreferences(context).edit()
            .remove(PREF_DESKTOP_SESSION_ACTIVE)
            .remove(PREF_FIRST_RUN)
            .apply()
    }

    @Test
    fun testFloatingDesktopTaskbarHasAShowDesktopStrip() {
        val sliver = host.added?.findViewById<View>(R.id.show_desktop_sliver)
        Assert.assertNotNull("The floating taskbar must offer a Show desktop strip", sliver)

        sliver!!.performClick()

        val application = ApplicationProvider.getApplicationContext<Application>()
        val started = Shadows.shadowOf(application).nextStartedActivity
        Assert.assertNotNull(started)
        Assert.assertEquals(DesktopLauncherActivity::class.java.name, started.component?.className)
    }

    @Test
    fun testStripIsHiddenWhileTheTaskbarIsCompact() {
        val sliver = host.added!!.findViewById<View>(R.id.show_desktop_sliver)

        controller.setCompact(true)
        Assert.assertEquals(View.GONE, sliver.visibility)

        controller.setCompact(false)
        Assert.assertEquals(View.VISIBLE, sliver.visibility)
    }
}
