package org.clockout.presentation.sky

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.clockout.R
import org.clockout.domain.model.Constellation
import org.clockout.domain.model.ConstellationCatalog
import org.clockout.domain.model.SkyLayout
import org.clockout.domain.model.SkyMonth
import org.clockout.domain.model.SkyPoint
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import kotlin.math.PI
import kotlin.math.sin

/**
 * One month of sky. Everything drawn is a function of [month] and the twinkle clock, so a capture
 * with the clock advanced by hand is the same on every machine.
 */
@Composable
fun SkyCanvas(
    month: SkyMonth,
    modifier: Modifier = Modifier,
    twinkle: Boolean = true,
    /** Off for a thumbnail, where fixed-size names would cover the stars they label. */
    showNames: Boolean = true,
    onStarTap: ((LocalDate) -> Unit)? = null
) {
    val names = stringArrayResource(R.array.constellation_names)
    val measurer = rememberTextMeasurer()
    val labelStyle = remember { TextStyle(color = SkyLine.copy(alpha = 0.9f), fontSize = 11.sp) }
    // Measured once per name and canvas width, not on every twinkle frame.
    val labels = remember(month) { mutableMapOf<Pair<Int, Int>, TextLayoutResult>() }
    val timeState: State<Float> = if (twinkle) {
        rememberInfiniteTransition(label = "twinkle").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Restart),
            label = "twinkle"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val slots = remember(month) {
        SkyLayout.slots(month.yearMonth.year, month.weeks.map { it.constellationNumber })
    }
    val placed = remember(month, slots) {
        month.weeks.zip(slots).flatMap { (week, slot) ->
            val figure = ConstellationCatalog.forWeek(week.constellationNumber)
            week.stars.map { star -> star to SkyLayout.project(slot, figure.stars[star.pointIndex]) }
        }
    }
    val field = remember(month) { SkyLayout.fieldStars(month.yearMonth.year, month.yearMonth.monthValue) }
    val tapReach = with(LocalDensity.current) { 24.dp.toPx() }
    val description = skyDescription(month, names)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(SkyLayout.ASPECT)
            .testTag("sky_canvas")
            .semantics { contentDescription = description }
            .then(
                if (onStarTap == null) {
                    Modifier
                } else {
                    Modifier.pointerInput(placed) {
                        detectTapGestures { offset ->
                            val tap = SkyPoint(offset.x / size.width, offset.y / size.height)
                            SkyLayout.nearest(placed.map { (star, p) -> star.date to p }, tap, tapReach / size.width)
                                ?.let(onStarTap)
                        }
                    }
                }
            )
    ) {
        val time = timeState.value
        fun at(p: SkyPoint) = Offset(p.x * size.width, p.y * size.height)
        fun twinkleAlpha(phase: Float) = 0.7f + 0.3f * sin(2 * PI * (time + phase)).toFloat()

        drawRect(Brush.verticalGradient(listOf(SkyBackgroundTop, SkyBackgroundBottom)))
        field.forEach { s ->
            drawCircle(
                SkyFieldStar.copy(alpha = SkyFieldStar.alpha * twinkleAlpha(s.phase)),
                radius = s.radius * 1.2.dp.toPx(),
                center = at(SkyPoint(s.x, s.y))
            )
        }
        month.weeks.zip(slots).forEach { (week, slot) ->
            if (!week.isLit) return@forEach
            val figure = ConstellationCatalog.forWeek(week.constellationNumber)
            val lit = week.stars.map { it.pointIndex }.toSet()
            figure.edges.filter { (a, b) -> a in lit && b in lit }.forEach { (a, b) ->
                drawLine(
                    SkyLine,
                    at(SkyLayout.project(slot, figure.stars[a])),
                    at(SkyLayout.project(slot, figure.stars[b])),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
            if (!showNames) return@forEach
            val label = labels.getOrPut(week.constellationNumber to size.width.toInt()) {
                measurer.measure(
                    names[week.constellationNumber - 1],
                    labelStyle,
                    constraints = Constraints(maxWidth = size.width.toInt())
                )
            }
            val anchor = at(SkyPoint(slot.centerX, slot.centerY + slot.size * SkyLayout.ASPECT * 0.55f))
            val origin = SkyLayout.labelOrigin(
                anchor.x, anchor.y, label.size.width.toFloat(), label.size.height.toFloat(), size.width, size.height
            )
            drawText(label, topLeft = Offset(origin.x, origin.y))
        }
        placed.forEach { (star, p) ->
            val phase = (star.date.toEpochDay() % 97) / 97f
            val color = star.mood.starColor()
            drawCircle(color.copy(alpha = 0.25f * twinkleAlpha(phase)), radius = 7.dp.toPx(), center = at(p))
            drawCircle(color, radius = 3.dp.toPx(), center = at(p))
        }
    }
}

/** A whole figure, every star and line, for the atlas. */
@Composable
internal fun ConstellationFigure(constellation: Constellation, modifier: Modifier, isLit: Boolean) {
    val alpha = if (isLit) 1f else 0.25f
    Canvas(modifier) {
        val inset = 0.12f
        fun at(p: SkyPoint) = Offset(
            (inset + p.x * (1 - 2 * inset)) * size.width,
            (inset + p.y * (1 - 2 * inset)) * size.height
        )
        constellation.edges.forEach { (a, b) ->
            drawLine(
                SkyLine.copy(alpha = SkyLine.alpha * alpha),
                at(constellation.stars[a]),
                at(constellation.stars[b]),
                strokeWidth = 1.dp.toPx()
            )
        }
        constellation.stars.forEach { drawCircle(SkyNeutralStar.copy(alpha = alpha), 2.dp.toPx(), at(it)) }
    }
}

/** "Říjen 2026" in the app's language, which LocalResources carries rather than the system locale. */
@Composable
internal fun monthTitle(yearMonth: YearMonth): String {
    val locale = LocalResources.current.configuration.locales[0]
    return yearMonth.month.getDisplayName(DateTextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.titlecase(locale) } + " " + yearMonth.year
}

@Composable
private fun skyDescription(month: SkyMonth, names: Array<String>): String {
    val stars = pluralStringResource(
        R.plurals.sky_canvas_description, month.starCount, month.starCount, monthTitle(month.yearMonth)
    )
    val lit = month.weeks.filter { it.isLit }.map { names[it.constellationNumber - 1] }
    return if (lit.isEmpty()) stars else stars + ", " + stringResource(R.string.sky_canvas_constellations, lit.joinToString(", "))
}
