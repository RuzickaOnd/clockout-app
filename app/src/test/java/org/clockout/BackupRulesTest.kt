package org.clockout

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Auto Backup gives each app a 25 MB cloud quota and backs up nothing at all once that is
 * exceeded, so a custom sound library would take the ritual history down with it. Device-to-device
 * transfer has no such ceiling. That split is a deliberate decision rather than an oversight,
 * and it is invisible in the Kotlin sources, so it is pinned here.
 */
class BackupRulesTest {

    private val extractionRules = File("src/main/res/xml/data_extraction_rules.xml")
    private val legacyRules = File("src/main/res/xml/backup_rules.xml")

    /** The (domain, path) pairs included under [section], e.g. "cloud-backup". */
    private fun includes(file: File, section: String): Set<Pair<String, String>> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = document.getElementsByTagName(section).item(0) as Element
        val nodes = root.getElementsByTagName("include")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .map { it.getAttribute("domain") to it.getAttribute("path") }
            .toSet()
    }

    @Test
    fun `the cloud backup carries the settings and the ritual log database`() {
        val cloud = includes(extractionRules, "cloud-backup")

        assertTrue(cloud.contains("file" to "datastore/"))
        assertTrue(cloud.contains("database" to "clockout.db"))
    }

    @Test
    fun `custom sound files stay out of the cloud backup so the 25 MB quota is never at risk`() {
        val cloudPaths = includes(extractionRules, "cloud-backup").map { it.second }

        assertFalse(cloudPaths.any { it.contains("custom_sounds") })
    }

    @Test
    fun `custom sound files ride along on a device to device transfer`() {
        val transfer = includes(extractionRules, "device-transfer")

        assertTrue(transfer.contains("file" to "custom_sounds/"))
        assertTrue(transfer.contains("file" to "datastore/"))
        assertTrue(transfer.contains("database" to "clockout.db"))
    }

    @Test
    fun `the pre Android 12 rules leave the custom sounds out, having only a cloud backup to offer`() {
        val paths = includes(legacyRules, "full-backup-content").map { it.second }

        assertTrue(paths.contains("datastore/"))
        assertTrue(paths.contains("clockout.db"))
        assertFalse(paths.any { it.contains("custom_sounds") })
    }
}
