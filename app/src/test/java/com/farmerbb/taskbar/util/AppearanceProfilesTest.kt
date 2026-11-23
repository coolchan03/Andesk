package com.farmerbb.taskbar.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.util.Constants.PREF_ACCENT_COLOR
import com.farmerbb.taskbar.util.Constants.PREF_CENTERED_ICONS
import com.farmerbb.taskbar.util.Constants.PREF_CLOCK_FORMAT
import com.farmerbb.taskbar.util.Constants.PREF_TASKBAR_STYLE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppearanceProfilesTest {
    private lateinit var context: Context

    private val pref get() = U.getSharedPreferences(context)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pref.edit().clear().commit()
    }

    @Test
    fun normalizeNameCollapsesWhitespaceAndBoundsLength() {
        assertEquals("Night mode", AppearanceProfiles.normalizeName("  Night \n  mode "))
        assertNull(AppearanceProfiles.normalizeName("   "))
        assertNull(AppearanceProfiles.normalizeName(null))
        assertEquals(
            AppearanceProfiles.MAX_NAME_LENGTH,
            AppearanceProfiles.normalizeName("x".repeat(100))!!.length
        )
    }

    @Test
    fun saveThenApplyRestoresEveryTypeOfSetting() {
        pref.edit()
            .putString(PREF_TASKBAR_STYLE, "gnome")
            .putInt(PREF_ACCENT_COLOR, 0xFF336699.toInt())
            .putBoolean(PREF_CENTERED_ICONS, true)
            .commit()
        assertEquals("Work", AppearanceProfiles.save(context, " Work "))

        pref.edit()
            .putString(PREF_TASKBAR_STYLE, "classic")
            .putInt(PREF_ACCENT_COLOR, 0)
            .putBoolean(PREF_CENTERED_ICONS, false)
            .commit()

        assertTrue(AppearanceProfiles.apply(context, "work"))
        assertEquals("gnome", pref.getString(PREF_TASKBAR_STYLE, null))
        assertEquals(0xFF336699.toInt(), pref.getInt(PREF_ACCENT_COLOR, 0))
        assertTrue(pref.getBoolean(PREF_CENTERED_ICONS, false))
    }

    @Test
    fun applyReturnsSettingsThatWereDefaultWhenSavedToDefault() {
        pref.edit().putString(PREF_TASKBAR_STYLE, "kde").commit()
        AppearanceProfiles.save(context, "Plain")

        pref.edit().putString(PREF_CLOCK_FORMAT, "24").commit()
        AppearanceProfiles.apply(context, "Plain")

        assertFalse(pref.contains(PREF_CLOCK_FORMAT))
        assertEquals("kde", pref.getString(PREF_TASKBAR_STYLE, null))
    }

    @Test
    fun savingTheSameNameReplacesTheProfile() {
        pref.edit().putString(PREF_TASKBAR_STYLE, "kde").commit()
        AppearanceProfiles.save(context, "Mine")
        pref.edit().putString(PREF_TASKBAR_STYLE, "gnome").commit()
        AppearanceProfiles.save(context, "MINE")

        assertEquals(listOf("MINE"), AppearanceProfiles.names(context))
        pref.edit().remove(PREF_TASKBAR_STYLE).commit()
        AppearanceProfiles.apply(context, "mine")
        assertEquals("gnome", pref.getString(PREF_TASKBAR_STYLE, null))
    }

    @Test
    fun namesAreSortedIgnoringCase() {
        listOf("beta", "Alpha", "gamma").forEach { AppearanceProfiles.save(context, it) }
        assertEquals(listOf("Alpha", "beta", "gamma"), AppearanceProfiles.names(context))
    }

    @Test
    fun profileCountIsLimited() {
        for (i in 1..AppearanceProfiles.MAX_PROFILES) {
            assertEquals("p$i", AppearanceProfiles.save(context, "p$i"))
        }
        assertNull(AppearanceProfiles.save(context, "one too many"))
        // Replacing an existing profile is still allowed at the limit.
        assertEquals("P1", AppearanceProfiles.save(context, "P1"))
        assertEquals(AppearanceProfiles.MAX_PROFILES, AppearanceProfiles.names(context).size)
    }

    @Test
    fun deleteRemovesTheProfile() {
        AppearanceProfiles.save(context, "Temp")
        assertTrue(AppearanceProfiles.delete(context, "temp"))
        assertFalse(AppearanceProfiles.exists(context, "Temp"))
        assertFalse(AppearanceProfiles.delete(context, "Temp"))
        assertFalse(AppearanceProfiles.apply(context, "Temp"))
    }

    @Test
    fun applyIgnoresKeysThatAreNotPartOfTheLook() {
        pref.edit().putString(
            "andesk_appearance_profiles",
            """{"Evil":{"unrelated_key":{"t":"s","v":"x"},"taskbar_style":{"t":"s","v":"kde"}}}"""
        ).commit()

        assertTrue(AppearanceProfiles.apply(context, "Evil"))
        assertEquals("kde", pref.getString(PREF_TASKBAR_STYLE, null))
        assertFalse(pref.contains("unrelated_key"))
    }

    @Test
    fun corruptStorageIsTreatedAsEmpty() {
        pref.edit().putString("andesk_appearance_profiles", "not json").commit()
        assertTrue(AppearanceProfiles.names(context).isEmpty())
        assertEquals("Fresh", AppearanceProfiles.save(context, "Fresh"))
    }
}
