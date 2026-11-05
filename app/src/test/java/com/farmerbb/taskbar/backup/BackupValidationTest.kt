package com.farmerbb.taskbar.backup

import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupValidationTest {
    private fun validBackup(): JSONBackupAgent {
        val agent = JSONBackupAgent(JSONObject())
        agent.putStringArray("pinned_apps_package_names", emptyArray())
        agent.putStringArray("pinned_apps_component_names", emptyArray())
        agent.putStringArray("pinned_apps_labels", emptyArray())
        agent.putStringArray("blocked_apps_package_names", emptyArray())
        agent.putStringArray("blocked_apps_component_names", emptyArray())
        agent.putStringArray("blocked_apps_labels", emptyArray())
        agent.putStringArray("blacklist_package_names", emptyArray())
        agent.putStringArray("blacklist_labels", emptyArray())
        agent.putStringArray("top_apps_package_names", emptyArray())
        agent.putStringArray("top_apps_labels", emptyArray())
        agent.putString("preferences", "<?xml version=\"1.0\"?><map/>")
        return agent
    }

    @Test fun acceptsValidEmptyBackup() {
        BackupUtils.validateBackup(validBackup())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMismatchedArrays() {
        val agent = validBackup()
        agent.putStringArray("pinned_apps_package_names", arrayOf("com.example"))
        BackupUtils.validateBackup(agent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBadComponent() {
        val agent = validBackup()
        agent.putStringArray("pinned_apps_package_names", arrayOf("com.example"))
        agent.putStringArray("pinned_apps_labels", arrayOf("Example"))
        agent.putStringArray("pinned_apps_component_names", arrayOf("not a component"))
        BackupUtils.validateBackup(agent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTruncatedXml() {
        val agent = validBackup()
        agent.putString("preferences", "<map><string name=\"oops\">")
        BackupUtils.validateBackup(agent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingPreferences() {
        val agent = validBackup()
        agent.putString("preferences", null)
        BackupUtils.validateBackup(agent)
    }
}
