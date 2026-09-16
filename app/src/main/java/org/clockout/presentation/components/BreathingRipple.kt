package org.clockout.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.util.lerp
import org.clockout.ui.theme.AuroraTeal
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.IndigoPrimaryLight
import org.clockout.ui.theme.PurpleAmbient

/** Rings in flight at once. Enough that one leaving the rim never opens a gap in the train. */
private const val RIPPLE_COUNT = 6

/**
 * How many ring spacings each ring covers between the bottom of the exhale and the top of the
 * inhale.
 *
 * The rings are spaced by their index and shifted together by the breath, rather than each one
 * reading the curve at a lag. A lag looks right at first and is wrong in between: the curve is
 * periodic, so the ring furthest behind wraps into the *previous* breath and comes back out of the
 * centre ahead of rings that should be inside it. Spacing by index cannot do that — the train is
 * monotonic and evenly spaced at every point of the cycle, and only its position changes.
 */
private const val RIPPLE_SPREAD = 2f

/** Where a ring is born and where it has faded out. Radii past [RIPPLE_REACH] are invisible. */
private const val RIPPLE_BIRTH = 0.10f
private const val RIPPLE_REACH = 0.95f

/** Brightness of a ring at the centre, before the fade towards the rim takes it down. */
private const val RIPPLE_ALPHA = 0.85f

private val RIPPLE_STROKE_NEAR = 3.dp
private val RIPPLE_STROKE_FAR = 1.dp

/**
 * The breathing visual as waves leaving the centre.
 *
 * The rings sit one spacing apart and the breath shifts the whole train along that axis, so they
 * travel outwards while the user breathes in and are drawn back into the core while they breathe
 * out. Nothing here runs on a clock of its own: the travel *is* the breath, which is why the
 * motion reverses instead of looping.
 *
 * A ring thins, fades and cools from indigo through purple to cyan as it goes, so distance from
 * the centre is legible without counting. Everything is a function of [cycle] alone — no
 * randomness — so a screenshot taken at a given point on the clock always renders the same frame.
 *
 * @param content sits centred on top of the core.
 */
@Composable
fun BreathingRipple(
    cycle: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val half = size.minDimension / 2f
            val breath = breathCurve(cycle)

            drawRippleAura(center, half * lerp(0.75f, 0.95f, breath), breath)

            // Outermost first, so the bright young rings near the centre are drawn over the faint
            // old ones rather than under them.
            for (index in RIPPLE_COUNT - 1 downTo 0) {
                val travel = (index + breath * RIPPLE_SPREAD) / RIPPLE_COUNT
                // Past the rim a ring is gone; it reappears on the way back in rather than
                // wrapping round to the centre, which would break the train's order.
                val fade = travel.coerceIn(0f, 1f)
                // Cubic rather than linear: a linear fall makes a ring half gone by the time it is
                // halfway out, so the cool end of the palette it travels through is never actually
                // seen. This holds a ring's brightness most of the way and then drops it quickly.
                val alpha = RIPPLE_ALPHA * (1f - fade * fade * fade)
                if (alpha <= 0.01f) continue

                drawCircle(
                    color = rippleColor(fade).copy(alpha = alpha),
                    radius = half * lerp(RIPPLE_BIRTH, RIPPLE_REACH, travel),
                    center = center,
                    style = Stroke(width = lerpDp(RIPPLE_STROKE_NEAR, RIPPLE_STROKE_FAR, fade).toPx())
                )
            }

            drawRippleCore(center, half * lerp(0.10f, 0.20f, breath))
        }
        content()
    }
}

/** Indigo at the centre, cooling through purple to cyan by the time a ring reaches the rim. */
private fun rippleColor(travel: Float): Color = if (travel < 0.5f) {
    lerpColor(IndigoPrimaryLight, PurpleAmbient, travel * 2f)
} else {
    lerpColor(PurpleAmbient, AuroraTeal, (travel - 0.5f) * 2f)
}

private fun DrawScope.drawRippleAura(center: Offset, radius: Float, breath: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                IndigoPrimaryLight.copy(alpha = lerp(0.10f, 0.20f, breath)),
                Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/** The source the rings leave from. Without it they would appear to come from nothing. */
private fun DrawScope.drawRippleCore(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                IndigoAccent.copy(alpha = 0.55f),
                IndigoPrimaryLight.copy(alpha = 0.22f),
                Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
