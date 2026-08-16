# Shell Refinements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the seven on-device refinements to the floating shell: LEFT edge-drawer expansion with start-justified rows, tighter TOP gap, accent+ring combined nav state, select→content focus, header weather/time separator, Material-flat nav icons.

**Architecture:** Contained edits to `FloatingRail.kt`, `NavLadder.kt` (+ CategoryRail parity), `ShellHeader.kt`, `OwnTVShell.kt`. Icons come from the Compose Material icons library (`Icons.Rounded.*` filled / `Icons.Outlined.*`) as tinted ImageVectors — no hand-drawn assets, R8 strips the unused set. (Deviation from the spec's "res/drawable vectors" mechanics, same intent: canonical Material flat glyphs, outlined-inactive/filled-active; record it.)

**Tech Stack:** Kotlin, Jetpack Compose for TV, `androidx.compose.material:material-icons-extended` (new dep via version catalog).

## Global Constraints

- Accent = selected; white ring = focus; combined state shows BOTH (accent content + white ring + accent bar). Never accent-as-focus alone.
- Everything else in the shell unchanged: BACK policy/floor, focus layers, pill gate, watermark, overlay-not-reflow insets. Previews updated where visuals change.
- i18n: no new strings; `verify-ci` no new unclassified attributable to touched files (classify preview literals `technical` if flagged).
- Git hygiene: stage edited files by explicit path only; NEVER `git commit -am`/`git add -A` (user's uncommitted gradle files in tree).
- Gates per task: `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` (0 errors); full suite + on-device sweep at the end.
- Branch: `shell-refinements` off `main`.

---

### Task 1: Material icons dependency + rail icon swap

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts` (add `androidx.compose.material:material-icons-extended` — verify the artifact coordinates against the existing compose BOM/version management in the catalog; if the repo pins compose artifacts individually, match that pattern)
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/FloatingRail.kt`

**Interfaces — Produces (Task 3 relies on it):** a private `navIcon(section: MainSection, selected: Boolean): ImageVector` mapping in FloatingRail.kt:
```kotlin
HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
LIVE_TV -> LiveTv         MOVIES -> Movie          SERIES -> VideoLibrary
DOWNLOADS -> Download     EPG -> GridOn (or TableRows — pick the closer guide glyph, record)
SETTINGS -> Settings      // avatar slot keeps OwnTVAvatar (not an icon swap)
```
(each following the same `if (selected) Rounded else Outlined` pattern; verify exact icon names exist in the library — substitutions recorded.)

- [ ] **Step 1:** Add the dependency through the version catalog following the file's existing conventions; sync/compile.
- [ ] **Step 2:** In FloatingRail.kt replace the `NavDuotoneIcon(section, color, ...)` call inside `RailNavItem` with `Icon(imageVector = navIcon(section, selected), contentDescription = null, tint = <the existing ladder tint>, modifier = Modifier.size(24.dp))` — Compose `Icon` from the tv-material3 or foundation package already in use elsewhere (verify import; androidx.tv.material3.Icon exists). Size 24dp per M3; keep the surrounding paddings.
- [ ] **Step 3:** Grep `NavDuotoneIcon` consumers: if FloatingRail was the sole remaining consumer, delete the composable (and its glyph data) from its home file; if others use it, leave it, record.
- [ ] **Step 4: Gate + commit.** `git add <exact files> && git commit -m "Rail: Material flat icons, filled when selected"`

### Task 2: NavLadder combined state + CategoryRail parity

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/ui/components/NavLadder.kt`
- Modify (only if needed for parity rendering): `app/src/main/java/tv/own/owntv/features/shell/components/CategoryRail.kt`

- [ ] **Step 1:** In `rememberNavLadderColors`, change the `activeSelected` (selected && focused) arm: container → what the focused-unselected arm uses today for fill semantics (i.e. NO primaryContainer fill; keep `colors.card`-style focus fill or Transparent to match the rail's overrides — read both consumers first and pick the value that renders identically in both; record), content/icon → `colors.accent`, and `focusBorder` → return the white ring for selected+focused too (today it's null when selected). `showAccentBar` stays = selected.
- [ ] **Step 2:** FloatingRail overrides some ladder values locally (it did for the old peak state) — reconcile: the rail's RailNavItem must render the combined state as accent content + white ring + accent bar with NO special fill. Remove any now-dead local peak-state overrides.
- [ ] **Step 3:** CategoryRail: read how it consumes the ladder; ensure the combined state renders the same (accent text + white ring + bar). Adjust only if its local overrides fight the new arm; record.
- [ ] **Step 4: Gate + commit.** `git add <exact files> && git commit -m "Nav ladder: selected+focused shows accent with focus ring"`

### Task 3: Rail geometry — LEFT edge drawer + start justification + TOP gap

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/FloatingRail.kt`
- Modify: `app/src/main/java/tv/own/owntv/features/shell/OwnTVShell.kt` (rail placement/padding + TOP gap)

- [ ] **Step 1: LEFT active drawer.** When `position == LEFT && active`: the rail container becomes full-height (`fillMaxHeight()`), pinned to the start edge with ZERO outer margin (the shell currently applies a 30dp start inset + vertical centering — in active state those must drop to 0/fill; animate between geometries on the existing tween — `animateDpAsState` on the inset/corner values driven by `active` is the simple route), corner radius 0 (animate 28dp→0), panel/border/blur unchanged. IDLE unchanged (floating, centered, rounded, 30dp inset). The shell's content inset stays frozen at idle width (overlay behavior preserved — the drawer overlays).
- [ ] **Step 2: Start justification.** In the LEFT expanded drawer: rows left-justified — `Column(horizontalAlignment = Alignment.Start)` + each RailNavItem row `Arrangement.Start` with a consistent start padding (~20dp inside the drawer); avatar row too. Idle (icons only) stays visually centered in its narrow column (unchanged geometry). TOP mode row layout unchanged.
- [ ] **Step 3: TOP gap.** In OwnTVShell's TOP placement, halve the header→rail gap (`GapMedium` → `GapMedium / 2` or a new `Dimens.RailTopGap = 8.dp` — pick the Dimens-constant route, matching how the file handles other gaps) and reduce the rail↔content gap symmetrically per the existing arithmetic. Verify the expanded TOP rail still clears the header pill.
- [ ] **Step 4:** Update FloatingRail previews (LEFT forceActive should show the squared full-height drawer).
- [ ] **Step 5: Gate + commit.** `git add <exact files> && git commit -m "Rail: edge drawer when active on left, tighter top gap"`

### Task 4: Select→content focus + header separator

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/shell/OwnTVShell.kt`
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/ShellHeader.kt`

- [ ] **Step 1: Select→content.** Read how `restoreFocus` drives section-entry focus today (set on player exit; browse screens consume it to focus their content). On rail `onSelect`: after switching sections, trigger the same content-entry focus path (set `restoreFocus = true` or the equivalent per-section entry request). Fallback: if the section's content can't take focus (still composing/loading), focus must remain on the rail (verify no strand — the existing runCatching-style guard pattern). Trace each section's entry target; record per-section behavior.
- [ ] **Step 2: Header separator.** In ShellHeader's end zone: between the weather block and the clock, add `Box(Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.25f)))` (match the header's soft-shadowed aesthetic; only when `weatherInfo != null`). Update the with-weather preview.
- [ ] **Step 3: Gate + commit.** `git add <exact files> && git commit -m "Shell: select jumps to content; header weather/time separator"`

### Task 5: Verification sweep + finish

**Files:** none (fix-forward only).

- [ ] **Step 1: Suite.** `./gradlew testStandardDebugUnitTest lintStandardDebug` → green; verify-ci parity.
- [ ] **Step 2: On-device (controller), emulator first, then 10.10.8.96.** (a) LEFT idle floating unchanged; activate → full-height squared edge drawer, rows start-justified, flat icons (selected = filled + accent; cursor = ring; combined = accent+ring+bar); (b) select a section → focus lands in content, rail collapses; (c) TOP mode → tighter gap, rail clears header; (d) header separator between weather and time (and absent when weather off); (e) CategoryRail combined state visual parity in Live; (f) BACK chain unchanged; (g) previews compile.
- [ ] **Step 3: Fix findings, re-verify, commit each fix.** When clean: final whole-branch review (most capable model), ONE fix wave if findings, then `superpowers:finishing-a-development-branch`.

## Self-Review

1. **Spec coverage:** §1→T3S1, §2→T3S2, §3→T3S3, §4→T2, §5→T4S1, §6→T4S2, §7→T1 (library deviation recorded in Architecture). No gaps.
2. **Placeholder scan:** clean — icon mapping enumerated with verify-and-record substitutions; geometry steps name exact mechanics and animation route; separator code given.
3. **Type consistency:** `navIcon(section, selected)` defined and consumed within FloatingRail only; no cross-task interfaces beyond files.
