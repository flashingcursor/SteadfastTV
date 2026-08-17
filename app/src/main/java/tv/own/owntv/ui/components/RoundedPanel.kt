package tv.own.owntv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tv.own.owntv.ui.theme.Dimens
import tv.own.owntv.ui.theme.GlassSurface
import tv.own.owntv.ui.theme.LocalGlass
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.theme.glass

// Phase 6 — per-region panel fills. Originally hardcoded dark-green hexes (owner-specified,
// 2026-06-27, "Option A — Clean + Premium"); re-based onto the theme's surface-container ramp
// on 2026-08-17 when the palette moved to the near-black cool-slate neutrals — the hardcoded
// greens were silently overriding every palette change on the browse screens. The relative
// ordering is preserved in both themes (content darkest region in dark / brightest in light,
// rail mid, preview most raised), and the fills now track any future palette change for free.
val RailPanelFill: Color
    @Composable @ReadOnlyComposable get() =
        if (OwnTVTheme.colors.isDark) OwnTVTheme.colors.surfaceContainerLow else OwnTVTheme.colors.surfaceContainerHigh

val ContentPanelFill: Color
    @Composable @ReadOnlyComposable get() =
        if (OwnTVTheme.colors.isDark) OwnTVTheme.colors.surfaceContainerLowest else OwnTVTheme.colors.surfaceContainerLow

val PreviewPanelFill: Color
    @Composable @ReadOnlyComposable get() =
        if (OwnTVTheme.colors.isDark) OwnTVTheme.colors.surfaceContainerHigh else OwnTVTheme.colors.surfaceContainerHighest

/**
 * Phase 6 — a rounded visual container matching the new-shell mockup's "panel 2/3/4" look: large rounded
 * corners, a subtle surface fill, and a hairline [outlineVariant] border. Content is clipped to the
 * rounded shape.
 *
 * This is a VISUAL wrapper only — a plain [Box], no `clickable`/`selectable`/focus of its own.
 *
 * Liquid Glass: when a background image is active and [GlassSurface.PANELS] is in scope, the fill
 * becomes translucent (alpha from the user's transparency setting) and gains a soft specular
 * top-edge highlight. Callers that pass an explicit [fillColor] still go glassy — the explicit
 * colour is simply used as the translucent base, so per-region tints (ContentPanelFill etc.)
 * keep their identity. Pass [surface] to tag this panel as something else (e.g. SIDEBAR/PREVIEW).
 *
 * @param fillColor the panel surface colour, or null for the theme default.
 * @param radius corner radius (≈24px on the mockup; Dimens.PanelCorner reads well at TV distance).
 * @param innerPadding inset between the rounded edge and the content.
 * @param surface which glass surface this panel represents (default PANELS).
 */
@Composable
fun RoundedPanel(
    modifier: Modifier = Modifier,
    radius: Dp = Dimens.PanelCorner,
    fillColor: Color? = null,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    surface: GlassSurface = GlassSurface.PANELS,
    content: @Composable () -> Unit,
) {
    val colors = OwnTVTheme.colors
    val bg = fillColor ?: colors.surfaceContainerLowest
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .glass(surface = surface, baseFill = bg, shape = shape, cornerRadius = radius)
            .border(width = Dimens.HairlineWidth, color = colors.outlineVariant, shape = shape)
            .padding(innerPadding),
    ) {
        content()
    }
}

/**
 * Phase 6 — the rounded-panel look as a [Modifier], for applying to an EXISTING container.
 * Same spec as [RoundedPanel]. See [RoundedPanel] for the glass behaviour; pass [surface] to tag
 * this container as SIDEBAR/PREVIEW/etc. when it is not a generic content panel.
 */
@Composable
fun Modifier.roundedPanel(
    radius: Dp = Dimens.PanelCorner,
    fillColor: Color? = null,
    surface: GlassSurface = GlassSurface.PANELS,
): Modifier {
    val colors = OwnTVTheme.colors
    val bg = fillColor ?: colors.surfaceContainerLowest
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .glass(surface = surface, baseFill = bg, shape = shape, cornerRadius = radius)
        .border(width = Dimens.HairlineWidth, color = colors.outlineVariant, shape = shape)
}
