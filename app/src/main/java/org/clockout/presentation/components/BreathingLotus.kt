package org.clockout.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import org.clockout.ui.theme.AuroraTeal
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.IndigoPrimaryLight
import org.clockout.ui.theme.PurpleAmbient
import kotlin.math.abs
import kotlin.math.min

/**
 * Where each petal points at full bloom, in degrees from upright. The outermost pair stops short
 * of horizontal so no petal ever reaches below the base it pivots on.
 */
private val PETAL_OPEN_ANGLES = floatArrayOf(-86f, -66f, -46f, -27f, -10f, 10f, 27f, 46f, 66f, 86f)

/** How far the petals part at the bottom of the exhale. Enough to read as layers, not as a fan. */
private const val PETAL_CLOSED_SPREAD = 7f

/** Petal half-width as a fraction of its length. */
private const val PETAL_WIDTH = 0.17f

/** Lowest the base may sit, so the widest petals keep a margin under them. */
private const val BASE_MAX_HEIGHT_FRACTION = 0.92f

/**
 * The breathing visual as a lotus: a bud at the bottom of the exhale, a full bloom at the top of
 * the inhale.
 *
 * Seen from the side rather than from above, which is the whole reason it works. Every other style
 * here is radially symmetric and looks the same from any angle, but petals viewed from above have
 * nowhere to fold to — shrinking them gives a smaller flower, never a bud. From the side each petal
 * pivots about its own base, from nearly upright and overlapping into a splayed fan.
 *
 * Unlike the other styles this one does **not** centre its [content]: the bud is narrower than the
 * label pill and would disappear behind it. The flower takes the space the content leaves.
 */
@Composable
fun BreathingLotus(
    cycle: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val petal = remember { Path() }
    val open = breathCurve(cycle)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // The bud is tall and the bloom is wide, so the unit has to satisfy both.
                val reach = min(size.width / 2f, size.height * 0.88f)
                // The flower grows upwards from its base and reaches about `reach` above it, so
                // the base has to sit below the middle for the whole thing to look centred. A box
                // wider than it is tall leaves slack, and putting the base at a fixed fraction of
                // the height parks the flower against the bottom of it.
                val base = Offset(
                    size.width / 2f,
                    min(size.height / 2f + reach / 2f, size.height * BASE_MAX_HEIGHT_FRACTION)
                )
                val middle = (PETAL_OPEN_ANGLES.size - 1) / 2f

                // Outermost petals first. Painted the other way round, the ones nearest upright end
                // up underneath and the bud reads as a flat fan instead of layers wrapped round a core.
                PETAL_OPEN_ANGLES.indices
                    .sortedByDescending { abs(PETAL_OPEN_ANGLES[it]) }
                    .forEach { index ->
                        val target = PETAL_OPEN_ANGLES[index]
                        val closed = PETAL_CLOSED_SPREAD * (index - middle) / middle
                        val angle = lerp(closed, target, open)
                        val outward = abs(target) / PETAL_OPEN_ANGLES.last()

                        val length = reach * lerp(1.02f, 0.80f, outward) * lerp(0.92f, 1f, open)
                        val width = reach * PETAL_WIDTH * lerp(0.62f, 1f, open)
                        val tone = lerpColor(IndigoPrimaryLight, PurpleAmbient, outward)

                        rotate(degrees = angle, pivot = base) {
                            petal.rewind()
                            petal.moveTo(base.x, base.y)
                            petal.cubicTo(
                                base.x + width, base.y - length * 0.30f,
                                base.x + width * 0.85f, base.y - length * 0.72f,
                                base.x, base.y - length
                            )
                            petal.cubicTo(
                                base.x - width * 0.85f, base.y - length * 0.72f,
                                base.x - width, base.y - length * 0.30f,
                                base.x, base.y
                            )
                            petal.close()

                            drawPath(
                                path = petal,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        lerpColor(tone, AuroraTeal, 0.40f).copy(alpha = 0.85f),
                                        tone.copy(alpha = 0.55f)
                                    ),
                                    startY = base.y - length,
                                    endY = base.y
                                )
                            )
                            drawPath(
                                path = petal,
                                color = IndigoAccent.copy(alpha = 0.28f),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }

                // The heart of the flower, which only exists once the petals have parted enough to
                // show it. Guarded because a radial gradient of zero radius throws.
                val heartRadius = reach * 0.22f * open
                if (heartRadius > 0.5f) {
                    val heart = Offset(base.x, base.y - reach * 0.18f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                IndigoAccent.copy(alpha = 0.60f * open),
                                Color.Transparent
                            ),
                            center = heart,
                            radius = heartRadius
                        ),
                        radius = heartRadius,
                        center = heart
                    )
                }
            }
        }

        // The slot is declared with a BoxScope receiver so every style can align it; this one
        // needs it under the flower rather than over it, so the Box is here rather than around
        // the whole visual.
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
