package tv.own.owntv.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Material 3 tonal palette for OwnTV. Neutral palette is charcoal slate (design spec 2026-08-12,
 * re-pinned to hue 220 on 2026-08-17): dark background #0C0C0C, neutrals identical under every
 * accent. All accent color carried by the `primary` roles, seeded per [AccentColor] (default
 * teal); secondary/tertiary are theme-only.
 */

// Brand mark color (the OwnTV play logo) — constant.
val AccentCyan = Color(0xFF52DBC8)

// ---------------- DARK (pure achromatic) ----------------
// 2026-08-17 (user-tuned to zero): the original ramp sat at hue ~200-210 and read GREEN on
// real TV panels (compounded by RoundedPanel's since-removed hardcoded green fills); every
// partial desaturation still "sensed" as color. Final: R=G=B exactly — the dark neutrals AND
// the dark secondary family (the tonal selected-pill fills) are pure grays at the same
// perceived lightness as before. Any residual tint on screen is panel processing, not pixels.
val DarkBackground = Color(0xFF0C0C0C)
val DarkSurface = Color(0xFF121212)
val DarkSurfaceContainerLowest = Color(0xFF0E0E0E)
val DarkSurfaceContainerLow = Color(0xFF161616)
val DarkSurfaceContainer = Color(0xFF1A1A1A)
val DarkSurfaceContainerHigh = Color(0xFF242424)
val DarkSurfaceContainerHighest = Color(0xFF2D2D2D)
val DarkOnSurface = Color(0xFFEAEAEA)
val DarkOnSurfaceVariant = Color(0xFFAFAFAF)
val DarkOutline = Color(0xFF858585)
val DarkOutlineVariant = Color(0xFF3A3A3A)
val DarkSecondary = Color(0xFFC0C0C0)
val DarkOnSecondary = Color(0xFF292929)
val DarkSecondaryContainer = Color(0xFF424242)
val DarkOnSecondaryContainer = Color(0xFFDEDEDE)
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
    val OnWhiteInk = Color(0xFF0C0C0C)
}
