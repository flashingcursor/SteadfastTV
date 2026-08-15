# Floating Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the fixed sidebar + chip top bar with the approved floating shell: floating rail (left/top, idle/active), transparent three-zone header, brand watermark, unified BACK-to-menu policy.

**Architecture:** New `FloatingRail.kt` and `ShellHeader.kt` composables built against the existing NavLadder/glass/focus systems; `OwnTVShell.kt` swaps them in, owns the TOP-mode content inset, the active-rail scrim, the watermark, and the shell-level BACK floor. `Sidebar.kt` deleted after adoption; `TopBar.kt` reduced or deleted per what survives. Visual contract: the approved mockup (https://claude.ai/code/artifact/f4440a9f-aa74-4dd2-946e-62d68a4a8e5b).

**Tech Stack:** Kotlin, Jetpack Compose for TV (tv-material3), existing `FocusableSurface`/`NavLadder`/glass system, DataStore-backed `SettingsRepository`, the shipped preview harness (`OwnTVPreview`, `@TvPreview`).

## Global Constraints

- **Design language:** accent = focus/active/selected only; white ring = sole focus cursor; the active rail panel is the one translucent surface of the idle shell; single-PRIMARY rule in dialogs.
- **Preserved behavior (spec §"Constraints"):** section switching, `visibleSections`/DYNAMIC mode, counts hook, avatar click/long-press actions, search entry to the Search section, weather/clock data sources, exit dialog, mini-player, Layer-2 `CategoryRail`, all page content. NavLadder stays (CategoryRail depends on it).
- **RTL:** rail LEFT = start edge; header zones start/center/end; TOP-mode areas start→end.
- **i18n:** new strings ALLOWED for the rail-position setting only — English into the right `values/strings_settings.xml` block, then seed locales with `tools/i18n/seed_translations.py` (read its --help/README first); `PluralsCandidate` fatal; pseudolocale checks must stay green.
- **Low-spec:** blur only via the existing glass system; translucent fallback = plain color fill, never RenderEffect.
- **Git hygiene:** stage edited files by explicit path only; NEVER `git commit -am`/`git add -A` (user's uncommitted gradle files in the tree).
- **Gates per task:** `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` (0 errors); `python3 tools/i18n/check_hardcoded_strings.py verify-ci` must show ZERO new unclassified entries attributable to your files (classify preview literals `technical` as shipped precedent; real UI strings go through res).
- Branch: `floating-shell` off `main`.

---

### Task 1: `railPosition` setting

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/settings/data/SettingsRepository.kt`
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/SettingsScreen.kt` (the Nav-menu / appearance settings region — find the existing "Nav menu" entry and add beside it)
- Modify: `app/src/main/res/values/strings_settings.xml` (+ locale seeding via tools)

**Interfaces — Produces (Tasks 2/4 rely on these):**
```kotlin
enum class RailPosition { LEFT, TOP }                    // features/settings/data (beside similar enums)
val SettingsRepository.railPosition: Flow<RailPosition>   // default LEFT
suspend fun SettingsRepository.setRailPosition(v: RailPosition)
```

- [ ] **Step 1:** Read how an existing enum-backed setting is stored (e.g. `miniPlayerPosition` ~SettingsRepository.kt:1260 region stores by `.name` with a `fromName` fallback). Add `RailPosition` + `railPosition` flow + setter following that exact pattern (string-keyed, default `LEFT`).
- [ ] **Step 2:** Settings UI: add a "Navigation rail position" row where the Nav-menu customization entry lives, cycling/choosing LEFT ("Left") / TOP ("Top") — copy the interaction pattern of the nearest enum setting row (e.g. mini-player position). New strings: setting title + the two value labels, added to `values/strings_settings.xml` with translator comments, then run `tools/i18n/seed_translations.py` per its usage to populate locale files; verify `check_pseudo_locales.py` and `verify-ci` stay green.
- [ ] **Step 3:** Unit test if the repository has existing tests for enum settings (grep `MiniPlayerPosition` in `app/src/test`); mirror one for `RailPosition.fromName` fallback if the pattern exists — otherwise record that repo settings have no unit-test precedent and skip.
- [ ] **Step 4: Gate + commit.** `git add <exact files incl. locale strings files> && git commit -m "Settings: navigation rail position (left/top)"`

### Task 2: `FloatingRail` component + previews

**Files:**
- Create: `app/src/main/java/tv/own/owntv/features/shell/components/FloatingRail.kt`

**Interfaces — Consumes:** `RailPosition` (Task 1), `MainSection`, `rememberNavLadderColors`/`NavAccentBar`, `OwnTVAvatar`, `NavDuotoneIcon`, `FocusableSurface`, glass (`GlassSurface.SIDEBAR`, `LocalGlass`), `OwnTVPreview`/`@TvPreview`.
**Interfaces — Produces (Task 4 relies on this exact signature):**
```kotlin
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
    onActiveChange: (Boolean) -> Unit,     // true while focus is inside the rail — Task 4 dims content with it
    counts: (MainSection) -> Int = { 0 },
    forceActive: Boolean = false,          // previews + BACK-activation: render expanded regardless of focus
    modifier: Modifier = Modifier,
)
```

- [ ] **Step 1:** Read `Sidebar.kt` in full — carry over verbatim: the focus-entry redirect (`onFocusChanged` + deferred `requestFocus`), the `focusSection` fallback rules, internal scroll at high zoom, avatar semantics. Build the three areas per the spec (§1): avatar / destinations / Settings, thin translucent separators.
- [ ] **Step 2:** Layout per orientation. LEFT: vertical, floating, `Alignment.CenterStart`-friendly (the SHELL positions it; the composable renders its own pill geometry), inset handled by caller. TOP: horizontal, areas start→end, vertical separators. One composable, `position` switches the arrangement (shared `RailNavItem` internals).
- [ ] **Step 3:** States. `active = forceActive || focusWithin`. Idle: no background, icons only, muted tint; selected keeps accent tint + `NavAccentBar` (LEFT: bar at start of item; TOP: rotate the accent bar under the icon — a horizontal accent pill, same animation). Active: labels expand (`animateContentSize`/max-width animation on `ownTvTween`), panel appears — `Modifier.glass(surface = GlassSurface.SIDEBAR, ...)` when glassy, else `colors.surfaceContainer.copy(alpha = .82f)`-family translucent fill, rounded, thin white 12% border, shadow. Report `onActiveChange` from focus changes.
- [ ] **Step 4:** Previews (bottom of file): `@TvPreview` × 4 — LEFT idle, LEFT `forceActive = true`, TOP idle, TOP forceActive — using fake profile data; classify preview literals `technical` if verify-ci flags them.
- [ ] **Step 5: Gate + commit.** `git add app/src/main/java/tv/own/owntv/features/shell/components/FloatingRail.kt tools/i18n/safe_literals.txt && git commit -m "FloatingRail: floating left/top nav with idle and active states"` (include safe_literals.txt only if actually changed).

### Task 3: `ShellHeader` component + previews

**Files:**
- Create: `app/src/main/java/tv/own/owntv/features/shell/components/ShellHeader.kt`

**Interfaces — Consumes:** `WeatherInfo` + `WeatherGlyph` (see current TopBar's weather chip for the data mapping), `OwnTVIcon.SEARCH`, `FocusableSurface`, glass, preview harness.
**Interfaces — Produces (Task 4 relies on this):**
```kotlin
@Composable
fun ShellHeader(
    title: String,                       // resolved page title (caller resolves MainSection.labelRes)
    onSearch: () -> Unit,
    weatherInfo: WeatherInfo?,           // null = hidden
    weatherFahrenheit: Boolean,
    modifier: Modifier = Modifier,       // caller pins to top; header is transparent full-width
)
```

- [ ] **Step 1:** Read `TopBar.kt` in full first — reuse its clock formatting (`DateFormat.getTimeFormat` + the minute-tick `LaunchedEffect`) and its weather glyph/temperature mapping VERBATIM; those behaviors move, not change.
- [ ] **Step 2:** Build the three zones as a `Row`/grid: start = `title` (`MaterialTheme.typography.headlineSmall`-class weight per the mockup, soft shadow: `Shadow(Color.Black.copy(alpha=.55f), blurRadius≈12f)` via `TextStyle.shadow`); center = the search pill (translucent: glass when on else `Color.White.copy(alpha=.09f)`+ 13% border; focusable, white ring on focus, `onSearch` on click; hint text = the EXISTING search hint string resource — find the one the Search screen/pill uses today, do NOT add a new string); end = weather (glyph + temp, plain text w/ shadow) then time. No capsules anywhere except the search pill.
- [ ] **Step 3:** Previews: `@TvPreview` header with weather + without (`weatherInfo = null`).
- [ ] **Step 4: Gate + commit.** `git add app/src/main/java/tv/own/owntv/features/shell/components/ShellHeader.kt tools/i18n/safe_literals.txt && git commit -m "ShellHeader: transparent three-zone header"` (safe_literals only if changed).

### Task 4: Shell integration (swap, watermark, TOP inset, scrim)

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/shell/OwnTVShell.kt`
- Delete: `app/src/main/java/tv/own/owntv/features/shell/components/Sidebar.kt`
- Modify or delete: `app/src/main/java/tv/own/owntv/features/shell/components/TopBar.kt` (delete if nothing else uses its internals after the header swap; `StaticGlassChip` may still be referenced — check; if only TopBar used it, it goes too)

**Interfaces — Consumes:** `FloatingRail` (Task 2 signature), `ShellHeader` (Task 3), `railPosition` flow (Task 1).

- [ ] **Step 1:** Read `OwnTVShell.kt`'s current arrangement (Sidebar + TopBar call sites — 2 each). Replace: collect `railPosition`; render `ShellHeader` pinned top (zones over full width); render `FloatingRail` — LEFT: `Alignment.CenterStart` with the mockup's inset; TOP: centered below the header. Content container gets a start inset in LEFT mode (rail width + gap, ~the current `SidebarWidthCollapsed` figure works as the reservation) and a top inset in TOP mode (header + rail heights) — implemented ONCE in the shell container, not per screen.
- [ ] **Step 2:** Active-rail scrim: when `onActiveChange` reports true (or BACK-activation forces it), dim the content area (`Color.Black.copy(alpha = .45f)` overlay, `ownTvTween` fade) — content stays visible underneath per the mockup.
- [ ] **Step 3:** Watermark: small private composable in OwnTVShell.kt — play-glyph + "OwnTV" wordmark row at 13% alpha, `Alignment.BottomEnd` inset ~2.6%, non-focusable, rendered above content and below dialogs/player; hidden when `playerMode != NONE`. Reuse the existing logo vector asset (grep the drawable the old Sidebar `AppLogo` used); if `R.drawable.owntv_wordmark` fits the mockup better use it (this un-orphans it — note in report).
- [ ] **Step 4:** Delete `Sidebar.kt`; delete `TopBar.kt` if fully unreferenced now (verify `StaticGlassChip`/chip helpers have no other consumers — grep; keep whatever is still used, e.g. if the playlist picker dialog reuses something). Remove dead imports in OwnTVShell.
- [ ] **Step 5: Gate + commit.** `git add <exact files> && git commit -m "Shell: floating rail + transparent header + watermark"`

### Task 5: Unified BACK policy

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/shell/OwnTVShell.kt` (the shell-level BackHandler / exit flow)
- Modify: only if a screen fights the floor — record each such edit (expected: none to few; page-local overlay BACK handling stays untouched)

- [ ] **Step 1:** Read the current exit flow (`showExit`, `onExitApp`, any BackHandlers in OwnTVShell) and how browse screens handle BACK today (grep `BackHandler` under features/shell, features/live, features/movies, features/series, features/home — READ each to know the unwind chain).
- [ ] **Step 2:** Implement the floor in the shell: a shell-level BackHandler that fires only when no child handler consumed BACK: state machine — content focused → activate rail (`forceActive = true` + focus the rail via `sidebarFocus`); rail active → `showExit = true`. Rail activation clears when focus leaves the rail or a section is selected. Page-local handlers (overlays, drills, player) keep priority by Compose's LIFO BackHandler order — verify the shell's handler is registered FIRST (outermost) so children win.
- [ ] **Step 3:** Manual trace per section (code-level): Live (rail→list→preview + overlays), Movies/Series (category→list→details), Home, Search (IME open!), Guide, Settings (nested screens), fullscreen player (owns BACK entirely while open). Record the chain for each in the report; fix only genuine conflicts.
- [ ] **Step 4: Gate + commit.** `git add <exact files> && git commit -m "Shell: BACK always leads through the rail to exit"`

### Task 6: Verification sweep + finish

**Files:** none (fix-forward only).

- [ ] **Step 1: Suite.** `./gradlew testStandardDebugUnitTest lintStandardDebug` → green; `verify-ci` i18n check → no new unclassified.
- [ ] **Step 2: On-device (controller), emulator-5554 Audit profile.** Screenshot + judge: (a) LEFT idle — floating icons, no panel, selected accent + bar, content laid out with start inset; (b) rail focused → expands, translucent panel, labels, content dims; (c) Settings → switch railPosition → TOP: rail below header, content top inset correct, focus UP from content enters rail, LEFT/RIGHT moves within, DOWN returns; (d) header: title left, search pill center (opens Search), time+weather right, legible over the brightest available content — if the title washes out, apply the pre-authorized title-only scrim fallback; (e) watermark bottom-right at rest, absent in fullscreen player; (f) BACK chain: from deep Live state unwind → page top → rail activates (expanded) → exit dialog; repeat spot-checks in Movies and Settings; (g) previews still compile (static check).
- [ ] **Step 3: Fix findings, re-verify, commit each fix.** When clean: final whole-branch review (most capable model), ONE fix wave if findings, then `superpowers:finishing-a-development-branch`.

## Self-Review

1. **Spec coverage:** §1→Tasks 2+4 (+1 for the setting), §2→Tasks 3+4 (chip removals in Task 4's TopBar deletion), §3→Task 4 Step 3, §4→Task 5, §5 previews→Tasks 2-3, constraints→Global Constraints, risks→Task 5 Step 3 + Task 6 (c)/(d). No gaps.
2. **Placeholder scan:** clean — signatures concrete; verbatim-carry-over items name their sources; fallbacks bounded with report-recording.
3. **Type consistency:** `RailPosition` (Task 1) consumed by Tasks 2/4 with the same name; `FloatingRail`/`ShellHeader` signatures identical between Produces and Consumes; `forceActive`/`onActiveChange` used consistently in Tasks 2/4/5.
