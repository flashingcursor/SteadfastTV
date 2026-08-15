# Floating Shell Redesign — Design Spec

**Date:** 2026-08-15
**Status:** approved design (mockup approved), pending implementation plan
**Mockup:** https://claude.ai/code/artifact/f4440a9f-aa74-4dd2-946e-62d68a4a8e5b (interactive; the visual contract for this spec)
**Goal:** Replace the fixed sidebar + chip-heavy top bar with a sleek minimal floating shell: a floating rail (left or top, user-configurable), a fully transparent three-zone header, a translucent brand watermark, and a unified BACK policy that always leads through the menu to an exit confirmation.

## 1. Floating rail (replaces `Sidebar`)

New `features/shell/components/FloatingRail.kt`; `Sidebar.kt` is deleted after adoption. `MainSection`, `visibleSections` logic, the counts hook, and the NavLadder selected/focused semantics all carry over unchanged.

**Structure — three areas** (in orientation order top→bottom or start→end):
1. **Profile** — the avatar (click = avatar picker, long-press = switch profile, as today). The brand logo LEAVES the rail (it becomes the watermark, §3).
2. **Destinations** — `MainSection.browseOrder` filtered by `visibleSections`, same fixed order.
3. **Settings** — pinned last, separated (thin translucent separator between areas).

**Orientation** — new persisted setting `railPosition` (`LEFT` default | `TOP`), exposed in Settings alongside the existing Nav-menu customization:
- **LEFT:** floating vertical pill-stack, vertically centered on the left edge (detached from screen edges — inset ~1.6% of width), scrolls internally at high UI zoom exactly like today.
- **TOP:** floating horizontal pill, horizontally centered, docked BELOW the header (§2); the three areas run start→end with vertical separators. Content top padding adjusts.

**States:**
- **Idle** (focus elsewhere): icons only, NO panel, NO fill — fully floating over the page. The selected section keeps the accent icon tint + the small accent bar (NavLadder's selected-at-rest marker, rotated to horizontal-under-icon in TOP mode). Idle icons are `onSurfaceVariant`-muted.
- **Active** (D-pad focus enters the rail): labels animate out beside every icon (single expansion animation, `ownTvTween`), a translucent panel fades in behind the rail (rounded 1.6em-equivalent; `GlassSurface.SIDEBAR` blur+frost when glass is enabled, plain `surfaceContainer`-family translucent fill when glass is off), and the page content behind dims (scrim ~45% like the mockup's `dimmable`). Focus cursor = white ring (existing sole-focus-signal rule); selected stays accent.
- Focus-entry redirect (enter rail → land on selected section's item) and the Search/hidden-section fallbacks carry over verbatim from `Sidebar`.

## 2. Header (reworks `TopBar`)

Fully transparent — no bar, no glass strip, no chip capsules. Three zones:
- **Left:** the current page title (`MainSection.labelRes` resolved; e.g. "Live TV") in `titleLarge`-class weight, soft text shadow for legibility over art. Replaces the section chip.
- **Center:** the search input — a single translucent pill (frosted when glass on, plain translucent when off) with search icon + hint text; focusing/clicking it opens the Search section exactly as the current Search pill does. This is the ONLY capsule in the header.
- **Right:** time + weather (when enabled) as plain floating text with soft shadow — no capsules. Weather glyph + temperature, then clock.

**Chips that leave the header (decisions, called out for review):**
- **Continue/Last-channel chip** → removed from the header; the Home screen's Keep Watching row and the startup-mode setting already cover its job.
- **Playlist chip** → removed; playlist identity/switching lives in Settings → Playlists (and the profile long-press path). No function is lost — only the always-on chrome.

The player HUD's own internal `TopBar` (PlayerHud.kt) is out of scope — this spec touches only the browse shell.

## 3. Watermark

Bottom-right corner: the play-triangle glyph + "OwnTV" wordmark at ~12-13% opacity (the mockup's look), non-focusable, non-interactive, drawn above page content but below dialogs/overlays; hidden whenever the fullscreen player or mini-player-over-that-corner is active. This resurrects `R.drawable.owntv_wordmark`-style branding (the orphaned drawable may be reused or replaced by the vector used in onboarding — implementer picks whichever asset matches the mockup).

## 4. Unified BACK policy

BACK always walks: deep page state → page top → **rail activates** (focus jumps to the rail, which expands per §1 Active) → exit confirmation (existing dialog).
- Page-local BACK handling (overlays, category drills, players) keeps unwinding as today — the change is the FLOOR: when a browse page has nothing left to unwind, BACK focuses the rail instead of whatever ad-hoc behavior exists per screen.
- When the rail is already active and BACK is pressed → exit confirmation (existing `Exit OwnTV?` dialog unchanged; it already satisfies the single-PRIMARY rule).
- Search, Settings, and single-pane sections follow the same policy.

## 5. Previews

The new `FloatingRail` and header get seeded previews via the shipped harness: `@TvPreview` for rail LEFT-idle, LEFT-active, TOP-idle, TOP-active (parameter-driven `active` state so previews CAN show the expanded panel — the active state must be hoistable as a parameter, with the default driven by real focus), and the header with fake title/weather/time.

## Constraints & invariants

- **Design language:** accent = focus/active/selected only; white ring sole focus cursor; the rail's active panel is the ONE translucent surface in the idle shell; single default action rule holds in any dialogs touched.
- **Behavior preserved:** section switching, visibleSections/DYNAMIC mode, counts hook, avatar/profile actions, search entry, weather/clock data sources, exit dialog, mini-player, Layer-2 CategoryRail and all page content — untouched except where §2/§4 say otherwise.
- **RTL:** rail LEFT = start edge (flips to right in RTL); header zones are start/center/end; TOP-mode area order start→end.
- **i18n:** no new user-facing strings except the Settings entry for rail position (new string resources through the normal i18n flow — this project MAY add strings, unlike the color phases; `PluralsCandidate` fatal, translations seeded for all locales per the i18n workflow).
- **Low-spec:** blur only via the existing glass system (which already gates by device class); the translucent fallback must not use `RenderEffect`.
- **Git hygiene:** explicit-path staging only. Gates: compile+lint per task, full suite + on-device sweep (emulator + a Fire TV) at the end.
- Branch: `floating-shell` off `main`.

## Risks

- **Focus behavior under TOP mode** is new ground (vertical D-pad from content into a horizontal rail): the focus-entry redirect must handle UP-into-rail; LEFT/RIGHT moves within the rail; DOWN returns to content. Needs explicit on-device verification.
- **BACK-policy regressions:** each browse screen currently owns quirks of its BACK handling; the floor change must not swallow overlay dismissals. Sweep every section on-device.
- **Content padding re-flow** when railPosition = TOP (content starts lower) touches every browse screen's top inset — implement once in the shell container, not per-screen.
- **Header legibility over bright art** without any scrim: the text-shadow treatment must be verified over bright content (the phase-4 scrim lesson); if titles wash out, a minimal per-text `hudTextScrim`-style treatment is the pre-authorized fallback for the title only.
