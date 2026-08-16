package tv.own.owntv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.theme.ownTvTween

/**
 * The single 4-state visual "ladder" shared by every top-level navigation surface (the floating
 * shell rail's [tv.own.owntv.features.shell.components.FloatingRail] nav items — the fixed Sidebar
 * this originally described was replaced by FloatingRail in the floating-shell redesign — and the
 * folder [CategoryRail] pills), so both panels read identically (#47).
 *
 * Standard TV `ListItem` colours collapse "selected" and "focused" into one visual, which makes it
 * impossible to tell "the section I'm actually in" from "the item my cursor is hovering". This ladder
 * keeps them distinct, strongest → weakest:
 *
 *  1. **selected + focused** — the same [card] fill as the focused-unselected cursor (no
 *     [primaryContainer] fill peak) + the white [focusBorder] ring + accent content: both the cursor
 *     AND the active-section marker are visible at once, never merged into their own third look.
 *  2. **focused, unselected** — [card] fill + white [focusBorder] ring, bright [onSurface] content:
 *     the cursor.
 *  3. **selected, unfocused** — soft [secondaryContainer] tonal fill + accent content + a left accent
 *     bar: a persistent, colour-independent marker of the active section while focus is elsewhere.
 *  4. **idle** — transparent, muted [onSurfaceVariant] content.
 *
 * Colours animate on the shared [ownTvTween]. [showAccentBar] and [focusBorder] are booleans/nullable
 * the call site draws itself (bar via [NavAccentBar], outline via `Modifier.border`). Accent marks
 * "selected", the white ring marks "focused" — combined state shows both, never an accent-coloured ring.
 */
data class NavLadderColors(
    val container: Color,
    val content: Color,
    val icon: Color,
    val showAccentBar: Boolean,
    /** White focus-ring colour whenever the item is focused (the cursor), selected or not; null otherwise. */
    val focusBorder: Color?,
)

@Composable
fun rememberNavLadderColors(selected: Boolean, focused: Boolean): NavLadderColors {
    val colors = OwnTVTheme.colors
    val activeSelected = selected && focused

    val container by animateColorAsState(
        when {
            // Combined state keeps the SAME fill as the plain focus cursor (no primaryContainer
            // peak) — accent content + the white ring below are what mark it as also selected.
            activeSelected -> colors.card
            focused -> colors.card
            selected -> colors.secondaryContainer.copy(alpha = 0.45f)
            else -> Color.Transparent
        },
        animationSpec = ownTvTween(140),
        label = "navLadderBg",
    )
    val content by animateColorAsState(
        when {
            activeSelected -> colors.accent   // accent cyan even while focused: "this is active"
            focused -> colors.onSurface       // bright white = "where the remote is"
            selected -> colors.accent         // accent cyan = "this is active"
            else -> colors.onSurfaceVariant
        },
        animationSpec = ownTvTween(140),
        label = "navLadderFg",
    )
    val icon by animateColorAsState(
        when {
            activeSelected -> colors.accent
            focused -> colors.onSurface
            selected -> colors.accent
            else -> colors.onSurfaceVariant
        },
        animationSpec = ownTvTween(140),
        label = "navLadderIcon",
    )

    return NavLadderColors(
        container = container,
        content = content,
        icon = icon,
        showAccentBar = selected,
        // White ring whenever focused, selected or not — the ring marks "focused", accent marks
        // "selected"; combined state shows both instead of merging into an accent-coloured ring.
        focusBorder = if (focused) colors.focusBorder else null,
    )
}

/**
 * The persistent left accent bar drawn on a [selected][NavLadderColors.showAccentBar] nav surface —
 * a thin vertical accent-coloured pill on the left edge that marks the active section regardless of
 * colour contrast (a spatial/shape cue for low-contrast panels and colour-blind users). Animates its
 * width so it slides in/out instead of popping. Must be called inside a [Box].
 */
@Composable
fun BoxScope.NavAccentBar(visible: Boolean, height: Dp = 22.dp, modifier: Modifier = Modifier) {
    val colors = OwnTVTheme.colors
    val width by animateDpAsState(
        if (visible) 3.dp else 0.dp,
        animationSpec = ownTvTween(160),
        label = "navAccentBar",
    )
    if (width > 0.dp) {
        Box(
            modifier = modifier
                .align(Alignment.CenterStart)
                .padding(start = 2.dp)
                .height(height)
                .width(width)
                .clip(RoundedCornerShape(50))
                .background(colors.accent),
        )
    }
}
