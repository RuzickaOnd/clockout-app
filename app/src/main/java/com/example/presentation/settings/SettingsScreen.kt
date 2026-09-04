package com.example.presentation.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppLanguage
import com.example.domain.model.LanguageOption
import com.example.notification.NotificationScheduler
import com.example.presentation.components.FrostedBackgroundContainer
import com.example.presentation.components.frostedGlass
import com.example.presentation.util.AppStrings
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSlate

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
    onSoundToggled: (Boolean) -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = remember(state.language) { AppStrings(state.language) }
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

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
            containerColor = Color(0xFF141526),
            titleContentColor = TextLight,
            textContentColor = TextSlate,
            title = { Text(strings.clearDataDialogTitle) },
            text = { Text(strings.clearDataDialogText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearData()
                        showResetDialog = false
                    }
                ) {
                    Text(strings.clearDataConfirm, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(strings.cancel, color = TextSlate)
                }
            }
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            containerColor = Color(0xFF141526),
            titleContentColor = TextLight,
            textContentColor = TextSlate,
            title = { Text(strings.permissionDialogTitle) },
            text = { Text(strings.permissionDeniedMessage) },
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
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = strings.settingsTitle,
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
                        IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.back,
                                tint = TextSlate,
                                modifier = Modifier.size(20.dp)
                            )
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
                                    text = strings.workdayEndTimeTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 17.sp
                                    ),
                                    color = TextLight
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (state.isNotificationEnabled) {
                                        strings.currentlySetTo(state.formattedTime)
                                    } else {
                                        strings.notificationsDisabled
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
                                    text = strings.quickPresetsLabel,
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
                                text = strings.testModeTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                ),
                                color = TextLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.testModeDesc,
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

                // Sound Toggle
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
                                text = strings.ambientSoundTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                ),
                                color = TextLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.ambientSoundDesc,
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
                            text = strings.languageSectionTitle,
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
                                            LanguageOption.SYSTEM -> "${state.selectedLanguageOption.label(state.language)} (${state.language.displayName})"
                                            else -> state.selectedLanguageOption.label(state.language)
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
                                                    text = option.label(state.language),
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

                // About Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 22.dp)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = strings.aboutTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = TextLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.aboutDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSlate,
                            lineHeight = 20.sp
                        )
                    }
                }

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
                        text = strings.clearDataButton,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
