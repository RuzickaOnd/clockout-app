package org.clockout.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.clockout.R
import org.clockout.domain.model.PathProgress
import org.clockout.presentation.components.PathStageEmblem
import org.clockout.presentation.components.frostedGlass
import org.clockout.presentation.util.titleRes
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.TextLight
import org.clockout.ui.theme.TextMuted
import org.clockout.ui.theme.TextSlate

@Composable
internal fun PathCard(progress: PathProgress, onOpenPath: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(cornerRadius = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                onClickLabel = stringResource(R.string.path_open_description),
                role = Role.Button,
                onClick = onOpenPath
            )
            .padding(20.dp)
            .testTag("path_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.path_card_title),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PathStageEmblem(stage = progress.stage, size = 52.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(progress.stage.titleRes),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = TextLight,
                        modifier = Modifier.testTag("path_card_stage_title")
                    )
                    Text(
                        text = progressLine(progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSlate
                    )
                }
            }
            val next = progress.nextStage
            if (next != null && progress.closedDays > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                val span = next.minDays - progress.stage.minDays
                LinearProgressIndicator(
                    progress = { (progress.closedDays - progress.stage.minDays).toFloat() / span },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = IndigoAccent,
                    trackColor = Color.White.copy(alpha = 0.08f),
                    drawStopIndicator = {}
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.path_next_stage, stringResource(next.titleRes)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            rhythmLine(progress)?.let { line ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = line, style = MaterialTheme.typography.bodySmall, color = TextSlate)
            }
        }
    }
}

@Composable
private fun progressLine(progress: PathProgress): String {
    val daysToNext = progress.daysToNextStage
    return when {
        progress.closedDays == 0 -> stringResource(R.string.path_card_beginning)
        daysToNext == null -> stringResource(R.string.path_card_final)
        else -> pluralStringResource(R.plurals.path_days_to_next, daysToNext, daysToNext)
    }
}

/** Null until a first week has been in rhythm, so a newcomer is not shown a row of zeros. */
@Composable
private fun rhythmLine(progress: PathProgress): String? {
    if (progress.longestRhythmWeeks == 0) return null
    val longest = stringResource(R.string.path_rhythm_longest, progress.longestRhythmWeeks)
    if (progress.rhythmWeeks == 0) return longest.replaceFirstChar { it.titlecase() }
    val current = pluralStringResource(R.plurals.path_rhythm_current, progress.rhythmWeeks, progress.rhythmWeeks)
    return "$current · $longest"
}
