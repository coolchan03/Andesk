package com.farmerbb.taskbar.util

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinnedAppSetsTest {
    private lateinit var context: Context
    private lateinit var pinned: PinnedBlockedApps

    private fun pin(vararg names: String) {
        for (name in names) {
            val component = ComponentName("com.example.$name", "com.example.$name.Main")
            pinned.addPinnedApp(
                context,
                AppEntry(component.packageName, component.flattenToString(), name, null, false),
            )
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        U.getSharedPreferences(context).edit().clear().commit()
        pinned = PinnedBlockedApps.getInstance(context)
        pinned.clear(context)
    }

    @After
    fun tearDown() {
        pinned.clear(context)
    }

    @Test
    fun savesPinnedAppsInOrder() {
        pin("mail", "docs", "chat")
        assertEquals("Work", PinnedAppSets.saveCurrent(context, "  Work "))

        assertEquals(
            listOf("mail", "docs", "chat"),
            PinnedAppSets.components(context, "work").map {
                it.packageName.removePrefix("com.example.")
            },
        )
    }

    @Test
    fun savingAgainReplacesRegardlessOfCase() {
        pin("mail")
        PinnedAppSets.saveCurrent(context, "Work")
        pin("docs")
        PinnedAppSets.saveCurrent(context, "WORK")

        assertEquals(listOf("WORK"), PinnedAppSets.names(context))
        assertEquals(2, PinnedAppSets.components(context, "work").size)
    }

    @Test
    fun rejectsBlankNamesAndEnforcesLimit() {
        assertNull(PinnedAppSets.saveCurrent(context, "   "))
        for (i in 1..PinnedAppSets.MAX_SETS) {
            assertEquals(
                "s$i",
                PinnedAppSets.saveCurrent(context, "s$i"),
            )
        }
        assertNull(PinnedAppSets.saveCurrent(context, "extra"))
        assertEquals("S1", PinnedAppSets.saveCurrent(context, "S1"))
    }

    @Test
    fun deleteAndMissingSets() {
        PinnedAppSets.saveCurrent(context, "Temp")
        assertTrue(PinnedAppSets.delete(context, "temp"))
        assertFalse(PinnedAppSets.delete(context, "temp"))
        assertTrue(PinnedAppSets.components(context, "temp").isEmpty())
    }

    @Test
    fun applyingMissingSetLeavesPinnedAppsAlone() {
        pin("mail")
        assertNull(PinnedAppSets.apply(context, "nope"))
        assertEquals(1, pinned.pinnedApps.size)
    }

    @Test
    fun applyReplacesPinnedAppsAndReportsUnresolvableOnes() {
        pin("mail", "docs")
        PinnedAppSets.saveCurrent(context, "Work")

        // No launcher activities exist in the test environment, so every app is skipped; the
        // important behavior is that the old pins are replaced and the skip is reported.
        val result = PinnedAppSets.apply(context, "Work")!!
        assertEquals(0, result.pinned)
        assertEquals(2, result.skipped)
        assertEquals(0, pinned.pinnedApps.size)
    }

    @Test
    fun corruptStorageIsTreatedAsEmpty() {
        U.getSharedPreferences(
            context,
        ).edit().putString("andesk_pinned_app_sets", "[broken").commit()
        assertTrue(PinnedAppSets.names(context).isEmpty())
    }
}
