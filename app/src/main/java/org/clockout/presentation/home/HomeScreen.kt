package org.clockout.presentation.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.DayStatus
import org.clockout.presentation.components.FrostedBackgroundContainer
import org.clockout.presentation.components.frostedGlass
import org.clockout.presentation.util.AppStrings
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.IndigoPrimary
import org.clockout.ui.theme.TextLight
import org.clockout.ui.theme.TextMuted
import org.clockout.ui.theme.TextSlate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    onStartRitual: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = remember(state.language) { AppStrings(state.language) }
    val scrollState = rememberScrollState()
    val isCompleted = state.streakInfo?.isTodayCompleted == true
    val todayLog = state.streakInfo?.todayLog

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x336366F1))
                                .border(1.dp, Color(0x666366F1), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nightlight,
                                contentDescription = null,
                                tint = IndigoAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp
                                ),
                                color = TextLight
                            )
                            Text(
                                text = strings.appSubtitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = TextMuted
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onNavigateToHistory,
                            modifier = Modifier.testTag("nav_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = strings.navHistory,
                                tint = TextSlate,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("nav_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = strings.navSettings,
                                tint = TextSlate,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Hero Card with Frosted Glass styling
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(isActive = isCompleted, cornerRadius = 26.dp)
                        .padding(24.dp)
                        .testTag("home_hero_card")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCompleted) Color(0x406366F1) else Color.White.copy(alpha = 0.06f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCompleted) Color(0x806366F1) else Color.White.copy(alpha = 0.12f)
                                )
                            ) {
                                Text(
                                    text = when {
                                        isCompleted -> strings.todayClosedBadge
                                        !state.isRitualDay -> strings.weekendSlotBadge
                                        else -> strings.slotWorkdayEnd
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isCompleted) IndigoAccent else TextSlate,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            if (todayLog != null) {
                                Text(
                                    text = todayLog.moodTag.emoji,
                                    fontSize = 28.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (isCompleted) {
                            Text(
                                text = strings.workdayDoneTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 26.sp
                                ),
                                color = TextLight
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.workdayDoneDesc(todayLog?.moodTag?.label(state.language) ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSlate
                            )

                            if (!todayLog?.selectedAnswer.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .frostedGlass(cornerRadius = 14.dp)
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "„${todayLog?.selectedAnswer}“",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = IndigoAccent
                                    )
                                }
                            }

                            if (state.isRitualDay) {
                                Spacer(modifier = Modifier.height(20.dp))

                                OutlinedButton(
                                    onClick = onStartRitual,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("repeat_ritual_button"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        contentColor = IndigoAccent
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x666366F1))
                                ) {
                                    Text(strings.repeatRitual)
                                }
                            }
                        } else if (!state.isRitualDay) {
                            Text(
                                text = strings.weekendRestTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 26.sp
                                ),
                                color = TextLight
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.weekendRestDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSlate,
                                lineHeight = 22.sp
                            )
                        } else {
                            Text(
                                text = strings.closeDayTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 26.sp
                                ),
                                color = TextLight
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.closeDayDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSlate,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .frostedGlass(cornerRadius = 14.dp)
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = strings.todayQuestionLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.sp,
                                            fontSize = 10.sp
                                        ),
                                        color = TextMuted
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = state.todayQuestion.text(state.language),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp
                                        ),
                                        color = TextLight
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            Button(
                                onClick = onStartRitual,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .testTag("start_ritual_button"),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IndigoPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.startRitualButton,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Weekly Streak Card with Frosted Glass styling
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 24.dp)
                        .padding(20.dp)
                        .testTag("weekly_streak_card")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = strings.thisWeekTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 17.sp
                                    ),
                                    color = TextLight
                                )
                                Text(
                                    text = strings.daysClosedSummary(
                                        state.streakInfo?.completedThisWeekCount ?: 0,
                                        state.streakInfo?.targetWorkDays ?: 0
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSlate
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x306366F1),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x606366F1))
                            ) {
                                Text(
                                    text = "${state.streakInfo?.completionPercent ?: 0}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = IndigoAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 7 Days Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            state.streakInfo?.weekDays?.forEach { day ->
                                DayIndicatorItem(day = day, language = state.language)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Home Office Ritual Value Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 22.dp)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = strings.whyRitualTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = TextLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.whyRitualDesc,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = TextSlate,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DayIndicatorItem(
    day: DayStatus,
    language: AppLanguage
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(38.dp)
    ) {
        Text(
            text = day.dayName(language),
            style = MaterialTheme.typography.labelSmall,
            color = if (day.isToday) IndigoAccent else TextMuted,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        day.isCompleted -> Color(0x3B6366F1)
                        day.isToday -> Color.White.copy(alpha = 0.08f)
                        else -> Color.White.copy(alpha = 0.03f)
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        day.isCompleted -> Color(0x806366F1)
                        day.isToday -> Color(0x606366F1)
                        else -> Color.White.copy(alpha = 0.08f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                day.isCompleted && day.moodTag != null -> {
                    Text(text = day.moodTag.emoji, fontSize = 16.sp)
                }
                day.isCompleted -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = IndigoAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                else -> {
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day.isToday) IndigoAccent else TextMuted
                    )
                }
            }
        }
    }
}
