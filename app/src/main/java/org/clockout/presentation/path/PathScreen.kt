package org.clockout.presentation.path

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.clockout.R
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.PathStage
import org.clockout.presentation.components.ContentMaxWidth
import org.clockout.presentation.components.FrostedBackgroundContainer
import org.clockout.presentation.components.PathStageEmblem
import org.clockout.presentation.components.frostedGlass
import org.clockout.presentation.components.maxContentWidth
import org.clockout.presentation.util.descriptionRes
import org.clockout.presentation.util.titleRes
import org.clockout.presentation.util.verseRes
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.TextLight
import org.clockout.ui.theme.TextMuted
import org.clockout.ui.theme.TextSlate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathScreen(progress: PathProgress, onBack: () -> Unit, modifier: Modifier = Modifier) {
    FrostedBackgroundContainer(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .maxContentWidth(ContentMaxWidth)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.path_screen_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextLight
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("path_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                                tint = TextSlate,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                CurrentStageHeader(progress)
                TextSection(R.string.path_origin_title, R.string.path_origin_text)
                Text(
                    text = stringResource(R.string.path_stages_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextLight
                )
                PathStage.entries.drop(1).forEach { stage ->
                    StageRow(
                        stage = stage,
                        isReached = progress.closedDays >= stage.minDays,
                        isCurrent = stage == progress.stage
                    )
                }
                TextSection(R.string.path_rhythm_title, R.string.path_rhythm_text)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CurrentStageHeader(progress: PathProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(isActive = true, cornerRadius = 24.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PathStageEmblem(stage = progress.stage, size = 80.dp)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.path_current_badge),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
            color = TextMuted
        )
        Text(
            text = stringResource(progress.stage.titleRes),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TextLight
        )
        Spacer(modifier = Modifier.height(6.dp))
        val days = pluralStringResource(R.plurals.path_closed_days, progress.closedDays, progress.closedDays)
        val rhythm = if (progress.longestRhythmWeeks == 0) null else
            pluralStringResource(R.plurals.path_rhythm_current, progress.rhythmWeeks, progress.rhythmWeeks) +
                " · " + stringResource(R.string.path_rhythm_longest, progress.longestRhythmWeeks)
        Text(
            text = listOfNotNull(days, rhythm).joinToString("\n"),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSlate,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun StageRow(stage: PathStage, isReached: Boolean, isCurrent: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(isActive = isCurrent, cornerRadius = 20.dp)
            .padding(16.dp)
            .testTag("path_stage_row_${stage.id}"),
        verticalAlignment = Alignment.Top
    ) {
        PathStageEmblem(stage = stage, size = 44.dp, isDimmed = !isReached)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f).alpha(if (isReached) 1f else 0.7f)) {
            Text(
                text = pluralStringResource(R.plurals.path_stage_threshold, stage.minDays, stage.minDays),
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrent) IndigoAccent else TextMuted
            )
            Text(
                text = stringResource(stage.titleRes),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextLight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(stage.verseRes),
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = TextSlate
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(stage.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = TextSlate,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun TextSection(titleRes: Int, textRes: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(cornerRadius = 22.dp)
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            color = TextLight
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodySmall,
            color = TextSlate,
            lineHeight = 20.sp
        )
    }
}
