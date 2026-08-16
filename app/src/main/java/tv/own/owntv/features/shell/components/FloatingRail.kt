package tv.own.owntv.features.shell.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import tv.own.owntv.R
import tv.own.owntv.features.settings.data.RailPosition
import tv.own.owntv.features.shell.MainSection
import tv.own.owntv.ui.components.FocusableSurface
import tv.own.owntv.ui.components.NavAccentBar
import tv.own.owntv.ui.components.OwnTVAvatar
import tv.own.owntv.ui.components.navIcon
import tv.own.owntv.ui.components.rememberNavLadderColors
import tv.own.owntv.ui.format.localizedInteger
import tv.own.owntv.ui.preview.OwnTVPreview
import tv.own.owntv.ui.preview.TvPreview
import tv.own.owntv.ui.theme.Dimens
import tv.own.owntv.ui.theme.GlassSurface
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.theme.glass
import tv.own.owntv.ui.theme.ownTvTween

// ---- Shared geometry / colour constants -----------------------------------------------------
// Kept local (not Dimens.kt — this file is additive-only for Task 2) since the floating rail's
// "pill" geometry doesn't map onto the fixed-column Sidebar metrics.
private val RailIconSize = 24.dp
private val RailAvatarSize = 48.dp
private val RailLabelMaxWidth = 160.dp
private val RailSeparatorLength = 28.dp
private val RailSeparatorThickness = 1.dp

// Mockup-literal values (visual contract, not user-facing strings — not subject to i18n review):
// a thin white-14%-alpha divider, and the active panel's white-12%-alpha rim.
private val RailSeparatorColor = Color.White.copy(alpha = 0.14f)
private val RailPanelBorderColor = Color.White.copy(alpha = 0.12f)
private const val RailPanelFillAlpha = 0.82f

private val RailItemShape = RoundedCornerShape(50)
private val RailPanelShapeLeft = RoundedCornerShape(28.dp)
private val RailPanelShapeTop = RoundedCornerShape(50)

/**
 * Floating shell nav rail (Task 2 of the floating-shell plan) — a self-positioning "pill" that
 * replaces the old fixed Sidebar (deleted; LEFT is this component's default orientation) and
 * introduces a horizontal TOP mode, per [RailPosition]. Unlike Sidebar, this composable does NOT
 * fill the screen: it wraps its own content, floats over the page, and the shell (Task 4) is
 * responsible for placement/inset (`Alignment.CenterStart` for LEFT, centered below the header for
 * TOP) via [modifier].
 *
 * Three areas, start→end (top→bottom for LEFT, start→end for TOP): avatar / destinations /
 * Settings, separated by thin translucent dividers.
 *
 * Idle: icons only, no panel background — the selected section still keeps its accent tint and
 * [NavAccentBar] (or the horizontal variant in TOP mode). Active — `forceActive` OR the D-pad
 * focus is anywhere inside the rail — expands item labels and fades in a translucent/glass panel
 * behind the whole rail with a thin white 12% rim. Accent is reserved for focus/active/selected;
 * the white focus ring (via the shared nav ladder) is the sole cursor indicator.
 */
