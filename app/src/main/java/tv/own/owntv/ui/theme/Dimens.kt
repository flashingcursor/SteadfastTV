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
    val IconTileCorner = 6.dp  // halved with the corner scale (2026-08-17)

    val GapHairline = 2.dp
    val GapTiny = 4.dp
    val GapSmall = 8.dp
    val GapCompact = 12.dp
    val GapMedium = 16.dp
    val GapWide = 20.dp
    val GapLarge = 24.dp

    // Settings-detail outer container padding (2026-08-17 token audit, Phase 5, §2): the
    // identical horizontal=40dp/vertical=28dp pattern repeated across 20+ settings/customize
    // detail screens — paired tokens rather than folded into the Gap ladder since neither value
    // is a general-purpose gap on its own.
    val DetailPanelPaddingH = 40.dp
    val DetailPanelPaddingV = 28.dp

    // Floating shell rail, TOP position only (shell-refinements Task 3): the header→rail gap, and
    // (doubled, matching the existing header/rail/content symmetric-gap arithmetic in
    // OwnTVShell.kt) the rail→content gap. Deliberately tighter than GapMedium — the TOP rail sits
    // right under the header pill and the old 16dp gap read as too loose.
    val RailTopGap = 8.dp

    // Poster tiles (PosterCard) — values match the shipped look exactly; centralized for tuning.
    val PosterProgressHeight = 4.dp

    // App-wide corner scale (2026-08-17 consolidation): components must round their corners
    // with these tokens, never dp literals, so the app's border radius stays centrally tunable.
    // Halved from the original M3-expressive values (8/12/16/24/20/22) on user direction the
    // same day — the app now reads noticeably squarer.
    val CornerXSmall = 4.dp    // small chips/badges/inputs
    val CornerSmall = 6.dp
    val CornerMedium = 8.dp
    val CornerLarge = 12.dp
    val CardCorner = 10.dp
    val PanelCorner = 11.dp    // the large RoundedPanel browse containers

    // Size tokens from the 2026-08-17 token audit (docs/superpowers/reports/2026-08-17-token-audit.md,
    // §5 "Sizes, Elevation, Text"). DialogPanelCorner (16dp) deliberately sits outside the halved
    // corner scale above — every dialogPanel(corner=...) override site converges on 16/18/20dp, all
    // far from the coded default (CardCorner, 10dp), so the override population gets its own token
    // rather than being forced onto the general corner ladder.
    val DialogPanelPadding = 28.dp
    val DialogPanelPaddingCompact = 18.dp
    val DialogPanelCorner = 16.dp
    val DialogPanelWidth = 480.dp
    val DialogPanelWidthWide = 560.dp
    val HairlineWidth = 1.dp
    val ThinProgressHeight = 2.dp
    val SelectionBorderWidth = 2.dp
    val IconSizeMedium = 18.dp
    val IconSizeLarge = 20.dp
    val TouchTargetSize = 48.dp
    val TouchTargetSizeCompact = 44.dp
    val ContentColumnMaxWidth = 640.dp
    val StatusDotSize = 8.dp

    val FocusBorderWidth = 2.5.dp

    val HomeRowPaddingH = 20.dp

    // Hero carousel
    val HeroBaseWidth = 180.dp
    val HeroMetaHeight = 84.dp
    val HeroGap = 14.dp
    val HeroCardCorner = 12.dp // halved with the corner scale (2026-08-17)
    val HeroPosterCorner = 7.dp
    val HeroMaxCardHeight = 354.dp
    val HeroMinCardHeight = 200.dp
    val HeroOverlayMaxWidth = 400.dp
    val HeroProgressHeight = 3.dp
}
