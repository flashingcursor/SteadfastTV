package tv.own.owntv.ui.theme

/**
 * Shared alpha / translucency tokens (2026-08-17 token audit, Family 3 — Alphas & Translucency).
 *
 * Each constant names a recurring translucency *role* found by the audit — not just a value —
 * so a call site should adopt one only when its own role matches the KDoc, not merely because
 * the numbers happen to coincide (see `docs/superpowers/reports/2026-08-17-token-audit.md`,
 * §4, for the full site inventory and the per-population reasoning). `GlassConfig.DEFAULT_GLASS_ALPHA`
 * (`Glass.kt`) and `OwnTVColors.focusGlow` predate this file and stay where they are — they're
 * structurally different roles (an opt-in "Liquid Glass" tint and a resolved glow `Color`, not a
 * reusable scalar), not additions to this scale.
 */
object AlphaTokens {
    /**
     * Full-screen modal-backdrop scrim behind centered dialogs/popups — the dominant
     * translucency value app-wide (45+ dialogs). Snap target for the near-identical 0.7/0.78/0.8
     * variants found scattered across the same role.
     */
    const val AlphaScrim: Float = 0.75f

    /** Lighter scrim for a handful of "quick confirm" dialogs that converged on a softer backdrop. */
    const val AlphaScrimLight: Float = 0.65f

    /** Fixed-width slide-in side-panel fill (category/channel browse overlays, the rail panel). */
    const val AlphaPanelFill: Float = 0.82f

    /** Blurred/dimmed backdrop image behind foreground content (e.g. the Home hero backdrop). */
    const val AlphaBlurredBackdrop: Float = 0.5f

    /** Focused-state container fill for Player HUD transport controls (speed/engine/ctrl buttons). */
    const val AlphaHudFocusFill: Float = 0.16f
}
