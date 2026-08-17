package tv.own.owntv.ui.theme

import androidx.annotation.StringRes
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import tv.own.owntv.R

/**
 * How much UI motion to render. A performance/comfort control: lower-end Android TV boxes can feel
 * laggy when moving quickly between menus, so the user can tone the animations down (or off).
 */
enum class AnimationLevel(@StringRes val labelRes: Int) {
    // On = normal motion; Off = instant (no transitions). The fixed grid (v4.0.0) removed the old reason for
    // a middle "Reduced" tier, so this is now a simple On/Off reduce-motion toggle. (Legacy "REDUCED" values
    // fall back to On via the settings store's safe parse.)
    FULL(R.string.common_on), OFF(R.string.common_off);

    /** Scale an animation duration to this level (OFF collapses to 0 → an instant snap). */
    fun scale(durationMs: Int): Int = when (this) {
        FULL -> durationMs
        OFF -> 0
    }
}

/** Current animation level, provided at the theme root from the user's setting. */
val LocalAnimationLevel = staticCompositionLocalOf { AnimationLevel.FULL }

/** True unless the user has turned animations fully Off — for spots that gate a transition entirely. */
val animationsOn: Boolean
    @Composable @ReadOnlyComposable get() = LocalAnimationLevel.current != AnimationLevel.OFF

/** A tween whose duration follows the user's Animations setting (Off → an instant 0 ms snap). */
@Composable
@ReadOnlyComposable
fun <T> ownTvTween(durationMs: Int = 200, easing: Easing = FastOutSlowInEasing): TweenSpec<T> =
    tween(LocalAnimationLevel.current.scale(durationMs), easing = easing)

/** Spring for focus scale (M3-expressive feel). AnimationLevel OFF collapses to an instant snap. */
@Composable
@ReadOnlyComposable
fun ownTvFocusSpring(): AnimationSpec<Float> =
    if (LocalAnimationLevel.current == AnimationLevel.OFF) snap()
    else spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)

/** Duration for focus/selection color cross-fades (`FocusableSurface`'s focus container, `NavLadder`'s bg/fg/icon). */
const val MotionColorMs = 140

/** Duration for the nav accent bar's reveal/hide (`NavLadder`, `FloatingRail`). */
const val MotionAccentBarMs = 160

/** Duration for `FloatingRail`'s expand/collapse `animateContentSize` calls. */
const val MotionRailMs = 220

// The three FocusSettleDelay* constants gate `delay(N); runCatching { requestFocus() }` calls that let
// layout settle before grabbing D-pad focus. TIMING-SENSITIVE: these three magnitudes are independently
// tuned per call site and are NOT interchangeable snap targets — LiveScreen.kt documents a real focus-
// restore regression from an earlier racy delay. Do not consolidate or "clean up" these values.

/** Focus-settle delay for lighter transitions (tab swaps, inline error focus, player-dialog retries). */
const val FocusSettleDelayShortMs = 50L

/** Focus-settle delay, the dominant magnitude across dialogs, settings screens, and list restores. */
const val FocusSettleDelayMs = 60L

/** Focus-settle delay for heavier layouts (grids/lists, IME-adjacent dialogs). */
const val FocusSettleDelayLongMs = 80L
