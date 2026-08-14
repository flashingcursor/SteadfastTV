# Compose Previews — Design Spec

**Date:** 2026-08-14
**Status:** approved design, pending implementation plan
**Goal:** Make Android Studio Compose previews work: a theme-correct preview harness, removal of the one broken `@Preview`, and seeded previews for the shell nav surfaces and shared `ui/components/` — leaf components with plain-data params only.

## Context

Today the app has exactly one `@Preview` (on `OwnTVShell`, which can never render: ~15 required params + Koin injection inside) and no harness. The tooling dependencies are already wired (`androidx-compose-ui-tooling-preview` as `implementation`, `ui-tooling` as `debugImplementation`) — nothing uses them. This pass also directly serves the upcoming nav-rail rework (pinned): the rail's pieces become previewable before that redesign starts.

## Scope

**In:**

1. **Harness — new `app/src/main/java/tv/own/owntv/ui/preview/OwnTVPreview.kt`:**
   - `@Composable fun OwnTVPreview(light: Boolean = false, content: @Composable () -> Unit)` — wraps content in the real `OwnTVTheme(themeMode = if (light) ThemeMode.LIGHT else ThemeMode.DARK, accent = AccentColor.TEAL, systemInDarkTheme = !light)` and a background `Box` painted `OwnTVTheme.colors.background`. Defaults of the other theme params (customAccent, animationLevel) are used as-is. Glass/animation locals keep their declared defaults.
   - `@TvPreview` — multipreview annotation class carrying `@Preview(device = "spec:width=1920dp,height=1080dp,dpi=213", showBackground = true, backgroundColor = 0xFF0E1214)` for full-screen/tall surfaces.
   - `@TvComponentPreview` — same background/showBackground but NO device spec, for small components that should render hugged.
   - The `backgroundColor` literal matches the dark theme's background token; verify the actual hex from `OwnTVColors.kt` in situ and use that value.
2. **Fix:** delete the `@Preview` annotation on `OwnTVShell` (annotation only — the composable is untouched).
3. **Seeded previews** — private `@Composable` functions annotated `@TvComponentPreview` (or `@TvPreview` where full height matters), co-located at the BOTTOM of each component's own file, named `<Component>Preview`, using plain fake data:
   - `Sidebar.kt`: (a) a `NavItem` ladder-states column (idle / focused / selected / selected+focused — focus states shown by rendering the visual states the ladder produces, see Constraints); (b) full `Sidebar` with all sections visible, fake profile ("Living Room"), zero counts and one non-zero count.
   - `CategoryRail.kt`: pill-mode rail with a fake `RailCategory` list including the Favorites/History icon rows and a plain folder.
   - `TopBar.kt`: the static chips (SectionChip, ClockChip with a fixed time, WeatherChip with a fixed `WeatherInfo`, PlaylistChip non-interactive branch).
   - `ui/components/`: `OwnTVButton` (PRIMARY / SECONDARY / compact / disabled in one row), `MediaListRow` (plain / dimmed / favorite), `MediaContextMenu` (entries + close), `CategoryHeader`, `StepperDialog` (with Reset and `onReset = null`), `OwnTVAvatar` (avatar id sampler).
   - Exact composable signatures are read in situ; where a component requires focus/interaction plumbing (FocusRequester etc.) the preview passes inert defaults. Components that require a ViewModel, Koin, or live state flows are OUT of scope.

**Out:** screens with ViewModels (Home, browse screens, Settings, player HUD); screenshot-testing infrastructure; any change to component behavior, layout, or public signatures; fake-data builder modules; moving previews to a separate source set.

## Constraints & invariants

- **Additive only:** no component's body, signature, or layout changes. The only deletion is `OwnTVShell`'s dead `@Preview` line.
- **Focus states can't be forced in a static preview** — previews may render state variants only where the component exposes them as parameters (e.g. `selected`); a truly focus-driven visual (ladder cursor state) is shown by previewing `rememberNavLadderColors`-consuming surfaces in their reachable states, not by faking focus. Record per-site what was shown.
- **i18n:** zero `res/values*` changes. Preview fake data may be raw strings (never shipped, not res); reuse `stringResource` where an id is already at hand.
- **Git hygiene:** stage edited files by explicit path only; NEVER `git commit -am`/`git add -A` (user's uncommitted gradle files in the tree).
- **Gates:** `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` per task (0 errors); full unit suite at the end. Render verification is manual in Android Studio (no CLI renderer): acceptance = previews compile + user spot-opens Sidebar.kt / OwnTVButton.kt.
- Branch: `compose-previews` off `main`.

## Risks

- **tv-material3 vs preview renderer:** TV Material components generally render in previews; if a specific component fails to render (Studio limitation), keep the preview but record the limitation — do not refactor the component to appease the renderer.
- **`@Preview` on main source set** is standard and already how the deps are declared; previews ship as unreferenced code stripped by R8 in release — no size concern for debug-only workflows.
- **Multipreview + lint:** `PreviewAnnotationInFunctionWithParameters` and friends gate mistakes; the plan's lint gate catches them.
