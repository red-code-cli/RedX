package com.redx.codex

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.redx.codex.domain.model.Model
import com.redx.codex.ui.screens.chat.ChatScreen
import com.redx.codex.ui.screens.home.HomeScreen
import com.redx.codex.ui.screens.pairing.PairingScreen
import com.redx.codex.ui.screens.settings.SettingsScreen
import com.redx.codex.ui.screens.voice.VoiceScreen

// ── App Navigation ────────────────────────────────────────────────────────────
// Using simple sealed-class back-stack since Navigation 3 alpha may not be stable
// Evidence: Phase 4 — type-safe navigation with custom back-stack handler

sealed class Screen {
    object Home : Screen()
    object Pairing : Screen()
    object Settings : Screen()
    data class Chat(val sessionId: String, val model: Model? = null) : Screen()
    data class Voice(val sessionId: String, val sessionTitle: String) : Screen()
}

@Composable
fun AppNavHost() {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val current = backStack.last()

    fun push(screen: Screen) { backStack.add(screen) }
    fun pop() { if (backStack.size > 1) backStack.removeLast() }

    // Screen transitions — slide + fade (270ms, FastOutSlowIn)
    AnimatedContent(
        targetState = current,
        transitionSpec = {
            val enter = slideInHorizontally(
                animationSpec = tween(270, easing = FastOutSlowInEasing),
                initialOffsetX = { it / 4 },
            ) + fadeIn(tween(270))
            val exit = slideOutHorizontally(
                animationSpec = tween(270, easing = FastOutSlowInEasing),
                targetOffsetX = { -it / 4 },
            ) + fadeOut(tween(180))
            enter togetherWith exit
        },
        label = "nav_transition",
    ) { screen ->
        when (screen) {
            is Screen.Home -> HomeScreen(
                onSessionClick = { sessionId -> push(Screen.Chat(sessionId)) },
                onSettingsClick = { push(Screen.Settings) },
                onPairingClick = { push(Screen.Pairing) },
            )
            is Screen.Chat -> ChatScreen(
                sessionId = screen.sessionId,
                model = screen.model,
                onBack = ::pop,
                onVoiceCall = { push(Screen.Voice(screen.sessionId, "Voice Call")) },
                onModelPicker = { },
            )
            is Screen.Voice -> VoiceScreen(
                sessionTitle = screen.sessionTitle,
                onBack = ::pop,
            )
            is Screen.Settings -> SettingsScreen(
                onBack = ::pop,
                onPairingClick = { push(Screen.Pairing) },
            )
            is Screen.Pairing -> PairingScreen(
                onBack = ::pop,
                onConnected = { pop() },
            )
        }
    }
}
