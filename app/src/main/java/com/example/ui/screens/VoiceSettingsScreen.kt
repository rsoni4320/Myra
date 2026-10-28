package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VolumeDown
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MayaViewModel
import com.example.ui.components.MayaWaveform
import com.example.ui.theme.MayaBackgroundDark
import com.example.ui.theme.MayaCardDark
import com.example.ui.theme.MayaCardSelected
import com.example.ui.theme.MayaPurple
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import com.example.voice.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: MayaViewModel
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val settings by viewModel.settings.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val rmsDb by viewModel.rmsDb.collectAsState()

    val isSpeaking = voiceState == VoiceState.SPEAKING

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MayaBackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Voice Settings",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 48.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("voice_settings_back")
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
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                // Audio Waveform Visualization matching Screen 4
                MayaWaveform(
                    isSpeaking = isSpeaking,
                    rmsDb = rmsDb,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Style Section
                Text(
                    text = "Voice Style",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val styles = listOf("Friendly", "Professional", "Calm", "Funny")
                    styles.forEach { style ->
                        val isSelected = settings.voiceStyle.equals(style, ignoreCase = true)
                        val bg = if (isSelected) MayaCardSelected else MayaCardDark
                        val border = if (isSelected) MayaPurple else Color(0xFF242F55)
                        val textColor = if (isSelected) TextWhite else TextGray

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateSettings(settings.copy(voiceStyle = style))
                                }
                                .padding(vertical = 10.dp)
                                .testTag("voice_style_$style"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = style,
                                color = textColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Speech Speed Section
                Text(
                    text = "Speech Speed",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = "Slow Speed",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = settings.speechSpeed,
                        onValueChange = { newSpeed ->
                            viewModel.updateSettings(settings.copy(speechSpeed = newSpeed))
                        },
                        valueRange = 0.6f..1.6f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .testTag("speech_speed_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MayaPurple,
                            activeTrackColor = MayaPurple,
                            inactiveTrackColor = Color(0xFF1E284D)
                        )
                    )
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = "Fast Speed",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = when {
                        settings.speechSpeed < 0.85f -> "Slow"
                        settings.speechSpeed > 1.25f -> "Fast"
                        else -> "Normal"
                    },
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Voice Pitch Section
                Text(
                    text = "Voice Pitch",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = "Low Pitch",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = settings.voicePitch,
                        onValueChange = { newPitch ->
                            viewModel.updateSettings(settings.copy(voicePitch = newPitch))
                        },
                        valueRange = 0.6f..1.6f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .testTag("voice_pitch_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MayaPurple,
                            activeTrackColor = MayaPurple,
                            inactiveTrackColor = Color(0xFF1E284D)
                        )
                    )
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = "High Pitch",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = when {
                        settings.voicePitch < 0.85f -> "Deep"
                        settings.voicePitch > 1.25f -> "High"
                        else -> "Normal"
                    },
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Volume Section
                Text(
                    text = "Volume",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VolumeDown,
                        contentDescription = "Volume Down",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = settings.volume.toFloat(),
                        onValueChange = { newVol ->
                            viewModel.updateSettings(settings.copy(volume = newVol.toInt()))
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .testTag("volume_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MayaPurple,
                            activeTrackColor = MayaPurple,
                            inactiveTrackColor = Color(0xFF1E284D)
                        )
                    )
                    Text(
                        text = "${settings.volume}%",
                        color = TextGray,
                        fontSize = 13.sp,
                        modifier = Modifier.width(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Preview Voice Action Button matching Screen 4
                Button(
                    onClick = { viewModel.previewVoice() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("preview_voice_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MayaCardSelected)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preview Voice",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
