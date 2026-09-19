package org.clockout.presentation.settings

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.clockout.BuildConfig
import org.clockout.R
import org.clockout.data.local.SoundImportError
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.MeditationSoundStyle
import org.clockout.domain.model.SoundChoice
import org.clockout.notification.NotificationScheduler
import org.clockout.presentation.components.FrostedBackgroundContainer
import org.clockout.presentation.components.frostedGlass
import org.clockout.presentation.components.ContentMaxWidth
import org.clockout.presentation.components.maxContentWidth
import org.clockout.presentation.util.labelRes
import org.clockout.presentation.util.localizedDescription
import org.clockout.presentation.util.localizedLabel
import org.clockout.ui.theme.DialogSurface
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.IndigoPrimary
import org.clockout.ui.theme.TextLight
import org.clockout.ui.theme.TextMuted
import org.clockout.ui.theme.TextSlate
import org.clockout.domain.model.TelemetryConsent
import org.clockout.util.openPlayStoreListing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onBack: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit = {},
    onLanguageOptionSelected: (LanguageOption) -> Unit = {},
    onNotificationToggled: (Boolean) -> Unit = {},
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onTestModeToggled: (Boolean) -> Unit,
    onIncludeWeekendsToggled: (Boolean) -> Unit = {},
    onSoundToggled: (Boolean) -> Unit,
    onSoundChoiceSelected: (SoundChoice) -> Unit = {},
    onTogglePreviewSound: (SoundChoice) -> Unit = {},
    onUploadCustomSound: (Uri) -> Unit = {},
    onReplaceCustomSound: (Uri, CustomSound) -> Unit = { _, _ -> },
    onDeleteCustomSound: (CustomSound) -> Unit = {},
    onDismissSoundImportError: () -> Unit = {},
    onOpenBreathingStyle: () -> Unit = {},
    onTelemetryConsentToggled: (Boolean) -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onClearData: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * False once a navigation rail is showing, because then this screen is a destination the user
     * picks rather than a detour they came into and have to step back out of.
     */
    showBackButton: Boolean = true
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var soundPendingDeletion by remember { mutableStateOf<CustomSound?>(null) }
    // Set when the picker was opened from a sound whose file is gone, so the file that comes
    // back replaces that entry instead of arriving as an eleventh sound.
    var soundPendingReplacement by remember { mutableStateOf<CustomSound?>(null) }

    val customSoundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val replaced = soundPendingReplacement
        soundPendingReplacement = null
        if (uri != null) {
            if (replaced != null) onReplaceCustomSound(uri, replaced) else onUploadCustomSound(uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onNotificationToggled(true)
        } else {
            showPermissionDialog = true
        }
    }

    val presetTimes = listOf(
        Pair(16, 0),
        Pair(16, 30),
        Pair(17, 0),
        Pair(17, 30),
        Pair(18, 0),
        Pair(18, 30),
        Pair(19, 0)
    )

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = DialogSurface,
            titleContentColor = TextLight,
            textContentColor = TextSlate,
            title = { Text(stringResource(R.string.settings_clear_data_dialog_title)) },
            text = { Text(stringResource(R.string.settings_clear_data_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearData()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.settings_clear_data_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.common_cancel), color = TextSlate)
                }
            }
        )
    }

    val pendingDeletion = soundPendingDeletion
    if (pendingDeletion != null) {
        AlertDialog(
            onDismissRequest = { soundPendingDeletion = null },
            containerColor = DialogSurface,
            titleContentColor = TextLight,
            textContentColor = TextSlate,
            title = { Text(stringResource(R.string.settings_delete_custom_sound_title)) },
            text = { Text(stringResource(R.string.settings_delete_custom_sound_text, pendingDeletion.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustomSound(pendingDeletion)
                        soundPendingDeletion = null
                    },
                    modifier = Modifier.testTag("confirm_delete_custom_sound")
                ) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { soundPendingDeletion = null }) {
                    Text(stringResource(R.string.common_cancel), color = TextSlate)
                }
            }
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            containerColor = DialogSurface,
            titleContentColor = TextLight,
            textContentColor = TextSlate,
            title = { Text(stringResource(R.string.settings_permission_dialog_title)) },
            text = { Text(stringResource(R.string.settings_permission_denied_message)) },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK", color = IndigoAccent)
                }
            }
        )
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
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        color = TextLight
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.common_back),
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Notification reminder time & Switch
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 22.dp)
                        .padding(20.dp)
                        .testTag("workday_time_card")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_workday_end_time_title),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 17.sp
                                    ),
                                    color = TextLight
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (state.isNotificationEnabled) {
                                        stringResource(R.string.settings_currently_set_to, state.formattedTime)
                                    } else {
                                        stringResource(R.string.settings_notifications_disabled)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.isNotificationEnabled) IndigoAccent else TextSlate
                                )
                            }

                            Switch(
                                checked = state.isNotificationEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        if (NotificationScheduler.hasNotificationPermission(context)) {
                                            onNotificationToggled(true)
                                        } else {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                onNotificationToggled(true)
                                            }
                                        }
                                    } else {
                                        onNotificationToggled(false)
                                    }
                                },
                                modifier = Modifier.testTag("workday_notification_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = IndigoPrimary,
                                    uncheckedThumbColor = TextSlate,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                                    uncheckedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }

                        AnimatedVisibility(
                            visible = state.isNotificationEnabled,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.settings_quick_presets_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSlate
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    presetTimes.forEach { (h, m) ->
                                        val isSelected = state.notificationHour == h && state.notificationMinute == m
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onTimeSelected(h, m) },
                                            label = {
                                                Text(String.format("%02d:%02d", h, m))
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = Color.White.copy(alpha = 0.04f),
                                                labelColor = TextSlate,
                                                selectedContainerColor = Color(0x3B6366F1),
                                                selectedLabelColor = IndigoAccent
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) Color(0x806366F1) else Color.White.copy(alpha = 0.10f)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("preset_time_${h}_${m}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Which days take part: weekdays only, or the weekend too
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 22.dp)
                        .padding(20.dp)
                        .testTag("include_weekends_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_include_weekends_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                ),
                                color = TextLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.settings_include_weekends_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSlate
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = state.includeWeekends,
                            onCheckedChange = onIncludeWeekendsToggled,
                            modifier = Modifier.testTag("include_weekends_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = IndigoPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }

                // Test Mode / Anytime Access Switch
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 22.dp)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_test_mode_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                ),
                                color = TextLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.settings_test_mode_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSlate
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = state.isTestModeEnabled,
                            onCheckedChange = onTestModeToggled,
                            modifier = Modifier.testTag("test_mode_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = IndigoPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }

                // Sound & Style Card
                var soundDropdownExpanded by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 22.dp)
                        .padding(20.dp)
                        .testTag("sound_settings_card")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_ambient_sound_title),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    ),
                                    color = TextLight
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.settings_ambient_sound_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSlate
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = state.isSoundEnabled,
                                onCheckedChange = onSoundToggled,
                                modifier = Modifier.testTag("sound_enabled_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = IndigoPrimary,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }

                        if (state.isSoundEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(R.string.settings_sound_style_title),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = TextLight
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Sound Style Dropdown
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(
                                            width = 1.dp,
                                            color = if (soundDropdownExpanded) IndigoAccent else Color.White.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { soundDropdownExpanded = !soundDropdownExpanded }
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                        .testTag("sound_style_dropdown_trigger"),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = soundChoiceIcon(state.soundChoice),
                                                contentDescription = null,
                                                tint = IndigoAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = state.soundChoice.localizedLabel(),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 15.sp
                                                ),
                                                color = TextLight,
                                                maxLines = 1
                                            )
                                        }

                                        Icon(
                                            imageVector = if (soundDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = if (soundDropdownExpanded) "Collapse" else "Expand",
                                            tint = TextSlate,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = soundDropdownExpanded,
                                    onDismissRequest = { soundDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(Color(0xFF181A2F))
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                ) {
                                    MenuSectionLabel(stringResource(R.string.settings_built_in_sound_group_label))

                                    MeditationSoundStyle.entries.forEach { style ->
                                        val choice = SoundChoice.BuiltIn(style)
                                        val isSelected = choice.id == state.soundChoice.id
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = soundChoiceIcon(choice),
                                                    contentDescription = null,
                                                    tint = if (isSelected) IndigoAccent else TextMuted,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            text = {
                                                SoundChoiceMenuText(
                                                    title = choice.localizedLabel(),
                                                    description = choice.localizedDescription(),
                                                    isSelected = isSelected
                                                )
                                            },
                                            trailingIcon = if (isSelected) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = IndigoAccent,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            } else null,
                                            onClick = {
                                                onSoundChoiceSelected(choice)
                                                soundDropdownExpanded = false
                                            },
                                            modifier = Modifier.testTag("sound_style_${style.id}")
                                        )
                                    }

                                    if (state.customSounds.isNotEmpty()) {
                                        MenuSectionLabel(stringResource(R.string.settings_custom_sound_group_label))
                                    }

                                    state.customSounds.forEach { customSound ->
                                        val choice = SoundChoice.Custom(customSound)
                                        val isSelected = choice.id == state.soundChoice.id
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = when {
                                                        !customSound.isAvailable -> MissingFileAmber
                                                        isSelected -> IndigoAccent
                                                        else -> TextMuted
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            text = {
                                                SoundChoiceMenuText(
                                                    title = choice.localizedLabel(),
                                                    description = choice.localizedDescription(),
                                                    isSelected = isSelected
                                                )
                                            },
                                            trailingIcon = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = IndigoAccent,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            soundDropdownExpanded = false
                                                            soundPendingDeletion = customSound
                                                        },
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .testTag("delete_custom_sound_${customSound.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = stringResource(R.string.common_delete),
                                                            tint = TextMuted,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                soundDropdownExpanded = false
                                                if (customSound.isAvailable) {
                                                    onSoundChoiceSelected(choice)
                                                } else {
                                                    soundPendingReplacement = customSound
                                                    customSoundPickerLauncher.launch(AUDIO_MIME_FILTER)
                                                }
                                            },
                                            modifier = Modifier.testTag("sound_style_${customSound.id}")
                                        )
                                    }

                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.LibraryMusic,
                                                contentDescription = null,
                                                tint = IndigoAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = stringResource(R.string.settings_upload_custom_sound_button),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = IndigoAccent
                                            )
                                        },
                                        onClick = {
                                            soundDropdownExpanded = false
                                            customSoundPickerLauncher.launch(AUDIO_MIME_FILTER)
                                        },
                                        modifier = Modifier.testTag("upload_custom_sound_menu_item")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.soundChoice.localizedDescription(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSlate,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            )

                            val isSilentChoice = state.soundChoice.let {
                                it is SoundChoice.BuiltIn && it.style == MeditationSoundStyle.SILENT_HAPTIC
                            }
                            if (!isSilentChoice) {
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = { onTogglePreviewSound(state.soundChoice) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sound_preview_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (state.isPreviewPlaying) IndigoAccent else Color.White.copy(alpha = 0.15f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (state.isPreviewPlaying) IndigoPrimary.copy(alpha = 0.25f) else Color.Transparent
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (state.isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (state.isPreviewPlaying) IndigoAccent else TextLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (state.isPreviewPlaying) {
                                            stringResource(R.string.settings_sound_preview_playing)
                                        } else {
                                            "${stringResource(R.string.settings_sound_preview_button)} (${state.soundChoice.localizedLabel()})"
                                        },
                                        color = if (state.isPreviewPlaying) IndigoAccent else TextLight,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(R.string.settings_custom_sound_section_title),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = TextLight
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.settings_custom_sound_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSlate,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.settings_custom_sound_backup_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { customSoundPickerLauncher.launch(AUDIO_MIME_FILTER) },
                                enabled = !state.isImportingSound,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upload_custom_sound_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = IndigoPrimary.copy(alpha = 0.15f)
                                )
                            ) {
                                if (state.isImportingSound) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = IndigoAccent,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.LibraryMusic,
                                        contentDescription = null,
                                        tint = IndigoAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isImportingSound) {
                                        stringResource(R.string.settings_uploading_custom_sound)
                                    } else {
                                        stringResource(R.string.settings_upload_custom_sound_button)
                                    },
                                    color = IndigoAccent,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                                )
                            }

                            val importError = state.soundImportError
                            if (importError != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                        .clickable { onDismissSoundImportError() }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .testTag("custom_sound_error"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (importError) {
                                            SoundImportError.UNSUPPORTED_FILE -> stringResource(R.string.settings_custom_sound_error_unsupported)
                                            SoundImportError.FILE_TOO_LARGE -> stringResource(R.string.settings_custom_sound_error_too_large)
                                            SoundImportError.LIMIT_REACHED -> stringResource(R.string.settings_custom_sound_error_limit)
                                            SoundImportError.READ_FAILED -> stringResource(R.string.settings_custom_sound_error_read)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.5.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Breathing animation style: right after the sound card, because both are about
                // how the ritual feels rather than when it happens. The choosing happens on its
                // own screen, which has room for running previews and for styles added later.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 22.dp)
                        .clickable(onClick = onOpenBreathingStyle)
                        .padding(20.dp)
                        .testTag("breathing_style_card"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_breathing_style_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = TextLight
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(state.breathingStyle.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = IndigoAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.settings_breathing_style_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextSlate,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // App Language Dropdown Card (penultimate position, before About Padla)
                var languageDropdownExpanded by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 22.dp)
                        .padding(20.dp)
                        .testTag("language_selection_card")
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.language_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = TextLight
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(
                                        width = 1.dp,
                                        color = if (languageDropdownExpanded) IndigoAccent else Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { languageDropdownExpanded = !languageDropdownExpanded }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .testTag("language_dropdown_trigger"),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = IndigoAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        val displayLabel = when (state.selectedLanguageOption) {
                                            LanguageOption.SYSTEM -> "${state.selectedLanguageOption.localizedLabel()} (${state.language.displayName})"
                                            else -> state.selectedLanguageOption.localizedLabel()
                                        }
                                        Text(
                                            text = displayLabel,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp
                                            ),
                                            color = TextLight
                                        )
                                    }

                                    Icon(
                                        imageVector = if (languageDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = if (languageDropdownExpanded) "Collapse" else "Expand",
                                        tint = TextSlate,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = languageDropdownExpanded,
                                onDismissRequest = { languageDropdownExpanded = false },
                                modifier = Modifier
                                    .background(Color(0xFF181A2F))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            ) {
                                LanguageOption.entries.forEach { option ->
                                    val isSelected = option == state.selectedLanguageOption
                                    DropdownMenuItem(
                                        text = {
                                            Column(verticalArrangement = Arrangement.Center) {
                                                Text(
                                                    text = option.localizedLabel(),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                    ),
                                                    color = if (isSelected) IndigoAccent else TextLight
                                                )
                                                if (option == LanguageOption.SYSTEM) {
                                                    Text(
                                                        text = AppLanguage.fromSystemLocale().displayName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) IndigoAccent.copy(alpha = 0.75f) else TextSlate
                                                    )
                                                }
                                            }
                                        },
                                        trailingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = IndigoAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            onLanguageOptionSelected(option)
                                            onLanguageSelected(option.resolveEffectiveLanguage())
                                            languageDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("lang_option_${option.code}")
                                    )
                                }
                            }
                        }
                    }
                }

                // Anonymous diagnostics: the way back from a "no" in the opt-in dialog, and the
                // only place the privacy policy is reachable once that dialog is gone.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 22.dp)
                        .padding(20.dp)
                        .testTag("diagnostics_card")
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_diagnostics_title),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    ),
                                    color = TextLight
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.settings_diagnostics_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSlate,
                                    lineHeight = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = state.telemetryConsent == TelemetryConsent.GRANTED,
                                onCheckedChange = onTelemetryConsentToggled,
                                modifier = Modifier.testTag("telemetry_consent_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = IndigoPrimary,
                                    uncheckedThumbColor = TextSlate,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                                    uncheckedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenPrivacyPolicy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("privacy_policy_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.settings_privacy_policy_button),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                // Rate the app: sits above the About card, so a user who has just read what the
                // app is for meets the ask right afterwards rather than next to the destructive
                // "Clear data" button at the bottom.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 22.dp)
                        .padding(20.dp)
                        .testTag("rate_app_card")
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.StarRate,
                                contentDescription = null,
                                tint = IndigoAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.settings_rate_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                ),
                                color = TextLight
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.settings_rate_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSlate,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { context.openPlayStoreListing() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("rate_app_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.settings_rate_button),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                // About Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 22.dp)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_about_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = TextLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.settings_about_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSlate,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Build identification, kept right under the About card so a user reporting a
                // problem can read the exact build back without leaving Settings.
                Text(
                    text = "${stringResource(R.string.settings_app_version_label)} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_version")
                )

                // Reset Data Button
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33EF4444)),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.settings_clear_data_button),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private const val AUDIO_MIME_FILTER = "audio/*"

/** Marks a custom sound whose audio file is gone - the same amber the import errors use. */
private val MissingFileAmber = Color(0xFFFCA5A5)

private fun soundChoiceIcon(choice: SoundChoice): ImageVector = when (choice) {
    is SoundChoice.Custom -> Icons.Default.MusicNote
    is SoundChoice.BuiltIn -> when (choice.style) {
        MeditationSoundStyle.TIBETAN_BOWL -> Icons.Default.GraphicEq
        MeditationSoundStyle.RAIN_PINK_NOISE -> Icons.Default.WaterDrop
        MeditationSoundStyle.BREATHING_WAVES -> Icons.Default.Waves
        MeditationSoundStyle.SILENT_HAPTIC -> Icons.Default.Vibration
    }
}

@Composable
private fun MenuSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = TextMuted,
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun SoundChoiceMenuText(title: String, description: String, isSelected: Boolean) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) IndigoAccent else TextLight
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = TextSlate,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
