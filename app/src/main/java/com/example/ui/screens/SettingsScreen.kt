package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MayaViewModel
import com.example.ui.ScreenDestination
import com.example.ui.theme.MayaBackgroundDark
import com.example.ui.theme.MayaCardDark
import com.example.ui.theme.MayaPurple
import com.example.ui.theme.SwitchTrackActive
import com.example.ui.theme.SwitchTrackInactive
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: MayaViewModel
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val settings by viewModel.settings.collectAsState()

    var pickerDialogTitle by remember { mutableStateOf<String?>(null) }
    var pickerOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var pickerCurrentValue by remember { mutableStateOf("") }
    var onPickerSelected by remember { mutableStateOf<(String) -> Unit>({}) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MayaBackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar matching reference screenshot: Back arrow & centered "Settings"
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Settings",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 48.dp) // center offset
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayaBackgroundDark)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // SECTION: GENERAL
                SettingsSectionHeader("General")

                SettingsRowClickable(
                    icon = Icons.Outlined.Language,
                    label = "Language",
                    value = settings.language,
                    onClick = {
                        pickerDialogTitle = "Select Language"
                        pickerOptions = listOf("English (US)", "English (India)", "Hindi (हिंदी)")
                        pickerCurrentValue = settings.language
                        onPickerSelected = { selected ->
                            viewModel.updateSettings(settings.copy(language = selected))
                        }
                    },
                    testTag = "settings_language"
                )

                SettingsRowClickable(
                    icon = Icons.Outlined.DarkMode,
                    label = "Theme",
                    value = settings.theme,
                    onClick = {
                        pickerDialogTitle = "Select Theme"
                        pickerOptions = listOf("Dark", "Light", "AMOLED", "System")
                        pickerCurrentValue = settings.theme
                        onPickerSelected = { selected ->
                            viewModel.updateSettings(settings.copy(theme = selected))
                        }
                    },
                    testTag = "settings_theme"
                )

                // App Accent Color Row with circular purple swatch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable {
                            pickerDialogTitle = "Select Accent Color"
                            pickerOptions = listOf("Neon Purple", "Deep Violet", "Cyber Blue", "Rose Pink")
                            pickerCurrentValue = "Neon Purple"
                            onPickerSelected = { chosen ->
                                val colorVal = when (chosen) {
                                    "Deep Violet" -> 0xFF6C5CE7
                                    "Cyber Blue" -> 0xFF388E3C
                                    "Rose Pink" -> 0xFFFF4081
                                    else -> 0xFF7C4DFF
                                }
                                viewModel.updateSettings(settings.copy(accentColor = colorVal))
                            }
                        }
                        .testTag("settings_accent_color")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ColorLens,
                        contentDescription = "Accent Color",
                        tint = TextWhite,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "App Accent Color",
                        color = TextWhite,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(settings.accentColor))
                            .border(1.dp, TextWhite.copy(alpha = 0.3f), CircleShape)
                    )
                }

                SettingsRowSwitch(
                    icon = Icons.Outlined.Vibration,
                    label = "Vibration",
                    checked = settings.vibration,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings(settings.copy(vibration = checked))
                    },
                    testTag = "settings_vibration"
                )

                SettingsRowSwitch(
                    icon = Icons.Outlined.VolumeUp,
                    label = "Sound Effects",
                    checked = settings.soundEffects,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings(settings.copy(soundEffects = checked))
                    },
                    testTag = "settings_sound_effects"
                )

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION: VOICE
                SettingsSectionHeader("Voice")

                SettingsRowClickable(
                    icon = Icons.Outlined.Language,
                    label = "Voice Language",
                    value = settings.voiceLanguage,
                    onClick = {
                        pickerDialogTitle = "Voice Language"
                        pickerOptions = listOf("English (US)", "English (India)", "Hindi (हिंदी)")
                        pickerCurrentValue = settings.voiceLanguage
                        onPickerSelected = { selected ->
                            viewModel.updateSettings(settings.copy(voiceLanguage = selected))
                        }
                    },
                    testTag = "settings_voice_language"
                )

                SettingsRowClickable(
                    icon = Icons.Outlined.RecordVoiceOver,
                    label = "Voice Style",
                    value = settings.voiceStyle,
                    onClick = {
                        viewModel.navigateTo(ScreenDestination.VOICE_SETTINGS)
                    },
                    testTag = "settings_voice_style"
                )

                SettingsRowClickable(
                    icon = Icons.Outlined.Speed,
                    label = "Speech Speed",
                    value = when {
                        settings.speechSpeed < 0.9f -> "Slow"
                        settings.speechSpeed > 1.2f -> "Fast"
                        else -> "Normal"
                    },
                    onClick = {
                        viewModel.navigateTo(ScreenDestination.VOICE_SETTINGS)
                    },
                    testTag = "settings_speech_speed"
                )

                SettingsRowClickable(
                    icon = Icons.Outlined.Tune,
                    label = "Voice Pitch",
                    value = when {
                        settings.voicePitch < 0.9f -> "Low"
                        settings.voicePitch > 1.2f -> "High"
                        else -> "Normal"
                    },
                    onClick = {
                        viewModel.navigateTo(ScreenDestination.VOICE_SETTINGS)
                    },
                    testTag = "settings_voice_pitch"
                )

                SettingsRowSwitch(
                    icon = Icons.Outlined.GraphicEq,
                    label = "Wake Word",
                    subtitle = "Hey Maya",
                    checked = settings.wakeWord,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings(settings.copy(wakeWord = checked))
                    },
                    testTag = "settings_wake_word"
                )

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION: AI
                SettingsSectionHeader("AI")

                SettingsRowClickable(
                    icon = Icons.Outlined.SmartToy,
                    label = "AI Model",
                    value = settings.aiModel,
                    onClick = {
                        viewModel.navigateTo(ScreenDestination.AI_SETTINGS)
                    },
                    testTag = "settings_ai_model"
                )

                SettingsRowClickable(
                    icon = Icons.Outlined.Psychology,
                    label = "Response Length",
                    value = settings.responseLength,
                    onClick = {
                        viewModel.navigateTo(ScreenDestination.AI_SETTINGS)
                    },
                    testTag = "settings_response_length"
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Generic selection dialog
        if (pickerDialogTitle != null) {
            AlertDialog(
                onDismissRequest = { pickerDialogTitle = null },
                title = { Text(text = pickerDialogTitle ?: "", color = TextWhite) },
                text = {
                    Column {
                        pickerOptions.forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onPickerSelected(option)
                                        pickerDialogTitle = null
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = (option == pickerCurrentValue),
                                    onClick = {
                                        onPickerSelected(option)
                                        pickerDialogTitle = null
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = MayaPurple)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = option, color = TextWhite, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { pickerDialogTitle = null }) {
                        Text("Close", color = MayaPurple)
                    }
                },
                containerColor = MayaCardDark
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = TextGray,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsRowClickable(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextWhite,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = TextWhite,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = TextGray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = TextGray,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SettingsRowSwitch(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextWhite,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TextWhite,
                fontSize = 15.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextWhite,
                checkedTrackColor = SwitchTrackActive,
                uncheckedThumbColor = TextGray,
                uncheckedTrackColor = SwitchTrackInactive
            )
        )
    }
}
