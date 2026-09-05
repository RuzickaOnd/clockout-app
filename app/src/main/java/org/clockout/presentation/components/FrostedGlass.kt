package org.clockout.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.clockout.ui.theme.FrostedCanvas
import org.clockout.ui.theme.FrostedGlassBackground
import org.clockout.ui.theme.FrostedGlassBackgroundActive
import org.clockout.ui.theme.FrostedGlassBorder
import org.clockout.ui.theme.FrostedGlassBorderActive
import org.clockout.ui.theme.GlowIndigo
import org.clockout.ui.theme.GlowPurple

fun Modifier.frostedGlass(
    isActive: Boolean = false,
    cornerRadius: Dp = 20.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        if (isActive) FrostedGlassBackgroundActive else FrostedGlassBackground
    )
    .border(
        width = 1.dp,
        color = if (isActive) FrostedGlassBorderActive else FrostedGlassBorder,
        shape = RoundedCornerShape(cornerRadius)
    )

@Composable
fun FrostedBackgroundContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FrostedCanvas)
            .drawBehind {
                // Top-Left Indigo ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlowIndigo, Color.Transparent),
                        center = Offset(x = size.width * 0.1f, y = size.height * 0.05f),
                        radius = size.width * 0.75f
                    ),
                    center = Offset(x = size.width * 0.1f, y = size.height * 0.05f),
                    radius = size.width * 0.75f
                )
                // Bottom-Right Purple ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlowPurple, Color.Transparent),
                        center = Offset(x = size.width * 0.9f, y = size.height * 0.85f),
                        radius = size.width * 0.7f
                    ),
                    center = Offset(x = size.width * 0.9f, y = size.height * 0.85f),
                    radius = size.width * 0.7f
                )
            }
    ) {
        content()
    }
}
