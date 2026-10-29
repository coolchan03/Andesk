package com.farmerbb.taskbar.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskbarTextureTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testNoTextureLeavesBackgroundUntouched() {
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_TASKBAR_TEXTURE, "none").commit()
        val base = ColorDrawable(Color.BLACK)
        Assert.assertSame(base, TaskbarTexture.wrap(context, base, Color.BLACK, 0f))
    }

    @Test
    fun testTextureLayersPatternOverBackground() {
        for (pattern in listOf("dots", "lines", "grid", "carbon", "noise")) {
            U.getSharedPreferences(context).edit()
                .putString(Constants.PREF_TASKBAR_TEXTURE, pattern).commit()
            val wrapped = TaskbarTexture.wrap(context, ColorDrawable(Color.BLACK), Color.BLACK, 8f)
            Assert.assertTrue(pattern, wrapped is LayerDrawable)
        }
    }

    @Test
    fun testStartMenuTextContrastsWithBackground() {
        val text = U.getStartMenuTextColor(context)
        val background = U.getStartMenuBackgroundColor(context)
        Assert.assertNotEquals(Color.alpha(text), 0)
        Assert.assertTrue(Color.alpha(background) > 0)
    }
}
