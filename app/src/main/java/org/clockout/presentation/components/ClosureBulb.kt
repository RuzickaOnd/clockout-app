package org.clockout.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.clockout.ui.theme.BulbAmber
import org.clockout.ui.theme.BulbAmberDeep
import org.clockout.ui.theme.BulbOff

/** How long the whole "switch the lamp off" gesture takes. */
const val CLOSURE_BULB_ANIM_MILLIS = 1100

/** Delays used by the surrounding screen so text and buttons trail the bulb. */
const val CLOSURE_TEXT_IN_DELAY_MILLIS = 550
const val CLOSURE_BUTTON_IN_DELAY_MILLIS = 750

/**
 * The desk lamp of the visual closure step.
 *
 * While lit it burns warm amber inside a slowly breathing halo. When [isOff] flips,
 * the halo contracts into the bulb, the amber cools to slate and the icon gives a
 * short click-shaped flinch, so the user watches one object go dark rather than two
 * screens swapping.
 */
@Composable
fun ClosureBulb(
    isOff: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    // A single 0f..1f progress drives every layer, which keeps halo, tint and ring
    // from drifting apart mid-animation.
    val off = remember { Animatable(if (isOff) 1f else 0f) }
    val flinch = remember { Animatable(1f) }

    LaunchedEffect(isOff) {
        if (isOff && off.value < 1f) {
            flinch.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 260
                    1f at 0 using FastOutSlowInEasing
                    1.05f at 90
                    0.93f at 170
                    1f at 260
                }
            )
        }
    }
    LaunchedEffect(isOff) {
        off.animateTo(
            targetValue = if (isOff) 1f else 0f,
            animationSpec = tween(CLOSURE_BULB_ANIM_MILLIS, easing = FastOutSlowInEasing)
        )
    }

    // The lamp only breathes while it is still burning; once it is out, it stays out.
    val breathing = rememberInfiniteTransition(label = "bulb_breathing")
    val pulse = breathing.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bulb_pulse"
    )

    val progress = off.value
    val lit = 1f - progress

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            if (lit <= 0.001f) return@Canvas
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val base = this.size.minDimension / 2f
            val haloScale = 1f + (pulse.value - 1f) * lit

            // Three concentric radial gradients: a tight core, the lamp's spill and a
            // wide ambient wash. All three shrink towards the bulb as `lit` falls.
            drawHaloLayer(center, base * 2.6f * lit * haloScale, BulbAmber, 0.16f * lit)
            drawHaloLayer(center, base * 1.7f * lit * haloScale, BulbAmber, 0.26f * lit)
            drawHaloLayer(center, base * 1.0f * lit * haloScale, BulbAmberDeep, 0.42f * lit)
        }

        Icon(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = null,
            modifier = Modifier
                .size(size * 0.5f)
                .scale(flinch.value),
            tint = lerp(BulbAmber, BulbOff, progress)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHaloLayer(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float
) {
    if (radius <= 0f || alpha <= 0f) return
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
