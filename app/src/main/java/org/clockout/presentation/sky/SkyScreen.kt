package org.clockout.presentation.sky

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.clockout.R
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.ConstellationCatalog
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.SkyData
import org.clockout.domain.model.SkyLayout
import org.clockout.domain.model.SkySummary
import org.clockout.presentation.components.ContentMaxWidth
import org.clockout.presentation.components.FrostedBackgroundContainer
import org.clockout.presentation.components.frostedGlass
import org.clockout.presentation.components.maxContentWidth
import org.clockout.presentation.util.labelRes
import org.clockout.presentation.util.localizedAnswer
import org.clockout.presentation.util.localizedQuestionText
import org.clockout.ui.theme.TextLight
import org.clockout.ui.theme.TextMuted
import org.clockout.ui.theme.TextSlate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** How much of the window height the sky may take, so it can be seen whole without scrolling. */
private const val MAX_SKY_HEIGHT_FRACTION = 0.7f

@Composable
fun SkyScreen(data: SkyData, language: AppLanguage, onBack: () -> Unit, modifier: Modifier = Modifier) {
    FrostedBackgroundContainer(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .maxContentWidth(ContentMaxWidth)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            SkyTopBar(onBack)
            val months = data.months
            if (months.isEmpty()) {
                Text(
                    text = stringResource(R.string.sky_empty),
                    modifier = Modifier.fillMaxWidth().padding(32.dp).testTag("sky_empty"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSlate,
                    textAlign = TextAlign.Center
                )
                return@Column
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = skySummaryText(data.summary, withYear = true),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSlate
                )
                val pager = rememberPagerState(initialPage = months.lastIndex) { months.size }
                // The selected star is kept as an ISO date so it survives a rotation. Only a real
                // change of month closes it: an effect keyed on the page would also run when the
                // recreated composition starts, and wipe the restored star straight away.
                var selected by rememberSaveable { mutableStateOf<String?>(null) }
                LaunchedEffect(pager) {
                    snapshotFlow { pager.currentPage }.drop(1).collect { selected = null }
                }
                val scope = rememberCoroutineScope()
                MonthHeader(
                    title = monthTitle(months[pager.currentPage]),
                    canGoBack = pager.currentPage > 0,
                    canGoForward = pager.currentPage < months.lastIndex,
                    onBack = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    onForward = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
                )
                // A 3:4 sky at full width would be taller than a phone turned sideways, so its
                // width is capped by the window height and the sky is centred instead.
                val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
                val maxSkyWidth = windowHeight * MAX_SKY_HEIGHT_FRACTION * SkyLayout.ASPECT
                HorizontalPager(state = pager, pageSpacing = 16.dp) { page ->
                    val month = remember(data, page) { data.month(months[page]) }
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        Box(modifier = Modifier.widthIn(max = maxSkyWidth).clip(RoundedCornerShape(24.dp))) {
                            SkyCanvas(month = month, onStarTap = { date ->
                                selected = date.toString().takeIf { it != selected }
                            })
                            if (month.starCount == 0) {
                                Text(
                                    text = stringResource(R.string.sky_month_empty),
                                    modifier = Modifier.align(Alignment.Center),
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
                selected?.let { iso ->
                    val date = LocalDate.parse(iso)
                    SkyDayCard(date = date, entry = data.logs[date], language = language)
                }
                AboutTheSky()
                Atlas(data.summary)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkyTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.sky_screen_title),
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
                IconButton(onClick = onBack, modifier = Modifier.testTag("sky_back_button")) {
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
}

@Composable
private fun MonthHeader(
    title: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, enabled = canGoBack, modifier = Modifier.testTag("sky_prev_month")) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.sky_previous_month),
                tint = if (canGoBack) TextLight else TextMuted.copy(alpha = 0.4f)
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f).testTag("sky_month_title"),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextLight,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onForward, enabled = canGoForward, modifier = Modifier.testTag("sky_next_month")) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.sky_next_month),
                tint = if (canGoForward) TextLight else TextMuted.copy(alpha = 0.4f)
            )
        }
    }
}

/** The day behind a tapped star. A star outlives its entry: then only the date is left to show. */
@Composable
internal fun SkyDayCard(date: LocalDate, entry: RitualLogEntry?, language: AppLanguage) {
    val locale = LocalResources.current.configuration.locales[0]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(cornerRadius = 20.dp)
            .padding(18.dp)
            .testTag("sky_day_card"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextLight
        )
        if (entry == null) {
            Text(stringResource(R.string.sky_record_deleted), style = MaterialTheme.typography.bodySmall, color = TextSlate)
        } else {
            Text(
                text = "${entry.moodTag.emoji} ${stringResource(entry.moodTag.labelRes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextLight
            )
            Text(entry.localizedQuestionText(language), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            entry.localizedAnswer(language)?.let { answer ->
                Text(answer, style = MaterialTheme.typography.bodyMedium, color = TextSlate)
            }
        }
    }
}

