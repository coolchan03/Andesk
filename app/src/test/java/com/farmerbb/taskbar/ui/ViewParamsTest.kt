package com.farmerbb.taskbar.ui

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ViewParamsTest {
    @Test
    fun testToFrameLayoutParamsPositionsTaskbarInsideActivity() {
        val bottom = ViewParams(400, 52, Gravity.BOTTOM or Gravity.START, 0, 12)
            .toFrameLayoutParams()
        Assert.assertEquals(400, bottom.width)
        Assert.assertEquals(52, bottom.height)
        Assert.assertEquals((Gravity.BOTTOM or Gravity.START).toLong(), bottom.gravity.toLong())
        Assert.assertEquals(12, bottom.bottomMargin)
        Assert.assertEquals(0, bottom.topMargin)

        val top = ViewParams(300, 110, Gravity.TOP or Gravity.END, 0, 8)
            .toFrameLayoutParams()
        Assert.assertEquals(8, top.topMargin)
        Assert.assertEquals(0, top.bottomMargin)

        val defaultGravity = ViewParams(250, 30, -1, 0, -1)
            .toFrameLayoutParams()
        Assert.assertEquals(
            (Gravity.TOP or Gravity.START).toLong(),
            defaultGravity.gravity.toLong(),
        )
    }

    @Test
    fun testToWindowManagerParams() {
        val width = 100
        val height = 101
        val gravity = Gravity.BOTTOM
        val flags = 1001
        val bottomMargin = 102
        val params = ViewParams(width, height, gravity, flags, bottomMargin)
        var generatedParams = params.toWindowManagerParams()
        Assert.assertEquals(width.toLong(), generatedParams.width.toLong())
        Assert.assertEquals(height.toLong(), generatedParams.height.toLong())
        Assert.assertEquals(gravity.toLong(), generatedParams.gravity.toLong())
        Assert.assertEquals(flags.toLong(), generatedParams.flags.toLong())
        Assert.assertEquals(bottomMargin.toLong(), generatedParams.y.toLong())
        Assert.assertEquals(PixelFormat.TRANSLUCENT.toLong(), generatedParams.format.toLong())
        Assert.assertEquals(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY.toLong(),
            generatedParams.type.toLong(),
        )
        params.gravity = -1
        generatedParams = params.toWindowManagerParams()
        Assert.assertEquals(0, generatedParams.gravity.toLong())
        params.bottomMargin = -1
        generatedParams = params.toWindowManagerParams()
        Assert.assertEquals(0, generatedParams.y.toLong())
    }

    @Test
    fun testDesktopOverlaysOnlyAvoidTappableNavigation() {
        val params = ViewParams(100, 50, Gravity.BOTTOM, 0, -1)
        params.extendIntoGestureArea = true

        val resized = params.updateWidth(WindowManager.LayoutParams.MATCH_PARENT)
        Assert.assertTrue(resized.extendIntoGestureArea)
        Assert.assertEquals(
            WindowInsets.Type.tappableElement(),
            resized.toWindowManagerParams().fitInsetsTypes,
        )

        val legacy = ViewParams(100, 50, Gravity.BOTTOM, 0, -1).toWindowManagerParams()
        Assert.assertNotEquals(WindowInsets.Type.tappableElement(), legacy.fitInsetsTypes)
    }
}
