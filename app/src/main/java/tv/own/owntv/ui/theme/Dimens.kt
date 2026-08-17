package tv.own.owntv.ui.theme

import androidx.compose.ui.unit.dp

/** Shared spacing / sizing tokens for the 4-layer TV shell. */
object Dimens {
    val ScreenPaddingH = 32.dp
    val ScreenPaddingV = 24.dp

    // Layer 1 — MD3 navigation panel. Expands to a drawer (labels) when focused,
    // collapses to an icon rail when focus moves into a submenu.
    val SidebarWidthCollapsed = 72.dp

    // Floating shell rail (features/shell/components/FloatingRail.kt): the idle icon-rail's nominal
    // cross-axis footprint (avatar 48dp + the rail panel's own padding), used ONLY as a same-frame
    // fallback for the shell's content-area reservation before the rail has reported its first real
    // idle measurement (cold start, or right after a runtime LEFT<->TOP switch resets the capture).
    val RailIdleNominal = 66.dp

    // Layer 2 — category rail (expands to show full names when it holds focus)
    val RailWidth = 92.dp
    val RailWidthExpanded = 325.dp // expanded (focused overlay) width — fits long category names
    val RailWidthFixed = 272.dp    // Phase 2: fixed full-label category column (names wrap to 2 lines)
    val RailPillSize = 56.dp
    val ChannelListWidth = 460.dp  // Phase 2: fixed channel-list column (Live); preview/detail fills the rest.
    // Widened from 400dp so each row can also show the current programme (EPG) on a second line — the
    // preview pane (Modifier.weight(1f)) narrows in step, so no other layout value needs to change.

    // MD3 settings tonal icon tile
    val IconTileSize = 42.dp
    val IconTileCorner = 12.dp

    val GapTiny = 4.dp
    val GapSmall = 8.dp
    val GapMedium = 16.dp
    val GapLarge = 24.dp

    // Floating shell rail, TOP position only (shell-refinements Task 3): the header→rail gap, and
    // (doubled, matching the existing header/rail/content symmetric-gap arithmetic in
    // OwnTVShell.kt) the rail→content gap. Deliberately tighter than GapMedium — the TOP rail sits
    // right under the header pill and the old 16dp gap read as too loose.
    val RailTopGap = 8.dp

    // Poster tiles (PosterCard) — values match the shipped look exactly; centralized for tuning.
    val PosterProgressHeight = 4.dp

    // M3 expressive shape scale (larger, rounder than the defaults).
    // App-wide corner scale (2026-08-17 consolidation): components must round their corners
    // with these tokens, never dp literals, so the app's border radius stays centrally tunable.
    val CornerXSmall = 8.dp    // small chips/badges/inputs
    val CornerSmall = 12.dp
    val CornerMedium = 16.dp
    val CornerLarge = 24.dp
    val CardCorner = 20.dp
    val PanelCorner = 22.dp    // the large RoundedPanel browse containers

    val FocusBorderWidth = 2.5.dp

    val HomeRowPaddingH = 20.dp

    // Hero carousel
    val HeroBaseWidth = 180.dp
    val HeroMetaHeight = 84.dp
    val HeroGap = 14.dp
    val HeroCardCorner = 24.dp
    val HeroPosterCorner = 14.dp
    val HeroMaxCardHeight = 354.dp
    val HeroMinCardHeight = 200.dp
    val HeroOverlayMaxWidth = 400.dp
    val HeroProgressHeight = 3.dp
}
