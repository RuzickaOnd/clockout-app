package org.clockout.presentation.ritual

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import org.clockout.R
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.BreathingStyle
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathStage
import org.clockout.domain.model.RitualStep
import org.clockout.presentation.components.CLOSURE_BULB_ANIM_MILLIS
import org.clockout.presentation.components.CLOSURE_BUTTON_IN_DELAY_MILLIS
import org.clockout.presentation.components.CLOSURE_TEXT_IN_DELAY_MILLIS
import org.clockout.presentation.components.BreathingVisual
import org.clockout.presentation.components.ClosureBulb
import org.clockout.presentation.components.FrostedBackgroundContainer
import org.clockout.presentation.components.PathStageEmblem
import org.clockout.presentation.components.frostedGlass
import org.clockout.presentation.components.isInhalePhase
import org.clockout.presentation.components.rememberBreathCycle
import org.clockout.presentation.components.ContentMaxWidth
import org.clockout.presentation.components.maxContentWidth
import org.clockout.presentation.util.descriptionRes
import org.clockout.presentation.util.labelRes
import org.clockout.presentation.util.titleRes
import org.clockout.presentation.util.verseRes
import org.clockout.ui.theme.FrostedGlassBackground
import org.clockout.ui.theme.FrostedGlassBorder
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.IndigoPrimary
import org.clockout.ui.theme.IndigoPrimaryLight
import org.clockout.ui.theme.TextLight
import org.clockout.ui.theme.TextMuted
import org.clockout.ui.theme.TextSlate

/**
 * Keeps the display awake while the ritual is on screen. The breathing and closing
 * steps run without any touch input, so without this the phone would dim and sleep
 * mid-ritual. The flag is released as soon as the screen leaves the composition.
 */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
fun RitualScreen(
    state: RitualState,
    onIntent: (RitualIntent) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    KeepScreenOn()

    val stepIndex = when (state.currentStep) {
        is RitualStep.Question -> 1
        is RitualStep.Breathing -> 2
        is RitualStep.Closing -> 3
        is RitualStep.Log -> 4
    }

    FrostedBackgroundContainer(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .maxContentWidth(ContentMaxWidth)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header & Segmented Progress Bar (Frosted Glass style)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { onFinish() }
                            .testTag("ritual_close_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.ritual_close_confirm),
                            tint = TextSlate,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.ritual_header_title),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.sp,
                                fontSize = 10.sp
                            ),
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.ritual_step_indicator, stepIndex, 4),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = IndigoAccent
                        )
                    }

                    if (state.currentStep is RitualStep.Breathing) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable { onIntent(RitualIntent.ToggleSound) }
                                .testTag("sound_toggle_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isSoundMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (state.isSoundMuted) "Unmute" else "Mute",
                                tint = IndigoAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4-Segmented Progress Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..4) {
                        val isSegmentActive = i <= stepIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isSegmentActive) {
                                        IndigoPrimaryLight
                                    } else {
                                        Color.White.copy(alpha = 0.06f)
                                    }
                                )
                        )
                    }
                }
            }

            // Step Content
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(250))
                },
                label = "ritual_step_transition",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    is RitualStep.Question -> {
                        StepQuestionContent(
                            state = state,
                            onIntent = onIntent
                        )
                    }
                    is RitualStep.Breathing -> {
                        StepBreathingContent(
                            secondsRemaining = state.breathingSecondsRemaining,
                            breathingStyle = state.breathingStyle,
                            onIntent = onIntent
                        )
                    }
                    is RitualStep.Closing -> {
                        StepClosingContent(
                            isClosureTriggered = state.isClosureTriggered,
                            onIntent = onIntent
                        )
                    }
                    is RitualStep.Log -> {
                        StepLogContent(
                            isSaved = state.isCompletedAndSaved,
                            selectedMood = state.selectedMood,
                            reachedStage = state.reachedStage,
                            rhythmWeekReached = state.rhythmWeekReached,
                            language = state.language,
                            onIntent = onIntent,
                            onFinish = onFinish
                        )
                    }
                }
            }
        }
    }
}

