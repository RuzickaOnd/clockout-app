package org.clockout.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.clockout.R
import org.clockout.domain.model.BreathingStyle
import org.clockout.presentation.components.BreathingVisual
import org.clockout.presentation.components.ContentMaxWidth
import org.clockout.presentation.components.FrostedBackgroundContainer
import org.clockout.presentation.components.frostedGlass
import org.clockout.presentation.components.maxContentWidth
import org.clockout.presentation.components.rememberBreathCycle
import org.clockout.presentation.util.descriptionRes
import org.clockout.presentation.util.labelRes
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.IndigoPrimary
import org.clockout.ui.theme.TextLight
import org.clockout.ui.theme.TextMuted
import org.clockout.ui.theme.TextSlate

/** The large preview at the top; below this the tiles have to carry the choice on their own. */
private val PREVIEW_HEIGHT = 240.dp

/** Below this a tile stops showing a usable miniature, so the row takes fewer columns instead. */
private val TILE_MIN_WIDTH = 150.dp

/** Gap between tiles, both ways. Also feeds the column arithmetic, so it lives in one place. */
private val TILE_SPACING = 14.dp

/** Lines every tile reserves for its description, so none of them is measured against a shorter one. */
private const val DESCRIPTION_LINES = 3

/**
 * Picks the visual the breathing step draws.
 *
 * Every style is shown running rather than described, because the choice is entirely a visual one
 * and a name tells the user nothing about what they are about to look at for three minutes. The
 * large preview exists because a tile-sized miniature flattens the differences between styles that
 * a full screen makes obvious.
 *
 * Tapping a tile applies the choice immediately — there is nothing to confirm, and the preview
 * above is the confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathingStyleScreen(
    selectedStyle: BreathingStyle,
    onStyleSelected: (BreathingStyle) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // One cycle drives the preview and every tile, so they all breathe together instead of
    // drifting apart into a page that flickers.
    val cycle by rememberBreathCycle()

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
                        text = stringResource(R.string.breathing_style_screen_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        color = TextLight
                    )
                },
                navigationIcon = {
                    // Always shown. This screen sits outside the navigation rail, so unlike the
                    // top-level screens it has no other way back.
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("breathing_style_back_button")) {
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
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PREVIEW_HEIGHT)
                        .testTag("breathing_style_preview"),
                    contentAlignment = Alignment.Center
                ) {
                    BreathingVisual(
                        style = selectedStyle,
                        cycle = cycle,
                        modifier = Modifier
                            .height(PREVIEW_HEIGHT)
                            .aspectRatio(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.breathing_style_screen_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSlate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Rows laid out by hand rather than by a wrapping container. Tiles have to be the
                // same width in every row, including a last row that is not full, and neither
                // ready-made option delivers that: a lazy grid measures its items with unbounded
                // height so they cannot share a height, and FlowRow either stretches a lone tile
                // across its row (with a weight) or refuses to seat two tiles whose measured
                // widths sum to the row exactly (without one). Chunking into rows and padding the
                // short one with empty weights has neither problem. Their common height comes
                // from [DESCRIPTION_LINES], which needs no help from the container at all.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val columns = maxOf(
                        1,
                        ((maxWidth + TILE_SPACING) / (TILE_MIN_WIDTH + TILE_SPACING)).toInt()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(TILE_SPACING)) {
                        BreathingStyle.entries.chunked(columns).forEach { rowStyles ->
                            Row(horizontalArrangement = Arrangement.spacedBy(TILE_SPACING)) {
                                rowStyles.forEach { style ->
                                    BreathingStyleTile(
                                        style = style,
                                        cycle = cycle,
                                        isSelected = style == selectedStyle,
                                        onClick = { onStyleSelected(style) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - rowStyles.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreathingStyleTile(
    style: BreathingStyle,
    cycle: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .frostedGlass(isActive = isSelected, cornerRadius = 22.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) IndigoPrimary else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("breathing_style_${style.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            BreathingVisual(style = style, cycle = cycle, modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(style.labelRes),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = TextLight
            )
            if (isSelected) {
                Spacer(modifier = Modifier.size(6.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.breathing_style_selected),
                    tint = IndigoAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(style.descriptionRes),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            // A fixed block makes every tile the same height by construction. Leaving it to the
            // text meant a style whose description ran to three lines was measured against a
            // neighbour whose description ran to two, and the longer one came back clipped.
            minLines = DESCRIPTION_LINES,
            maxLines = DESCRIPTION_LINES
        )
    }
}
