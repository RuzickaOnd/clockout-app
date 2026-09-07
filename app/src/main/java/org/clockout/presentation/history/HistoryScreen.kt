package org.clockout.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.presentation.components.FrostedBackgroundContainer
import org.clockout.presentation.components.frostedGlass
import org.clockout.presentation.util.AppStrings
import org.clockout.presentation.util.localizedAnswer
import org.clockout.presentation.util.localizedQuestionText
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.TextLight
import org.clockout.ui.theme.TextMuted
import org.clockout.ui.theme.TextSlate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryState,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = remember(state.language) { AppStrings(state.language) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Color(0xFF141526),
            titleContentColor = TextLight,
            textContentColor = TextSlate,
            title = { Text(strings.clearHistoryDialogTitle) },
            text = { Text(strings.clearHistoryDialogText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(strings.cancel, color = TextSlate)
                }
            }
        )
    }

    FrostedBackgroundContainer(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = strings.historyTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
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
                        IconButton(onClick = onBack, modifier = Modifier.testTag("history_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.back,
                                tint = TextSlate,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (state.logs.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = strings.clearHistoryTooltip,
                                    tint = TextSlate,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            if (state.logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🌙", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.emptyHistoryTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = TextLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.emptyHistoryDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSlate,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Summary Mood Breakdown Frosted Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .frostedGlass(cornerRadius = 22.dp)
                                .padding(18.dp)
                        ) {
                            Column {
                                Text(
                                    text = strings.totalClosedDays(state.totalCount),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 17.sp
                                    ),
                                    color = TextLight
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    MoodTag.entries.forEach { mood ->
                                        val count = state.moodCounts[mood] ?: 0
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = mood.emoji, fontSize = 24.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "$count×",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = IndigoAccent
                                            )
                                            Text(
                                                text = mood.label(state.language),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(state.logs, key = { it.date + it.timestamp }) { entry ->
                        HistoryEntryCard(entry = entry, language = state.language, doneBadgeText = strings.padlaBadge)
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: RitualLogEntry,
    language: AppLanguage,
    doneBadgeText: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(cornerRadius = 20.dp)
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x336366F1))
                            .border(1.dp, Color(0x606366F1), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = entry.moodTag.emoji, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = entry.date,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Text(
                            text = entry.moodTag.label(language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = TextLight
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x306366F1),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x606366F1))
                ) {
                    Text(
                        text = doneBadgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = IndigoAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            val answer = entry.localizedAnswer(language)
            if (!answer.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 14.dp)
                        .padding(12.dp)
                ) {
                    Column {
                        if (entry.questionText.isNotBlank()) {
                            Text(
                                text = entry.localizedQuestionText(language),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = AppStrings(language).quoted(answer),
                            style = MaterialTheme.typography.bodyMedium,
                            color = IndigoAccent
                        )
                    }
                }
            }
        }
    }
}