/**
 * Every ritual step wants to spread its content over the whole height - a title at the top, the
 * visual in the middle, the button at the bottom. That only works while the content actually fits.
 * On a short window (a phone turned sideways, a tablet in split screen, a half-opened foldable)
 * the same layout used to push its button past the bottom edge, where it could not be reached and
 * the ritual could not be finished. Keeping the minimum height at the height of the window
 * preserves the spread-out look wherever there is room, and lets the step scroll where there is not.
 */
@Composable
private fun RitualStepColumn(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalPadding: Dp = 16.dp,
    content: @Composable ColumnScope.(availableWidth: Dp, availableHeight: Dp) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = availableHeight)
                .padding(horizontal = 24.dp, vertical = verticalPadding),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            content(availableWidth, availableHeight)
        }
    }
}

@Composable
private fun StepQuestionContent(
    state: RitualState,
    onIntent: (RitualIntent) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Headline & prompt
        Text(
            text = state.question.text(state.language),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Light,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp
            ),
            color = TextLight,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.ritual_question_instruction),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = TextSlate,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Frosted Glass Answer Cards
        val sampleIcons = listOf(
            Icons.Default.Inventory2,
            Icons.Default.Schedule,
            Icons.Default.Forum,
            Icons.Default.Check
        )

        val answers = state.question.suggestedAnswers(state.language)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            answers.forEachIndexed { index, answer ->
                val isSelected = state.selectedAnswer == answer && state.customAnswerText.isBlank()
                val icon = sampleIcons.getOrElse(index) { Icons.Default.Forum }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(isActive = isSelected, cornerRadius = 18.dp)
                        .clickable {
                            focusManager.clearFocus()
                            onIntent(RitualIntent.AnswerSelected(answer))
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .testTag("chip_answer_${answer.take(10)}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) IndigoAccent else TextSlate,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = answer,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 15.sp
                            ),
                            color = if (isSelected) IndigoAccent else TextLight
                        )
                    }
                }
            }

            // Custom answer card / input in frosted style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(
                        width = 1.dp,
                        color = if (state.customAnswerText.isNotBlank()) IndigoPrimaryLight else Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = state.customAnswerText,
                        onValueChange = { onIntent(RitualIntent.CustomAnswerChanged(it)) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.ritual_custom_answer_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_answer_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(36.dp))

        // CTA Button (Frosted Glass Theme Indigo 600)
        Button(
            onClick = {
                focusManager.clearFocus()
                onIntent(RitualIntent.ProceedToBreathing)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("proceed_to_breathing_button"),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = IndigoPrimary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = stringResource(R.string.ritual_button_continue_to_breathing),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }

        // Streak hint in footer
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(IndigoPrimaryLight)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.ritual_question_category),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontSize = 11.sp
                ),
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** Full size of the breathing visual, kept wherever the window is tall enough for it. */
private val BREATH_VISUAL_SIZE = 300.dp

/** Height the step needs for its heading, its guidance line and its button. */
private val BREATH_VISUAL_RESERVED_HEIGHT = 200.dp

/**
 * Below this the core pill gets too narrow for the "Inhale..." / "Nádech..." label it holds, so
 * the visual stops shrinking here and the step scrolls instead.
 */
private val BREATH_VISUAL_MIN_SIZE = 200.dp

/** Inset that keeps the label clear of the pill's curved edge, per side. */
private val BREATH_LABEL_INSET = 14.dp

