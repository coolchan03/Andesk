package com.farmerbb.taskbar.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.apache.commons.lang3.SerializationUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinnedBlockedAppsTest {
    private lateinit var context: Context
    private lateinit var packageName: String
    private lateinit var pinnedBlockedApps: PinnedBlockedApps
    private lateinit var appEntry: AppEntry

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pinnedBlockedApps = PinnedBlockedApps.getInstance(context)
        packageName = context.packageName
        appEntry = AppEntry(packageName, packageName, packageName, null, false)
    }

    @After
    fun tearDown() {
        pinnedBlockedApps.clear(context)
    }

    @Test
    fun testGetInstance() {
        Assert.assertNotNull(pinnedBlockedApps)
        for (i in 1..20) {
            Assert.assertEquals(pinnedBlockedApps, PinnedBlockedApps.getInstance(context))
        }
    }

    @Test
    fun testAddPinnedApp() {
        var pinnedApps = pinnedBlockedApps.pinnedApps
        Assert.assertEquals(0, pinnedApps.size.toLong())
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        pinnedApps = pinnedBlockedApps.pinnedApps
        Assert.assertEquals(1, pinnedApps.size.toLong())
        Assert.assertEquals(appEntry, pinnedApps[0])
    }

    @Test
    fun testRemovePinnedApp() {
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        pinnedBlockedApps.removePinnedApp(context, packageName)
        Assert.assertEquals(0, pinnedBlockedApps.pinnedApps.size.toLong())
    }

    @Test
    fun testAddBlockedApp() {
        var blockedApps = pinnedBlockedApps.blockedApps
        Assert.assertEquals(0, blockedApps.size.toLong())
        pinnedBlockedApps.addBlockedApp(context, appEntry)
        blockedApps = pinnedBlockedApps.blockedApps
        Assert.assertEquals(1, blockedApps.size.toLong())
        Assert.assertEquals(appEntry, blockedApps[0])
    }

    @Test
    fun testRemoveBlockedApp() {
        pinnedBlockedApps.addBlockedApp(context, appEntry)
        pinnedBlockedApps.removeBlockedApp(context, packageName)
        Assert.assertEquals(0, pinnedBlockedApps.blockedApps.size.toLong())
    }

    @Test
    fun testIsPinned() {
        Assert.assertFalse(pinnedBlockedApps.isPinned(packageName))
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        Assert.assertTrue(pinnedBlockedApps.isPinned(packageName))
    }

    @Test
    fun testIsBlocked() {
        Assert.assertFalse(pinnedBlockedApps.isBlocked(packageName))
        pinnedBlockedApps.addBlockedApp(context, appEntry)
        Assert.assertTrue(pinnedBlockedApps.isBlocked(packageName))
    }

    @Test
    fun testClear() {
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        pinnedBlockedApps.addBlockedApp(context, appEntry)
        pinnedBlockedApps.clear(context)
        Assert.assertEquals(0, pinnedBlockedApps.pinnedApps.size.toLong())
        Assert.assertEquals(0, pinnedBlockedApps.blockedApps.size.toLong())
    }

    @Test
    fun testSerializable() {
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        pinnedBlockedApps.addBlockedApp(context, appEntry)
        val newPinnedBlockedApps =
            SerializationUtils.deserialize<PinnedBlockedApps>(
                SerializationUtils.serialize(pinnedBlockedApps),
            )
        Assert.assertTrue(newPinnedBlockedApps.isPinned(packageName))
        Assert.assertTrue(newPinnedBlockedApps.isBlocked(packageName))
    }

    @Test
    fun testMovePinnedAppReordersAndStaysInBounds() {
        val first = AppEntry(packageName, "first", "First", null, false)
        val second = AppEntry(packageName, "second", "Second", null, false)
        val third = AppEntry(packageName, "third", "Third", null, false)
        pinnedBlockedApps.addPinnedApp(context, first)
        pinnedBlockedApps.addPinnedApp(context, second)
        pinnedBlockedApps.addPinnedApp(context, third)

        Assert.assertTrue(pinnedBlockedApps.movePinnedApp(context, "third", -1))
        Assert.assertEquals(
            listOf("first", "third", "second"),
            pinnedBlockedApps.pinnedApps.map { it.componentName },
        )

        Assert.assertTrue(pinnedBlockedApps.movePinnedApp(context, "first", 1))
        Assert.assertEquals(
            listOf("third", "first", "second"),
            pinnedBlockedApps.pinnedApps.map { it.componentName },
        )

        Assert.assertFalse(pinnedBlockedApps.movePinnedApp(context, "third", -1))
        Assert.assertFalse(pinnedBlockedApps.movePinnedApp(context, "second", 1))
        Assert.assertFalse(pinnedBlockedApps.movePinnedApp(context, "missing", 1))
    }

    @Test
    fun testDuplicateEntriesAreIgnored() {
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        pinnedBlockedApps.addBlockedApp(context, appEntry)
        pinnedBlockedApps.addBlockedApp(context, appEntry)
        Assert.assertEquals(1, pinnedBlockedApps.pinnedApps.size)
        Assert.assertEquals(1, pinnedBlockedApps.blockedApps.size)
    }

    @Test
    fun testGettersReturnSnapshots() {
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        val snapshot = pinnedBlockedApps.pinnedApps
        pinnedBlockedApps.removePinnedApp(context, packageName)
        Assert.assertEquals(1, snapshot.size)
        Assert.assertEquals(0, pinnedBlockedApps.pinnedApps.size)
    }

    @Test
    fun testSurvivesRoundTripThroughDisk() {
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        val loaded = SerializedStore.load(context, "PinnedBlockedApps", PinnedBlockedApps::class.java)
        Assert.assertNotNull(loaded)
        Assert.assertEquals(1, loaded!!.pinnedApps.size)
        Assert.assertEquals(packageName, loaded.pinnedApps[0].componentName)
    }

    @Test
    fun testCorruptFileLoadsAsNull() {
        context.openFileOutput("CorruptStore", Context.MODE_PRIVATE).use { it.write(byteArrayOf(1, 2, 3)) }
        Assert.assertNull(SerializedStore.load(context, "CorruptStore", PinnedBlockedApps::class.java))
        Assert.assertNull(SerializedStore.load(context, "MissingStore", PinnedBlockedApps::class.java))
    }

    @Test
    fun testFailedSaveKeepsPreviousContents() {
        Assert.assertTrue(SerializedStore.save(context, "Atomic", java.util.ArrayList(listOf("good"))))
        class NotSerializable
        val bad = java.util.ArrayList<Any>(listOf(NotSerializable()))
        Assert.assertFalse(SerializedStore.save(context, "Atomic", bad))
        @Suppress("UNCHECKED_CAST")
        val back = SerializedStore.load(context, "Atomic", java.util.ArrayList::class.java) as java.util.ArrayList<String>
        Assert.assertEquals(listOf("good"), back)
    }
}
