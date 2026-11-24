package com.redx.codex.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.redx.codex.domain.model.Model
import com.redx.codex.ui.theme.*

// ── Model Picker Bottom Sheet ─────────────────────────────────────────────────
// Evidence: Phase 2 — Modal bottom sheet, searchable list, provider groups,
//           selected model has blue checkmark, 500ms spring enter

@Composable
fun ModelPickerSheet(
    models: List<Model> = emptyList(),
    selectedModelId: String? = null,
    onSelect: (Model) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, models) {
        if (query.isBlank()) models
        else models.filter { it.name.contains(query, ignoreCase = true) || it.provider.contains(query, ignoreCase = true) }
    }

    // Group by provider
    val grouped = remember(filtered) { filtered.groupBy { it.provider } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(OnBackgroundTertiary)
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Select Model",
                style = MaterialTheme.typography.titleLarge,
                color = OnBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )

            // Search field
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Search models…", color = OnBackgroundTertiary) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = OnBackgroundSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DividerColor,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                ),
            )

            Spacer(Modifier.height(8.dp))

            if (models.isEmpty()) {
                // Placeholder models for demo
                val placeholders = listOf(
                    Model("gpt-4o", "GPT-4o", "OpenAI", 128000, true, true),
                    Model("gpt-4o-mini", "GPT-4o Mini", "OpenAI", 128000, true, true),
                    Model("claude-3-5-sonnet", "Claude 3.5 Sonnet", "Anthropic", 200000, true, true),
                    Model("claude-3-5-haiku", "Claude 3.5 Haiku", "Anthropic", 200000, true, true),
                    Model("gemini-2.0-flash", "Gemini 2.0 Flash", "Google", 1000000, true, true),
                    Model("deepseek-chat", "DeepSeek Chat", "DeepSeek", 128000, false, true),
                )
                val filteredPlaceholders = if (query.isBlank()) placeholders
                    else placeholders.filter { it.name.contains(query, ignoreCase = true) }
                val groupedPlaceholders = filteredPlaceholders.groupBy { it.provider }

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    groupedPlaceholders.forEach { (provider, providerModels) ->
                        item {
                            ProviderHeader(provider)
                        }
                        items(providerModels) { model ->
                            ModelItem(
                                model = model,
                                isSelected = model.id == selectedModelId,
                                onClick = { onSelect(model) },
                            )
                        }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    grouped.forEach { (provider, providerModels) ->
                        item { ProviderHeader(provider) }
                        items(providerModels) { model ->
                            ModelItem(
                                model = model,
                                isSelected = model.id == selectedModelId,
                                onClick = { onSelect(model) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderHeader(provider: String) {
    Text(
        text = provider.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = OnBackgroundSecondary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun ModelItem(model: Model, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) AccentBlueDim else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(model.name, style = MaterialTheme.typography.bodyMedium, color = OnBackground)
            if (model.contextLength > 0) {
                Text(
                    "${model.contextLength / 1000}K context",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnBackgroundSecondary,
                )
            }
        }
        if (isSelected) {
            Icon(Icons.Default.Check, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
        }
    }
}
