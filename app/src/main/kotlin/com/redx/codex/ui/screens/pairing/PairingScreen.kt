package com.redx.codex.ui.screens.pairing

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redx.codex.domain.model.ServerConfig
import com.redx.codex.ui.screens.settings.SettingsViewModel
import com.redx.codex.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

// ── Pairing / Connection Screen ───────────────────────────────────────────────
// Evidence: Phase 2 — tab switcher (QR | Manual), camera preview for QR,
//           manual form with host:port + optional token

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PairingScreen(
    onBack: () -> Unit,
    onConnected: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var tab by remember { mutableStateOf(0) } // 0=QR, 1=Manual
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("4096") }
    var token by remember { mutableStateOf("") }
    var hostError by remember { mutableStateOf<String?>(null) }

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    val uiState by viewModel.uiState.collectAsState()

    // Auto-navigate when connected
    LaunchedEffect(uiState.wsState) {
        if (uiState.wsState is com.redx.codex.data.network.WsState.Connected) {
            onConnected()
        }
    }

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
            Text("Connect to Server", style = MaterialTheme.typography.titleLarge, color = OnBackground)
        }

        // Tab row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .padding(4.dp),
        ) {
            listOf("QR Code", "Manual").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (tab == index) SurfaceElevated else Surface)
                        .clickable { tab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (tab == index) OnBackground else OnBackgroundSecondary,
                    )
                }
            }
        }

        AnimatedContent(targetState = tab, label = "pairing_tab") { currentTab ->
            when (currentTab) {
                0 -> QrScanTab(
                    cameraPermissionGranted = cameraPermission.status.isGranted,
                    onRequestPermission = { cameraPermission.launchPermissionRequest() },
                    onQrScanned = { payload ->
                        viewModel.updateServer(payload.host, payload.port, payload.token)
                    },
                )
                1 -> ManualEntryTab(
                    host = host,
                    port = port,
                    token = token,
                    hostError = hostError,
                    onHostChange = { host = it; hostError = null },
                    onPortChange = { port = it },
                    onTokenChange = { token = it },
                    onConnect = {
                        val portNum = port.toIntOrNull() ?: 4096
                        if (host.isBlank()) {
                            hostError = "Please enter server address"
                        } else {
                            hostError = null
                            viewModel.updateServer(host.trim(), portNum, token.takeIf { it.isNotBlank() })
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun QrScanTab(
    cameraPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onQrScanned: (com.redx.codex.domain.model.PairingPayload) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (cameraPermissionGranted) {
            // CameraX QR preview would go here
            // Using placeholder since actual CameraX integration is complex
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.QrCodeScanner, null, tint = AccentBlue, modifier = Modifier.size(80.dp))
                Spacer(Modifier.height(16.dp))
                Text("Point camera at QR code", style = MaterialTheme.typography.bodyMedium, color = OnBackgroundSecondary)
                Spacer(Modifier.height(8.dp))
                Text("Generated by OpenCode server", style = MaterialTheme.typography.bodySmall, color = OnBackgroundTertiary)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp),
            ) {
                Icon(Icons.Outlined.CameraAlt, null, tint = OnBackgroundSecondary, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("Camera permission required", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                Spacer(Modifier.height(8.dp))
                Text("Allow camera access to scan QR codes", style = MaterialTheme.typography.bodySmall, color = OnBackgroundSecondary)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
private fun ManualEntryTab(
    host: String,
    port: String,
    token: String,
    hostError: String?,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Text("Server Address", style = MaterialTheme.typography.labelLarge, color = OnBackgroundSecondary)
        OutlinedTextField(
            value = host,
            onValueChange = onHostChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("192.168.1.100 or hostname", color = OnBackgroundTertiary) },
            isError = hostError != null,
            supportingText = { hostError?.let { Text(it, color = AccentRed) } },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = textFieldColors(),
        )

        Text("Port", style = MaterialTheme.typography.labelLarge, color = OnBackgroundSecondary)
        OutlinedTextField(
            value = port,
            onValueChange = { if (it.length <= 5 && it.all(Char::isDigit)) onPortChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("4096", color = OnBackgroundTertiary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = textFieldColors(),
        )

        Text("Auth Token (Optional)", style = MaterialTheme.typography.labelLarge, color = OnBackgroundSecondary)
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Leave blank if no auth required", color = OnBackgroundTertiary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors(),
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onConnect,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Connect", style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = DividerColor,
    focusedContainerColor = Surface,
    unfocusedContainerColor = Surface,
    focusedTextColor = OnBackground,
    unfocusedTextColor = OnBackground,
)

// Make Box clickable
private fun Modifier.clickable(onClick: () -> Unit) = this.then(
    Modifier.clickable(onClick = onClick)
)
