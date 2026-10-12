package org.clockout

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pins the direction of dependencies: presentation sees the domain but never the data layer, and
 * the domain sees nothing Android at all. Reads the sources, so it needs no Robolectric.
 */
class LayerDependencyTest {

    private val root = File("src/main/java/org/clockout")

    private fun importsIn(layer: String): List<Pair<String, String>> {
        val dir = File(root, layer)
        val files = dir.walkTopDown().filter { it.extension == "kt" }.toList()
        // A wrong working directory would otherwise yield no files and pass without enforcing anything.
        assertTrue("No Kotlin sources under ${dir.absolutePath}; the test is not looking at the code", files.isNotEmpty())
        return files.flatMap { file ->
            file.readLines().filter { it.startsWith("import ") }.map { file.relativeTo(root).path to it }
        }
    }

    @Test
    fun `presentation never imports the data layer`() {
        val offending = importsIn("presentation").filter { (_, line) -> line.startsWith("import org.clockout.data.") }
        assertTrue("Presentation reaches into data:\n${offending.joinToString("\n")}", offending.isEmpty())
    }

    @Test
    fun `the domain imports nothing from android, the data layer or presentation`() {
        val forbidden = listOf(
            "import android.", "import androidx.", "import org.clockout.R",
            "import org.clockout.data.", "import org.clockout.presentation."
        )
        val offending = importsIn("domain").filter { (_, line) -> forbidden.any(line::startsWith) }
        assertTrue("The domain is not pure:\n${offending.joinToString("\n")}", offending.isEmpty())
    }
}
