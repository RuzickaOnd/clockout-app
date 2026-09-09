package org.clockout.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How wide a single column of text and cards may grow. Past roughly this width a line of text
 * becomes tiring to read and a card stops looking like a card, so on a tablet the column stops
 * here and sits in the middle of the window instead of stretching from edge to edge. A phone is
 * narrower than this in every orientation, so there the cap never applies.
 */
val ContentMaxWidth: Dp = 600.dp

/**
 * The same idea for a screen that lays several cards side by side rather than stacking one
 * column: it may use much more of a large window before the layout stops making sense.
 */
val WideContentMaxWidth: Dp = 1120.dp

/**
 * Caps the width of a screen's content. Pair it with an alignment on the parent so the column
 * sits centred once the window is wider than the cap.
 */
fun Modifier.maxContentWidth(max: Dp = ContentMaxWidth): Modifier =
    this.widthIn(max = max).fillMaxWidth()
