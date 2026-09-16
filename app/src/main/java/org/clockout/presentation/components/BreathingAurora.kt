package org.clockout.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import org.clockout.ui.theme.AuroraCrest
import org.clockout.ui.theme.AuroraDeep
import org.clockout.ui.theme.AuroraTeal
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.IndigoPrimaryLight
import org.clockout.ui.theme.PurpleAmbient
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One full inhale and exhale: 4 s each way, the rhythm the whole step is built on. */
const val BREATH_CYCLE_MILLIS = 8000

/**
 * How long the waves take to drift once through their own loop. Deliberately out of step with
 * [BREATH_CYCLE_MILLIS]: the two only line up again every 72 s, so nine breaths pass before the
 * surface repeats a shape the user has already watched.
 */
private const val WAVE_DRIFT_MILLIS = 9000

/** How far behind the blob the echoes trail, in cycles. 0.15 of 8 s is a little over a second. */
private val GHOST_LAGS = floatArrayOf(0.15f, 0.30f)
private val GHOST_SCALES = floatArrayOf(1.07f, 1.15f)
private val GHOST_ALPHAS = floatArrayOf(0.07f, 0.04f)

/** Anchor points the blob outline is bent through. Eight is enough to look organic, not spiky. */
private const val BLOB_ANCHORS = 8

/** Horizontal samples per wave. Above this the extra points cost time and change nothing. */
private const val WAVE_SAMPLES = 56

private const val TAU = (2.0 * PI).toFloat()

/**
 * A layer of the liquid filling the blob. Amplitude and level are fractions of the blob radius,
 * so the whole visual scales with whatever size the caller gives it.
 */
private class WaveLayer(
    val amplitude: Float,
    val periods: Float,
    val speed: Float,
    val phase: Float,
    val depth: Float,
    val color: Color,
    val alpha: Float
)

/**
 * Listed in draw order, which has to run from the highest surface down to the lowest: every layer
 * fills all the way to the bottom of the blob, so a lower one painted later covers the band the
 * layer above it was supposed to leave visible, and the water flattens into a single slab. Teal
 * therefore goes down first and deep indigo last, and each band is what remains of the layer above
 * the next surface. Their wavelengths and directions differ so no two crests ever line up.
 */
private val WAVE_LAYERS = listOf(
    WaveLayer(0.042f, 3.0f, 1.4f, 4.3f, 0f, AuroraTeal, 0.30f),
    WaveLayer(0.060f, 2.25f, -0.7f, 2.1f, 0.10f, PurpleAmbient, 0.52f),
    WaveLayer(0.085f, 1.5f, 1.0f, 0f, 0.22f, AuroraDeep, 0.72f)
)

/**
 * The shape of a calm breath: a raised cosine over the cycle. It stands still at both turning
 * points, moves fastest halfway through, and treats the exhale as the inhale in reverse. Feeding
 * it a linear 0f..1f cycle also means the loop closes on itself with no seam to see.
 */
fun breathCurve(cycle: Float): Float = (1f - cos(cycle * TAU)) / 2f

/** True for the first half of the cycle, so the guidance text turns exactly where the breath does. */
fun isInhalePhase(cycle: Float): Boolean = cycle < 0.5f

/** A linear 0f..1f that restarts every [BREATH_CYCLE_MILLIS]; feed it to [breathCurve]. */
@Composable
fun rememberBreathCycle(): State<Float> {
    val transition = rememberInfiniteTransition(label = "breath_cycle")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(BREATH_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath_cycle_progress"
    )
}

/**
 * The breathing visual: an organic blob that fills with layered water as the user breathes in and
 * empties as they breathe out, trailed by two slower echoes of its own outline.
 *
 * Everything it draws is a function of [cycle] alone — no randomness — so a screenshot taken at a
 * given point on the clock always renders the same frame.
 *
 * @param cycle the linear breath progress from [rememberBreathCycle].
 * @param content sits centred on top of the water; the step puts its guidance pill there.
 */
