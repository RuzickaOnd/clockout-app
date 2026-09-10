package org.clockout.presentation.ritual

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualStep
import org.clockout.presentation.components.CLOSURE_BULB_ANIM_MILLIS
import org.clockout.presentation.components.CLOSURE_BUTTON_IN_DELAY_MILLIS
import org.clockout.presentation.components.CLOSURE_TEXT_IN_DELAY_MILLIS
import org.clockout.presentation.components.ClosureBulb
import org.clockout.presentation.components.FrostedBackgroundContainer
import org.clockout.presentation.components.frostedGlass
import org.clockout.presentation.components.ContentMaxWidth
import org.clockout.presentation.components.maxContentWidth
import org.clockout.presentation.util.AppStrings
import org.clockout.presentation.util.descriptionRes
import org.clockout.presentation.util.labelRes
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

    val strings = remember(state.language) { AppStrings(state.language) }

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
                            contentDescription = strings.closeRitualConfirm,
                            tint = TextSlate,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strings.ritualHeaderTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.sp,
                                fontSize = 10.sp
                            ),
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.stepIndicator(stepIndex, 4),
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
                            strings = strings,
                            onIntent = onIntent
                        )
                    }
                    is RitualStep.Breathing -> {
                        StepBreathingContent(
                            secondsRemaining = state.breathingSecondsRemaining,
                            strings = strings,
                            onIntent = onIntent
                        )
                    }
                    is RitualStep.Closing -> {
                        StepClosingContent(
                            isClosureTriggered = state.isClosureTriggered,
                            strings = strings,
                            onIntent = onIntent
                        )
                    }
                    is RitualStep.Log -> {
                        StepLogContent(
                            isSaved = state.isCompletedAndSaved,
                            selectedMood = state.selectedMood,
                            language = state.language,
                            strings = strings,
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
    strings: AppStrings,
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
            text = strings.questionInstruction,
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
                                text = strings.customAnswerPlaceholder,
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
                text = strings.buttonContinueToBreathing,
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
                text = strings.questionCategory,
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

@Composable
private fun StepBreathingContent(
    secondsRemaining: Int,
    strings: AppStrings,
    onIntent: (RitualIntent) -> Unit
) {
    // 8-second cycle: 4s inhale, 4s exhale
    val infiniteTransition = rememberInfiniteTransition(label = "breathing_cycle")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circle_scale"
    )

    val isInhale = scale >= 0.85f
    val breathText = if (isInhale) strings.breathingInhale else strings.breathingExhale
    val breathGuidance = if (isInhale) strings.breathingInhaleDesc else strings.breathingExhaleDesc

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(isInhale) {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Exception) {}
    }

    RitualStepColumn { _, availableHeight ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = strings.breathingCategory,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontSize = 10.sp
                ),
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.breathingTitle,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 24.sp
                ),
                color = TextLight
            )
        }

        // Animated Breathing Visual (Frosted Glass .breath-circle gradient style)
        // The circle is the calm centre of this step, so it keeps its full size on every window
        // that has room for it. Only once the header, the guidance and the button no longer fit
        // alongside it does it give way, rather than pushing the button below the fold.
        val visualSize = min(BREATH_VISUAL_SIZE, availableHeight - BREATH_VISUAL_RESERVED_HEIGHT)
            .coerceAtLeast(BREATH_VISUAL_MIN_SIZE)
        val visualRatio = visualSize / BREATH_VISUAL_SIZE

        Box(
            modifier = Modifier.size(visualSize),
            contentAlignment = Alignment.Center
        ) {
            // Outer atmospheric radial aura
            Box(
                modifier = Modifier
                    .size(280.dp * visualRatio)
                    .scale(scale * 1.05f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x386366F1), // indigo radial glow
                                Color.Transparent
                            )
                        )
                    )
            )

            // Middle Frosted ring
            Box(
                modifier = Modifier
                    .size(220.dp * visualRatio)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x2E6366F1),
                                Color(0x106366F1)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x66A5B4FC),
                        shape = CircleShape
                    )
            )

            // Core center frosted glass pill
            Box(
                modifier = Modifier
                    .size(136.dp * visualRatio)
                    .frostedGlass(isActive = true, cornerRadius = 68.dp * visualRatio),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = breathText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = TextLight
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
                    text = if (secondsRemaining <= 10) strings.buttonFinishBreathing else strings.buttonSkipBreathing,
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
    strings: AppStrings,
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
                text = strings.closureCategory,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontSize = 10.sp
                ),
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            ClosureCrossfade(isClosureTriggered) { triggered ->
                Text(
                    text = if (triggered) strings.closureDoneTitle else strings.closureTapPrompt,
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
                                text = strings.closureFinishedPrompt,
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
                                text = strings.closureDoneMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSlate,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // No title here: the heading right above the card already
                            // carries the same prompt, and so does the button below.
                            Text(
                                text = strings.closureLampTurnOffPrompt,
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
                            text = strings.buttonRecordMood,
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
                            text = strings.closureTapPrompt,
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
    language: AppLanguage,
    strings: AppStrings,
    onIntent: (RitualIntent) -> Unit,
    onFinish: () -> Unit
) {
    val moods = listOf(MoodTag.CALM, MoodTag.TIRED, MoodTag.SATISFIED, MoodTag.TENSE)

    RitualStepColumn { _, _ ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = strings.moodCategory,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontSize = 10.sp
                ),
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.moodTitle,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 24.sp
                ),
                color = TextLight,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.moodSubtitle,
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
                        text = strings.ritualCompletedNotice,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        ),
                        color = TextLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.workdayDoneDesc(
                            selectedMood?.let { stringResource(it.labelRes) } ?: ""
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSlate,
                        textAlign = TextAlign.Center
                    )
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
                        text = strings.returnHome,
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
                        text = strings.buttonSkipMood,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
