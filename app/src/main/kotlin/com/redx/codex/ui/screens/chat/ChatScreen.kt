package com.redx.codex.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redx.codex.domain.model.Message
import com.redx.codex.domain.model.MessageRole
import com.redx.codex.domain.model.MessageStatus
import com.redx.codex.domain.model.Model
import com.redx.codex.ui.components.MessageBubble
import com.redx.codex.ui.theme.*
import kotlinx.coroutines.launch

// ── Chat Screen ───────────────────────────────────────────────────────────────
// Evidence: Phase 2 — LazyColumn messages, bottom composer bar, model button top-right
//           Phase 3 — scroll-to-bottom on new message, spring enter animation per bubble

@Composable
fun ChatScreen(
    sessionId: String,
    model: Model?,
    onBack: () -> Unit,
    onVoiceCall: () -> Unit,
    onModelPicker: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    LaunchedEffect(sessionId) {
        viewModel.init(sessionId, model)
    }

    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size, state.isStreaming) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Top bar
        ChatTopBar(
            title = "Chat",
            modelName = state.selectedModel?.name ?: "Select Model",
            isStreaming = state.isStreaming,
            onBack = onBack,
            onModelPicker = onModelPicker,
            onVoiceCall = onVoiceCall,
        )

        // Messages list
        Box(modifier = Modifier.weight(1f)) {
            if (state.messages.isEmpty()) {
                ChatEmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        AnimatedMessageBubble(message = message)
                    }
                    // Streaming spacer
                    if (state.isStreaming) {
                        item { Spacer(Modifier.height(4.dp)) }
                    }
                }
            }

            // Scroll to bottom FAB
            androidx.compose.animation.AnimatedVisibility(
                visible = !listState.isScrolledToEnd(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (state.messages.isNotEmpty()) {
                                listState.animateScrollToItem(state.messages.lastIndex)
                            }
                        }
                    },
                    containerColor = SurfaceElevated,
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = OnBackground)
                }
            }
        }

        // Error banner
        AnimatedVisibility(visible = state.error != null) {
            Surface(color = AccentRed.copy(alpha = 0.15f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(state.error ?: "", style = MaterialTheme.typography.bodySmall, color = AccentRed)
                    TextButton(onClick = viewModel::dismissError) {
                        Text("Dismiss", color = AccentRed)
                    }
                }
            }
        }

        // Composer
        ChatComposer(
            text = state.draftText,
            isStreaming = state.isStreaming,
            onTextChange = viewModel::updateDraft,
            onSend = { viewModel.sendMessage(state.draftText) },
            onInterrupt = viewModel::interruptStreaming,
        )
    }

    // Model picker bottom sheet
    if (state.showModelPicker) {
        ModelPickerSheet(
            onSelect = viewModel::selectModel,
            onDismiss = viewModel::toggleModelPicker,
        )
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    modelName: String,
    isStreaming: Boolean,
    onBack: () -> Unit,
    onModelPicker: () -> Unit,
    onVoiceCall: () -> Unit,
) {
    Surface(color = Background, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = OnBackground)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = OnBackground)
                // Model chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SurfaceElevated)
                        .clickable { onModelPicker() }
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                ) {
                    Text(modelName, style = MaterialTheme.typography.labelMedium, color = AccentBlue)
                }
            }
            // Streaming indicator
            if (isStreaming) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(AccentGreen)
                )
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onVoiceCall) {
                Icon(Icons.Outlined.Phone, "Voice Call", tint = OnBackground)
            }
        }
    }
}

@Composable
private fun AnimatedMessageBubble(message: Message) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "bubble_scale",
    )
    Box(Modifier.fillMaxWidth()) {
        MessageBubble(
            message = message,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChatEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Chat, null, tint = OnBackgroundTertiary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("Start a conversation", style = MaterialTheme.typography.titleMedium, color = OnBackgroundSecondary)
        Text("Ask anything…", style = MaterialTheme.typography.bodySmall, color = OnBackgroundTertiary)
    }
}

@Composable
private fun ChatComposer(
    text: String,
    isStreaming: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onInterrupt: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    Surface(
        color = SurfaceElevated,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Text field
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                placeholder = { Text("Message…", color = OnBackgroundTertiary) },
                maxLines = 6,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = Surface,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
            )

            // Send / Stop button
            if (isStreaming) {
                IconButton(
                    onClick = onInterrupt,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(AccentRed),
                ) {
                    Icon(Icons.Default.Stop, "Stop", tint = ColorOnPrimary)
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (text.isNotBlank()) AccentBlue else Surface),
                ) {
                    Icon(Icons.Default.Send, "Send", tint = if (text.isNotBlank()) ColorOnPrimary else OnBackgroundTertiary)
                }
            }
        }
    }
}

// Helper
@Composable
private fun LazyListState.isScrolledToEnd(): Boolean {
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return true
    return lastVisibleItem.index >= layoutInfo.totalItemsCount - 1
}
