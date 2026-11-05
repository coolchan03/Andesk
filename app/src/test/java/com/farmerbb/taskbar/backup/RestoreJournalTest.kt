package com.farmerbb.taskbar.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class RestoreJournalTest {
    @Test fun snapshotRestoresPreviousDataAndImage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val data = File(context.filesDir, "PinnedBlockedApps")
        val wallpaper = File(context.filesDir, "tb_images/desktop_wallpaper")
        val oldData = if (data.exists()) data.readBytes() else null
        val oldWallpaper = if (wallpaper.exists()) wallpaper.readBytes() else null
        try {
            RestoreJournal.clear(context)
            wallpaper.parentFile!!.mkdirs()
            data.writeText("original pinned data")
            wallpaper.writeText("original wallpaper")
            RestoreJournal.capture(context)
            data.writeText("new pinned data")
            wallpaper.writeText("new wallpaper")
            RestoreJournal.rollback(context)
            assertEquals("original pinned data", data.readText())
            assertEquals("original wallpaper", wallpaper.readText())
        } finally {
            RestoreJournal.clear(context)
            if (oldData == null) data.delete() else data.writeBytes(oldData)
            if (oldWallpaper == null) wallpaper.delete() else wallpaper.writeBytes(oldWallpaper)
        }
    }

    @Test fun snapshotRemovesFilesThatWereOriginallyAbsent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val image = File(context.filesDir, "tb_images/custom_image")
        val old = if (image.exists()) image.readBytes() else null
        try {
            RestoreJournal.clear(context)
            image.delete()
            RestoreJournal.capture(context)
            image.parentFile!!.mkdirs()
            image.writeText("created by import")
            RestoreJournal.rollback(context)
            assertFalse(image.exists())
        } finally {
            RestoreJournal.clear(context)
            if (old != null) image.writeBytes(old)
        }
    }

    @Test(expected = IOException::class)
    fun cannotRollbackWithoutCompletedSnapshot() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        RestoreJournal.clear(context)
        RestoreJournal.rollback(context)
    }

    @Test fun snapshotCreatesReadyMarkerAndClearsIt() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        RestoreJournal.clear(context)
        RestoreJournal.capture(context)
        val marker = File(context.filesDir, "restore_rollback/ready")
        assertTrue(marker.isFile)
        RestoreJournal.clear(context)
        assertFalse(marker.exists())
    }
}
