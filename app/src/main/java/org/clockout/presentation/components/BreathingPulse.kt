package org.clockout.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.IndigoPrimaryLight

/** How far the circles contract at the bottom of the exhale. */
private const val PULSE_MIN_SCALE = 0.70f

/** Fractions of the visual the two rings occupy, kept from the 280 dp / 220 dp of a 300 dp square. */
private const val AURA_FRACTION = 280f / 300f
private const val RING_FRACTION = 220f / 300f

/**
 * The breathing visual the app shipped with: an atmospheric glow around a frosted ring, both
 * scaling with the breath.
 *
 * It takes the same [cycle] as [BreathingAurora] and reads it through the same [breathCurve], so
 * switching styles changes what the step looks like and not how it is paced. The original used an
 * eased tween played in `RepeatMode.Reverse`, which made the exhale end in a jolt; that was a
 * defect rather than part of the look, and it did not come along.
 *
 * @param content sits centred on top of the rings.
 */
@Composable
fun BreathingPulse(
    cycle: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val scale = lerp(PULSE_MIN_SCALE, 1f, breathCurve(cycle))

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize(AURA_FRACTION)
                .scale(scale * 1.05f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            IndigoPrimaryLight.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize(RING_FRACTION)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            IndigoPrimaryLight.copy(alpha = 0.18f),
                            IndigoPrimaryLight.copy(alpha = 0.063f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = IndigoAccent.copy(alpha = 0.40f),
                    shape = CircleShape
                )
        )

        content()
    }
}
