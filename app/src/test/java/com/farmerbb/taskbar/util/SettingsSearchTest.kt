package com.farmerbb.taskbar.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.fragment.AppearanceFragment
import com.farmerbb.taskbar.fragment.GeneralFragment
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsSearchTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testIndexCoversSettingsAcrossScreens() {
        val keys = SettingsSearch.index(context).map { it.key }

        Assert.assertTrue(keys.contains(Constants.PREF_BATTERY_PERCENT))
        Assert.assertTrue(keys.contains(Constants.PREF_TASKBAR_STYLE))
        Assert.assertTrue(keys.contains(Constants.PREF_SEARCH_ENGINE))
    }

    @Test
    fun testSearchFindsABatterySettingAndNamesItsScreen() {
        val results = SettingsSearch.search(context, "battery")

        val battery = results.firstOrNull { it.key == Constants.PREF_BATTERY_PERCENT }
        Assert.assertNotNull("Searching 'battery' must find the battery percentage option", battery)
        Assert.assertEquals(AppearanceFragment::class.java, battery!!.fragment)
    }

    @Test
    fun testSearchMatchesEveryWordInAnyOrder() {
        val results = SettingsSearch.search(context, "engine search")

        Assert.assertTrue(results.any { it.key == Constants.PREF_SEARCH_ENGINE })
        Assert.assertEquals(
            GeneralFragment::class.java,
            results.first { it.key == Constants.PREF_SEARCH_ENGINE }.fragment,
        )
    }

    @Test
    fun testSearchWithNoMatchOrBlankReturnsNothing() {
        Assert.assertTrue(SettingsSearch.search(context, "zzzznotasetting").isEmpty())
        Assert.assertTrue(SettingsSearch.search(context, "   ").isEmpty())
    }
}
