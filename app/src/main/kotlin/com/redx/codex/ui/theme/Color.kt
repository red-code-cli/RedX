package com.redx.codex.ui.theme

import androidx.compose.ui.graphics.Color

// ── Obsidian Dark Palette (sampled from OpenCode APK screenshots) ──────────────
// Evidence: Phase 2 hex analysis of 20 screenshots @ 1080×2400px

/** True-black canvas — primary background */
val Background = Color(0xFF161616)

/** Slightly lighter surface for cards / sheets */
val Surface = Color(0xFF232323)

/** Elevated surface — dialogs, bottom sheets, composer container */
val SurfaceElevated = Color(0xFF2E2E2E)

/** Even higher elevation — tooltips, context menus */
val SurfaceHigh = Color(0xFF3A3A3A)

/** Primary interactive accent — Apple-style blue */
val AccentBlue = Color(0xFF0A84FF)
val AccentBlueDim = Color(0xFF0A84FF).copy(alpha = 0.18f)

/** Success / AI-response accent — muted sage green */
val AccentGreen = Color(0xFF82CB93)
val AccentGreenDim = Color(0xFF82CB93).copy(alpha = 0.15f)

/** Warning / needs-input accent */
val AccentAmber = Color(0xFFFFB340)

/** Error / destructive accent */
val AccentRed = Color(0xFFFF453A)

/** Primary text — near-white */
val OnBackground = Color(0xFFF2F2F7)

/** Secondary text — reduced emphasis */
val OnBackgroundSecondary = Color(0xFF8E8E93)

/** Tertiary / placeholder text */
val OnBackgroundTertiary = Color(0xFF48484A)

/** Thin dividers / strokes */
val DividerColor = Color(0xFF2C2C2E)

/** Message bubble — user */
val BubbleUser = Color(0xFF0A84FF)

/** Message bubble — assistant */
val BubbleAssistant = Color(0xFF2C2C2E)

/** Tool-call card background */
val ToolCardBg = Color(0xFF1C1C1E)

/** Ripple color for interactive elements */
val RippleColor = Color(0x1AFFFFFF)

// ── Semantic aliases ───────────────────────────────────────────────────────────
val ColorPrimary = AccentBlue
val ColorOnPrimary = Color(0xFFFFFFFF)
val ColorSecondary = AccentGreen
val ColorError = AccentRed
val ColorWarning = AccentAmber

// ── Voice / Waveform colors ────────────────────────────────────────────────────
val WaveformActive = AccentBlue
val WaveformIdle = OnBackgroundTertiary
val WaveformEcg = AccentGreen

// ── Status colors ──────────────────────────────────────────────────────────────
val StatusConnected = AccentGreen
val StatusDisconnected = AccentRed
val StatusConnecting = AccentAmber
