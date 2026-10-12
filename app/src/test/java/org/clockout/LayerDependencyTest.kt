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

    private fun importsIn(layer: String): List<Pair<String, String>> =
        File(root, layer).walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            file.readLines().filter { it.startsWith("import ") }.map { file.relativeTo(root).path to it }
        }.toList()

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
