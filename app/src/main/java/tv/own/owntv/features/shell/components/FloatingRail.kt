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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
// LEFT's panel shape is no longer a constant (Task 3: corners animate 28dp -> 0dp when active via
// leftCornerRadius below) — only TOP's stays fixed (always a pill, active or idle).
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

    // LEFT active drawer (shell-refinements Task 3): corners square off (28dp -> 0dp) as the panel
    // becomes a full-height edge drawer — animated here (not in the shell) since this composable
    // already owns `active` and already threads a single `shape` value through shadow/clip/glass/
    // border below. TOP's pill shape never changes with `active`, so it stays the constant.
    val leftCornerRadius by animateDpAsState(
        targetValue = if (position == RailPosition.LEFT && active) 0.dp else 28.dp,
        animationSpec = ownTvTween(),
        label = "railLeftCornerRadius",
    )
    val shape = if (position == RailPosition.LEFT) RoundedCornerShape(leftCornerRadius) else RailPanelShapeTop

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
                        cornerRadius = if (position == RailPosition.LEFT) leftCornerRadius else 999.dp,
                    )
                    .border(1.dp, RailPanelBorderColor, shape)
            } else {
                Modifier
            },
        )

    if (position == RailPosition.LEFT) {
        // Idle: narrow icon-only column, centered as before. Active edge drawer (Task 3): rows
        // start-justify against a consistent ~20dp inset instead of each row centering independently
        // within the (now content-width-driven, per-row-varying) column — see RailNavItem below,
        // whose expanded rows are no longer all the same width once labels differ in length.
        //
        // Invariant (review round 2, min-convergence): this active horizontal inset (20dp start +
        // 16dp end = 36dp total) must stay >= idle's 9dp+9dp=18dp total. OwnTVShell's idle-width
        // capture (`railWidthPx = minOf(railWidthPx, it.width)`) assumes every transient frame
        // between idle and active is >= the true idle width — narrowing the active padding below
        // idle's would risk a collapse-animation frame measuring narrower than idle and corrupting
        // that floor.
        val columnPadding = if (active) {
            PaddingValues(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
        } else {
            PaddingValues(horizontal = 9.dp, vertical = 16.dp)
        }
        val columnAlignment = if (active) Alignment.Start else Alignment.CenterHorizontally
        Column(
            modifier = panel.padding(columnPadding),
            horizontalAlignment = columnAlignment,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            RailAvatar(avatarId = avatarId, profileName = profileName, onSwitchProfile = onSwitchProfile, onPickAvatar = onPickAvatar)
            RailSeparator(position = position, active = active)
            // Review round 2 (M1): three-area drawer, matching the idle column's own distribution —
            // avatar pinned top, Settings pinned bottom (both outside this Box, unchanged below), and
            // this destinations area as the ONE weighted middle region so the nav cluster stays
            // vertically centered in the drawer instead of teleporting to the top on expand. `weight`
            // is applied ONLY while active: the idle Column is wrap-content height (no fillMaxHeight
            // upstream), and weighting a child there would force Compose to stretch the whole idle
            // pill to fill the screen — exactly the floating-pill geometry idle must NOT have.
            Box(
                modifier = if (active) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier.verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp), horizontalAlignment = columnAlignment) {
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
            RailSeparator(position = position, active = active)
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

/**
 * Thin translucent divider between the rail's three areas — vertical bar for LEFT, horizontal for
 * TOP. Review round 2 (M2): the LEFT active edge drawer spans the full drawer width, so its
 * separators should too — `active` switches the fixed 28dp centered tick to `fillMaxWidth()`.
 * Idle (and TOP, which never becomes a full-width drawer) keep the original fixed-length tick.
 */
@Composable
private fun RailSeparator(position: RailPosition, active: Boolean = false) {
    if (position == RailPosition.LEFT) {
        Box(
            modifier = (if (active) Modifier.fillMaxWidth() else Modifier.width(RailSeparatorLength))
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
        // Ladder alone is the single source of truth for tint (it already returns accent for both
        // selected states, focused or not) — no local peak-state override needed here.
        val ladder = rememberNavLadderColors(selected = selected, focused = focused)
        val iconTint = ladder.icon
        val contentTint = ladder.content
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
                // Review round 2 (L2): 12dp (not 14dp) start padding here is what makes this icon's
                // center land on the same vertical line as the avatar's, once the active drawer
                // start-justifies both instead of centering them independently. Both sit under the
                // same outer Column start inset (20dp, see the LEFT branch above), so: avatar center
                // = 20 + 24 (half of the 48dp avatar) = 44dp; icon center = 20 + 12 (this padding) +
                // 12 (half of the 24dp icon) = 44dp. (Raising the outer 20dp inset instead — the
                // drawer's other proposed fix — would NOT close this gap: both shift by the same
                // amount, so the relative 2dp offset survives; only this row's own padding controls
                // the icon's offset from the shared start inset.)
                modifier = Modifier.padding(horizontal = if (expanded) 12.dp else 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                // `expanded` packs Icon/Text/badge with no extra space to distribute (the Row is
                // wrap-content width, not fillMaxWidth), so they already sit flush at the row's own
                // start regardless of Arrangement's alignment parameter — plain spacedBy(12.dp).
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

/**
 * LEFT + forceActive renders the Task 3 edge drawer: full-height, flush to the start edge (no
 * outer padding on the preview's own [Box], unlike the idle preview above), squared corners — the
 * same geometry [tv.own.owntv.features.shell.OwnTVShell] produces by dropping its 30dp inset to
 * 0dp and adding `fillMaxHeight()` once the rail is active (see OwnTVShell's FloatingRail call
 * site).
 */
@TvPreview
@Composable
private fun FloatingRailLeftActivePreview() = OwnTVPreview {
    Box(Modifier.fillMaxSize()) {
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
            modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight(),
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
