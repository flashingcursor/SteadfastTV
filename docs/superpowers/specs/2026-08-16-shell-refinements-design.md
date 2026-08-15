# Floating Shell Refinements — Design Spec

**Date:** 2026-08-16
**Status:** approved design (user's on-device feedback IS the design), pending implementation plan
**Parent:** the merged floating-shell redesign (`docs/superpowers/specs/2026-08-15-floating-shell-design.md`); feedback from real-hardware testing on 10.10.8.96.

## Changes

1. **LEFT expanded = edge drawer.** When active, the rail becomes a full-height panel hugging the start edge: zero outer margin on start/top/bottom, zero corner radius (a squared drawer), translucent panel + blur/border treatment otherwise unchanged. IDLE keeps today's floating, vertically-centered, detached icon column (user-confirmed). The expand/collapse animates between the two geometries on the existing tween.
2. **LEFT expanded content = start-justified.** Rows (icon + label) align to a common start edge with consistent start padding — no centering. The three areas (avatar / destinations / Settings) keep their separators; avatar row also start-aligned.
3. **TOP mode gap.** Reduce the header→rail spacing so the rail sits close under the header (target: halve the current `GapMedium`; exact value tuned on device — the rail must still clear the header text/pill).
4. **Selected+focused state.** Replace the `primaryContainer`-fill peak treatment: when an item is BOTH the active section and the focus cursor, show the accent content color (icon+label) AND the white focus ring together. Ladder becomes: idle = muted; focused-unselected = white content + white ring; selected-unfocused = accent content + accent bar; selected+focused = accent content + white ring (+ accent bar). `NavLadder` is shared with `CategoryRail` — apply the same combined state there so the two nav surfaces stay identical (per its own #47 contract), and verify CategoryRail visually.
5. **Select → content.** Choosing a section moves focus immediately into the new page's content (rail collapses). Implementation: after `onSelect`, the shell requests focus into the content container once the new section composes (reuse the existing `restoreFocus`/section-entry focus machinery; each browse screen already has an entry-focus target). BACK-activation and D-pad entry behavior unchanged.
6. **Header separator.** Thin vertical separator (1dp, `onSurface` at low alpha, ~16dp tall) between the weather block and the clock in the header's end zone; only rendered when weather is visible.
7. **Material Symbols nav icons.** Replace the duotone nav glyphs with Material Symbols (Rounded family) imported as vector drawables, following m3.material.io/styles/icons/applying-icons:
   - 24dp base size, weight 400, optical size 24.
   - **Outlined variant for inactive items; filled variant for the selected item** (the canonical M3 nav-state practice — fill signals selection alongside the accent color).
   - Icons: home, live-tv, movie, series (subscriptions/video-library), download, guide (table/grid), settings, plus the avatar/person used in the rail's profile slot.
   - One family app-wide *for the rail*; the rest of the app's `OwnTVIcon` set is untouched in this pass (a follow-up may unify).
   - Assets land as `res/drawable/ic_nav_*.xml` vectors (both variants per glyph), tinted via the ladder colors (no hardcoded fills).

## Constraints & invariants

- Everything else in the shell (BACK policy, focus layers, pill gate, watermark, overlay-not-reflow, insets, previews) unchanged; previews updated where signatures/visuals change (rail previews should show the drawer + new icons).
- Accent discipline: accent = selected; white ring = focus — the combined state shows both simultaneously, never accent-as-focus alone.
- i18n: no new strings. `verify-ci`: no new unclassified attributable to touched files.
- Drawables are res additions (not strings) — allowed; `git status` res gate applies to string files only this pass.
- Git hygiene, gates, branch flow as always. Branch: `shell-refinements` off `main`.

## Risks

- **Drawer geometry vs mini-player/dialogs:** full-height drawer must stay below dialogs and the mini-player z-order; verify.
- **Select→content focus** rides on per-screen entry targets; a section whose content isn't focusable yet (loading) must not strand focus — fall back to keeping rail focus if the content request fails (record per-section behavior on device).
- **NavLadder change touches CategoryRail** — visual check of the folder rail in Live/Movies/Series required.
