package com.redx.codex.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redx.codex.data.network.WsState
import com.redx.codex.domain.model.Session
import com.redx.codex.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ── Home Screen ───────────────────────────────────────────────────────────────
// Evidence: Phase 2 — session list with folder groups, pinned section, FAB,
//           hamburger menu top-left, model indicator top-center

@Composable
fun HomeScreen(
    onSessionClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onPairingClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showDrawer by remember { mutableStateOf(false) }
    var showNewSessionDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Scaffold(
            containerColor = Background,
            topBar = {
                HomeTopBar(
                    wsState = state.wsState,
                    modelName = state.settings.selectedModelId ?: "No Model",
                    onMenuClick = { showDrawer = true },
                    onSettingsClick = onSettingsClick,
                )
            },
            floatingActionButton = {
                NewChatFab(onClick = { showNewSessionDialog = true })
            },
        ) { paddingValues ->
            if (state.sessions.isEmpty() && state.wsState is WsState.Disconnected) {
                // Not connected — show pairing prompt
                NotConnectedPlaceholder(
                    onPairClick = onPairingClick,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                )
            } else {
                SessionList(
                    sessions = state.sessions,
                    onSessionClick = onSessionClick,
                    onSessionDelete = viewModel::deleteSession,
                    onSessionPin = { id, pinned -> viewModel.pinSession(id, pinned) },
                    onSessionRename = viewModel::renameSession,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                )
            }
        }

        // Side drawer
        AnimatedVisibility(
            visible = showDrawer,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it },
        ) {
            SessionsDrawerContent(
                sessions = state.sessions,
                folders = state.folders,
                onSessionClick = { onSessionClick(it); showDrawer = false },
                onClose = { showDrawer = false },
                onNewSession = { showNewSessionDialog = true },
            )
        }
    }

    if (showNewSessionDialog) {
        NewSessionDialog(
            onCreate = { title ->
                viewModel.createNewSession(title)
                showNewSessionDialog = false
            },
            onDismiss = { showNewSessionDialog = false },
        )
    }
}

@Composable
private fun HomeTopBar(
    wsState: WsState,
    modelName: String,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Surface(color = Background, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = OnBackground)
            }

            // Connection status + model
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val dotColor = when (wsState) {
                    is WsState.Connected -> StatusConnected
                    is WsState.Connecting -> StatusConnecting
                    else -> StatusDisconnected
                }
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = OnBackground)
            }
        }
    }
}

@Composable
private fun NewChatFab(onClick: () -> Unit) {
    // Spring-based scale animation on appear
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "fab_scale",
    )

    FloatingActionButton(
        onClick = onClick,
        containerColor = AccentBlue,
        contentColor = ColorOnPrimary,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(bottom = 16.dp, end = 4.dp),
    ) {
        Icon(Icons.Default.Add, contentDescription = "New Chat", modifier = Modifier.size(28.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionList(
    sessions: List<Session>,
    onSessionClick: (String) -> Unit,
    onSessionDelete: (String) -> Unit,
    onSessionPin: (String, Boolean) -> Unit,
    onSessionRename: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pinned = sessions.filter { it.isPinned }
    val unpinned = sessions.filter { !it.isPinned }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (pinned.isNotEmpty()) {
            item {
                SectionLabel("Pinned")
            }
            items(pinned, key = { it.id }) { session ->
                SessionItem(
                    session = session,
                    onClick = { onSessionClick(session.id) },
                    onDelete = { onSessionDelete(session.id) },
                    onPin = { onSessionPin(session.id, !session.isPinned) },
                    onRename = { title -> onSessionRename(session.id, title) },
                    modifier = Modifier.animateItem(),
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        if (unpinned.isNotEmpty()) {
            item { SectionLabel("Recents") }
            items(unpinned, key = { it.id }) { session ->
                SessionItem(
                    session = session,
                    onClick = { onSessionClick(session.id) },
                    onDelete = { onSessionDelete(session.id) },
                    onPin = { onSessionPin(session.id, !session.isPinned) },
                    onRename = { title -> onSessionRename(session.id, title) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = OnBackgroundSecondary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionItem(
    session: Session,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true },
                )
                .background(Surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (session.isPinned) Icons.Default.PushPin else Icons.Outlined.Chat,
                contentDescription = null,
                tint = if (session.isPinned) AccentBlue else OnBackgroundSecondary,
                modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (session.lastMessage != null) {
                    Text(
                        text = session.lastMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnBackgroundSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = formatTimestamp(session.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = OnBackgroundTertiary,
            )
        }

        // Context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = SurfaceElevated,
        ) {
            DropdownMenuItem(
                text = { Text(if (session.isPinned) "Unpin" else "Pin", color = OnBackground) },
                onClick = { onPin(); showMenu = false },
                leadingIcon = { Icon(Icons.Default.PushPin, null, tint = AccentBlue) },
            )
            DropdownMenuItem(
                text = { Text("Rename", color = OnBackground) },
                onClick = { showRenameDialog = true; showMenu = false },
                leadingIcon = { Icon(Icons.Outlined.Edit, null, tint = OnBackgroundSecondary) },
            )
            HorizontalDivider(color = DividerColor)
            DropdownMenuItem(
                text = { Text("Delete", color = AccentRed) },
                onClick = { onDelete(); showMenu = false },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = AccentRed) },
            )
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            currentName = session.title,
            onRename = { onRename(it); showRenameDialog = false },
            onDismiss = { showRenameDialog = false },
        )
    }
}

@Composable
private fun NotConnectedPlaceholder(onPairClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = OnBackgroundSecondary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Not Connected", style = MaterialTheme.typography.headlineSmall, color = OnBackground)
        Spacer(Modifier.height(8.dp))
        Text("Connect to your OpenCode server to start chatting",
            style = MaterialTheme.typography.bodyMedium, color = OnBackgroundSecondary)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onPairClick,
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Connect to Server")
        }
    }
}

@Composable
private fun NewSessionDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = { Text("New Chat", color = OnBackground) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Session title (optional)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(title.ifBlank { "New Chat" }) }) {
                Text("Create", color = AccentBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnBackgroundSecondary)
            }
        },
    )
}

@Composable
private fun RenameDialog(currentName: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = { Text("Rename", color = OnBackground) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onRename(name) }) {
                Text("Rename", color = AccentBlue)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OnBackgroundSecondary) } },
    )
}

private fun formatTimestamp(epochMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMs))
}
