package org.clockout.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.clockout.domain.model.PathStage
import org.clockout.presentation.util.iconRes
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.TextMuted

/** A stage's emblem in the indigo disc the home screen uses for its own icon. Decorative. */
@Composable
fun PathStageEmblem(
    stage: PathStage,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isDimmed: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isDimmed) Color.White.copy(alpha = 0.04f) else Color(0x336366F1))
            .border(1.dp, if (isDimmed) Color.White.copy(alpha = 0.08f) else Color(0x666366F1), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(stage.iconRes),
            contentDescription = null,
            tint = if (isDimmed) TextMuted else IndigoAccent,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