@Composable
fun BreathingAurora(
    cycle: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val drift = rememberInfiniteTransition(label = "wave_drift")
    val driftProgress by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(WAVE_DRIFT_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_drift_progress"
    )

    // Rebuilt every frame but never reallocated; a dropped frame here would be visible.
    val paths = remember { AuroraPaths() }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val half = size.minDimension / 2f
            val breath = breathCurve(cycle)
            val radius = half * lerp(0.58f, 0.78f, breath)

            drawAura(center, half * lerp(0.80f, 1.0f, breath), breath)

            GHOST_LAGS.forEachIndexed { index, lag ->
                val lagged = wrap(cycle - lag)
                val ghostRadius = half * lerp(0.58f, 0.78f, breathCurve(lagged)) * GHOST_SCALES[index]
                paths.ghosts[index].buildBlob(center, ghostRadius, wrap(driftProgress - lag))
                drawPath(
                    path = paths.ghosts[index],
                    color = IndigoAccent.copy(alpha = GHOST_ALPHAS[index]),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            paths.blob.buildBlob(center, radius, driftProgress)
            clipPath(paths.blob) {
                drawGlass(center, radius)
                drawWater(center, radius, breath, driftProgress, paths)
            }
            drawPath(
                path = paths.blob,
                color = IndigoAccent.copy(alpha = 0.40f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        content()
    }
}

private class AuroraPaths {
    val blob = Path()
    val ghosts = List(GHOST_LAGS.size) { Path() }
    val waves = List(WAVE_LAYERS.size) { Path() }
    val crest = Path()
}

private fun DrawScope.drawAura(center: Offset, radius: Float, breath: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                IndigoPrimaryLight.copy(alpha = lerp(0.14f, 0.30f, breath)),
                Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/** The unfilled part of the blob still has to read as glass rather than as a hole. */
private fun DrawScope.drawGlass(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.015f)),
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f),
            radius = radius * 1.4f
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawWater(
    center: Offset,
    radius: Float,
    breath: Float,
    drift: Float,
    paths: AuroraPaths
) {
    // Brimming above the middle at the top of the inhale, down to a crescent at the bottom of the
    // exhale. It never drains completely: the lower bands would fall outside the blob and clip
    // away, leaving one thin line in an empty ring, which reads as the visual dying rather than
    // as a held breath.
    val surface = center.y + radius * lerp(0.62f, -0.78f, breath)
    val bottom = center.y + radius * 1.2f

    WAVE_LAYERS.forEachIndexed { index, layer ->
        val level = surface + radius * layer.depth
        paths.waves[index].buildWave(layer, level, bottom, radius, drift, size.width)
        drawPath(
            path = paths.waves[index],
            // Each layer is brightest where it meets the air and loses itself further down, so
            // the stack gains depth instead of reading as three flat cut-outs.
            brush = Brush.verticalGradient(
                colors = listOf(
                    layer.color.copy(alpha = layer.alpha),
                    layer.color.copy(alpha = layer.alpha * 0.62f)
                ),
                startY = level - radius * layer.amplitude,
                endY = bottom
            )
        )
    }

    // The topmost surface drawn as a line is what makes the fill read as waves instead of as a
    // gradient; it is the only hard edge in the whole visual.
    val crestLayer = WAVE_LAYERS.first()
    paths.crest.buildCrest(crestLayer, surface + radius * crestLayer.depth, radius, drift, size.width)
    drawPath(
        path = paths.crest,
        color = AuroraCrest.copy(alpha = 0.70f),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

private fun Path.buildBlob(center: Offset, radius: Float, drift: Float) {
    rewind()
    val points = Array(BLOB_ANCHORS) { index ->
        val angle = index * TAU / BLOB_ANCHORS
        // Two waves of different frequency travelling in opposite directions: neither ever
        // repeats against the other, so the outline never settles into an obvious polygon.
        val wobble = 1f +
            0.070f * sin(2f * angle + drift * TAU) +
            0.045f * sin(3f * angle - drift * TAU * 0.6f)
        val r = radius * wobble
        Offset(center.x + r * cos(angle), center.y + r * sin(angle))
    }

    moveTo(points[0].x, points[0].y)
    for (index in points.indices) {
        val previous = points[(index - 1 + BLOB_ANCHORS) % BLOB_ANCHORS]
        val start = points[index]
        val end = points[(index + 1) % BLOB_ANCHORS]
        val next = points[(index + 2) % BLOB_ANCHORS]
        // Catmull-Rom tangents expressed as cubic controls: a closed curve that passes through
        // every anchor with no corner at the seam.
        cubicTo(
            start.x + (end.x - previous.x) / 6f,
            start.y + (end.y - previous.y) / 6f,
            end.x - (next.x - start.x) / 6f,
            end.y - (next.y - start.y) / 6f,
            end.x,
            end.y
        )
    }
    close()
}

private fun Path.buildWave(
    layer: WaveLayer,
    level: Float,
    bottom: Float,
    radius: Float,
    drift: Float,
    width: Float
) {
    rewind()
    moveTo(0f, layer.surfaceAt(0f, level, radius, drift, width))
    for (step in 1..WAVE_SAMPLES) {
        val x = width * step / WAVE_SAMPLES
        lineTo(x, layer.surfaceAt(x, level, radius, drift, width))
    }
    lineTo(width, bottom)
    lineTo(0f, bottom)
    close()
}

private fun Path.buildCrest(
    layer: WaveLayer,
    level: Float,
    radius: Float,
    drift: Float,
    width: Float
) {
    rewind()
    moveTo(0f, layer.surfaceAt(0f, level, radius, drift, width))
    for (step in 1..WAVE_SAMPLES) {
        val x = width * step / WAVE_SAMPLES
        lineTo(x, layer.surfaceAt(x, level, radius, drift, width))
    }
}

private fun WaveLayer.surfaceAt(
    x: Float,
    level: Float,
    radius: Float,
    drift: Float,
    width: Float
): Float = level + radius * amplitude * sin(x / width * periods * TAU + phase + drift * speed * TAU)

private fun wrap(value: Float): Float = (value % 1f + 1f) % 1f