@Composable
fun FloatingRail(
    position: RailPosition,
    selected: MainSection,
    visibleSections: Set<MainSection>,
    onSelect: (MainSection) -> Unit,
    avatarId: Int,
    onPickAvatar: () -> Unit,
    onSwitchProfile: () -> Unit,
    profileName: String,
    selectedItemFocusRequester: FocusRequester,
    onActiveChange: (Boolean) -> Unit,
    // ModifierParameter (final-review cleanup, M8): modifier must be the first OPTIONAL parameter,
    // so it sits right after the last required one — ahead of counts/forceActive below, not after.
    modifier: Modifier = Modifier,
    counts: (MainSection) -> Int = { 0 },
    forceActive: Boolean = false,
) {
    val colors = OwnTVTheme.colors
    var focusWithin by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val active = forceActive || focusWithin

    // Carried over verbatim from Sidebar.kt: Search has no rail item (it lives in the top bar) so
    // BACK/left out of Search redirects to Home; a hidden selected section (Nav menu customization)
    // falls back to the first visible browse item, or Settings if every browse item is hidden.
    val focusSection = when {
        selected == MainSection.SEARCH -> MainSection.HOME
        selected == MainSection.SETTINGS -> MainSection.SETTINGS
        selected in visibleSections -> selected
        else -> MainSection.browseOrder.firstOrNull { it in visibleSections } ?: MainSection.SETTINGS
    }

    val shape = if (position == RailPosition.LEFT) RailPanelShapeLeft else RailPanelShapeTop

    val panel = modifier
        .onFocusChanged { state ->
            // Same entry-redirect as Sidebar: D-pad focus search is spatial, so landing anywhere in
            // the rail jumps straight to the SELECTED item instead of whatever is geometrically
            // aligned. Deferred a frame — requesting focus inside onFocusChanged is rejected
            // mid-transaction. onActiveChange fires only on an actual focus-within transition.
            val within = state.hasFocus
            val entered = within && !focusWithin
            if (within != focusWithin) {
                focusWithin = within
                onActiveChange(within)
            }
            if (entered) scope.launch { runCatching { selectedItemFocusRequester.requestFocus() } }
        }
        .focusGroup()
        .then(
            if (active) {
                Modifier
                    .shadow(
                        elevation = 14.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = colors.focusGlow,
                        spotColor = colors.focusGlow,
                    )
                    .clip(shape)
                    // Liquid Glass panel when the SIDEBAR surface is glassy; otherwise glass() falls
                    // back to a plain translucent surfaceContainer fill (~82% alpha) — exactly the
                    // two states the brief calls for, with no branching needed here.
                    .glass(
                        surface = GlassSurface.SIDEBAR,
                        baseFill = colors.surfaceContainer.copy(alpha = RailPanelFillAlpha),
                        shape = shape,
                        cornerRadius = if (position == RailPosition.LEFT) 28.dp else 999.dp,
                    )
                    .border(1.dp, RailPanelBorderColor, shape)
            } else {
                Modifier
            },
        )

    if (position == RailPosition.LEFT) {
        Column(
            modifier = panel.padding(horizontal = 9.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            RailAvatar(avatarId = avatarId, profileName = profileName, onSwitchProfile = onSwitchProfile, onPickAvatar = onPickAvatar)
            RailSeparator(position = position)
            Box(modifier = Modifier.verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MainSection.browseOrder.filter { it in visibleSections }.forEach { section ->
                        RailNavItem(
                            section = section,
                            selected = section == selected,
                            expanded = active,
                            count = counts(section),
                            position = position,
                            onClick = { onSelect(section) },
                            modifier = if (section == focusSection) {
                                Modifier.focusRequester(selectedItemFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
            RailSeparator(position = position)
            RailNavItem(
                section = MainSection.SETTINGS,
                selected = selected == MainSection.SETTINGS,
                expanded = active,
                count = 0,
                position = position,
                onClick = { onSelect(MainSection.SETTINGS) },
                modifier = if (focusSection == MainSection.SETTINGS) {
                    Modifier.focusRequester(selectedItemFocusRequester)
                } else {
                    Modifier
                },
            )
        }
    } else {
        Row(
            modifier = panel.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            RailAvatar(avatarId = avatarId, profileName = profileName, onSwitchProfile = onSwitchProfile, onPickAvatar = onPickAvatar)
            RailSeparator(position = position)
            Box(modifier = Modifier.horizontalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    MainSection.browseOrder.filter { it in visibleSections }.forEach { section ->
                        RailNavItem(
                            section = section,
                            selected = section == selected,
                            expanded = active,
                            count = counts(section),
                            position = position,
                            onClick = { onSelect(section) },
                            modifier = if (section == focusSection) {
                                Modifier.focusRequester(selectedItemFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
            RailSeparator(position = position)
            RailNavItem(
                section = MainSection.SETTINGS,
                selected = selected == MainSection.SETTINGS,
                expanded = active,
                count = 0,
                position = position,
                onClick = { onSelect(MainSection.SETTINGS) },
                modifier = if (focusSection == MainSection.SETTINGS) {
                    Modifier.focusRequester(selectedItemFocusRequester)
                } else {
                    Modifier
                },
            )
        }
    }
}

/**
 * Avatar area — always just the round avatar button (there is no expanded "who's watching" card
 * here, unlike Sidebar's `ProfileCard`): click = [onSwitchProfile], long-press = [onPickAvatar],
 * carried over verbatim from Sidebar's collapsed `AvatarButton`. [profileName] isn't painted
 * on-screen (the mockup never shows a name in the avatar area) but is exposed as the a11y
 * `contentDescription` so screen readers still get the profile identity.
 */
@Composable
private fun RailAvatar(
    avatarId: Int,
    profileName: String,
    onSwitchProfile: () -> Unit,
    onPickAvatar: () -> Unit,
) {
    val colors = OwnTVTheme.colors
    val description = profileName.ifBlank { stringResource(R.string.common_own_tv_user) }
    FocusableSurface(
        onClick = onSwitchProfile,
        onLongClick = onPickAvatar,
        modifier = Modifier
            .size(RailAvatarSize)
            .semantics { contentDescription = description },
        shape = CircleShape,
        focusedScale = 1.08f,
        focusedContainerColor = colors.surfaceContainerHighest,
        unfocusedContainerColor = Color.Transparent,
        selectedContainerColor = Color.Transparent,
        contentAlignment = Alignment.Center,
        surface = GlassSurface.SIDEBAR,
    ) { _ ->
        OwnTVAvatar(avatarId = avatarId, modifier = Modifier.size(RailAvatarSize - 4.dp))
    }
}

/** Thin translucent divider between the rail's three areas — vertical bar for LEFT, horizontal for TOP. */
@Composable
private fun RailSeparator(position: RailPosition) {
    if (position == RailPosition.LEFT) {
        Box(
            modifier = Modifier
                .width(RailSeparatorLength)
                .height(RailSeparatorThickness)
                .background(RailSeparatorColor),
        )
    } else {
        Box(
            modifier = Modifier
                .width(RailSeparatorThickness)
                .height(RailSeparatorLength)
                .background(RailSeparatorColor),
        )
    }
}

/**
 * One destination pill — shared internals for both browse items and Settings, in both
 * orientations. Idle: icon only, muted tint (via the shared nav ladder), no background. Selected:
 * accent tint + [NavAccentBar] (LEFT) / [HorizontalNavAccentBar] (TOP) regardless of focus.
 * Focused: the shared ladder's white focus ring — the sole focus cursor (no container fill; the
 * rail's own panel is the only background here, matching the "accent = selected only" rule).
 * `expanded` (the rail's active state) reveals the label + count badge and animates the pill's
 * size via [Modifier.animateContentSize].
 */
@Composable
private fun RailNavItem(
    section: MainSection,
    selected: Boolean,
    expanded: Boolean,
    count: Int,
    position: RailPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    FocusableSurface(
        onClick = onClick,
        modifier = modifier,
        selected = selected,
        shape = RailItemShape,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        selectedContainerColor = Color.Transparent,
        showFocusBorder = false,
        contentAlignment = Alignment.Center,
    ) { focused ->
        val ladder = rememberNavLadderColors(selected = selected, focused = focused)
        val iconTint = if (selected) colors.accent else ladder.icon
        val contentTint = if (selected) colors.accent else ladder.content
        Box(
            modifier = Modifier
                .clip(RailItemShape)
                .then(
                    if (ladder.focusBorder != null) {
                        Modifier.border(Dimens.FocusBorderWidth, ladder.focusBorder, RailItemShape)
                    } else {
                        Modifier
                    },
                )
                .animateContentSize(animationSpec = ownTvTween(220)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (expanded) 14.dp else 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (expanded) Arrangement.spacedBy(12.dp) else Arrangement.Center,
            ) {
                Icon(
                    imageVector = navIcon(section = section, selected = selected),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(RailIconSize),
                )
                if (expanded) {
                    Text(
                        text = stringResource(section.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = contentTint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = RailLabelMaxWidth).then(
                            if (focused) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier,
                        ),
                    )
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = localizedInteger(count),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            if (position == RailPosition.LEFT) {
                NavAccentBar(visible = selected)
            } else {
                HorizontalNavAccentBar(visible = selected)
            }
        }
    }
}

/**
 * TOP-mode counterpart to [NavAccentBar] (`ui/components/NavLadder.kt`) — that helper hard-codes a
 * vertical bar pinned to [Alignment.CenterStart], which reads correctly for the LEFT rail's
 * vertical list but not for a horizontal row of items. Same animation spec (grows from 0 on
 * [ownTvTween]\(160\)) and same accent colour, rotated 90°: a horizontal pill centered under the
 * icon at the bottom of the item.
 */
@Composable
private fun BoxScope.HorizontalNavAccentBar(visible: Boolean, modifier: Modifier = Modifier) {
    val colors = OwnTVTheme.colors
    val thickness by animateDpAsState(
        if (visible) 3.dp else 0.dp,
        animationSpec = ownTvTween(160),
        label = "navAccentBarTop",
    )
    if (thickness > 0.dp) {
        Box(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .width(22.dp)
                .height(thickness)
                .clip(RoundedCornerShape(50))
                .background(colors.accent),
        )
    }
}

// ---- Previews ----------------------------------------------------------------------------------

private val PreviewVisibleSections = MainSection.allBrowse
private val PreviewCounts: (MainSection) -> Int = { if (it == MainSection.DOWNLOADS) 3 else 0 }

@TvPreview
@Composable
private fun FloatingRailLeftIdlePreview() = OwnTVPreview {
    Box(Modifier.fillMaxSize().padding(24.dp)) {
        FloatingRail(
            position = RailPosition.LEFT,
            selected = MainSection.LIVE_TV,
            visibleSections = PreviewVisibleSections,
            onSelect = {},
            avatarId = 0,
            onPickAvatar = {},
            onSwitchProfile = {},
            profileName = "Living Room",
            selectedItemFocusRequester = remember { FocusRequester() },
            onActiveChange = {},
            counts = PreviewCounts,
            forceActive = false,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

@TvPreview
@Composable
private fun FloatingRailLeftActivePreview() = OwnTVPreview {
    Box(Modifier.fillMaxSize().padding(24.dp)) {
        FloatingRail(
            position = RailPosition.LEFT,
            selected = MainSection.LIVE_TV,
            visibleSections = PreviewVisibleSections,
            onSelect = {},
            avatarId = 0,
            onPickAvatar = {},
            onSwitchProfile = {},
            profileName = "Living Room",
            selectedItemFocusRequester = remember { FocusRequester() },
            onActiveChange = {},
            counts = PreviewCounts,
            forceActive = true,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

@TvPreview
@Composable
private fun FloatingRailTopIdlePreview() = OwnTVPreview {
    Box(Modifier.fillMaxSize().padding(24.dp)) {
        FloatingRail(
            position = RailPosition.TOP,
            selected = MainSection.HOME,
            visibleSections = PreviewVisibleSections,
            onSelect = {},
            avatarId = 3,
            onPickAvatar = {},
            onSwitchProfile = {},
            profileName = "Bedroom",
            selectedItemFocusRequester = remember { FocusRequester() },
            onActiveChange = {},
            counts = PreviewCounts,
            forceActive = false,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@TvPreview
@Composable
private fun FloatingRailTopActivePreview() = OwnTVPreview {
    Box(Modifier.fillMaxSize().padding(24.dp)) {
        FloatingRail(
            position = RailPosition.TOP,
            selected = MainSection.HOME,
            visibleSections = PreviewVisibleSections,
            onSelect = {},
            avatarId = 3,
            onPickAvatar = {},
            onSwitchProfile = {},
            profileName = "Bedroom",
            selectedItemFocusRequester = remember { FocusRequester() },
            onActiveChange = {},
            counts = PreviewCounts,
            forceActive = true,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
