package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ripple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.MayaViewModel
import com.example.ui.theme.MayaCyan
import com.example.ui.theme.MayaNeonBlue
import com.example.ui.theme.MayaPurple
import com.example.ui.theme.MayaPurpleLight
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import com.example.voice.VoiceState

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MayaViewModel
) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceState.collectAsState()
    val userSpokenText by viewModel.userSpokenText.collectAsState()
    val mayaResponseText by viewModel.mayaResponseText.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val pendingConfirmationStep by viewModel.pendingConfirmationStep.collectAsState()
    val pendingConfirmationWarning by viewModel.pendingConfirmationWarning.collectAsState()

    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onMicTapped()
        } else {
            showPermissionRationale = true
        }
    }

    fun handleMicClick() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.onMicTapped()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Pulse animation for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    // Deep Dark Navy Background matching reference
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF040818),
            Color(0xFF070F2B),
            Color(0xFF0B163B),
            Color(0xFF060C22)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // TOP BAR: Menu button on left, Title + Subtitle centered
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Circular hamburger menu button with dark blue outline & glow
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0E1838))
                    .border(1.5.dp, Color(0xFF28407E), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = MayaNeonBlue)
                    ) {
                        viewModel.openDrawer()
                    }
                    .testTag("home_menu_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Menu",
                    tint = TextWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Maya & "Your Voice, My World" centered
            Column(
                modifier = Modifier
                    .weight(1f)
                    .offset(x = (-23).dp), // Offset by half menu button to perfectly center
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Maya",
                    color = TextWhite,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Your Voice, My World",
                    color = Color(0xFFF1F5F9),
                    fontSize = 17.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Cursive,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.offset(y = (-2).dp)
                )
            }
        }

        // CENTER: The Maya Anime Character
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 90.dp, bottom = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.maya_character),
                contentDescription = "Maya Voice Assistant Character",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("maya_character_image")
            )
        }

        // FLOATING CONVERSATION / TRANSCRIPT CARD OVERLAY (when active)
        val showOverlay = liveTranscript.isNotBlank() ||
                voiceState == VoiceState.PROCESSING ||
                voiceState == VoiceState.SPEAKING ||
                userSpokenText.isNotBlank() ||
                errorMessage != null

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 145.dp, start = 20.dp, end = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xE60D1533))
                    .border(1.dp, MayaPurple.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (errorMessage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ $errorMessage",
                                color = Color(0xFFFF6B6B),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss Error",
                                    tint = TextGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (liveTranscript.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "You: ",
                                color = MayaCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "\"$liveTranscript\"",
                                color = TextWhite,
                                fontSize = 14.sp
                            )
                        }
                    } else if (userSpokenText.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "You: ",
                                color = MayaCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = userSpokenText,
                                color = TextWhite,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (voiceState == VoiceState.PROCESSING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MayaPurple
                            )
                            Text(
                                text = "Maya is thinking...",
                                color = TextGray,
                                fontSize = 13.sp
                            )
                        }
                    } else if (mayaResponseText.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Maya Speaking",
                                tint = MayaPurpleLight,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = mayaResponseText,
                                color = TextWhite,
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (voiceState == VoiceState.SPEAKING) {
                                IconButton(
                                    onClick = { viewModel.stopSpeaking() },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Speech",
                                        tint = MayaPurpleLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM: Neon Horizon Curved Arc + 4-Point Star + Mic Button + "Tap to speak"
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(130.dp)
        ) {
            val canvasWidth = maxWidth
            val canvasHeight = maxHeight

            // Draw glowing curved arc horizon matching reference
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val w = size.width
                val h = size.height

                val arcPath = Path().apply {
                    moveTo(0f, h * 0.58f)
                    // Quadratic bezier curve arching upwards to the center
                    quadraticTo(
                        w * 0.5f, h * 0.12f,
                        w, h * 0.58f
                    )
                }

                // Outer soft halo
                drawPath(
                    path = arcPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MayaNeonBlue.copy(alpha = 0.35f),
                            MayaPurple.copy(alpha = 0.55f),
                            MayaNeonBlue.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Crisp inner arc
                drawPath(
                    path = arcPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MayaNeonBlue.copy(alpha = 0.85f),
                            Color(0xFF9070FF),
                            MayaNeonBlue.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4-Point Sparkle Star on the right side of the arc (matching reference screenshot)
            Text(
                text = "✦",
                color = Color(0xFFA6C4FF),
                fontSize = 24.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 42.dp, top = 22.dp)
            )

            // Push-to-Talk Mic Button centered on the arc
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isListening = voiceState == VoiceState.LISTENING
                val isSpeaking = voiceState == VoiceState.SPEAKING
                val isProcessing = voiceState == VoiceState.PROCESSING

                val currentScale = if (isListening) pulseScale else 1f

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .scale(currentScale)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            handleMicClick()
                        }
                        .testTag("push_to_talk_mic_button")
                ) {
                    // Outer glowing circular ring
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        MayaNeonBlue.copy(alpha = if (isListening) haloAlpha else 0.8f),
                                        MayaPurpleLight,
                                        MayaCyan,
                                        MayaNeonBlue.copy(alpha = if (isListening) haloAlpha else 0.8f)
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // Inner circle
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isListening) listOf(
                                        Color(0xFF4A148C),
                                        Color(0xFF1A237E)
                                    ) else listOf(
                                        Color(0xFF28346A),
                                        Color(0xFF141A38)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = TextWhite
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Push to Talk Microphone",
                                tint = TextWhite,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Label below mic button matching reference screenshot: "Tap to speak"
                val labelText = when (voiceState) {
                    VoiceState.LISTENING -> "Listening..."
                    VoiceState.PROCESSING -> "Processing..."
                    VoiceState.SPEAKING -> "Tap to pause"
                    else -> "Tap to speak"
                }

                Text(
                    text = labelText,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("tap_to_speak_label")
                )
            }
        }

        if (pendingConfirmationStep != null) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelPendingAction() },
                title = { Text("Confirm Action", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = pendingConfirmationWarning ?: "Are you sure you want to execute this action?",
                        color = Color(0xFFD1D5DB),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmPendingAction() },
                        colors = ButtonDefaults.buttonColors(containerColor = MayaPurple)
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelPendingAction() }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = Color(0xFF0F1738)
            )
        }
    }
}
