package com.farmerbb.taskbar.widget

import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class DesktopWidgetManagerTest {
    @Test
    fun pendingWidgetIdSurvivesActivityRecreation() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val manager = DesktopWidgetManager(activity, FrameLayout(activity))
        val initial = Bundle().apply { putInt("andesk_pending_widget_id", 4601) }
        manager.restorePendingState(initial)

        val saved = Bundle()
        manager.savePendingState(saved)
        val recreated = DesktopWidgetManager(activity, FrameLayout(activity))
        recreated.restorePendingState(saved)
        val verified = Bundle()
        recreated.savePendingState(verified)
        assertEquals(4601, verified.getInt("andesk_pending_widget_id"))
    }

    @Test
    fun widgetWithoutSetupScreenIsAddedDirectly() {
        assertFalse(DesktopWidgetManager.needsConfiguration(AppWidgetProviderInfo()))
    }

    @Test
    fun requiredSetupScreenIsOpened() {
        val info = AppWidgetProviderInfo().apply {
            configure = ComponentName("com.example", "com.example.Configure")
        }
        assertTrue(DesktopWidgetManager.needsConfiguration(info))
    }

    @Test
    @Config(sdk = [31])
    fun optionalSetupScreenIsSkipped() {
        val info = AppWidgetProviderInfo().apply {
            configure = ComponentName("com.google.android.googlequicksearchbox", "Configure")
            widgetFeatures = AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE or
                AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL
        }
        assertFalse(DesktopWidgetManager.needsConfiguration(info))
    }

    @Test
    fun widgetsInflateWithoutTheActivityViewFactory() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        // Simulate AppCompat, which installs a Factory2 that swaps in AppCompat views.
        LayoutInflater.from(activity).factory2 = object : LayoutInflater.Factory2 {
            override fun onCreateView(
                parent: View?,
                name: String,
                context: android.content.Context,
                attrs: AttributeSet,
            ): View? = null

            override fun onCreateView(
                name: String,
                context: android.content.Context,
                attrs: AttributeSet,
            ): View? = null
        }

        val widgetContext = DesktopWidgetManager.createWidgetContext(activity)
        assertNotSame(activity, widgetContext)
        assertNull(
            "RemoteViews reject AppCompat views, so widgets need a factory-free inflater",
            LayoutInflater.from(widgetContext).factory2,
        )
    }
}
