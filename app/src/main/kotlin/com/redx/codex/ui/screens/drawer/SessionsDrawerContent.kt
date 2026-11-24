package com.redx.codex.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.redx.codex.domain.model.Folder
import com.redx.codex.domain.model.Session
import com.redx.codex.ui.theme.*

// ── Sessions Drawer (side navigation) ────────────────────────────────────────
// Evidence: Phase 2 — left-side drawer, 320dp wide, folder sections with expand/collapse

@Composable
fun SessionsDrawerContent(
    sessions: List<Session>,
    folders: List<Folder>,
    onSessionClick: (String) -> Unit,
    onClose: () -> Unit,
    onNewSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(Surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("RedX", style = MaterialTheme.typography.headlineSmall, color = OnBackground)
                Row {
                    IconButton(onClick = onNewSession) {
                        Icon(Icons.Default.Add, "New Chat", tint = AccentBlue)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close", tint = OnBackgroundSecondary)
                    }
                }
            }
            HorizontalDivider(color = DividerColor)

            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                // Uncategorized sessions
                val uncategorized = sessions.filter { it.parentFolderId == null }
                items(uncategorized, key = { it.id }) { session ->
                    DrawerSessionItem(session = session, onClick = { onSessionClick(session.id) })
                }

                // Folders
                folders.forEach { folder ->
                    item {
                        DrawerFolderHeader(folder = folder)
                    }
                    val folderSessions = sessions.filter { it.parentFolderId == folder.id }
                    items(folderSessions, key = { it.id }) { session ->
                        DrawerSessionItem(
                            session = session,
                            onClick = { onSessionClick(session.id) },
                            indented = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerSessionItem(
    session: Session,
    onClick: () -> Unit,
    indented: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = if (indented) 32.dp else 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = 10.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (session.isPinned) Icons.Default.PushPin else Icons.Outlined.Chat,
            contentDescription = null,
            tint = if (session.isPinned) AccentBlue else OnBackgroundSecondary,
            modifier = Modifier.size(16.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.bodyMedium,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DrawerFolderHeader(folder: Folder, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Folder, contentDescription = null, tint = OnBackgroundSecondary, modifier = Modifier.size(16.dp))
        Text(text = folder.name, style = MaterialTheme.typography.labelLarge, color = OnBackgroundSecondary)
    }
}
