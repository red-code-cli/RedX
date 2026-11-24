package com.redx.codex.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.redx.codex.ui.theme.WaveformActive
import com.redx.codex.ui.theme.WaveformIdle
import kotlin.math.*

// ── Voice Waveform (Canvas fallback — no AGSL runtime shaders needed) ─────────
// Evidence: Phase 3 — spring-animated ECG/audio waveform at ~15 bars,
//           AGSL shader uses same math as a sin-based bar chart.

@Composable
fun VoiceWaveform(
    isActive: Boolean,
    amplitudes: List<Float>,   // 0f..1f per bar
    modifier: Modifier = Modifier,
    barCount: Int = 24,
    activeColor: Color = WaveformActive,
    idleColor: Color = WaveformIdle,
) {
    val animatedAmplitudes = amplitudes.take(barCount).map { target ->
        val anim = remember { Animatable(0f) }
        LaunchedEffect(target, isActive) {
            anim.animateTo(
                targetValue = if (isActive) target else 0.08f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
        anim.value
    }

    // ECG idle animation
    val idlePhase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "ecg_phase",
    )

    Canvas(modifier = modifier) {
        val barWidth = (size.width / barCount) * 0.6f
        val gap = (size.width / barCount) * 0.4f
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val x = i * (barWidth + gap) + barWidth / 2f
            val amp = if (isActive) {
                animatedAmplitudes.getOrElse(i) { 0.08f }
            } else {
                // ECG idle: sin wave
                0.08f + 0.07f * sin(idlePhase + i * 0.4f)
            }
            val halfH = (size.height * 0.5f * amp).coerceAtLeast(4f)

            drawLine(
                color = if (isActive) activeColor else idleColor,
                start = Offset(x, centerY - halfH),
                end = Offset(x, centerY + halfH),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

// ── Pulsing dot (connecting state indicator) ──────────────────────────────────
@Composable
fun PulsingDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot_scale",
    )
    Canvas(modifier = modifier.size(10.dp)) {
        drawCircle(color = color, radius = size.minDimension / 2f * scale)
    }
}
