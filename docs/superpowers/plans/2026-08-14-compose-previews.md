# Compose Previews Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Working Android Studio Compose previews: a theme-correct harness, the one dead `@Preview` removed, and seeded previews across the shell nav surfaces and shared `ui/components/`.

**Architecture:** One new file (`ui/preview/OwnTVPreview.kt`) holds the harness composable and two multipreview annotations; preview functions are private, co-located at the bottom of each component's own file, and use plain fake data. Additive only — no component body/signature/layout changes.

**Tech Stack:** Kotlin, Jetpack Compose for TV (tv-material3), `androidx.compose.ui.tooling.preview` (already an `implementation` dep; `ui-tooling` already `debugImplementation`).

## Global Constraints

- **Additive only:** no component's body, signature, or layout changes. The only deletion is `OwnTVShell.kt`'s dead `@Preview` annotation line.
- **Focus states can't be forced in static previews** — show only parameter-reachable states (e.g. `selected`/`active` params); record per-site what was shown. Never refactor a component to fake focus.
- **Components requiring a ViewModel, Koin, or live state flows are OUT of scope.** Leaf components with plain-data params only.
- **i18n:** zero `res/values*` changes; preview fake data may be raw strings; reuse `stringResource` where an id is already at hand.
- **Git hygiene:** stage edited files by explicit path only; NEVER `git commit -am`/`git add -A` (user's uncommitted gradle files in the tree).
- **Gates per task:** `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` (0 errors); `git status --porcelain -- app/src/main/res` empty.
- Branch: `compose-previews` off `main`.

---

### Task 1: Preview harness + remove the dead shell `@Preview`

**Files:**
- Create: `app/src/main/java/tv/own/owntv/ui/preview/OwnTVPreview.kt`
- Modify: `app/src/main/java/tv/own/owntv/features/shell/OwnTVShell.kt` (delete the `@Preview` line at ~:91 and its now-unused import if any)

**Interfaces — Produces (Tasks 2-3 rely on these exact names):**
```kotlin
@Composable fun OwnTVPreview(light: Boolean = false, content: @Composable () -> Unit)
annotation class TvPreview          // full-screen 1920x1080 TV spec
annotation class TvComponentPreview // hugged, no device spec
```

- [ ] **Step 1:** Read `app/src/main/java/tv/own/owntv/ui/theme/OwnTVColors.kt` and note the DARK theme's `background` hex (the value behind `ownTvColors(isDark = true, ...)`). Call it `<DARK_BG>` below — use the real literal.
- [ ] **Step 2:** Create the harness file:

```kotlin
package tv.own.owntv.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import tv.own.owntv.ui.theme.AccentColor
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.theme.ThemeMode

/**
 * Preview-only theme harness: real OwnTVTheme (Figtree type, colour tokens, default glass/animation
 * locals) around [content], on the theme background. Never referenced from production code.
 */
@Composable
fun OwnTVPreview(light: Boolean = false, content: @Composable () -> Unit) {
    OwnTVTheme(
        themeMode = if (light) ThemeMode.LIGHT else ThemeMode.DARK,
        accent = AccentColor.TEAL,
        systemInDarkTheme = !light,
    ) {
        Box(Modifier.background(OwnTVTheme.colors.background)) { content() }
    }
}

/** Full-screen TV canvas (1080p at TV density) for tall/anchored surfaces. */
@Preview(device = "spec:width=1920dp,height=1080dp,dpi=213", showBackground = true, backgroundColor = <DARK_BG>)
annotation class TvPreview

/** Hugged canvas for small components — no device spec so the preview wraps its content. */
@Preview(showBackground = true, backgroundColor = <DARK_BG>)
annotation class TvComponentPreview
```
Verify in situ: the exact enum names (`ThemeMode`, `AccentColor.TEAL`) and their packages — adjust imports to the real ones; the file's KDoc style matches the codebase.
- [ ] **Step 3:** In `OwnTVShell.kt` delete the `@Preview` annotation line above `fun OwnTVShell(` (~:91) and remove the `androidx.compose.ui.tooling.preview.Preview` import if nothing else in the file uses it.
- [ ] **Step 4:** Add one proving preview at the bottom of `OwnTVPreview.kt` itself:

```kotlin
@TvComponentPreview
@Composable
private fun HarnessPreview() = OwnTVPreview {
    androidx.tv.material3.Text("OwnTV preview harness", style = androidx.tv.material3.MaterialTheme.typography.titleMedium)
}
```
- [ ] **Step 5: Gate.** `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` → 0 errors.
- [ ] **Step 6: Commit.** `git add app/src/main/java/tv/own/owntv/ui/preview/OwnTVPreview.kt app/src/main/java/tv/own/owntv/features/shell/OwnTVShell.kt && git commit -m "Add Compose preview harness; drop dead shell preview"`

### Task 2: Shell nav-surface previews

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/Sidebar.kt`
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/CategoryRail.kt`
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/TopBar.kt`

**Interfaces — Consumes:** `OwnTVPreview`, `@TvPreview`, `@TvComponentPreview` from Task 1 (exact signatures above).

- [ ] **Step 1: Sidebar.** At the bottom of `Sidebar.kt` add two private previews. Read the real signatures first; `NavItem` is file-private so previewing it directly from the same file is fine.

```kotlin
@TvPreview
@Composable
private fun SidebarPreview() = OwnTVPreview {
    Sidebar(
        selected = MainSection.LIVE_TV,
        onSelect = {},
        visibleSections = MainSection.allBrowse,
        avatarId = 0,
        onPickAvatar = {},
        profileName = "Living Room",
        sourceSummary = "My Playlist",
        onSwitchProfile = {},
        selectedItemFocusRequester = FocusRequester(),
        onFocused = {},
        counts = { if (it == MainSection.DOWNLOADS) 3 else 0 },
    )
}

@TvComponentPreview
@Composable
private fun NavItemStatesPreview() = OwnTVPreview {
    Column(Modifier.width(Dimens.SidebarWidthCollapsed).padding(8.dp)) {
        NavItem(section = MainSection.HOME, active = false, expanded = false, count = 0, onClick = {})
        NavItem(section = MainSection.LIVE_TV, active = true, expanded = false, count = 0, onClick = {})
        NavItem(section = MainSection.DOWNLOADS, active = false, expanded = false, count = 3, onClick = {})
    }
}
```
Adjust to the REAL `Sidebar`/`NavItem` parameter lists in situ (they were read this week: Sidebar takes exactly the params above; verify nothing drifted). Record in the report: focus-driven ladder states not shown (parameter-reachable only: idle, active, count badge).
- [ ] **Step 2: CategoryRail.** Read `CategoryRail`'s public signature in full. If it takes plain data (`List<RailCategory>`, selected index, callbacks, focus plumbing with defaultable/inert values) add a `@TvPreview` `CategoryRailPreview` rendering: Favorites row (icon), History row (icon), "All Channels" (showGenreDot = false), two plain folders. If its signature requires live state that can't be inertly faked (verify — e.g. non-defaultable FocusRequester params are fine, pass `FocusRequester()`), preview `RailPill` (file-private) states instead and record the fallback. Use raw strings for names.
- [ ] **Step 3: TopBar chips.** At the bottom of `TopBar.kt` add `@TvComponentPreview` `TopBarChipsPreview` rendering a `Row` of the static chips: `SectionChip` (verify name/params — the static current-section chip), `ClockChip` if it accepts a time/format param or renders from system time (fine either way), `WeatherChip` with a fixed `WeatherInfo` instance (read its constructor — fake ~22°C, a real `WeatherGlyph` value), and the non-interactive `PlaylistChip` branch if it's a separate composable or reachable via params. Private chips in the same file are directly callable. Skip any chip whose params can't be inertly faked; record skips.
- [ ] **Step 4: Gate + commit.** Gate green, then `git add app/src/main/java/tv/own/owntv/features/shell/components/Sidebar.kt app/src/main/java/tv/own/owntv/features/shell/components/CategoryRail.kt app/src/main/java/tv/own/owntv/features/shell/components/TopBar.kt && git commit -m "Previews for shell nav surfaces"`

### Task 3: Shared ui/components previews

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/ui/components/OwnTVButton.kt`
- Modify: `app/src/main/java/tv/own/owntv/ui/components/MediaListRow.kt`
- Modify: `app/src/main/java/tv/own/owntv/ui/components/MediaContextMenu.kt`
- Modify: `app/src/main/java/tv/own/owntv/ui/components/CategoryHeader.kt`
- Modify: `app/src/main/java/tv/own/owntv/ui/components/StepperDialog.kt`
- Modify: `app/src/main/java/tv/own/owntv/ui/components/OwnTVAvatar.kt` (verify actual file name via grep for `fun OwnTVAvatar`)

**Interfaces — Consumes:** `OwnTVPreview`, `@TvComponentPreview` from Task 1.

- [ ] **Step 1: OwnTVButton.** Bottom-of-file preview:

```kotlin
@TvComponentPreview
@Composable
private fun OwnTVButtonPreview() = OwnTVPreview {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OwnTVButton(label = "Primary", onClick = {})
        OwnTVButton(label = "Secondary", onClick = {}, style = OwnTVButtonStyle.SECONDARY)
        OwnTVButton(label = "Compact", onClick = {}, compact = true)
        OwnTVButton(label = "Disabled", onClick = {}, enabled = false)
    }
}
```
- [ ] **Step 2: MediaListRow.** Read its signature; render three rows in a `Column` (~420.dp wide): plain, `dimmed = true`, and one with the favorite affordance if it's a parameter. Fake titles ("Big Buck Bunny" etc.). Pass inert lambdas/focus params.
- [ ] **Step 3: MediaContextMenu.** Render with `title = "Big Buck Bunny"`, 3 `MenuEntry` items (verify the real `MenuEntry(label, onClick, icon?)` shape) and the close label as a raw string or the existing `R.string.content_close` — whichever the component's signature takes. If the component is Popup/Dialog-based and won't render inline in a preview (verify), render its content-level composable if one exists; otherwise record the limitation and skip.
- [ ] **Step 4: CategoryHeader + StepperDialog + OwnTVAvatar.** `CategoryHeader(title = "All Movies", count = 3)` (verify real params). `StepperDialog`: two previews or one column — with `onReset = {}` and with `onReset = null` (title "UI Zoom", value 100, step 5, min 70, max 130, `format = { "$it%" }`); if it's Dialog/Popup-hosted and can't render inline, apply the same content-level-or-skip rule as Step 3 and record. `OwnTVAvatar`: a row of 4 avatar ids at 46.dp.
- [ ] **Step 5: Gate + commit.** `git add <the six files> && git commit -m "Previews for shared ui components"`

### Task 4: Verification + finish

**Files:** none (fix-forward only).

- [ ] **Step 1: Suite.** `./gradlew testStandardDebugUnitTest lintStandardDebug` → green.
- [ ] **Step 2: Static render sanity.** No CLI renderer exists; as a proxy, confirm (a) every new preview function is private, zero-arg, and annotated with exactly one of the two multipreview annotations; (b) `grep -rn "@Preview" app/src/main/java` shows ONLY `OwnTVPreview.kt`'s annotation classes (definitions) and no direct `@Preview` on component functions; (c) no preview references Koin/ViewModel symbols (`grep -n "koin\|ViewModel" <the preview blocks>`).
- [ ] **Step 3: Hand to user.** The user opens `Sidebar.kt` and `OwnTVButton.kt` in Android Studio to confirm rendering; fix-forward anything they report. Then final whole-branch review, ONE fix wave if findings, `superpowers:finishing-a-development-branch`.

## Self-Review

1. **Spec coverage:** §1 harness→Task 1 (both annotations + light param), §2 shell fix→Task 1 Step 3, §3 seeded previews→Tasks 2-3 (all listed components), out-of-scope rules→Global Constraints, verification→Task 4. No gaps.
2. **Placeholder scan:** `<DARK_BG>` is an explicit read-then-inline instruction (not a TBD); all preview code blocks concrete with verify-in-situ adjustments bounded by report-recording. Clean.
3. **Type consistency:** `OwnTVPreview(light, content)`, `TvPreview`, `TvComponentPreview` names identical across Tasks 1-3; `OwnTVButtonStyle.SECONDARY` matches the real enum; `MainSection.allBrowse` exists (ShellViewModel.kt:62).
