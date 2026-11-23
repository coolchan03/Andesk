package com.farmerbb.taskbar.util

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UiTest {
    private val context: Context
        get() = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Taskbar)

    @Test
    fun dpScalesWithDensity() {
        val density = context.resources.displayMetrics.density
        assertEquals(Math.round(48 * density), Ui.dp(context, 48f))
    }

    @Test
    fun themeColorFallsBackWhenAttributeIsMissing() {
        assertEquals(0x12345678, Ui.themeColor(context, R.attr.tb_text_color + 1, 0x12345678))
    }

    @Test
    fun lightAndDarkThemesUseDifferentAccents() {
        val light =
            ContextThemeWrapper(
                ApplicationProvider.getApplicationContext<Context>(),
                R.style.Taskbar,
            )
        val dark =
            ContextThemeWrapper(
                ApplicationProvider.getApplicationContext<Context>(),
                R.style.Taskbar_Dark,
            )
        assertEquals(light.getColor(R.color.tb_colorAccent), Ui.accent(light))
        assertEquals(dark.getColor(R.color.tb_colorAccent_dark), Ui.accent(dark))
    }

    @Test
    fun disabledIconButtonIsDimmedAndIgnoresClicks() {
        var clicks = 0
        val button = Ui.iconButton(context, R.drawable.tb_close, R.string.andesk_remove, false) {
            clicks++
        }
        assertFalse(button.isEnabled)
        button.performClick()
        assertEquals(0, clicks)
        assertEquals(context.getString(R.string.andesk_remove), button.contentDescription)
    }

    @Test
    fun namedRowRoutesApplyAndDelete() {
        val events = mutableListOf<String>()
        val row = Ui.namedRow(context, "Night", "3 settings", { events += "apply" }, {
            events +=
                "delete"
        })

        // Text block first, then the delete button.
        (row.getChildAt(0) as LinearLayout).performClick()
        row.getChildAt(1).performClick()
        assertEquals(listOf("apply", "delete"), events)
        assertNotNull(row.getChildAt(0).contentDescription)
        assertTrue(row.getChildAt(0).contentDescription.toString().endsWith("Night"))
    }
}
