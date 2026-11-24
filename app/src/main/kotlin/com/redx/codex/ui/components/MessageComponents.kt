package com.redx.codex.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.redx.codex.domain.model.Message
import com.redx.codex.domain.model.MessagePart
import com.redx.codex.domain.model.MessageRole
import com.redx.codex.domain.model.MessageStatus
import com.redx.codex.ui.theme.*

// ── Message Bubble ────────────────────────────────────────────────────────────
// Evidence: Phase 2 — user bubble right-aligned Blue, assistant left-aligned #2C2C2E
//           corner radius 18dp user / 4dp bottom-leading + 18dp rest for assistant

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp,
                    )
                )
                .background(if (isUser) BubbleUser else BubbleAssistant)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                message.parts.forEach { part ->
                    when (part) {
                        is MessagePart.Text -> {
                            if (message.status == MessageStatus.STREAMING && part.text.isEmpty()) {
                                TypingIndicator()
                            } else {
                                SelectionContainer {
                                    Text(
                                        text = part.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = ColorOnPrimary,
                                    )
                                }
                            }
                        }
                        is MessagePart.Code -> CodeBlock(language = part.language, code = part.content)
                        is MessagePart.ToolCall -> ToolCallCard(part = part)
                        is MessagePart.Image -> {} // Image display (Coil)
                    }
                }

                // Error state
                if (message.status == MessageStatus.ERROR && message.error != null) {
                    Text(
                        text = "⚠ ${message.error}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentRed,
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    val dot1 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse, initialStartOffset = StartOffset(0)), label = "d1")
    val dot2 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse, initialStartOffset = StartOffset(150)), label = "d2")
    val dot3 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse, initialStartOffset = StartOffset(300)), label = "d3")

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(dot1, dot2, dot3).forEach { alpha ->
            Box(
                Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(OnBackgroundSecondary.copy(alpha = 0.3f + 0.7f * alpha))
            )
        }
    }
}

@Composable
fun CodeBlock(language: String, code: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ToolCardBg)
    ) {
        // Language label
        if (language.isNotBlank()) {
            Text(
                text = language,
                style = MaterialTheme.typography.labelSmall,
                color = OnBackgroundSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        }
        // Code content
        Text(
            text = code,
            style = CodeTextStyle,
            modifier = Modifier.padding(10.dp),
        )
    }
}

@Composable
fun ToolCallCard(part: MessagePart.ToolCall, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ToolCardBg)
            .clickable { expanded = !expanded }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "⚙ ${part.toolName}",
                style = MaterialTheme.typography.labelLarge,
                color = AccentAmber,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (part.result != null) "✓" else "…",
                color = if (part.isError) AccentRed else AccentGreen,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Args:", style = MaterialTheme.typography.labelSmall, color = OnBackgroundSecondary)
                Text(part.args, style = CodeTextStyle, color = OnBackground)
                if (part.result != null) {
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    Text("Result:", style = MaterialTheme.typography.labelSmall, color = OnBackgroundSecondary)
                    Text(part.result, style = CodeTextStyle, color = if (part.isError) AccentRed else OnBackground)
                }
            }
        }
    }
}
