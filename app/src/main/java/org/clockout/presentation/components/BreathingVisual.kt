package org.clockout.presentation.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.clockout.domain.model.BreathingStyle

/**
 * Draws whichever breathing visual the user has chosen.
 *
 * Every style takes the same [cycle] and centres the same [content], so the ritual step and the
 * picker in Settings both hand over one call and let the choice decide the rest. A new style is
 * a new composable plus a branch here and an entry in [BreathingStyle].
 */
@Composable
fun BreathingVisual(
    style: BreathingStyle,
    cycle: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    when (style) {
        BreathingStyle.CLASSIC_CIRCLE -> BreathingPulse(cycle, modifier, content)
        BreathingStyle.AURORA -> BreathingAurora(cycle, modifier, content)
        BreathingStyle.CONCENTRIC_RIPPLE -> BreathingRipple(cycle, modifier, content)
        BreathingStyle.LOTUS -> BreathingLotus(cycle, modifier, content)
    }
}
