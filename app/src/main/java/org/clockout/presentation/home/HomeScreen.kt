package org.clockout.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.clockout.R
import org.clockout.domain.model.DayStatus
import org.clockout.domain.model.RitualLogEntry
import org.clockout.presentation.components.FrostedBackgroundContainer
import org.clockout.presentation.components.frostedGlass
import org.clockout.presentation.components.ContentMaxWidth
import org.clockout.presentation.components.WideContentMaxWidth
import org.clockout.presentation.components.maxContentWidth
import org.clockout.presentation.util.labelRes
import org.clockout.presentation.util.localizedAnswer
import org.clockout.presentation.util.localizedDayName
import org.clockout.presentation.util.localizedQuestionText
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
    modifier: Modifier = Modifier,
    /**
     * False once a navigation rail is carrying History and Settings, so a wide window does not
     * offer the same two destinations twice.
     */
    showTopBarNavigation: Boolean = true
) {
    val scrollState = rememberScrollState()
    val isCompleted = state.streakInfo?.isTodayCompleted == true
    val todayLog = state.streakInfo?.todayLog

    FrostedBackgroundContainer(
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTwoPane = maxWidth >= HomeTwoPaneMinWidth
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .maxContentWidth(if (isTwoPane) WideContentMaxWidth else ContentMaxWidth)
                    .align(Alignment.TopCenter)
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
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp
                                    ),
                                    color = TextLight
                                )
                                Text(
                                    text = stringResource(R.string.app_subtitle),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = TextMuted
                                )
                            }
                        }
                    },
                    actions = {
                        // A navigation rail, where there is one, already carries these two.
                        if (showTopBarNavigation) {
                            TopBarIconButton(
                                icon = Icons.Default.History,
                                contentDescription = stringResource(R.string.nav_history),
                                onClick = onNavigateToHistory,
                                testTag = "nav_history_button"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TopBarIconButton(
                                icon = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.nav_settings),
                                onClick = onNavigateToSettings,
                                testTag = "nav_settings_button"
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
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
                    HomeContent(
                        state = state,
                        isCompleted = isCompleted,
                        todayLog = todayLog,
                        onStartRitual = onStartRitual,
                        isTwoPane = isTwoPane
                    )
                }
            }
        }
    }
}

/**
 * Past this width the window has room for two columns of cards side by side. Below it - which is
 * every phone, and a tablet held upright - the cards stay in one column, one under the other.
 */
private val HomeTwoPaneMinWidth = 840.dp

/**
 * The dashboard's three cards. On a narrow window they are a single scrolling column, exactly as
 * they have always been; on a wide one the card the user acts on keeps the left column to itself
 * and the two cards they only read move alongside it, so a tablet is not mostly empty space.
 */
@Composable
private fun HomeContent(
    state: HomeState,
    isCompleted: Boolean,
    todayLog: RitualLogEntry?,
    onStartRitual: () -> Unit,
    isTwoPane: Boolean
) {
    Spacer(modifier = Modifier.height(6.dp))
    if (isTwoPane) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                HeroCard(state, isCompleted, todayLog, onStartRitual)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                WeeklyStreakCard(state)
                WhyRitualCard()
            }
        }
    } else {
        HeroCard(state, isCompleted, todayLog, onStartRitual)
        Spacer(modifier = Modifier.height(18.dp))
        WeeklyStreakCard(state)
        Spacer(modifier = Modifier.height(18.dp))
        WhyRitualCard()
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun HeroCard(
    state: HomeState,
    isCompleted: Boolean,
    todayLog: RitualLogEntry?,
    onStartRitual: () -> Unit
) {
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
                            isCompleted -> stringResource(R.string.home_today_closed_badge)
                            !state.isRitualDay -> stringResource(R.string.home_weekend_slot_badge)
                            else -> stringResource(R.string.home_slot_workday_end)
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
                    text = stringResource(R.string.home_workday_done_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 26.sp
                    ),
                    color = TextLight
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.home_workday_done_desc,
                        todayLog?.moodTag?.let { stringResource(it.labelRes) } ?: ""
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSlate
                )

                val todayAnswer = todayLog?.localizedAnswer(state.language)
                val todayQuestion = todayLog
                    ?.takeIf { it.questionText.isNotBlank() }
                    ?.localizedQuestionText(state.language)
                if (!todayAnswer.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .frostedGlass(cornerRadius = 14.dp)
                            .padding(14.dp)
                    ) {
                        Column {
                            if (!todayQuestion.isNullOrBlank()) {
                                Text(
                                    text = todayQuestion,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = stringResource(R.string.common_quoted, todayAnswer),
                                style = MaterialTheme.typography.bodyMedium,
                                color = IndigoAccent
                            )
                        }
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
                        Text(stringResource(R.string.repeat_ritual))
                    }
                }
            } else if (!state.isRitualDay) {
                Text(
                    text = stringResource(R.string.home_weekend_rest_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 26.sp
                    ),
                    color = TextLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_weekend_rest_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSlate,
                    lineHeight = 22.sp
                )
            } else {
                Text(
                    text = stringResource(R.string.home_close_day_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 26.sp
                    ),
                    color = TextLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_close_day_desc),
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
                            text = stringResource(R.string.home_today_question_label),
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
                        text = stringResource(R.string.start_ritual),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyStreakCard(
    state: HomeState
) {
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
                        text = stringResource(R.string.home_this_week_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = TextLight
                    )
                    val closedCount = state.streakInfo?.completedThisWeekCount ?: 0
                    val targetCount = state.streakInfo?.targetWorkDays ?: 0
                    Text(
                        text = pluralStringResource(
                            R.plurals.home_days_closed_summary,
                            targetCount,
                            closedCount,
                            targetCount
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
                    DayIndicatorItem(day = day)
                }
            }
        }
    }
}

@Composable
private fun WhyRitualCard() {
    // Home Office Ritual Value Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(cornerRadius = 22.dp)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_why_ritual_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = TextLight
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_why_ritual_desc),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = TextSlate,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = TextSlate,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DayIndicatorItem(
    day: DayStatus
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(38.dp)
    ) {
        Text(
            text = day.localizedDayName(),
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
