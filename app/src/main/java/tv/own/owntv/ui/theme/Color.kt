package tv.own.owntv.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Material 3 tonal palette for OwnTV. Neutral palette is charcoal slate (design spec 2026-08-12,
 * re-pinned to hue 220 on 2026-08-17): dark background #090A0E, neutrals identical under every
 * accent. All accent color carried by the `primary` roles, seeded per [AccentColor] (default
 * teal); secondary/tertiary are theme-only.
 */

// Brand mark color (the OwnTV play logo) — constant.
val AccentCyan = Color(0xFF52DBC8)

// ---------------- DARK (near-black, cool hint) ----------------
// 2026-08-17 (user-tuned over three rounds): the original ramp sat at hue ~200-210 and read
// GREEN on real TV panels; a plain hue rotation was imperceptible, and a full navy lean
// (s≈0.30) overshot. Final: hue 222, saturation 0.24 on the dark surfaces (≤0.12 on
// text/outlines), surface lightness pulled ×0.90 toward black — black-first with a cool hint,
// same elevation-ramp shape.
val DarkBackground = Color(0xFF090A0E)
val DarkSurface = Color(0xFF0E1117)
val DarkSurfaceContainerLowest = Color(0xFF0B0D12)
val DarkSurfaceContainerLow = Color(0xFF11141C)
val DarkSurfaceContainer = Color(0xFF141821)
val DarkSurfaceContainerHigh = Color(0xFF1B202C)
val DarkSurfaceContainerHighest = Color(0xFF222938)
val DarkOnSurface = Color(0xFFE7E8EC)
val DarkOnSurfaceVariant = Color(0xFFA6ABB8)
val DarkOutline = Color(0xFF7B8190)
val DarkOutlineVariant = Color(0xFF2C3447)
val DarkSecondary = Color(0xFFB6C1C9)
val DarkOnSecondary = Color(0xFF212A31)
val DarkSecondaryContainer = Color(0xFF39434B)
val DarkOnSecondaryContainer = Color(0xFFD5DFE7)
val DarkTertiary = Color(0xFFA9CBE4)
val DarkOnTertiary = Color(0xFF0B3445)
val DarkTertiaryContainer = Color(0xFF294B5D)
val DarkOnTertiaryContainer = Color(0xFFC5E7FF)
val DarkError = Color(0xFFFFB4AB)

// ---------------- LIGHT (neutral, de-greened) ----------------
val LightBackground = Color(0xFFFAFBFC)
val LightSurface = Color(0xFFFAFBFC)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF1F3F5)
val LightSurfaceContainer = Color(0xFFEBEEF0)
val LightSurfaceContainerHigh = Color(0xFFE4E8EB)
val LightSurfaceContainerHighest = Color(0xFFDFE3E6)
val LightOnSurface = Color(0xFF191C1E)
val LightOnSurfaceVariant = Color(0xFF42474B)
val LightOutline = Color(0xFF72787D)
val LightOutlineVariant = Color(0xFFC1C7CC)
val LightSecondary = Color(0xFF4E5B66)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD5DFE7)
val LightOnSecondaryContainer = Color(0xFF0A1922)
val LightTertiary = Color(0xFF416278)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFC5E7FF)
val LightOnTertiaryContainer = Color(0xFF001E2F)
val LightError = Color(0xFFBA1A1A)

/** Pictorial palette for the top-bar weather glyph (canvas art, not chrome). */
object WeatherGlyph {
    val Sun = Color(0xFFFFD166)
    val Moon = Color(0xFFDDF8FF)
    val Cloud = Color(0xFFDDEFE9)
    val Rain = Color(0xFF76A7FF)
    val Snow = Color(0xFFF0FCFF)
    val Fog = Color(0xFFDDF8FF)
    val Thunder = Color(0xFFFFD166)
}

/** Pictorial HUD constants — drawn over video, deliberately theme-independent (like [WeatherGlyph]). */
object HudPictorial {
    /** The LIVE badge fill — saturated broadcast red; white text/dot on it needs the depth. */
    val LiveBadge = Color(0xCCDC3232)
    /** Ink on the white circular transport button. Charcoal-neutral (was the old teal-tinted surface). */
    val OnWhiteInk = Color(0xFF090A0E)
}
