package com.farmerbb.taskbar.widget

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

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
}
