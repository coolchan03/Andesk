package com.farmerbb.taskbar.activity

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.IconOverrides
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
class IconPickerActivityTest {
    private lateinit var context: Context
    private val key = IconOverrides.specialKey(Constants.START_BUTTON_ICON)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        IconOverrides.clear(context, key)
    }

    @After
    fun tearDown() {
        IconOverrides.clear(context, key)
    }

    @Test
    fun testResetRestoresTheNormalIcon() {
        IconOverrides.setEmoji(context, key, "🚀")
        Assert.assertTrue(IconOverrides.hasOverride(context, key))

        val intent = Intent(context, IconPickerActivity::class.java)
            .putExtra(Constants.EXTRA_ICON_KEY, key)
        val activity = Robolectric.buildActivity(IconPickerActivity::class.java, intent)
            .setup()
            .get()

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        Assert.assertNotNull("The picker must offer its choices", dialog)
        dialog.listView.performItemClick(null, 2, 2L)

        Assert.assertFalse(IconOverrides.hasOverride(context, key))
        Assert.assertTrue(activity.isFinishing)
    }

    @Test
    fun testStartButtonUsesAnEmojiOverride() {
        IconOverrides.setEmoji(context, key, "🚀")

        val drawable = IconOverrides.getDrawable(context, key, 64)

        Assert.assertNotNull(drawable)
    }
}
