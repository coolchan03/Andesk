package com.farmerbb.taskbar.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class TaskbarClockTest {
    private lateinit var context: Context
    private val afternoon: Date = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 26, 13, 5, 9)
    }.time

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearPrefs()
    }

    @After
    fun tearDown() {
        clearPrefs()
    }

    private fun clearPrefs() {
        U.getSharedPreferences(context).edit()
            .remove(Constants.PREF_CLOCK_FORMAT)
            .remove(Constants.PREF_CLOCK_SHOW_DATE)
            .remove(Constants.PREF_CLOCK_SHOW_SECONDS)
            .apply()
    }

    @Test
    fun testDefaultShowsTimeAndDateOnTwoLines() {
        val lines = U.getTaskbarClockText(context, afternoon).split("\n")

        Assert.assertEquals(2, lines.size.toLong())
        Assert.assertFalse(U.hasCustomClockFormat(context))
    }

    @Test
    fun testDateCanBeHidden() {
        U.getSharedPreferences(context).edit()
            .putBoolean(Constants.PREF_CLOCK_SHOW_DATE, false)
            .apply()

        val text = U.getTaskbarClockText(context, afternoon)

        Assert.assertFalse("The clock must be a single line", text.contains("\n"))
        Assert.assertTrue(U.hasCustomClockFormat(context))
    }

    @Test
    fun test24HourFormat() {
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_CLOCK_FORMAT, "24")
            .putBoolean(Constants.PREF_CLOCK_SHOW_DATE, false)
            .apply()

        Assert.assertEquals("13:05", U.getTaskbarClockText(context, afternoon))
    }

    @Test
    fun test12HourFormat() {
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_CLOCK_FORMAT, "12")
            .putBoolean(Constants.PREF_CLOCK_SHOW_DATE, false)
            .apply()

        val text = U.getTaskbarClockText(context, afternoon)

        Assert.assertTrue(text, text.startsWith("1:05"))
        Assert.assertFalse(text, text.contains("13"))
    }

    @Test
    fun testSecondsCanBeShown() {
        U.getSharedPreferences(context).edit()
            .putString(Constants.PREF_CLOCK_FORMAT, "24")
            .putBoolean(Constants.PREF_CLOCK_SHOW_DATE, false)
            .putBoolean(Constants.PREF_CLOCK_SHOW_SECONDS, true)
            .apply()

        Assert.assertEquals("13:05:09", U.getTaskbarClockText(context, afternoon))
    }
}
