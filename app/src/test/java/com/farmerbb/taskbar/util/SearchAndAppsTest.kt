package com.farmerbb.taskbar.util

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.activity.MainActivity
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchAndAppsTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clear()
    }

    @After
    fun tearDown() {
        clear()
    }

    private fun clear() {
        U.getSharedPreferences(context).edit()
            .remove(Constants.PREF_SEARCH_ENGINE)
            .remove(Constants.PREF_SEARCH_URL)
            .remove(Constants.PREF_CLOCK_APP)
            .apply()
    }

    private fun useEngine(engine: String) {
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_SEARCH_ENGINE, engine)
            .apply()
    }

    @Test
    fun testSystemDefaultEngineHasNoWebsite() {
        Assert.assertNull(U.getSearchEngineUri(context, "cats"))
    }

    @Test
    fun testNamedEnginesBuildAnEncodedSearchAddress() {
        useEngine("duckduckgo")

        val uri = U.getSearchEngineUri(context, "cats & dogs")!!

        Assert.assertEquals("duckduckgo.com", uri.host)
        Assert.assertEquals("cats & dogs", uri.getQueryParameter("q"))
        Assert.assertEquals("DuckDuckGo", U.getSearchEngineName(context))
    }

    @Test
    fun testCustomEngineNeedsAPlaceholder() {
        useEngine("custom")
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_SEARCH_URL, "https://example.org/find?term=%s")
            .apply()

        Assert.assertEquals(
            "https://example.org/find?term=hello%20world",
            U.getSearchEngineUri(context, "hello world").toString(),
        )

        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_SEARCH_URL, "https://example.org/find")
            .apply()
        Assert.assertNull(U.getSearchEngineUri(context, "hello"))
    }

    @Test
    fun testChosenAppIsOnlyReturnedWhileInstalled() {
        Assert.assertNull(U.getChosenApp(context, Constants.PREF_CLOCK_APP))

        val missing = ComponentName("com.example.missing", "com.example.missing.Main")
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_CLOCK_APP, missing.flattenToString())
            .apply()
        Assert.assertNull(U.getChosenApp(context, Constants.PREF_CLOCK_APP))

        val installed = ComponentName(context, MainActivity::class.java)
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_CLOCK_APP, installed.flattenToString())
            .apply()
        Assert.assertEquals(installed, U.getChosenApp(context, Constants.PREF_CLOCK_APP))
    }

    @Test
    fun testColorCodesAcceptSixOrEightDigits() {
        Assert.assertEquals(0xFF112233.toInt(), ColorPickerDialog.parseColor("#112233"))
        Assert.assertEquals(0x80112233.toInt(), ColorPickerDialog.parseColor("80112233"))
        Assert.assertNull(ColorPickerDialog.parseColor("#12345"))
        Assert.assertNull(ColorPickerDialog.parseColor("#GGGGGG"))
    }
}
