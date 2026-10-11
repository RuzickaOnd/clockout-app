package org.clockout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.ConstellationCatalog
import org.clockout.util.localizedFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.hypot

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConstellationCatalogTest {

    private val all = ConstellationCatalog.all

    @Test
    fun `the catalog holds one constellation per ISO week`() {
        assertEquals((1..53).toList(), all.map { it.number })
    }

    @Test
    fun `every figure has seven stars inside the unit square`() {
        all.forEach { c ->
            assertEquals("figure ${c.number}", 7, c.stars.size)
            assertTrue("figure ${c.number}", c.stars.all { it.x in 0f..1f && it.y in 0f..1f })
        }
    }

    @Test
    fun `every figure has five to eight valid distinct edges`() {
        all.forEach { c ->
            assertTrue("figure ${c.number}: ${c.edges.size}", c.edges.size in 5..8)
            assertTrue("figure ${c.number}", c.edges.all { (a, b) -> a != b && a in 0..6 && b in 0..6 })
            val undirected = c.edges.map { (a, b) -> minOf(a, b) to maxOf(a, b) }
            assertEquals("figure ${c.number}", undirected.size, undirected.toSet().size)
        }
    }

    @Test
    fun `the weekdays alone draw at least three lines`() {
        all.forEach { c ->
            assertTrue("figure ${c.number}", c.edges.count { (a, b) -> a < 5 && b < 5 } >= 3)
        }
    }

    /**
     * Compares what is actually drawn, not how the stars are numbered: two figures can share an
     * edge set walked from different starting stars and still be the same picture. The distance is
     * the Hausdorff distance between the normalised weekday segments, plus the lone weekday stars.
     */
    @Test
    fun `weekday outlines are distinct`() {
        val drawings = all.map { it.number to it.weekdayDrawing() }
        for (i in drawings.indices) for (j in i + 1 until drawings.size) {
            val (na, a) = drawings[i]
            val (nb, b) = drawings[j]
            val distance = maxOf(hausdorff(a.first, b.first, ::segmentGap), hausdorff(a.second, b.second, ::pointGap))
            assertTrue("figures $na and $nb look the same without the weekend ($distance)", distance > 0.2f)
        }
    }

    @Test
    fun `there is a name for every constellation in both languages`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        AppLanguage.entries.forEach { language ->
            val names = context.localizedFor(language).resources.getStringArray(R.array.constellation_names)
            assertEquals(language.name, 53, names.size)
            assertTrue(language.name, names.all { it.isNotBlank() })
        }
    }

    private data class P(val x: Float, val y: Float)

    /** Weekday segments and lone weekday stars, centred on their centroid and scaled to unit size. */
    private fun org.clockout.domain.model.Constellation.weekdayDrawing(): Pair<List<Pair<P, P>>, List<P>> {
        val pts = stars.take(5)
        val cx = pts.map { it.x }.average().toFloat()
        val cy = pts.map { it.y }.average().toFloat()
        val scale = pts.maxOf { hypot(it.x - cx, it.y - cy) }.coerceAtLeast(1e-3f)
        val norm = pts.map { P((it.x - cx) / scale, (it.y - cy) / scale) }
        val weekdayEdges = edges.filter { (a, b) -> a < 5 && b < 5 }
        val connected = weekdayEdges.flatMap { listOf(it.first, it.second) }.toSet()
        return weekdayEdges.map { (a, b) -> norm[a] to norm[b] } to (0 until 5).filterNot { it in connected }.map { norm[it] }
    }

    private fun pointGap(a: P, b: P) = hypot(a.x - b.x, a.y - b.y)

    private fun segmentGap(a: Pair<P, P>, b: Pair<P, P>) = minOf(
        maxOf(pointGap(a.first, b.first), pointGap(a.second, b.second)),
        maxOf(pointGap(a.first, b.second), pointGap(a.second, b.first))
    )

    private fun <T> hausdorff(a: List<T>, b: List<T>, gap: (T, T) -> Float): Float {
        if (a.isEmpty() && b.isEmpty()) return 0f
        if (a.isEmpty() || b.isEmpty()) return Float.MAX_VALUE
        val ab = a.maxOf { x -> b.minOf { y -> gap(x, y) } }
        val ba = b.maxOf { y -> a.minOf { x -> gap(x, y) } }
        return maxOf(ab, ba)
    }
}
