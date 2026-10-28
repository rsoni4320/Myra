package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MayaCyan
import com.example.ui.theme.MayaNeonBlue
import com.example.ui.theme.MayaPurple
import kotlin.math.sin

@Composable
fun MayaWaveform(
    modifier: Modifier = Modifier,
    isSpeaking: Boolean = false,
    rmsDb: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val barCount = 48
        val spacing = size.width / barCount
        val barWidth = spacing * 0.45f
        val centerY = size.height / 2f

        val gradient = Brush.verticalGradient(
            colors = listOf(
                MayaPurple,
                MayaNeonBlue,
                MayaCyan,
                MayaNeonBlue,
                MayaPurple
            )
        )

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / barCount
            // Bell curve envelope so bars are taller in the center
            val envelope = sin(normalizedX * Math.PI.toFloat())

            val wave1 = sin(normalizedX * 12f + phase) * 0.5f + 0.5f
            val wave2 = sin(normalizedX * 6f - phase * 0.8f) * 0.5f + 0.5f
            val composite = (wave1 + wave2) / 2f

            val dynamicFactor = if (isSpeaking) 1.5f else if (rmsDb > 0f) (rmsDb / 6f).coerceIn(0.8f, 2.0f) else 1.0f
            val barHeight = ((size.height * 0.85f * envelope * composite * dynamicFactor)
                .coerceIn(6f, size.height * 0.95f))

            val x = i * spacing + (spacing - barWidth) / 2f
            val y = centerY - barHeight / 2f

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
