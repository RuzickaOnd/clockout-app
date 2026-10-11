package org.clockout.presentation.sky

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.clockout.R
import org.clockout.domain.model.SkyData
import org.clockout.presentation.components.frostedGlass
import org.clockout.ui.theme.TextLight
import org.clockout.ui.theme.TextMuted
import org.clockout.ui.theme.TextSlate

/**
 * The current month's sky, small and still, at the top of History. The whole sky is shrunk rather
 * than cropped to fit: a crop would cut off the constellations of the month's later weeks.
 */
@Composable
fun SkyPreviewCard(data: SkyData, onOpen: () -> Unit) {
    val current = data.months.lastOrNull() ?: return
    val month = remember(data) { data.month(current) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(cornerRadius = 22.dp)
            .clickable(onClickLabel = stringResource(R.string.sky_screen_title), role = Role.Button, onClick = onOpen)
            .padding(14.dp)
            .testTag("sky_preview_card"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkyCanvas(
            month = month,
            modifier = Modifier.width(120.dp).clip(RoundedCornerShape(14.dp)),
            twinkle = false,
            showNames = false
        )
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(
                text = stringResource(R.string.sky_screen_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextLight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = monthTitle(current), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = data.daysToFirstConstellation
                    ?.let { skyCountdownText(data.summary.stars, it) }
                    ?: skySummaryText(data.summary, withYear = false),
                style = MaterialTheme.typography.bodySmall,
                color = TextSlate
            )
        }
    }
}
