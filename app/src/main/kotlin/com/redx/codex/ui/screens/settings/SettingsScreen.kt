package com.redx.codex.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redx.codex.BuildConfig
import com.redx.codex.data.network.WsState
import com.redx.codex.ui.theme.*

// ── Settings Screen ───────────────────────────────────────────────────────────
// Evidence: Phase 2 — nested settings sections: Server, Appearance, Sounds,
//           About. Each row has icon, title, value/toggle, chevron if navigable.

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPairingClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = OnBackground)
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = OnBackground)
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                // ── Server Section ────────────────────────────────────────
                SettingsSection(title = "Server") {
                    // Connection status
                    val config = settings.serverConfig
                    val wsState = state.wsState
                    SettingsRow(
                        icon = Icons.Outlined.Cloud,
                        title = "Server",
                        value = when {
                            config == null -> "Not configured"
                            wsState is WsState.Connected -> "${config.host}:${config.port}"
                            wsState is WsState.Connecting -> "Connecting…"
                            else -> "${config.host}:${config.port} (offline)"
                        },
                        valueColor = when (wsState) {
                            is WsState.Connected -> StatusConnected
                            is WsState.Connecting -> StatusConnecting
                            else -> OnBackgroundSecondary
                        },
                        onClick = onPairingClick,
                    )

                    if (settings.serverConfig != null) {
                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            icon = Icons.Outlined.LinkOff,
                            title = "Disconnect",
                            titleColor = AccentRed,
                            onClick = { viewModel.disconnect() },
                        )
                    }
                }
            }

            item {
                // ── Appearance ────────────────────────────────────────────
                SettingsSection(title = "Appearance") {
                    SettingsToggleRow(
                        icon = Icons.Outlined.FormatSize,
                        title = "Show Token Usage",
                        checked = settings.showTokenUsage,
                        onToggle = viewModel::setShowTokens,
                    )
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSliderRow(
                        icon = Icons.Outlined.TextFields,
                        title = "Message Font Size",
                        value = settings.messageFontSizeSp.toFloat(),
                        range = 12f..20f,
                        label = "${settings.messageFontSizeSp}sp",
                        onValueChange = { viewModel.setFontSize(it.toInt()) },
                    )
                }
            }

            item {
                // ── Sounds & Haptics ──────────────────────────────────────
                SettingsSection(title = "Sounds & Haptics") {
                    SettingsToggleRow(
                        icon = Icons.Outlined.VolumeUp,
                        title = "Sound Effects",
                        checked = settings.soundEffects,
                        onToggle = viewModel::setSounds,
                    )
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggleRow(
                        icon = Icons.Outlined.Vibration,
                        title = "Haptic Feedback",
                        checked = settings.hapticFeedback,
                        onToggle = viewModel::setHaptic,
                    )
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggleRow(
                        icon = Icons.Outlined.Keyboard,
                        title = "Send with Enter",
                        checked = settings.sendWithEnter,
                        onToggle = viewModel::setSendWithEnter,
                    )
                }
            }

            item {
                // ── About ─────────────────────────────────────────────────
                SettingsSection(title = "About") {
                    SettingsRow(
                        icon = Icons.Outlined.Info,
                        title = "Version",
                        value = "RedX v${BuildConfig.VERSION_NAME}",
                    )
                }
            }
        }
    }
}

// ── Reusable settings composables ─────────────────────────────────────────────

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = OnBackgroundSecondary,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface),
            content = content,
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    titleColor: androidx.compose.ui.graphics.Color = OnBackground,
    valueColor: androidx.compose.ui.graphics.Color = OnBackgroundSecondary,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = titleColor, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = titleColor, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor)
        }
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, null, tint = OnBackgroundTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = OnBackground, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = OnBackground, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ColorOnPrimary,
                checkedTrackColor = AccentBlue,
                uncheckedThumbColor = OnBackgroundSecondary,
                uncheckedTrackColor = SurfaceElevated,
            ),
        )
    }
}

@Composable
private fun SettingsSliderRow(
    icon: ImageVector,
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    label: String,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = OnBackground, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, color = OnBackground, modifier = Modifier.weight(1f))
            Text(label, style = MaterialTheme.typography.labelMedium, color = OnBackgroundSecondary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = ((range.endInclusive - range.start).toInt() - 1),
            colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue),
        )
    }
}
