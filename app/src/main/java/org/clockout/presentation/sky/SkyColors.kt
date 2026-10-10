package org.clockout.presentation.sky

import androidx.compose.ui.graphics.Color
import org.clockout.domain.model.MoodTag

internal val SkyBackgroundTop = Color(0xFF070818)
internal val SkyBackgroundBottom = Color(0xFF1A1740)
internal val SkyLine = Color(0x8CA5B4FC)
internal val SkyNeutralStar = Color(0xFFF8FAFC)
internal val SkyFieldStar = Color(0x99CBD5E1)

/** A star keeps the colour of its mood for as long as the history still holds the entry. */
internal fun MoodTag?.starColor(): Color = when (this) {
    MoodTag.CALM -> Color(0xFF93C5FD)
    MoodTag.SATISFIED -> Color(0xFFFDE68A)
    MoodTag.TIRED -> Color(0xFFC4B5FD)
    MoodTag.TENSE -> Color(0xFFFDA4AF)
    null -> SkyNeutralStar
}
