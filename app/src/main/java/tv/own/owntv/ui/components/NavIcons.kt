package tv.own.owntv.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import tv.own.owntv.features.shell.MainSection

/**
 * Shared nav glyph for a [MainSection] — the standard M3 nav-icon convention: [Icons.Outlined]
 * at rest, [Icons.Rounded] (filled) when [selected]. Hoisted out of `FloatingRail.kt` so the
 * floating rail (`features/shell/components/FloatingRail.kt`, where "selected" is the current
 * screen) and the nav-menu customization screen
 * (`features/settings/NavMenuSettingsScreen.kt`, where "selected" is a row's shown/hidden toggle)
 * share one mapping and can never visually disagree about which glyph represents a section.
 *
 * EPG maps to `GridOn` over `TableRows`: an EPG is a two-axis grid (channel rows × time
 * columns), which `GridOn`'s grid-of-cells glyph reads as more directly than `TableRows`'
 * column-less horizontal-bar glyph.
 *
 * SEARCH has no icon slot in either screen — it lives in the top bar (see [MainSection.isBrowse])
 * — so passing it is a caller bug, not a state to render.
 */
internal fun navIcon(section: MainSection, selected: Boolean): ImageVector = when (section) {
    MainSection.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
    MainSection.LIVE_TV -> if (selected) Icons.Rounded.LiveTv else Icons.Outlined.LiveTv
    MainSection.MOVIES -> if (selected) Icons.Rounded.Movie else Icons.Outlined.Movie
    MainSection.SERIES -> if (selected) Icons.Rounded.VideoLibrary else Icons.Outlined.VideoLibrary
    MainSection.DOWNLOADS -> if (selected) Icons.Rounded.Download else Icons.Outlined.Download
    MainSection.EPG -> if (selected) Icons.Rounded.GridOn else Icons.Outlined.GridOn
    MainSection.SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
    MainSection.SEARCH -> error("navIcon has no Search glyph; Search lives in the top bar")
}
