package tv.own.owntv.features.shell.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
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
// The panel (fill/border/shadow) only renders when active, and LEFT's active state is the squared
// edge drawer — so LEFT's shape is a constant rectangle. It used to animate 28dp -> 0dp on expand,
// but the idle rail draws no panel at all, so the rounded start value was only ever visible as a
// transient corner-morph artifact during the expand tween. TOP stays a pill, active or idle.
private val RailPanelShapeLeft = RectangleShape
private val RailPanelShapeTop = RoundedCornerShape(50)

/**
 * Floating shell nav rail (Task 2 of the floating-shell plan) — a self-positioning "pill" that
 * replaces the old fixed Sidebar (deleted; LEFT is this component's default orientation) and
 * introduces a horizontal TOP mode, per [RailPosition]. LEFT is a full-height edge strip in both
 * states (invisible when idle — icons centered by its weighted middle region — so height never
 * animates; only its width does); TOP wraps its content. The shell (Task 4) is responsible for
 * placement (`Alignment.CenterStart` for LEFT, centered below the header for TOP) via [modifier].
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

    // Collapse ease (user feedback): the mirror of the open. `active` drops the labels immediately
    // (they shrink on their own animateContentSize), but the panel, avatar/Settings, full height
    // and intrinsic-width bound must OUTLIVE it while the container width eases back down —
    // `settling` covers that window, so `expandedVisuals` is what the geometry/chrome key off.
    // Set synchronously on the active->idle transition (a LaunchedEffect would hand the visuals
    // one decomposed frame first); cleared by the container animateContentSize's finishedListener
    // when the width lands, with a timeout net in case an interrupted animation swallows the
    // callback. Once it clears, the spec below is already snap(), so shedding fillMaxHeight and
    // the drawer furniture doesn't animate — the panel is gone by then, and easing the invisible
    // wrapper would drag the idle icons across the screen.
    var settling by remember { mutableStateOf(false) }
    var prevActive by remember { mutableStateOf(active) }
    if (active != prevActive) {
        prevActive = active
        if (!active) settling = true
    }
    LaunchedEffect(settling) {
        if (settling) {
            delay(600)
            settling = false
        }
    }
    val expandedVisuals = active || settling

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
            if (expandedVisuals) {
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
                        cornerRadius = if (position == RailPosition.LEFT) 0.dp else 999.dp,
                    )
                    .border(1.dp, RailPanelBorderColor, shape)
            } else {
                Modifier
            },
        )

    if (position == RailPosition.LEFT) {
        // Narrow icon-only column when idle; full-height edge drawer when active (Task 3).
        //
        // Both states are START-justified with the SAME 9dp start inset and the same 12dp item
        // padding (RailNavItem below), which pins the avatar and icon centers to one 33dp line
        // (9 + 24 half-avatar = 9 + 12 + 12 half-icon = 33) through the whole expand/collapse
        // cycle. Idle used to center items instead — visually identical (every idle row is the
        // avatar's 48dp width), but on COLLAPSE the centering re-applied instantly while each
        // label was still animating away (animateContentSize, 220ms), so the icon rows got
        // re-centered against a column still as wide as the widest shrinking label and visibly
        // slid sideways until the last label finished. Start justification makes intermediate
        // widths irrelevant: icons never move, only the labels grow/shrink beside them.
        //
        // Invariant (review round 2, min-convergence): the active horizontal inset (9dp start +
        // 16dp end = 25dp total) must stay >= idle's 9dp+9dp=18dp total. OwnTVShell's idle-width
        // capture (`railWidthPx = minOf(railWidthPx, it.width)`) assumes every transient frame
        // between idle and active is >= the true idle width — narrowing the active padding below
        // idle's would risk a collapse-animation frame measuring narrower than idle and corrupting
        // that floor.
        val columnPadding = if (active) {
            PaddingValues(start = 9.dp, top = Dimens.GapMedium, end = Dimens.GapMedium, bottom = Dimens.GapMedium)
        } else {
            PaddingValues(horizontal = 9.dp, vertical = Dimens.GapMedium)
        }
        val columnAlignment = Alignment.Start
        Column(
            // Ease-open (user feedback): the drawer's WIDTH animates — below the panel's
            // shadow/clip/glass/border chain, so those visuals track the animated size — while its
            // height snaps to full immediately. The axis split comes from the modifier ORDER:
            // fillMaxHeight() sits ABOVE animateContentSize, so the animator runs under exact
            // height constraints (animateContentSize clamps its animated size to incoming
            // constraints — height is pinned to full from the first frame) while width, loosely
            // constrained, eases 220ms in step with the labels' own animateContentSize. The
            // width(IntrinsicSize.Max) underneath snaps the CONTENT to its final drawer layout
            // immediately (focus targets exist at once; the clip(shape) above reveals it as the
            // panel widens) and keeps the C1 bound: fillMaxWidth separators report zero intrinsic
            // width, so they can't balloon the drawer to screen width — the widest real row
            // dictates it. Collapse mirrors this through `settling` (see its declaration): panel
            // and furniture persist while the labels shrink and the width eases back, and only
            // once the width lands does the chrome decompose.
            //
            // fillMaxHeight is UNCONDITIONAL: the idle column is a full-height invisible strip
            // (no panel, and the weighted middle Box below centers the icons exactly where the
            // old wrap-content column's screen-centering put them). That pins the animator's
            // height to a constant, so animateContentSize only ever animates WIDTH — crucial
            // because its animated size runs one frame behind composition: when the height also
            // changed, the teardown frame briefly reported the stale full height around the
            // already-idle content (which it top-anchors), flashing the icons above their
            // resting spot. With height constant, teardown is a layout no-op.
            modifier = panel
                .fillMaxHeight()
                .animateContentSize(
                    animationSpec = if (expandedVisuals) ownTvTween(220) else snap(),
                    finishedListener = { _, _ -> settling = false },
                )
                .then(if (expandedVisuals) Modifier.width(IntrinsicSize.Max) else Modifier)
                .padding(columnPadding),
            horizontalAlignment = columnAlignment,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Profile + Settings (and their separators) are drawer furniture, not idle chrome —
            // composed only while expanded (user feedback; kept through `settling` so they don't
            // vanish mid-collapse), so the idle rail is purely the browse destinations.
            if (expandedVisuals) {
                RailAvatar(avatarId = avatarId, profileName = profileName, onSwitchProfile = onSwitchProfile, onPickAvatar = onPickAvatar)
                RailSeparator(position = position, active = expandedVisuals)
            }
            // Review round 2 (M1): three-area drawer — avatar pinned top, Settings pinned bottom
            // (both outside this Box), and this destinations area as the ONE weighted middle
            // region so the nav cluster stays vertically centered. The weight is unconditional
            // now that the column is always full-height (see the modifier comment above): idle
            // has no furniture, so the weighted region spans nearly the whole strip and centers
            // the icons where the old wrap-content column's screen-centering did.
            Box(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
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
            if (expandedVisuals) {
                RailSeparator(position = position, active = expandedVisuals)
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
    } else {
        Row(
            // Same ease treatment as LEFT, both directions: avatar/Settings popping in/out would
            // otherwise jump the pill width in one frame while the labels ease.
            modifier = panel
                .animateContentSize(
                    animationSpec = if (expandedVisuals) ownTvTween(220) else snap(),
                    finishedListener = { _, _ -> settling = false },
                )
                .padding(horizontal = Dimens.HeroGap, vertical = Dimens.GapSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            if (expandedVisuals) {
                RailAvatar(avatarId = avatarId, profileName = profileName, onSwitchProfile = onSwitchProfile, onPickAvatar = onPickAvatar)
                RailSeparator(position = position)
            }
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
            if (expandedVisuals) {
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
        // No focus glow on rail items (user feedback): the halo bloomed around the ring and read
        // as a brighter, fatter border — the crisp FocusBorderWidth ring alone is the cursor here.
        glowElevation = 0,
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
                // center land on the same vertical line as the avatar's, once the drawer
                // start-justifies both instead of centering them independently. Both sit under the
                // same outer Column start inset (9dp, see the LEFT branch above), so: avatar center
                // = 9 + 24 (half of the 48dp avatar) = 33dp; icon center = 9 + 12 (this padding) +
                // 12 (half of the 24dp icon) = 33dp. The SAME 12dp applies collapsed (it used to be
                // 10dp): LEFT idle is start-justified too, so a differing collapsed padding would
                // shift the icons 2dp on every expand/collapse. (Changing the shared outer inset
                // instead would NOT close an avatar/icon gap: both shift by the same amount; only
                // this row's own padding controls the icon's offset from the shared start inset.)
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                                .clip(RoundedCornerShape(Dimens.CornerXSmall))
                                .background(colors.primaryContainer)
                                .padding(horizontal = Dimens.GapSmall, vertical = 2.dp),
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
                // Expanded drawer drops the bar (user feedback): the accent icon+label already
                // marks the active section there, and the bar crowded the ring's left edge on the
                // selected+focused item. Idle (icons only) keeps it as the active-section marker.
                NavAccentBar(visible = selected && !expanded)
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
            forceActive = false,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

/**
 * LEFT + forceActive renders the Task 3 edge drawer: full-height, flush to the start edge, squared
 * corners, content-width — the active geometry (`fillMaxHeight().width(IntrinsicSize.Max)`) is
 * internal to FloatingRail now, so the preview only places it like the shell does (both states sit
 * flush at the edge; the widest row — not a fixed Dp — dictates the drawer width).
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
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

@TvPreview
@Composable
private fun FloatingRailTopIdlePreview() = OwnTVPreview {
    Box(Modifier.fillMaxSize().padding(Dimens.GapLarge)) {
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
    Box(Modifier.fillMaxSize().padding(Dimens.GapLarge)) {
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