/** What the sky means, the way the path screen explains the path: a collection, not a target. */
@Composable
private fun AboutTheSky() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(cornerRadius = 22.dp)
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.sky_about_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            color = TextLight
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.sky_about_text),
            style = MaterialTheme.typography.bodySmall,
            color = TextSlate,
            lineHeight = 20.sp
        )
    }
}

/**
 * All 53 constellations. The rows are chunked by hand for the reason BreathingStyleScreen gives:
 * a lazy grid cannot live inside this scrolling column, and a short last row is padded instead.
 */
@Composable
private fun Atlas(summary: SkySummary) {
    val names = stringArrayResource(R.array.constellation_names)
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = stringResource(R.string.sky_atlas_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextLight
            )
            Text(
                text = stringResource(R.string.sky_atlas_desc),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        BoxWithConstraints {
            val columns = if (maxWidth >= 600.dp) 5 else 3
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ConstellationCatalog.all.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { constellation ->
                            val number = constellation.number
                            val times = summary.timesLitByNumber[number] ?: 0
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .frostedGlass(isActive = selected == number, cornerRadius = 16.dp)
                                    .clickable(onClickLabel = names[number - 1], role = Role.Button) {
                                        selected = number.takeIf { it != selected }
                                    }
                                    .padding(8.dp)
                                    .testTag("sky_atlas_tile_$number"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ConstellationFigure(
                                    constellation,
                                    Modifier.fillMaxWidth().aspectRatio(1f),
                                    isLit = times > 0
                                )
                                Text(
                                    text = names[number - 1],
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (times > 0) TextLight else TextMuted,
                                    textAlign = TextAlign.Center,
                                    minLines = 2,
                                    maxLines = 2
                                )
                                Text(
                                    text = when {
                                        times > 0 -> stringResource(R.string.sky_atlas_times, times)
                                        number == ConstellationCatalog.SIZE -> stringResource(R.string.sky_atlas_rare)
                                        else -> stringResource(R.string.sky_atlas_rises, number)
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextMuted,
                                    textAlign = TextAlign.Center,
                                    minLines = 2,
                                    maxLines = 2
                                )
                            }
                        }
                        repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    // Opens under the row of the tapped tile: above the atlas it would be out of
                    // sight for any tile further down than the first few rows.
                    selected?.takeIf { number -> row.any { it.number == number } }?.let { number ->
                        AtlasDetailCard(
                            name = names[number - 1],
                            number = number,
                            years = summary.litYearsByNumber[number].orEmpty()
                        )
                    }
                }
            }
        }
    }
}

/** "2 stars · 3 more closed days to your first constellation" - the teaser before the sky has a figure. */
/** Which years a constellation was lit in - the atlas tile itself only has room for "2×". */
@Composable
private fun AtlasDetailCard(name: String, number: Int, years: List<Int>) {
    // A tile in the last visible row opens its card below the fold; scroll it into sight.
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(number) { bringIntoView.bringIntoView() }
    Column(
        modifier = Modifier
            .bringIntoViewRequester(bringIntoView)
            .fillMaxWidth()
            .frostedGlass(isActive = true, cornerRadius = 18.dp)
            .padding(16.dp)
            .testTag("sky_atlas_detail"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextLight
        )
        Text(
            text = when {
                years.isNotEmpty() -> stringResource(R.string.sky_atlas_lit_years, years.joinToString(", "))
                number == ConstellationCatalog.SIZE -> stringResource(R.string.sky_atlas_rare)
                else -> stringResource(R.string.sky_atlas_not_lit_yet, number)
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextSlate
        )
    }
}

@Composable
fun skyCountdownText(stars: Int, daysToFirstConstellation: Int): String =
    pluralStringResource(R.plurals.sky_summary_stars, stars, stars) + " · " +
        pluralStringResource(R.plurals.sky_teaser_days_left, daysToFirstConstellation, daysToFirstConstellation)

@Composable
fun skySummaryText(summary: SkySummary, withYear: Boolean): String {
    val parts = mutableListOf(
        pluralStringResource(R.plurals.sky_summary_stars, summary.stars, summary.stars),
        pluralStringResource(R.plurals.sky_summary_constellations, summary.constellationsLit, summary.constellationsLit)
    )
    if (withYear) {
        parts += stringResource(R.string.sky_summary_this_year, summary.litThisYear, summary.weeksThisYear)
        // Recalled only when there is something to recall, so a first year is not told "last year 0".
        if (summary.litLastYear > 0) parts += stringResource(R.string.sky_summary_last_year, summary.litLastYear)
    }
    return parts.joinToString(" · ")
}