@Composable
private fun StepBreathingContent(
    secondsRemaining: Int,
    breathingStyle: BreathingStyle,
    onIntent: (RitualIntent) -> Unit
) {
    // A linear cycle shaped by `breathCurve` rather than an eased tween played in reverse. The
    // reversal made the exhale crawl out of the turn and end in a jolt, and it left the phase to
    // be guessed from the scale instead of read off the turning point itself.
    val cycle by rememberBreathCycle()

    val isInhale = isInhalePhase(cycle)
    val breathText = stringResource(if (isInhale) R.string.ritual_breathing_inhale else R.string.ritual_breathing_exhale)
    val breathGuidance = stringResource(if (isInhale) R.string.ritual_breathing_inhale_desc else R.string.ritual_breathing_exhale_desc)

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(isInhale) {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Exception) {}
    }

    RitualStepColumn { _, availableHeight ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.ritual_breathing_category),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontSize = 10.sp
                ),
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.ritual_breathing_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 24.sp
                ),
                color = TextLight
            )
        }

        // The blob is the calm centre of this step, so it keeps its full size on every window
        // that has room for it. Only once the header, the guidance and the button no longer fit
        // alongside it does it give way, rather than pushing the button below the fold.
        val visualSize = min(BREATH_VISUAL_SIZE, availableHeight - BREATH_VISUAL_RESERVED_HEIGHT)
            .coerceAtLeast(BREATH_VISUAL_MIN_SIZE)
        val visualRatio = visualSize / BREATH_VISUAL_SIZE

        BreathingVisual(
            style = breathingStyle,
            cycle = cycle,
            modifier = Modifier.size(visualSize)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp * visualRatio)
                    .frostedGlass(isActive = false, cornerRadius = 60.dp * visualRatio)
                    .testTag("breath_label_pill"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    // The pill is round, so its corners are not usable width. Without this inset
                    // the label runs to the bounding box and the curve cuts its ends off.
                    modifier = Modifier.padding(horizontal = BREATH_LABEL_INSET * visualRatio)
                ) {
                    Text(
                        modifier = Modifier.testTag("breath_label"),
                        text = breathText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TextLight,
                        // The pill has a fixed width while this is in sp and grows with the
                        // system font setting, so at a large one the word wrapped and the round
                        // edge ate its first letters. It shrinks to fit instead: one line, always
                        // whole. At the default setting it still renders at 17sp.
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 9.sp,
                            maxFontSize = 17.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${secondsRemaining}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = IndigoAccent
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = breathGuidance,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSlate,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { onIntent(RitualIntent.BreathingFinished) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("finish_breathing_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IndigoPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(
                        if (secondsRemaining <= 10) R.string.ritual_button_finish_breathing else R.string.ritual_button_skip_breathing
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepClosingContent(
    isClosureTriggered: Boolean,
    onIntent: (RitualIntent) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    // The card is warmest while the lamp still burns and settles into plain quiet
    // glass once it is out, so turning the lamp off takes light off the screen.
    val cardBackground by animateColorAsState(
        targetValue = if (isClosureTriggered) FrostedGlassBackground else Color(0x14FBBF24),
        animationSpec = tween(CLOSURE_BULB_ANIM_MILLIS),
        label = "closure_card_background"
    )
    val cardBorder by animateColorAsState(
        targetValue = if (isClosureTriggered) FrostedGlassBorder else Color(0x4DFBBF24),
        animationSpec = tween(CLOSURE_BULB_ANIM_MILLIS),
        label = "closure_card_border"
    )

    RitualStepColumn { _, _ ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.ritual_closure_category),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontSize = 10.sp
                ),
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            ClosureCrossfade(isClosureTriggered) { triggered ->
                Text(
                    text = stringResource(
                        if (triggered) R.string.ritual_closure_done_title else R.string.ritual_closure_tap_prompt
                    ),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 24.sp
                    ),
                    color = TextLight,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Frosted Glass Closure Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(cardBackground)
                .border(1.dp, cardBorder, RoundedCornerShape(28.dp))
                .clickable(enabled = !isClosureTriggered) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIntent(RitualIntent.TriggerClosure)
                }
                .testTag("closure_card")
                .padding(vertical = 36.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ClosureBulb(isOff = isClosureTriggered)

                Spacer(modifier = Modifier.height(24.dp))

                ClosureCrossfade(isClosureTriggered) { triggered ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (triggered) {
                            Text(
                                text = stringResource(R.string.ritual_closure_finished_prompt),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 26.sp
                                ),
                                color = TextLight,
                                textAlign = TextAlign.Center,
                                lineHeight = 34.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.ritual_closure_done_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSlate,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // No title here: the heading right above the card already
                            // carries the same prompt, and so does the button below.
                            Text(
                                text = stringResource(R.string.ritual_closure_lamp_turn_off_prompt),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSlate,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            ClosureCrossfade(
                isClosureTriggered,
                enterDelayMillis = CLOSURE_BUTTON_IN_DELAY_MILLIS
            ) { triggered ->
                if (triggered) {
                    Button(
                        onClick = { onIntent(RitualIntent.ProceedToMoodLog) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("proceed_to_mood_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndigoPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.ritual_button_record_mood),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onIntent(RitualIntent.TriggerClosure)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("trigger_closure_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = TextLight
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.ritual_closure_tap_prompt),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Swaps closure copy so it trails the bulb instead of racing it: the old line fades
 * out while the halo is still contracting, and the new one arrives from below only
 * once the lamp has gone dark.
 */
@Composable
private fun ClosureCrossfade(
    isClosureTriggered: Boolean,
    enterDelayMillis: Int = CLOSURE_TEXT_IN_DELAY_MILLIS,
    content: @Composable (Boolean) -> Unit
) {
    AnimatedContent(
        targetState = isClosureTriggered,
        transitionSpec = {
            val enter = fadeIn(tween(400, delayMillis = enterDelayMillis)) +
                slideInVertically(tween(400, delayMillis = enterDelayMillis)) { it / 4 }
            enter togetherWith fadeOut(tween(250)) using
                SizeTransform(clip = false) { _, _ -> tween(CLOSURE_BULB_ANIM_MILLIS) }
        },
        label = "closure_copy"
    ) { triggered ->
        content(triggered)
    }
}

@Composable
private fun StepLogContent(
    isSaved: Boolean,
    selectedMood: MoodTag?,
    reachedStage: PathStage?,
    rhythmWeekReached: Boolean,
    language: AppLanguage,
    onIntent: (RitualIntent) -> Unit,
    onFinish: () -> Unit
) {
    val moods = listOf(MoodTag.CALM, MoodTag.TIRED, MoodTag.SATISFIED, MoodTag.TENSE)

    RitualStepColumn { _, _ ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.ritual_mood_category),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontSize = 10.sp
                ),
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.ritual_mood_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 24.sp
                ),
                color = TextLight,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.ritual_mood_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSlate
            )
        }

        if (isSaved) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlass(isActive = true, cornerRadius = 24.dp)
                    .padding(28.dp)
                    .testTag("ritual_completed_card"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = stringResource(R.string.ritual_completed_notice),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        ),
                        color = TextLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.home_workday_done_desc,
                            selectedMood?.let { stringResource(it.labelRes) } ?: ""
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSlate,
                        textAlign = TextAlign.Center
                    )
                    if (reachedStage != null) {
                        Spacer(modifier = Modifier.height(22.dp))
                        PathStageEmblem(stage = reachedStage, size = 56.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.ritual_path_stage_reached),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                            color = TextMuted,
                            modifier = Modifier.testTag("ritual_path_stage_reached")
                        )
                        Text(
                            text = stringResource(reachedStage.titleRes),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextLight,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(reachedStage.verseRes),
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = TextSlate,
                            textAlign = TextAlign.Center
                        )
                    } else if (rhythmWeekReached) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.ritual_path_rhythm_week),
                            style = MaterialTheme.typography.bodySmall,
                            color = IndigoAccent,
                            modifier = Modifier.testTag("ritual_path_rhythm_week")
                        )
                    }
                }
            }
        } else {
            // 4 Frosted Glass Mood selection cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                moods.forEach { mood ->
                    val isSelected = selectedMood == mood
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .frostedGlass(isActive = isSelected, cornerRadius = 20.dp)
                            .clickable { onIntent(RitualIntent.MoodSelected(mood)) }
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .testTag("mood_card_${mood.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mood.emoji,
                                fontSize = 30.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(mood.labelRes),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    ),
                                    color = if (isSelected) IndigoAccent else TextLight
                                )
                                Text(
                                    text = stringResource(mood.descriptionRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) IndigoAccent.copy(alpha = 0.8f) else TextSlate
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSaved) {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("finish_and_return_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.ritual_return_home),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onIntent(RitualIntent.SkipMood) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("skip_mood_log_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSlate
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = stringResource(R.string.ritual_button_skip_mood),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
