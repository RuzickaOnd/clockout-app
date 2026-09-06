package org.clockout.ui.theme

import androidx.compose.ui.graphics.Color

// Frosted Glass design system colors
// Deep dark cosmic canvas (#0A0B14) paired with translucent glassmorphic surfaces,
// subtle luminous indigo-purple ambient glows, and clean slate typography.

val FrostedCanvas = Color(0xFF0A0B14)
val FrostedGlassBackground = Color(0x0EFFFFFF) // ~5.5% white
val FrostedGlassBackgroundActive = Color(0x3B6366F1) // ~23% indigo
val FrostedGlassBorder = Color(0x1AFFFFFF) // ~10% white
val FrostedGlassBorderActive = Color(0x806366F1) // 50% indigo
val FrostedGlassSurfaceLight = Color(0x14FFFFFF) // ~8% white

val IndigoPrimary = Color(0xFF4F46E5)
val IndigoPrimaryLight = Color(0xFF6366F1)
val IndigoAccent = Color(0xFFA5B4FC)
val IndigoContainer = Color(0x404F46E5)
val PurpleAmbient = Color(0xFF9333EA)

val TextLight = Color(0xFFF8FAFC)
val TextSlate = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val GlowIndigo = Color(0x336366F1)
val GlowPurple = Color(0x1F9333EA)

// Desk lamp colors for the visual closure step. Warm amber reads as "the lamp is
// still on" against the cold indigo canvas, so switching it off actually removes
// light from the screen instead of adding another accent.
val BulbAmber = Color(0xFFFBBF24)
val BulbAmberDeep = Color(0xFFF59E0B)
val BulbGlow = Color(0x66FBBF24)
val BulbOff = Color(0xFF475569)
