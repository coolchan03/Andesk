package com.farmerbb.taskbar.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IconOverridesTest {
    private lateinit var context: Context
    private val appKey = IconOverrides.appKey("com.example/.Main")
    private val folderKey = IconOverrides.documentKey("content://tree/folder")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        IconOverrides.clear(context, appKey)
        IconOverrides.clear(context, folderKey)
    }

    @After
    fun tearDown() {
        IconOverrides.clear(context, appKey)
        IconOverrides.clear(context, folderKey)
    }

    @Test
    fun testNoOverrideByDefault() {
        Assert.assertFalse(IconOverrides.hasOverride(context, appKey))
        Assert.assertNull(IconOverrides.getDrawable(context, appKey, 48))
    }

    @Test
    fun testEmojiOverrideAppliesToAppsAndFoldersIndependently() {
        IconOverrides.setEmoji(context, appKey, "\uD83D\uDE80")

        Assert.assertTrue(IconOverrides.hasOverride(context, appKey))
        Assert.assertNotNull(IconOverrides.getDrawable(context, appKey, 48))
        Assert.assertFalse(IconOverrides.hasOverride(context, folderKey))
    }

    @Test
    fun testBlankEmojiIsIgnored() {
        IconOverrides.setEmoji(context, folderKey, "   ")

        Assert.assertFalse(IconOverrides.hasOverride(context, folderKey))
    }

    @Test
    fun testClearRestoresTheDefaultIcon() {
        IconOverrides.setEmoji(context, folderKey, "\uD83D\uDCC1")
        IconOverrides.clear(context, folderKey)

        Assert.assertFalse(IconOverrides.hasOverride(context, folderKey))
        Assert.assertNull(IconOverrides.getDrawable(context, folderKey, 48))
    }

    @Test
    fun testEmojiDrawableHasTheRequestedSize() {
        val drawable = IconOverrides.emojiDrawable(context, "\uD83D\uDE00", 64)

        Assert.assertEquals(64, drawable.intrinsicWidth.toLong())
        Assert.assertEquals(64, drawable.intrinsicHeight.toLong())
    }
}
