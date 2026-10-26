package com.farmerbb.taskbar.activity

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.helper.GlobalHelper
import com.farmerbb.taskbar.helper.LauncherHelper
import com.farmerbb.taskbar.util.Constants.ACTION_DESKTOP_TASKBAR_COMPACT
import com.farmerbb.taskbar.util.Constants.ACTION_DESKTOP_TASKBAR_EXPAND
import com.farmerbb.taskbar.util.Constants.PREF_DESKTOP_SESSION_ACTIVE
import com.farmerbb.taskbar.util.Constants.PREF_DESKTOP_WIDGETS
import com.farmerbb.taskbar.util.Constants.PREF_FIRST_RUN
import com.farmerbb.taskbar.util.Constants.PREF_TASKBAR_ACTIVE
import com.farmerbb.taskbar.util.DesktopSessionState
import com.farmerbb.taskbar.util.TaskbarPosition
import com.farmerbb.taskbar.util.U
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowSettings
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class DesktopLauncherActivitySmokeTest {
    private lateinit var context: Context
    private var priorRotation: Int? = null
    private var controller: ActivityController<DesktopLauncherActivity>? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        priorRotation = ReflectionHelpers.getStaticField<Int?>(
            TaskbarPosition::class.java,
            "cachedRotation",
        )
        U.getSharedPreferences(context).edit()
            .putBoolean(PREF_FIRST_RUN, false)
            .remove(PREF_DESKTOP_WIDGETS)
            .remove(PREF_DESKTOP_SESSION_ACTIVE)
            .apply()
        resetLauncherHelper()
        ShadowSettings.setCanDrawOverlays(false)
    }

    @After
    fun tearDown() {
        controller?.let {
            if (!it.get().isDestroyed) it.pause().stop().destroy()
        }
        resetLauncherHelper()
        ReflectionHelpers.setStaticField(
            TaskbarPosition::class.java,
            "cachedRotation",
            priorRotation,
        )
        GlobalHelper.getInstance().setReflectionAllowed(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P,
        )
        U.getSharedPreferences(context).edit().clear().apply()
        ShadowSettings.setCanDrawOverlays(true)
    }

    @Test
    fun desktopShowsTaskbarWithoutExternalOverlayPermission() {
        val activity = launchDesktop()
        val startButton = activity.findViewById<View>(R.id.start_button)
        val appArea = activity.findViewById<View>(R.id.taskbar_scrollview)
        val alert = ShadowAlertDialog.getLatestAlertDialog()
        val alertMessage = if (alert == null) "" else Shadows.shadowOf(alert).message
        val pref = U.getSharedPreferences(context)

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
        Assert.assertNull(
            "The legacy collapse arrow must not be shown on the desktop",
            activity.findViewById<View>(R.id.hide_taskbar_button)
                ?.takeIf { it.visibility == View.VISIBLE },
        )
    }

    @Test
    fun desktopStartupDoesNotStartBackgroundOverlayServices() {
        launchDesktop()

        val application = Shadows.shadowOf(
            ApplicationProvider.getApplicationContext<android.app.Application>(),
        )
        val started = generateSequence { application.nextStartedService }
            .mapNotNull { it.component?.className }
            .toList()
        Assert.assertTrue(
            "Desktop must host its UI in the Activity, but started: $started",
            started.none { it.startsWith("com.farmerbb.taskbar.service.") },
        )
    }

    @Test
    fun appearanceRestartRedrawsDesktopTaskbarWithoutOverlayServices() {
        val activity = launchDesktop()
        val application = Shadows.shadowOf(
            ApplicationProvider.getApplicationContext<android.app.Application>(),
        )
        application.clearStartedServices()

        U.restartTaskbar(activity)
        idleMainLooper()

        Assert.assertNull(
            "Restarting the desktop taskbar must not start overlay services",
            application.nextStartedService,
        )
        Assert.assertEquals(
            "The redrawn desktop must contain exactly one taskbar",
            1,
            countViews(activity.window.decorView, R.id.start_button),
        )
    }

    @Test
    fun desktopClockStaysPinnedToTheEnd() {
        val activity = launchDesktop()
        val params = activity.findViewById<View>(R.id.taskbar_scrollview).layoutParams
            as LinearLayout.LayoutParams

        Assert.assertEquals("The app area must fill the remaining width", 1f, params.weight, 0f)
        Assert.assertEquals(0, params.width)
    }

    @Test
    fun emojiShortcutAppearsOnTheDesktop() {
        U.getSharedPreferences(context).edit()
            .putString(
                "andesk_documents",
                """[{"column":1,"row":1,"emoji":"\uD83D\uDE80","title":"Launchpad"}]""",
            )
            .apply()

        val activity = launchDesktop()
        idleMainLooper()

        val labels = ArrayList<View>()
        activity.window.decorView.findViewsWithText(
            labels,
            "Launchpad",
            View.FIND_VIEWS_WITH_TEXT,
        )
        Assert.assertEquals("The emoji shortcut must be drawn on the desktop", 1, labels.size)
    }

    @Test
    fun floatingTaskbarShrinksToStartButtonAndExpandsAgain() {
        val activity = launchDesktop()
        val startButton = activity.findViewById<View>(R.id.start_button)
        val root = startButton.desktopChild(activity)

        U.sendBroadcast(activity, ACTION_DESKTOP_TASKBAR_COMPACT)
        idleMainLooper()

        Assert.assertEquals(View.VISIBLE, startButton.visibility)
        Assert.assertEquals(
            "Only the Start button may remain while compact",
            View.GONE,
            activity.findViewById<View>(R.id.taskbar_scrollview).visibility,
        )
        Assert.assertEquals(
            "A compact taskbar must not span the screen",
            ViewGroup.LayoutParams.WRAP_CONTENT,
            root.layoutParams.width,
        )

        U.sendBroadcast(activity, ACTION_DESKTOP_TASKBAR_EXPAND)
        idleMainLooper()

        Assert.assertEquals(
            ViewGroup.LayoutParams.MATCH_PARENT,
            root.layoutParams.width,
        )
    }

    @Test
    fun leavingDesktopRemovesTaskbarAndReturningRestoresIt() {
        val activity = launchDesktop()
        Assert.assertNotNull(activity.findViewById<View>(R.id.start_button))

        // Opening another Android app stops the desktop Activity without finishing it.
        controller!!.pause().stop()
        idleMainLooper()

        Assert.assertNull(
            "No taskbar may remain attached while another app is in front",
            activity.findViewById<View>(R.id.start_button),
        )
        Assert.assertFalse(
            "Leaving the desktop must not keep the taskbar active",
            U.getSharedPreferences(context).getBoolean(PREF_TASKBAR_ACTIVE, false),
        )
        Assert.assertTrue(
            "Opening another app must not end the desktop session",
            U.getSharedPreferences(context).getBoolean(PREF_DESKTOP_SESSION_ACTIVE, false),
        )

        controller!!.restart().start().resume()
        idleMainLooper()

        Assert.assertNotNull(
            "Returning to Andesk must restore the taskbar",
            activity.findViewById<View>(R.id.start_button),
        )
        Assert.assertEquals(
            "Returning must not create duplicate taskbars",
            1,
            countViews(activity.window.decorView, R.id.start_button),
        )
    }

    @Test
    fun finishingDesktopClosesTheSession() {
        val activity = launchDesktop()
        Assert.assertTrue(DesktopSessionState.isActive(activity))

        activity.finish()
        controller!!.pause().stop().destroy()

        val pref = U.getSharedPreferences(context)
        Assert.assertFalse(pref.getBoolean(PREF_DESKTOP_SESSION_ACTIVE, false))
        Assert.assertFalse(pref.getBoolean(PREF_TASKBAR_ACTIVE, false))
        Assert.assertFalse(LauncherHelper.getInstance().isDesktopLauncherOpen)
    }

    @Test
    fun taskbarStaysAboveDesktopIconsAndWidgets() {
        val activity = launchDesktop()
        val startButton = activity.findViewById<View>(R.id.start_button)
        Assert.assertNotNull(startButton)

        val root = startButton.desktopChild(activity)
        val desktop = root.parent as ViewGroup
        val taskbarIndex = desktop.indexOfChild(root)
        for (i in 0 until desktop.childCount) {
            val child = desktop.getChildAt(i)
            if (child is GridLayout) {
                Assert.assertTrue(
                    "Desktop icons must not cover the taskbar",
                    taskbarIndex > i,
                )
            }
        }
    }

    @Test
    fun manifestDoesNotRequestSystemOnlyWidgetPermission() {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val requested = info.requestedPermissions?.toList() ?: emptyList()
        Assert.assertFalse(
            "BIND_APPWIDGET is signature|privileged; widgets use the user approval screen",
            requested.contains("android.permission.BIND_APPWIDGET"),
        )
    }

    private fun launchDesktop(): DesktopLauncherActivity {
        val created = Robolectric.buildActivity(DesktopLauncherActivity::class.java)
        controller = created
        val activity = created.create().start().resume().visible().get()
        idleMainLooper()
        return activity
    }

    private fun idleMainLooper() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun resetLauncherHelper() {
        LauncherHelper.getInstance().setDesktopLauncherOpen(false)
        LauncherHelper.getInstance().setOnPrimaryHomeScreen(false)
        LauncherHelper.getInstance().setOnSecondaryHomeScreen(false, -1)
    }

    private fun View.desktopChild(activity: Activity): View {
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        var view: View = this
        while (view.parent is View && (view.parent as View).parent !== content) {
            view = view.parent as View
        }
        return view
    }

    private fun countViews(view: View, id: Int): Int {
        var count = if (view.id == id) 1 else 0
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) count += countViews(view.getChildAt(i), id)
        }
        return count
    }
}
