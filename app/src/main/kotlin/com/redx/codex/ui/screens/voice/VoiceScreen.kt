package com.redx.codex.ui.screens.voice

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.redx.codex.ui.components.VoiceWaveform
import com.redx.codex.ui.theme.*

// ── Voice Call Screen ─────────────────────────────────────────────────────────
// Evidence: Phase 2 — full-screen dark overlay, waveform centered,
//           duration timer top, mute + end-call buttons bottom
//           Phase 3 — ECG idle animation → active bars when speaking

@Composable
fun VoiceScreen(
    sessionTitle: String,
    onBack: () -> Unit,
) {
    var isMuted by remember { mutableStateOf(false) }
    var isAiSpeaking by remember { mutableStateOf(false) }
    var userSpeaking by remember { mutableStateOf(false) }
    var durationSeconds by remember { mutableStateOf(0) }

    // Simulate timer
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            durationSeconds++
        }
    }

    // Simulate speaking states
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            isAiSpeaking = !isAiSpeaking
        }
    }

    // Fake amplitudes
    val amplitudes = remember {
        List(24) { (Math.random() * 0.8f + 0.1f).toFloat() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = OnBackground)
                }
            }

            Spacer(Modifier.weight(1f))

            // Session label
            Text(
                text = sessionTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = OnBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isAiSpeaking) "AI is speaking…" else if (isMuted) "Muted" else "Listening…",
                style = MaterialTheme.typography.bodyMedium,
                color = OnBackgroundSecondary,
            )

            Spacer(Modifier.height(32.dp))

            // Waveform
            VoiceWaveform(
                isActive = isAiSpeaking || userSpeaking,
                amplitudes = amplitudes,
                activeColor = if (isAiSpeaking) AccentGreen else AccentBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 32.dp),
            )

            Spacer(Modifier.height(24.dp))

            // Duration
            Text(
                text = formatDuration(durationSeconds),
                style = MaterialTheme.typography.headlineLarge,
                color = OnBackground,
            )

            Spacer(Modifier.weight(1f))

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Mute
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) SurfaceElevated else Surface),
                    ) {
                        Icon(
                            if (isMuted) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                            "Mute",
                            tint = if (isMuted) AccentRed else OnBackground,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(if (isMuted) "Unmute" else "Mute", style = MaterialTheme.typography.labelSmall, color = OnBackgroundSecondary)
                }

                // End call
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AccentRed),
                    ) {
                        Icon(
                            Icons.Outlined.CallEnd,
                            "End Call",
                            tint = ColorOnPrimary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("End Call", style = MaterialTheme.typography.labelSmall, color = OnBackgroundSecondary)
                }

                // Speaker (placeholder)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Surface),
                    ) {
                        Icon(Icons.Outlined.VolumeUp, "Speaker", tint = OnBackground, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Speaker", style = MaterialTheme.typography.labelSmall, color = OnBackgroundSecondary)
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
