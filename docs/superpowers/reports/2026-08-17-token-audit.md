# Design-Token Audit

**Date:** 2026-08-17
**Status:** complete — report only, no production code changed
**Spec:** `docs/superpowers/specs/2026-08-17-token-audit-design.md`
**Plan:** `docs/superpowers/plans/2026-08-17-token-audit.md`
**Related prior work:** `docs/superpowers/reports/2026-08-12-design-compliance-audit.md` (surface-level scorecard audit); the 2026-08-17 corner-radius consolidation (referenced throughout as the sizing precedent for remediation sweeps).

## 1. Introduction

### Scope

`app/src/main/java/tv/own/owntv/{ui,features,player}/**/*.kt` — 189 files. `core/` and `di/`
carry no design values and were excluded per the spec. Main source set only; no tests, no
resources.

Five families were audited, each against `ui/theme/Dimens.kt` (34 tokens, 27 distinct dp
values), `ui/theme/Animations.kt`, `ui/theme/Type.kt`, `ui/theme/Glass.kt`,
`ui/theme/OwnTVColors.kt`, and the achromatic color / corner-radius scales established by
earlier consolidations:

1. **Spacing & padding** — raw `.dp` literals in `padding(...)`/`Arrangement.spacedBy(...)`/
   `offset(...)`/`PaddingValues(...)`/`Spacer(...).width|height(...)` vs. the `Gap*` scale.
2. **Animation timing** — `ownTvTween(N)`/`tween(N)` duration literals, `delay(N)` gating a
   `requestFocus()` call, spring specs at call sites.
3. **Alphas & translucency** — `.copy(alpha = N)`, `Color.White/Black.copy(...)` rims/scrims/
   fills, `Modifier.alpha(N)`, literal alpha floats.
4. **Sizes, elevations, text** — `Modifier.size/height/width(N.dp)`, `shadow(elevation=N.dp)`,
   `.border(N.dp, ...)`, named `Dp` arguments to shared composables (`dialogPanel(...)`), `sp`
   literals outside the typography scale.
5. **Residual re-checks** — `Color(0x........)` literals vs. the achromatic dark palette /
   sanctioned pictorial sets, and `RoundedCornerShape(...)`/`CornerRadius(...)` literals vs.
   the 2026-08-17 corner scale.

### Method

Six purpose-built Python extraction scripts (regex over raw source text, not AST — see
"Reproducibility" below) produced one TSV inventory per family: `family, value, file, line,
snippet`. Each script also ran a **broad companion grep** (an intentionally wider, unfiltered
pattern) so that anything the narrow, context-scoped pattern missed would still surface as a
`residual-unexplained` count rather than silently vanish — see §7.

Every row was then classified into exactly one of four buckets, applied per-population (the
cluster is judged, not the individual site), per `rubric.md`:

- **`MIGRATE-EXACT`** — the literal equals an existing `Dimens`/theme token's value exactly;
  mechanical swap, zero visual change.
- **`MIGRATE-NEW`** — a repeated cluster with one semantic role that isn't covered by an
  existing token. Two flavors: **new token** (mint a named constant) or **snap-to-existing**
  (reuse a token and accept a small, stated visual delta).
- **`INTENTIONAL`** — geometric/derived or component-specific one-offs that must stay literal;
  every population/site carries a one-line reason.
- **`SANCTIONED`** — the settled design-language exceptions (SubtitleOverlay hand-tuned
  metrics, Player HUD plain-black scrims, pictorial constants, FloatingRail mockup geometry,
  hairline 1–3dp roundings, percent pills, progress-bar half-heights, aspect-ratio-derived
  sizes) — re-affirmed, not re-litigated.

Before any population was classified, every value in `Dimens.kt` (and the two pre-existing
alpha constants) was swept against the family's own histogram for exact numeric collisions,
governed by a **usage-shape-congruence rule**: an exact (Δ0) value match only counts as
`MIGRATE-EXACT` if the existing token's own call sites are the *same structural kind* of value
as the candidate population's dominant pattern (a gap-shaped population reuses a gap-shaped
token; a padding-shaped population reuses a padding-shaped token; cross-family numeric
coincidences — e.g. a spacing literal that happens to equal a corner-radius token — are never
a match). This rule is stated in full in `rubric.md` and is the reason, for example, that the
14dp spacing population migrates exactly to `Dimens.HeroGap` (both gap-shaped) while the 20dp
population does not migrate to the numerically-identical `Dimens.HomeRowPaddingH` (that
token's own role is single-screen layout arithmetic, a structural mismatch).

### Reproducibility

The extraction scripts (`extract.py`) and every intermediate TSV live in the session
workspace (`.superpowers/sdd/2026-08-17-token-audit/`), **not** in the repository — per the
spec, the only repo artifact from this project is this report. Re-running the audit means
re-deriving the six regex extractors from the family definitions in §Scope above and the
per-family heuristics documented inline in each section below (each section states its exact
trigger patterns, exclusions, and known limitations). All counts in this report are extracted
counts, not estimates, and every family's classification reconciles exactly to its inventory's
row count (shown in each section's "Reconciliation" line) — this was verified programmatically
against the TSVs at every classification revision, not by manual tally.

### Headline totals

| family | rows | `MIGRATE-EXACT` | `MIGRATE-NEW` | `INTENTIONAL` | `SANCTIONED` |
|---|---|---|---|---|---|
| 1. Spacing & padding | 1309 | 546 | 728 (370 new-token + 358 snap) | 33 | 2 |
| 2. Animation timing | 114 | 2 | 69 | 43 | 0 |
| 3. Alphas & translucency | 183 | 0 | 76 (43 new-token + 33 snap) | 91 | 16 |
| 4. Sizes/elevation/text | 473 | 10 | 177 (146 new-token + 31 snap) | 284 | 2 |
| 5a. Color residuals | 137 | — | — | — | 122 sanctioned / 15 findings |
| 5b. Corner residuals | 57 | — | — | — | 55 sanctioned / 2 findings |
| **Total literals audited** | **2273** | | | | |

(1309 + 114 + 183 + 473 + 137 + 57 = 2273. Every per-family split above reconciles exactly to
its own inventory row count — see the Reconciliation line closing each section.)

---

## 2. Family 1 — Spacing & Padding

Source: 1309 rows, 35 distinct `.dp` values, extracted from `padding(...)` /
`Arrangement.spacedBy(...)` / `offset(...)` / `PaddingValues(...)` /
`Spacer(...).width|height(N.dp)` call sites. Lines already using `Dimens.` are excluded by
construction (the extractor matches only literal `N.dp` tokens, which can't overlap with a
`Dimens.X` identifier).

### Histogram (all 35 distinct values)

| value (dp) | rows | value (dp) | rows | value (dp) | rows |
|---|---|---|---|---|---|
| 12 | 202 | 40 | 22 | 30 | 1 |
| 8 | 171 | 22 | 20 | 36 | 1 |
| 16 | 143 | 9 | 14 | 44 | 1 |
| 10 | 141 | 3 | 12 | 64 | 1 |
| 6 | 120 | 5 | 9 | 92 | 1 |
| 14 | 109 | 48 | 7 | 96 | 1 |
| 4 | 89 | 32 | 6 | 104 | 1 |
| 20 | 73 | 7 | 6 | 112 | 1 |
| 2 | 51 | 1 | 2 | 120 | 1 |
| 28 | 37 | 56 | 2 | 13 | 1 |
| 18 | 31 | 0 | 1 | 17 | 1 |
| 24 | 29 | 11 | 1 | | |

**Total: 1309 rows.** Top 10 values (12, 8, 16, 10, 6, 14, 4, 20, 2, 28) account for 1172 rows
(89.5%).

### Classification summary

| bucket | rows | % | notes |
|---|---|---|---|
| `MIGRATE-EXACT` | 546 | 41.7% | values 4, 8, 16 (minus 1 sanctioned), 24, 32, 14 — 14dp migrates to `Dimens.HeroGap` (Δ0), found on the collision sweep (below) |
| `MIGRATE-NEW` — new token | 370 | 28.3% | values 2, 12, 20 (new `Gap*` rungs) + the paired 40/28 `DetailPanel*` pattern |
| `MIGRATE-NEW` — snap to existing | 358 | 27.3% | values 1, 3, 5, 6, 7, 9, 10, 18, 22, and part of 28 |
| **`MIGRATE-NEW` subtotal** | **728** | **55.6%** | |
| `INTENTIONAL` | 33 | 2.5% | one-off/derived/component-specific values, one-line reason each |
| `SANCTIONED` | 2 | 0.2% | both `SubtitleOverlay.kt:103` (hand-tuned metrics) |
| **Total** | **1309** | **100%** | |

### Existing-token collision sweep

Every one of `Dimens.kt`'s 34 tokens (27 distinct dp values, several shared across tokens) was
checked against the 35 spacing-population values for exact numeric matches, then filtered
through the usage-shape-congruence rule:

| `Dimens` token | value | own usage shape | population match? | verdict |
|---|---|---|---|---|
| `ScreenPaddingH` | 32 | `padding(horizontal=…,vertical=…)`, 7+ unrelated top-level screens, no arithmetic entanglement | value 32 (6 rows), all `padding(...)` container padding | **MIGRATE-EXACT** |
| `ScreenPaddingV` | 24 | paired with `ScreenPaddingH`, same shape | already the `GapLarge` target (same number) | no separate action |
| `RailWidth` | 92 | `.width()` sizing (rail), size-family concern | value 92 (1 row) — a conditional HUD `padding(top=...)` offset | **rejected** — cross-family |
| `RailPillSize` | 56 | `.size(...)` component dimension | value 56 (2 rows) — bottom safe-area padding offsets | **rejected** — cross-family |
| `IconTileCorner`, `CornerXSmall/Small/Medium/Large`, `CardCorner`, `PanelCorner` | 4/6/8/12/6/8/12/10/11 | corner-family radii | values 4/6/8/12/10/11 all present in the spacing population | **rejected**, all — cross-family, numeric coincidences only |
| `RailTopGap` | 8 | header→rail/rail→content gap, `FloatingRail`/`OwnTVShell` TOP only | duplicate value of `GapSmall` | no separate action — `GapSmall` already correct |
| `PosterProgressHeight` | 4 | size-family progress-bar height | duplicate value of `GapTiny` | no separate action — cross-family |
| `HomeRowPaddingH` | 20 | `padding(horizontal=...)` **and** Home-screen row-width arithmetic (`screenWidthDp - SidebarWidthCollapsed - HomeRowPaddingH`), used only in `features/home/` | value 20 (73 rows), dominant shape `Spacer`/gap, not `padding`/arithmetic | **rejected** — shape mismatch despite Δ0; new `GapWide` proposed instead |
| `HeroGap` | 14 | `Arrangement.spacedBy(Dimens.HeroGap)` (primary use), `HomeScreen.kt`-only | value 14 (109 rows), dominant shape `Spacer`/`spacedBy` gap (74/109 rows) | **MIGRATE-EXACT** — shape matches despite Home-only/Hero-named origin; Δ0 beats any snap |
| `HeroCardCorner`, `HeroPosterCorner`, `HeroProgressHeight` | 12/7/3 | corner/size-family | values 12/7/3 present | **rejected** — cross-family |

**Net effect:** one previously-missed collision (`HeroGap`, 109 rows) moved from
`MIGRATE-NEW` (snap → `GapMedium`, Δ+2) to `MIGRATE-EXACT` (Δ0) after a full sweep. Every
other collision was either already correctly targeted (`Gap*`, `ScreenPaddingH/V`) or
correctly rejected as cross-family/shape-mismatched — the `HomeRowPaddingH`/20dp rejection
holds under the same rule.

### Proposed token additions (`ui/theme/Dimens.kt`)

| name | value | sites (direct + snapped) | rationale |
|---|---|---|---|
| `GapHairline` | `2.dp` | 51 + 2 snapped (@1) = 53 | Tight title/subtitle two-line rhythm (25 sites) and compact pill/badge vertical padding (8 sites, `LiveBadge` pattern). Below `GapTiny`; doubling to 4dp would visibly loosen dozens of tight rows. |
| `GapCompact` | `12.dp` | 202 + 141 snapped (@10) = 343 | Largest population in the family. Fills `GapSmall`(8)–`GapMedium`(16). |
| `GapWide` | `20.dp` | 73 + 30 snapped (@18) = 103 | Fills `GapMedium`(16)–`GapLarge`(24). Distinct from `HomeRowPaddingH` — zero of the 73 raw sites are in `HomeScreen.kt`. |
| `DetailPanelPaddingH` | `40.dp` | 22 | Paired with `DetailPanelPaddingV`. 100% of value 40's population: `.padding(horizontal = 40.dp, vertical = 28.dp)` outer container padding on 20 distinct settings/customize detail screens. |
| `DetailPanelPaddingV` | `28.dp` | 22 (of the 37-row value-28 population; remaining 15 are unrelated one-offs — 5 snap to `GapLarge`, 10 stay `INTENTIONAL`) | Vertical half of the same 22-site pattern. |

Five additions complete a clean 4dp-multiple ladder: **`GapHairline` 2 / `GapTiny` 4 /
`GapSmall` 8 / `GapCompact` 12 / `GapMedium` 16 / `GapWide` 20 / `GapLarge` 24**, plus the
standalone `DetailPanelPaddingH`/`V` pair.

### Snap-to-existing recommendations

| value (dp) | rows | target token | Δ (dp) | basis |
|---|---|---|---|---|
| 1 | 2 | `GapHairline` (2, new) | +1 | only neighbor |
| 3 | 12 | `GapTiny` (4) | +1 | closer neighbor |
| 5 | 9 | `GapTiny` (4) | −1 | closer neighbor |
| 6 | 119 (of 120; 1 sanctioned) | `GapSmall` (8) | +2 | tie → round up |
| 7 | 6 | `GapSmall` (8) | +1 | closer neighbor |
| 9 | 14 | `GapSmall` (8) | −1 | closer neighbor |
| 10 | 141 | `GapCompact` (12, new) | +2 | tie → round up |
| 18 | 30 (of 31; 1 responsive-pair) | `GapWide` (20, new) | +2 | tie → round up |
| 22 | 20 | `GapLarge` (24) | +2 | tie → round up |
| 28 (partial) | 5 (of 37) | `GapLarge` (24) | −4 | generic Spacer/dialog-padding rhythm sites only |

All deltas ≤4dp — sub-perceptual on a 10-foot TV UI, per the tie-break rule (round up)
applied uniformly.

### `INTENTIONAL` values — one-line reasons

| value (dp) | rows | reason |
|---|---|---|
| 0 | 1 | `RoundedPanel.kt:63` — `PaddingValues(0.dp)`, a no-op default sentinel. |
| 11 / 13 / 17 | 1 each | `LanguageSettingsScreen.kt:124,126` — one screen's hand-tuned locale-chip grid metrics. |
| 18 (1 of 31) | 1 | `GuideCore.kt:238` — `padding(if (compact) 18.dp else 28.dp)`, coupled responsive pair. |
| 28 (10 of 37) | 10 | Heterogeneous: the `GuideCore.kt:238` pair-half; `ProfileGate.kt:80` avatar-grid gutter; `RemoteBackupRestoreScreen.kt:76,162` wide-dialog margin; `MediaDetailsScreen.kt:137,175` detail padding; `PlayerHud.kt:525`(×2),`:621`,`:999` HUD placement. |
| 30 | 1 | `PlayerHud.kt:1316` — HUD badge bottom offset, hand-tuned to clear transport bar. |
| 32 | — | not INTENTIONAL — see `MIGRATE-EXACT` (`ScreenPaddingH`). |
| 36 | 1 | `ProfileGate.kt:78` — one-off `Spacer` rhythm; nearest token (`ScreenPaddingH`, Δ4) is a `padding` role, shape mismatch. |
| 44 | 1 | `SetupScaffold.kt:139` — same shape mismatch as 36dp, larger delta. |
| 48 | 7 | Mixed component-scoped: wide-dialog margins (2×`GapLarge`) vs. icon-clearance offsets. |
| 56 | 2 | `PlayerHud.kt:1545`, `InAppToast.kt:63` — bottom safe-area clearance, below token threshold. |
| 64 | 1 | `OpenSubtitlesAccountScreen.kt:255` — one-off dialog spacer. |
| 92/104/112/120 | 1 each | `PlayerHud.kt:525,629,513,621` — HUD control-cluster placement offsets, hand-tuned. |
| 96 | 1 | `MiniPlayer.kt:66` — icon-clearance offset, same character as the PlayerHud offsets, different component. |

**Total `INTENTIONAL`: 33 rows.**

### `SANCTIONED` values

| value (dp) | rows | reason |
|---|---|---|
| 16, 6 | 1 each | `SubtitleOverlay.kt:103` — `.padding(horizontal = 16.dp * sizeScale, vertical = 6.dp * sizeScale)`; the sanctioned SubtitleOverlay hand-tuned metrics exception. |

### Per-population site data

Per the workspace convention: populations ≥30 rows get per-file counts; populations <30 rows
get explicit `file:line`.

**≥30-row populations (per-file counts):**

- **value 12 — 202 rows, → `GapCompact` — 58 files.** Top: SettingsScreen.kt 20,
  VideoPlayerSettingsScreen.kt 16, PlayerHud.kt 11, SeriesScreen.kt 10, EpgScreen.kt 9,
  BackupScreen.kt 8, LanguageSettingsScreen.kt 8, MoviesScreen.kt 6,
  PanelWidthSettingsScreen.kt 6, SetupWizard.kt 6, UpdateDialog.kt 6, +48 files with 1–5 each.
- **value 8 — 171 rows, → `Dimens.GapSmall` — 54 files.** Top: SettingsScreen.kt 20,
  EpgSourcesScreen.kt 10, VideoPlayerSettingsScreen.kt 10, EpgScreen.kt 9,
  ManageSourcesScreen.kt 9, PlayerHud.kt 9, HomeScreen.kt 7, LiveScreen.kt 7, +46 files with
  1–5 each.
- **value 16 — 143 rows (142 → `Dimens.GapMedium` + 1 SANCTIONED) — 53 files.** Top:
  SettingsScreen.kt 15, AddSourceScreen.kt 8, VideoPlayerSettingsScreen.kt 8, HomeScreen.kt 7,
  PlayerHud.kt 7, CustomizeScreen.kt 6, OpenSubtitlesAccountScreen.kt 6, SeriesScreen.kt 6,
  +45 files with 1–5 each.
- **value 10 — 141 rows, → `GapCompact` (Δ+2) — 51 files.** Top: CustomizeScreen.kt 12,
  PlayerHud.kt 10, SettingsScreen.kt 10, EpgScreen.kt 7, ManageSourcesScreen.kt 7,
  VideoPlayerSettingsScreen.kt 7, LiveScreen.kt 5, SubtitleSearchScreen.kt 5, +43 files with
  1–4 each.
- **value 6 — 120 rows (119 → `GapSmall` Δ+2 + 1 SANCTIONED) — 43 files.** Top:
  SettingsScreen.kt 9, BulkRenameDialogs.kt 8, CustomizeScreen.kt 8, LiveScreen.kt 8,
  CustomizeItemsScreen.kt 7, HomeSettingsScreen.kt 6, MiniPlayer.kt 6, +36 files with 1–5 each.
- **value 14 — 109 rows, → `Dimens.HeroGap` (Δ0) — 45 files.** Top: AddSourceScreen.kt 9,
  SeriesScreen.kt 9, LiveScreen.kt 8, EpgSourcesScreen.kt 7, SettingsScreen.kt 6,
  AvatarPickerDialog.kt 4, OpenSubtitlesAccountScreen.kt 4, PlayerHud.kt 4,
  VideoPlayerSettingsScreen.kt 4, +36 files with 1–3 each.
- **value 4 — 89 rows, → `Dimens.GapTiny` — 43 files.** Top: PlayerHud.kt 10,
  VideoPlayerSettingsScreen.kt 8, SettingsScreen.kt 6, CustomizeScreen.kt 5,
  RemoteBackupRestoreScreen.kt 4, SeriesScreen.kt 4, +37 files with 1–3 each.
- **value 20 — 73 rows, → `GapWide` — 30 files.** Top: SettingsScreen.kt 12, BackupScreen.kt
  7, PlayerHud.kt 6, ManageSourcesScreen.kt 4, RemoteBackupRestoreScreen.kt 4, SetupWizard.kt
  3, UpdateDialog.kt 3, +23 files with 1–2 each.
- **value 2 — 51 rows, → `GapHairline` — 25 files.** Top: SettingsScreen.kt 8, PlayerHud.kt
  5, LiveScreen.kt 4, EpgSourcesScreen.kt 3, ManageSourcesScreen.kt 3,
  PanelWidthSettingsScreen.kt 3, +19 files with 1–2 each.
- **value 28 — 37 rows, split — 28 files.** 22 → `DetailPanelPaddingV`; 5 → `GapLarge` (Δ−4:
  `AddSourceScreen.kt:540`, `SetupWizard.kt:508,515`, `UpdateDialog.kt:77`,
  `PlayerHud.kt:819`); 10 → `INTENTIONAL` (see table above).
- **value 18 — 31 rows, split — 22 files.** 30 → `GapWide` (Δ+2); 1 → `INTENTIONAL`
  (`GuideCore.kt:238`, compact-branch half).

**<30-row populations (`file:line`):**

- **value 40 — 22 rows, → `DetailPanelPaddingH`:** `CustomizeItemsScreen.kt:168`,
  `CustomizeScreen.kt:146,222`, `BackupScreen.kt:145`, `ChNavSettingsScreen.kt:90`,
  `DeleteSubtitlesScreen.kt:69`, `DnsSettingsScreen.kt:123`, `EpgSourcesScreen.kt:163,381`,
  `HomeSettingsScreen.kt:74`, `LanguageSettingsScreen.kt:310`, `ManageProfilesScreen.kt:129`,
  `ManageSourcesScreen.kt:311`, `MetadataSettingsScreen.kt:169`,
  `MiniPlayerSettingsScreen.kt:78`, `NavMenuSettingsScreen.kt:78`, `NetworkSettingsScreen.kt:87`,
  `OpenSubtitlesAccountScreen.kt:155`, `PanelWidthSettingsScreen.kt:99`,
  `VideoPlayerSettingsScreen.kt:237`, `WeatherSettingsScreen.kt:75`, `SettingsScreen.kt:388`.
- **value 22 — 20 rows, → `GapLarge` (Δ+2):** `CustomizeItemsScreen.kt:405`,
  `CustomizeScreen.kt:634`, `ProfileComponents.kt:200`, `EpgSyncPrompt.kt:96,112`,
  `ManageSourcesScreen.kt:558,600`, `PanelWidthSettingsScreen.kt:223`, `SetupScaffold.kt:118`,
  `FloatingRail.kt:337`, `SettingsScreen.kt:1439,1479,1707,2003`,
  `AutoFrameRatePrompt.kt:110`, `PlayerHud.kt:1521`, `InAppToast.kt:71`, `ResumeDialog.kt:61`,
  `SetTmdbNameDialog.kt:91`, `TextInputDialog.kt:83`.
- **value 9 — 14 rows, → `GapSmall` (Δ−1):** `FloatingRail.kt:234,236,291,344`,
  `BulkRenameDialogs.kt:612,619`, `HomeGuideSlice.kt:353`(×2), `RemoteBackupRestoreScreen.kt:109,196`,
  `RemoteSetupScreen.kt:102`, `ShellHeader.kt:136`, `DownloadStatusStrip.kt:91`.
- **value 3 — 12 rows, → `GapTiny` (Δ+1):** `BulkRenameDialogs.kt:566`, `HomeScreen.kt:1183`,
  `LiveScreen.kt:797`, `ProfileComponents.kt:233`, `SeriesScreen.kt:1386`,
  `VideoPlayerSettingsScreen.kt:1345`, `AddSourceScreen.kt:708`, `PlayerHud.kt:915,1283,1316`,
  `CountBadge.kt:40`, `PosterCard.kt:94`.
- **value 5 — 9 rows, → `GapTiny` (Δ−1):** `BulkRenameDialogs.kt:570,612,619`,
  `HomeGuideSlice.kt:387`, `LiveScreen.kt:775,895`, `LiveEpgCard.kt:92`,
  `AudioNowPlayingBar.kt:280`, `StreamInfoOverlay.kt:68`.
- **value 48 — 7 rows, `INTENTIONAL`:** `ProfileGate.kt:81`, `DatabaseRecoveryScreen.kt:50`,
  `PanelWidthSettingsScreen.kt:312`, `RemoteBackupRestoreScreen.kt:76,162`,
  `SetupScaffold.kt:109`, `MiniPlayer.kt:66`.
- **value 7 — 6 rows, → `GapSmall` (Δ+1):** `BulkRenameDialogs.kt:560`, `HomeGuideSlice.kt:280`,
  `LiveScreen.kt:1138`, `SearchScreen.kt:471`, `SyncStatusPill.kt:129`,
  `DownloadStatusStrip.kt:92`.
- **value 32 — 6 rows, → `Dimens.ScreenPaddingH`:** `EpgScreen.kt:336`, `OwnTVShell.kt:539`,
  `AvatarPickerDialog.kt:73`, `ExitDialog.kt:64`, `IncompleteRestoreDialog.kt:69`,
  `ShellHeader.kt:89`.
- **value 1 — 2 rows, → `GapHairline` (Δ+1):** `HomeGuideSlice.kt:245`, `LiveEpgCard.kt:142`.
- **value 56 — 2 rows, `INTENTIONAL`:** `PlayerHud.kt:1545`, `InAppToast.kt:63`.
- **values 0/11/13/17/30/36/44/64/92/96/104/112/120 — 1 row each, `INTENTIONAL`:**
  `RoundedPanel.kt:63` (0); `LanguageSettingsScreen.kt:124`(×2, 11 & 17),`:126` (13);
  `PlayerHud.kt:1316` (30); `ProfileGate.kt:78` (36); `SetupScaffold.kt:139` (44);
  `OpenSubtitlesAccountScreen.kt:255` (64); `PlayerHud.kt:525` (92); `MiniPlayer.kt:66` (96);
  `PlayerHud.kt:629` (104); `PlayerHud.kt:513` (112); `PlayerHud.kt:621` (120).

### Reconciliation

546 (`MIGRATE-EXACT`) + 728 (`MIGRATE-NEW`) + 33 (`INTENTIONAL`) + 2 (`SANCTIONED`) = 1309,
matching the spacing inventory exactly.

---

## 3. Family 2 — Animation Timing (Motion)

Source: 114 rows, 33 distinct duration values (+2 bare `ownTvTween()` default-call rows),
extracted from `tween(...)`, `ownTvTween(...)`, `animationSpec = ...`, and
`kotlinx.coroutines.delay(...)` call sites. `ui/theme/Animations.kt` defines only the
*default* tween duration (200ms via `ownTvTween(durationMs: Int = 200)`), not a named-constant
scale — every numeric literal in this family is a `MIGRATE-NEW` candidate by construction; the
job here is clustering by semantic role, not an existing-token collision sweep.

Roughly half the rows are not animation durations at all but `delay(...)` arguments gating a
`FocusRequester.requestFocus()` call after a scroll/composition settles — Task 1's extractor
correctly grouped them with motion timing (both govern "how long the UI waits before something
happens"), and the histogram bears out a clean semantic signal once split out.

### Histogram

| value (ms) | rows | value (ms) | rows | value (ms) | rows |
|---|---|---|---|---|---|
| 60 | 35 | 600 | 2 | 250 | 1 |
| 80 | 18 | 5000 | 2 | 2200 | 1 |
| 50 | 8 | 40 | 2 | 20000 | 1 |
| 120 | 5 | 32 | 2 | 2000 | 1 |
| 140 | 4 | 3000 | 2 | 200 | 1 |
| 220 | 3 | 300 | 2 | 15000 | 1 |
| 1000 | 3 | 1800 | 2 | 1200 | 1 |
| `default(ownTvTween())` | 2 | 160 | 2 | | |
| 700 | 2 | 150 | 2 | | |
| — | — | — | — | 900/90/60000/6000/520/500/4500/420/2500 | 1 each |

**Total: 114 rows.** Top 3 values (60, 80, 50 — all `delay()`-before-`requestFocus()` sites)
account for 61 rows (53.5%), a single semantic role at three tuned magnitudes.

### Classification summary

| bucket | rows | % | notes |
|---|---|---|---|
| `MIGRATE-EXACT` | 2 | 1.8% | the 2 bare `ownTvTween()` calls — already correctly using the implicit 200ms default |
| `MIGRATE-NEW` (new token) | 69 | 60.5% | 6 new named constants across 2 semantic role families |
| `INTENTIONAL` | 43 | 37.7% | component-specific one-offs, derived/staggered arithmetic, thin mixed-purpose clusters |
| `SANCTIONED` | 0 | 0% | no sanctioned category applies |
| **Total** | **114** | **100%** | |

### Semantic-role clustering

**A. Focus-settle delays** — `delay(N); runCatching { xFocus.requestFocus() } }`, letting
layout settle before grabbing D-pad focus. 57 of the 61 rows at 50/60/80ms are exactly this
one-shot shape; 2 more (`PlayerHud.kt:1370,1459`) are a `repeat(10) { withFrameNanos {};
requestFocus(); delay(50) }` retry-loop variant of the same idea (59/61 total). The remaining
2 are `MoveOrderOverlay.kt:66`'s `50L*(it+1)` staggered-retry arithmetic and a comment
reference at `LiveScreen.kt:214` (not code). The three magnitudes are not interchangeable snap
targets — `LiveScreen.kt:214`'s own comment documents a real regression from an earlier racy
delay, so collapsing them onto one value risks reintroducing focus-restore races on slower
hardware. Each magnitude gets its own named constant instead.

**B. Animation-duration roles** — `ownTvTween(N)`/`tween(N)`/`animateColorAsState`/
`animateContentSize` sites clustering by what they animate: focus/selection color cross-fades
(140ms), the nav accent bar's reveal/hide (160ms), and `FloatingRail`'s expand/collapse
content-size animations (220ms). These match the brief's "140/160/220" cues exactly.

### Proposed token additions (`ui/theme/Animations.kt`)

| name | value | sites | rationale |
|---|---|---|---|
| `FocusSettleDelayMs` | `60L` | 34 code (+1 comment) | Dominant focus-settle magnitude, 25 files (dialogs, settings screens, list restores). |
| `FocusSettleDelayLongMs` | `80L` | 18 | Same pattern, heavier layouts (grids/lists, IME-adjacent dialogs). |
| `FocusSettleDelayShortMs` | `50L` | 7 | Same pattern, lighter transitions (tab swaps, inline error focus, player-dialog retries). |
| `MotionColorMs` | `140L` | 4 | Focus/selection color cross-fades — `FocusableSurface.kt`'s `focusContainer` + `NavLadder.kt`'s three (bg/fg/icon). |
| `MotionAccentBarMs` | `160L` | 2 | Nav accent bar reveal — vertical (`NavLadder.kt`) and horizontal (`FloatingRail.kt`) variants. |
| `MotionRailMs` | `220L` | 3 | `FloatingRail.kt`'s `animateContentSize` expand/collapse calls. |

No snap-to-existing recommendations — every token is a Δ0 match for its own population, and
(per the reasoning above) nudging a delay's magnitude carries real behavioral risk, not just a
sub-perceptual visual delta.

### `INTENTIONAL` values — grouped, one-line reasons

| value (ms) | rows | reason |
|---|---|---|
| 50 (stagger) | 1 | `MoveOrderOverlay.kt:66` — `delay(50L*(it+1))`, deliberate documented retry cadence, not a focus-settle delay. |
| 120 | 5 | 3 sub-themes: IME-show settle (`OwnTVTextField.kt:118`, `SearchBar.kt:105`); deferred-overlay-open (`BackupScreen.kt:77`); a 5th focus-settle magnitude at only 2 sites (`StorageBrowser.kt:123`, `ManageSourcesScreen.kt:108`) — below the 7-site floor, each tied to a locally-documented race. |
| 1000 | 3 | Player-internal one-second waits (retry/auto-hide), each independently tuned. |
| 700 | 2 | `LiveScreen.kt:162` (preview-arm debounce), `AudioNowPlayingBar.kt:277` (live-dot pulse) — unrelated roles sharing a value. |
| 600 | 2 | `FloatingRail.kt:155` (settle-flag reset), `PlayerHud.kt:896` (blinking caret) — unrelated. |
| 5000 | 2 | `OwnTVShell.kt:1170` (startup toast), `SyncStatusPill.kt:92` (sync-complete auto-clear) — unrelated. |
| 40 | 2 | `HomeScreen.kt:163` (hero-focus-loss debounce), `SeriesScreen.kt:1006` (composite double-delay scroll-settle) — unrelated. |
| 32 | 2 | `OwnTVTextField.kt:128`, `SearchBar.kt:113` — real 2-site duplicate (IME geometry settle), below token threshold. |
| 3000 | 2 | `HomeScreen.kt:175` (hero dwell-to-expand), `PlayerHud.kt:294` (channel-zap flash duration) — unrelated. |
| 300 | 2 | `PlayerHud.kt:689,702` — identical track-list poll cadence, same file, only 2 sites. |
| 1800 | 2 | `EpgSyncPrompt.kt:78`, `PlayerHud.kt:300` — auto-dismiss role, part of a wider toast-dismiss family (1000/1800/2000/2200/2500), each tuned to its own message's reading time. |
| 150 | 2 | `AddSourceScreen.kt:317` (4th focus-settle magnitude, single-site), `HomeScreen.kt:546` (responsive pair with 500). |
| 500 | 1 | `HomeScreen.kt:546` — paired with 150 above, one coupled unit. |
| 900/90/60000/6000/520/4500/420/2500/250/2200/20000/2000/200/15000/1200 | 1 each | Single-purpose, component-specific (spinner rotation, equalizer stagger, poll intervals, toast dismiss, retry cadence) — see `section-motion.md` in the audit workspace for full site attribution. |

**Total `INTENTIONAL`: 43 rows.**

**Observation (non-actionable):** toast/message auto-dismiss delays (1000, 1800, 2000, 2200,
2500) form a recognizable role but not a mintable value — each is legitimately tuned to its
own message's reading time.

### Per-population site data

**≥30-row population:**

- **value 60 — 35 rows (34 code + 1 comment), → `FocusSettleDelayMs` — 18 files.**
  `BulkRenameDialogs.kt` 8, `LiveScreen.kt` 5 (incl. the comment reference), `CustomizeItemsScreen.kt`
  3, `SeriesScreen.kt` 2, `OpenSubtitlesAccountScreen.kt` 2, `EpgScreen.kt` 2,
  `CustomizeScreen.kt` 2, +11 files with 1 each.

**<30-row populations (`file:line`):**

- **value 80 — 18 rows, → `FocusSettleDelayLongMs`:** `CustomizeScreen.kt:194`,
  `EpgScreen.kt:233,247,311`, `SeriesScreen.kt:1003`, `BackupScreen.kt:125`,
  `EpgSourcesScreen.kt:98`, `LanguageSettingsScreen.kt:91,96,194`,
  `MetadataSettingsScreen.kt:312`, `OpenSubtitlesAccountScreen.kt:302`,
  `PanelWidthSettingsScreen.kt:195`, `VideoPlayerSettingsScreen.kt:205,680`,
  `WeatherSettingsScreen.kt:57`, `shell/components/SettingsScreen.kt:242`,
  `TextInputDialog.kt:65`.
- **value 50 — 7 (of 8), → `FocusSettleDelayShortMs`:** `HomeScreen.kt:267`,
  `BackupScreen.kt:93`, `EpgSyncPrompt.kt:73`, `ManageProfilesScreen.kt:81`,
  `ManageSourcesScreen.kt:143`, `PlayerHud.kt:1370,1459`.
- **value 120 — 5 rows, `INTENTIONAL` (all):** `OwnTVTextField.kt:118`, `SearchBar.kt:105`,
  `BackupScreen.kt:77`, `StorageBrowser.kt:123`, `ManageSourcesScreen.kt:108`.
- **value 140 — 4 rows, → `MotionColorMs`:** `FocusableSurface.kt:79`, `NavLadder.kt:69,79,89`.
- **value 220 — 3 rows, → `MotionRailMs`:** `FloatingRail.kt:266,332,489`.
- **value `default(ownTvTween())` — 2 rows, already correct:** `OwnTVShell.kt:805`,
  `ShellHeader.kt:108`.
- **value 160 — 2 rows, → `MotionAccentBarMs`:** `FloatingRail.kt:567`, `NavLadder.kt:117`.

### Reconciliation

2 (`MIGRATE-EXACT`) + 69 (`MIGRATE-NEW`) + 43 (`INTENTIONAL`) + 0 (`SANCTIONED`) = 114,
matching the motion inventory exactly.

---

## 4. Family 3 — Alphas & Translucency

Source: 183 rows, 39 distinct values (post-merge — see below), extracted from
`Color.copy(alpha=...)` and `Modifier.alpha(...)` call sites. Two source-text representations
of the same value (`0.9f`, 2 sites; `0.90f`, 1 site) are merged into one 3-row population at
value `0.9`.

Alphas live in `ui/theme`, not `Dimens.kt`: the two pre-existing named alpha values are
`GlassConfig.DEFAULT_GLASS_ALPHA = 0.75f` (`Glass.kt`) and `OwnTVColors.focusGlow` (built from
`0.45f` dark / `0.22f` light, `OwnTVColors.kt`). Unlike spacing, minting a new alpha token is
not gated on a large population — alpha's purpose is visual consistency across a handful of
high-visibility surfaces, so a token justified by 2–3 independently-converged sites is treated
the same way a 200-site spacing cluster would be. Proposed home for all new alpha constants:
**`ui/theme/AlphaTokens.kt`** (new file).

### Histogram (post-merge)

| value | rows | value | rows | value | rows |
|---|---|---|---|---|---|
| 0.7 | 38 | 0.16 | 4 | 0.62 | 2 |
| 0.75 | 32 | 0.25 | 4 | 0.68 | 2 |
| 0.55 | 11 | 0.82 | 3 | 0.95 | 1 |
| 0.45 | 11 | 0.65 | 3 | 0.92 | 1 |
| 0.5 | 8 | 0.6 | 3 | 0.85 | 1 |
| 0.78 | 6 | 0.35 | 3 | 0.74 | 1 |
| 0.22 | 5 | 0.3 | 3 | 0.58 | 1 |
| 0.18 | 5 | 0.12 | 3 | 0.52 | 1 |
| 0.8 | 4 | 0.9 | 3 | 0.44 | 1 |
| 0.72 | 4 | 0.88 | 2 | 0.43 | 1 |
| 0.4 | 4 | 0.14/0.13/0.10/0.09 | 2 each | 0.28/0.20/0.08/0.05 | 1 each |

**Total: 183 rows.** Top 2 values (0.7, 0.75) account for 70 rows (38.3%), overwhelmingly the
same modal-scrim role at two near-identical magnitudes.

### Existing-token collision sweep

| existing token | value | own usage shape | population match? | verdict |
|---|---|---|---|---|
| `GlassConfig.DEFAULT_GLASS_ALPHA` | 0.75 | opt-in panel-background tint (`Modifier.glass()`, only when "Liquid Glass" is on) | value 0.75 (32 rows), dominant shape is an always-on modal-dialog backdrop | **rejected** — structurally different (opt-in tint vs. always-on scrim); the number is adopted as the *new* `AlphaScrim` value anyway, reinforcing 0.75 as the app's own "right darkness," but not as a reuse of `DEFAULT_GLASS_ALPHA` |
| `OwnTVColors.focusGlow` | 0.45 (dark) | glow/shadow tint behind focused elements, baked into a resolved `Color`, not a reusable scalar | value 0.45 (11 rows) is maximally heterogeneous (scrim outliers, rail-scrim, nav-pill fill, subtitle backgrounds, chrome text) | **rejected** — no dominant shape to match; the token's own literal (`OwnTVColors.kt:153/183`) is classified `INTENTIONAL` (it IS the token's own definition) |

`MIGRATE-EXACT` is therefore empty for alpha (0 rows) — every action row falls under
`MIGRATE-NEW`.

### Classification summary

| bucket | rows | % | notes |
|---|---|---|---|
| `MIGRATE-EXACT` | 0 | 0% | no pre-existing alpha token survives the collision sweep |
| `MIGRATE-NEW` | 76 | 41.5% | 43 new-token rows (5 new constants) + 33 snap-to-new-token rows |
| `INTENTIONAL` | 91 | 49.7% | component-specific one-offs, hand-tuned tints, gradient stops, thin clusters |
| `SANCTIONED` | 16 | 8.7% | 13 Player HUD plain-black scrims + 3 `WeatherGlyph` pictorial canvas art |
| **Total** | **183** | **100%** | |

### Semantic-role method — notable cross-checks

- **"White rims 0.12/0.14" tested and found real but already tokenized.** `FloatingRail.kt`'s
  `RailSeparatorColor`(0.14)/`RailPanelBorderColor`(0.12) are already file-local named
  constants, consistent with the rubric's "file-local geometry is already a token" treatment.
  Two other sites share the raw numbers but a different role (decorative gradient, unrelated
  chip fill) — rejected as coincidental, `INTENTIONAL`.
- **"Header separator 0.25" tested, found real but thin** — 2 sites, 2 magnitudes (0.25,
  0.18), below the new-token threshold.
- **"Focus ring 0.7" confirmed intentional, not a mint candidate** — `NavLadder.kt:102`'s
  comment is explicit and final ("70% alpha (user feedback): full-strength focusBorder read as
  glaring on nav items"); a single documented site, not a repeated cluster.
- **Player HUD internal chrome (21 rows, `Color.White`/theme-color, not black)** — 18 are
  distinct one-offs (`INTENTIONAL`); 3 (`:1092`/`:1172`/`:1195`) are an exact-duplicate
  `focusedContainerColor = Color.White.copy(alpha = 0.16f)` across `SpeedButton`/
  `EngineToggle`/`CtrlButton` — pulled into `MIGRATE-NEW` as `AlphaHudFocusFill`.

### Proposed token additions (`ui/theme/AlphaTokens.kt`, new file)

| name | value | sites (direct + snapped) | rationale |
|---|---|---|---|
| `AlphaScrim` | `0.75f` | 32 + 33 snapped = 65 | Dominant full-screen modal-backdrop value across 45+ dialogs/popups app-wide. |
| `AlphaScrimLight` | `0.65f` | 3 | 3 independent "quick confirm" dialogs (`AvatarPickerDialog.kt:57`, `ExitDialog.kt:53`, `IncompleteRestoreDialog.kt:58`) converged on the same lighter scrim. |
| `AlphaPanelFill` | `0.82f` | 2 (+1 sanctioned) | Fixed-width slide-in side panel fill — `CategoryBrowserOverlay.kt:82`, `ChannelListOverlay.kt:89`; matches the brief's cited "rail panel fill 0.82" (third site is `PlayerHud.kt:1128`, sanctioned). |
| `AlphaBlurredBackdrop` | `0.5f` | 3 | `HomeScreen.kt:640,689,794` — identical `AsyncImage(..., alpha = 0.5f)` blurred backdrop. |
| `AlphaHudFocusFill` | `0.16f` | 3 | `PlayerHud.kt:1092,1172,1195` — exact-duplicate `focusedContainerColor` across 3 HUD control composables. |

No `AlphaRim`/`AlphaSeparator`/`AlphaFocusRing` tokens are proposed — those hypothesized
clusters were either already file-scoped or too thin/singular to mint.

### Snap-to-new-token recommendations

| value | rows | target token | Δ | basis |
|---|---|---|---|---|
| 0.7 | 30 (of 38; 2 sanctioned + 6 other-role excluded) | `AlphaScrim` (0.75) | −0.05 | same modal-scrim shape at every site |
| 0.8 | 2 | `AlphaScrim` (0.75) | +0.05 | same modal-scrim shape |
| 0.78 | 1 | `AlphaScrim` (0.75) | +0.03 | same modal-scrim shape |

### `INTENTIONAL` values — grouped by role

| role | rows | reason |
|---|---|---|
| Scrim-shaped outliers, standalone | 3 | `PosterCard.kt:106` (watched-status dim), `ResumeDialog.kt:50` (deliberately lighter), `MoveOrderOverlay.kt:83` (deliberately darker during drag). |
| Player HUD internal chrome (non-black) | 18 | Distinct one-off text/icon/pill alphas per control; no further duplication. |
| Progress-bar track backgrounds | 6 | Same conceptual role at 5 different alphas across 4 components — real drift, no dominant value. |
| Small pill/badge backgrounds | 4 | Same shape, 4 different alphas, organic drift. |
| Gradient stops (non-sanctioned) | 5 | Component-specific decorative gradients. |
| Onboarding decorative glow/rings | 4 | Single animated component, hand-tuned. |
| Whole-composable dim | 3 | 3 components, 3 reasons/magnitudes. |
| Hairline dividers | 2 | Same shape, 2 magnitudes, below threshold. |
| Border strokes | 3 | 3 independent treatments, 3 magnitudes. |
| Rail rim colors (already file-local tokens) | 2 | Not raw literals needing migration. |
| Focus ring | 1 | Single documented, already-final decision. |
| Container/chip tints | 12 | Every themed chip/pill/panel hand-tuned independently. |
| Text/icon tint (non-PlayerHud) | 14 | 10 distinct values across 14 rows, no consolidation candidate. |
| Glassy input-field tint pair | 4 | Real 2+2-site duplicate, below the family's new-token bar. |
| Floating chrome panel background (non-scrim) | 2 | 2 unrelated floating chrome backgrounds. |
| Theme-surface panel fill | 2 | Same concept, Δ0.02, only 2 sites. |
| Rail-active background dim | 1 | Single site, distinct mechanism (`animateFloatAsState`). |
| Subtitle-related backgrounds | 2 | 2 single-purpose subtitle backgrounds. |
| `focusGlow` token definitions | 2 | `OwnTVColors.kt:153,183` — the token's own definition. |
| Header text shadow | 1 | Single file-scoped shadow-color constant. |

**Total `INTENTIONAL`: 91 rows.**

### `SANCTIONED` values

| role | rows | reason |
|---|---|---|
| Player HUD plain-black scrims/overlays | 13 | Every `Color.Black.copy(alpha=...)` in `player/PlayerHud.kt` — matches the sanctioned exception verbatim. `MiniPlayer.kt`/`StreamInfoOverlay.kt`/`SubtitleOverlay.kt`/`AudioNowPlayingBar.kt` are separate player-adjacent components per `CLAUDE.md`'s architecture list, so their overlays are classified normally, not sanctioned. |
| `WeatherGlyph` pictorial canvas art | 3 | `ShellHeader.kt:204,219,230` — alpha applied to `WeatherGlyph.*` colors inside `WeatherConditionIcon`'s `Canvas` draw block. |

### Per-population site data

**≥30-row populations (per-file counts):**

- **value 0.75 — 32 rows, 100% → `AlphaScrim` — 19 files.** `SettingsScreen.kt`
  (shell/components) 6, `BulkRenameDialogs.kt` 4, `ManageSourcesScreen.kt` 2,
  `OpenSubtitlesAccountScreen.kt` 2, `VideoPlayerSettingsScreen.kt` 2, `CustomizeScreen.kt` 2,
  `GuideCore.kt` 2, +12 files with 1 each.
- **value 0.7 — 38 rows, split — 18 files.** `VideoPlayerSettingsScreen.kt` 5,
  `PlayerHud.kt` 5 (2 sanctioned + 3 other-role), `BackupScreen.kt` 5,
  `SettingsScreen.kt`(shell/components) 4, `LiveScreen.kt` 4 (3 scrim + 1 badge),
  `EpgScreen.kt` 2, `BackgroundImageChooserDialog.kt` 2, +11 files with 1 each. Breakdown: 30
  → `AlphaScrim`; 2 → SANCTIONED (`PlayerHud.kt:1512,1602`); 1 → INTENTIONAL badge
  (`LiveScreen.kt:774`); 3 → INTENTIONAL chrome (`PlayerHud.kt:655,1010,1012`); 1 →
  INTENTIONAL focus ring (`NavLadder.kt:102`); 1 → INTENTIONAL gradient (`MiniPlayer.kt:101`).

**<30-row populations — see `section-alpha.md` in the audit workspace for full per-value
`file:line` breakdowns** (0.55, 0.45, 0.5, 0.78, 0.22, 0.18, 0.8, 0.72, 0.4, 0.16, 0.25, 0.82,
0.65, 0.6, 0.35, 0.3, 0.12, 0.9, 0.88, 0.14, 0.13, 0.10, 0.09, 0.62, 0.68, 0.74, and the 11
single-row values — every one classified and attributed to a specific file:line).

### Reconciliation

0 (`MIGRATE-EXACT`) + 76 (`MIGRATE-NEW`) + 91 (`INTENTIONAL`) + 16 (`SANCTIONED`) = 183,
matching the alpha inventory exactly (post 0.9/0.90 merge).

---

## 5. Family 4 — Sizes, Elevation, Text

Source: 473 rows across two extraction sub-tags: **`size`** (270 rows — `.dp` literals inside
`.size(`/`.height(`/`.width(`/`shadow(elevation=`/`.border(` trigger spans, plus every
unrestricted `N.sp` literal) and **`size-namedarg`** (203 rows — arbitrary
`identifier = N.dp` named arguments outside those triggers, e.g.
`Modifier.dialogPanel(width = 480.dp, …)`). 270 + 203 = 473.

### Part A — the 11 stray `.sp` literals

Compared against `OwnTVTypography`'s scale (44/36/30/28/24/20/22/17/15/16/14/12/11).
`SubtitleOverlay.kt:94-95`'s `(24 * textScale * sizeScale).sp` — the sanctioned hand-tuned
metrics — is a computed expression, not a literal, so it was never extracted.

| value | rows | verdict | reason |
|---|---|---|---|
| 30, 18, 14, 14 | 4 | `INTENTIONAL` | `DatabaseRecoveryScreen.kt:53,56,60,67` — pre-theme fallback screen, cannot reference `MaterialTheme.typography` by design. |
| 8 | 3 | `MIGRATE-NEW` (new token) | `RemoteBackupRestoreScreen.kt:102,189`, `RemoteSetupScreen.kt:95` — exact-duplicate `letterSpacing = 8.sp` on the PIN display. |
| 20 | 1 | `INTENTIONAL` | `OwnTVShell.kt:1248` — wordmark logo text, custom `buildAnnotatedString`, no style to adopt. |
| 1.2 | 1 | `INTENTIONAL` | `LiveEpgCard.kt:139` — tracking nudge on an already-tokenized base style. |
| 2, 3 | 2 | `INTENTIONAL` | `PlayerHud.kt:921,927` — HUD digital-readout tracking, hand-tuned per size. |

**Totals (sp): `MIGRATE-NEW` 3, `INTENTIONAL` 8.** Proposed token:
`PinCodeLetterSpacing = 8.sp` in `ui/theme/Type.kt`.

### Part B — the 462 `.dp` rows

**Existing-token collision sweep:** all 27 distinct `Dimens.kt` values were checked; only one
produced a shape-congruent match — `PosterProgressHeight`(4dp, `.height()` progress
track/fill) → **`MIGRATE-EXACT`**, 10 sites. Every other Dimens value that numerically
coincided with a size-population value (`HeroProgressHeight`(3), `IconTileSize`(42),
`RailPillSize`(56), `ChannelListWidth`(460), `GapMedium`(16), `HeroBaseWidth`(180),
`HeroMinCardHeight`(200), the `Gap*`/`ScreenPadding*`/corner-family tokens at 24dp) was
rejected as shape-mismatched or scope-mismatched (single-screen entanglement, cross-family
coincidence, or a `Dp`-named-argument shape vs. the token's own `spacedBy`/`Spacer` shape).

**Six clusters were identified within the `.dp` population:**

1. **`Modifier.dialogPanel(...)` named arguments (138 rows).** `padding=` (61 rows: 28dp→35
   sites new token, 24dp→9 snapped Δ−4, 18dp→8 sites new token, 16dp→4 snapped Δ−2, 14dp→4
   snapped Δ−4, 12dp→1 `INTENTIONAL`); `corner=` (19 rows: 16dp→15 sites new token, 18dp→3
   snapped Δ−2, 20dp→1 `INTENTIONAL`); `width=` (63 rows: 480dp→11 sites new token, 560dp→9
   sites new token, 40 rows across 21 magnitudes `INTENTIONAL` — content-driven one-off widths,
   too visually significant to snap).
2. **Hairline border/divider stroke (14 rows, all 1dp)** → new `HairlineWidth = 1.dp`, 10
   files.
3. **Progress-bar/selection-ring 2dp split (12 rows)** — 6 rows `.height(2.dp)` progress
   pairs → new `ThinProgressHeight = 2.dp`; 6 rows `.border(2.dp,…)` selection rings → new
   `SelectionBorderWidth = 2.dp` (kept separate from `FocusBorderWidth`=2.5dp deliberately —
   different semantic role, aliasing would create hidden coupling).
4. **Status/genre dot (4 rows, value 8dp)** → new `StatusDotSize = 8.dp`, 4 files
   (byte-identical `Box(Modifier.size(8.dp).clip(CircleShape)...)`).
5. **Touch-target sizes (14 rows: 8 at 48dp + 6 at 44dp)** — two real, distinct clusters, not
   one scale → new `TouchTargetSize = 48.dp` (general UI) and
   `TouchTargetSizeCompact = 44.dp` (player HUD).
6. **Icon-size scale (30 rows across 4 magnitudes)** — dominant magnitudes are **18dp and
   20dp**, not the brief's illustrative 24dp (2 sites only) → new `IconSizeMedium = 18.dp`
   (11 sites) and `IconSizeLarge = 20.dp` (8 sites, snapping 22dp Δ−2 and 16dp Δ+2 onto them).

Plus a `widthIn`/`heightIn` `max=`/`min=` cluster (44 rows): one real cross-file cluster
found, `Dimens.ContentColumnMaxWidth = 640.dp` (4 sites, byte-near-identical
`Column(widthIn(max=640.dp), CenterHorizontally)`); every other ≥2-site magnitude in the
bucket (680, 560, 460, 420, 360, 300, 620, 520) was individually checked for a shared call
shape and correctly held `INTENTIONAL` — none clears the 3-site same-shape floor (a
cross-shape merge candidate at 680dp, `ProseMaxWidth`, was minted in an earlier drafting pass
and reverted on review for contradicting this section's own reasoning about the 680dp
sub-groups).

### Classification summary

| bucket | rows | % |
|---|---|---|
| `MIGRATE-EXACT` | 10 | 2.1% |
| `MIGRATE-NEW` — new token | 146 | 30.9% |
| `MIGRATE-NEW` — snap to existing/new | 31 | 6.6% |
| **`MIGRATE-NEW` subtotal** | **177** | **37.4%** |
| `INTENTIONAL` | 284 | 60.0% |
| `SANCTIONED` | 2 | 0.4% |
| **Total** | **473** | **100%** |

(dp: `MIGRATE-EXACT` 10, `MIGRATE-NEW` 174 [143 new-token + 31 snap], `INTENTIONAL` 276,
`SANCTIONED` 2 = 462. sp: `MIGRATE-NEW` 3 [new-token], `INTENTIONAL` 8 = 11. 462+11=473.)

### Proposed token additions

**`ui/theme/Dimens.kt`** (14 additions):

| name | value | sites (direct + snapped) |
|---|---|---|
| `DialogPanelPadding` | `28.dp` | 35 + 9 (@24) = 44 |
| `DialogPanelCorner` | `16.dp` | 15 + 3 (@18) = 18 |
| `HairlineWidth` | `1.dp` | 12 |
| `IconSizeMedium` | `18.dp` | 11 + 5 (@16) = 16 |
| `DialogPanelWidth` | `480.dp` | 11 |
| `DialogPanelWidthWide` | `560.dp` | 9 |
| `TouchTargetSize` | `48.dp` | 8 |
| `DialogPanelPaddingCompact` | `18.dp` | 8 + 8 (@16:4, @14:4) = 16 |
| `IconSizeLarge` | `20.dp` | 8 + 6 (@22) = 14 |
| `TouchTargetSizeCompact` | `44.dp` | 6 |
| `ThinProgressHeight` | `2.dp` | 6 |
| `SelectionBorderWidth` | `2.dp` | 6 |
| `ContentColumnMaxWidth` | `640.dp` | 4 |
| `StatusDotSize` | `8.dp` | 4 |

`Dimens.PosterProgressHeight` (existing) absorbs 10 sites at `MIGRATE-EXACT`, no new token
needed.

**`ui/theme/Type.kt`:** `PinCodeLetterSpacing = 8.sp`, 3 sites.

### Notable findings

- `DialogPanel.kt`'s own coded default (`padding: Dp = 24.dp`) is a minority pattern: 35 of
  61 padding-overriding call sites converge on 28dp; only 9 pass 24dp explicitly (redundant
  with the current default).
- `corner=` overrides diverge even more sharply from the coded default (`Dimens.CardCorner` =
  10dp) — every one of 19 override sites uses 16, 18, or 20; none is near 10dp. 10dp may be a
  stale default.
- Icon-size reality contradicts the brief's own "24dp icons" illustrative example — the
  dominant magnitudes are 18dp and 20dp; 24dp icons exist at only 2 sites.
- Two touch-target magnitudes (44dp, 48dp) are both real and serve different roles (general UI
  vs. player HUD), proposed as two tokens rather than merged.

### Reconciliation

dp: 10 + 174 + 276 + 2 = 462. sp: 3 + 8 = 11. 462 + 11 = 473, matching the size inventory
exactly.

---

## 6. Family 5 — Residual Re-checks (Color & Corner)

Both families were already substantially consolidated before this audit (color to a pure
achromatic dark palette + sanctioned pictorial sets; corner to the 2026-08-17 scale). Each
literal here is either **SANCTIONED** (theme home or documented exception) or a **FINDING**
(unexpected) — not run through the four-bucket rubric.

### 5a. Color — 137 rows (`Color(0x........)` literals)

| file | rows | verdict |
|---|---|---|
| `ui/theme/Color.kt` | 50 | SANCTIONED — theme file, the token home itself |
| `ui/theme/AccentColor.kt` | 40 | SANCTIONED — theme file |
| `ui/components/GenreColor.kt` | 18 | SANCTIONED — pictorial constant |
| `ui/components/OwnTVAvatar.kt` | 13 | SANCTIONED — pictorial constant (canvas art) |
| `ui/theme/OwnTVColors.kt` | 1 | SANCTIONED — theme file |
| **theme/pictorial subtotal** | **122** | |
| `features/recovery/DatabaseRecoveryScreen.kt` | 4 | FINDING (justified) |
| `features/settings/VideoPlayerSettingsScreen.kt` | 3 | FINDING (justified) |
| `ui/components/ColorPicker.kt` | 3 | FINDING (justified, low severity) |
| `player/AudioNowPlayingBar.kt` | 2 | FINDING (benign) |
| `ui/components/BackgroundImageChooserDialog.kt` | 2 | FINDING |
| `ui/components/DownloadStatusStrip.kt` | 1 | FINDING |
| **residual subtotal** | **15** | |
| **Total** | **137** | |

**Findings, descending severity:**

1. **Hardcoded error-red bypasses the theme, 3 sites across 2 files** —
   `Color(0xFFEF4444)` at `BackgroundImageChooserDialog.kt:165,173` and
   `DownloadStatusStrip.kt:79`, an exact-duplicate literal, same error/failure role. The theme
   already defines error colors (`DarkError = 0xFFFFB4AB`, `LightError = 0xFFBA1A1A`, wired to
   `colors.favorite`), but neither matches — these 3 sites don't respond to light/dark theme
   switching at all. **The one residual-color finding with real design-language impact.**
2. `VideoPlayerSettingsScreen.kt:1323` — a deliberately colorful gradient swatch (3 literals),
   justified by its own comment ("A busy-ish backdrop"). Flagged for completeness only.
3. `ColorPicker.kt` — 3 component-internal literals (amber glow ring matching its own doc
   comment; achromatic 60%-black swatch border). Low severity — the one component whose
   purpose is displaying colors.
4. `AudioNowPlayingBar.kt:181` — benign, achromatic near-black/white raw hex instead of
   `Color.Black`/`Color.White`; style nit, not a design-language violation.
5. `DatabaseRecoveryScreen.kt` — 4 literals, justified: "Pre-theme surface: renders when Room
   fails before DI/theme init — deliberately hardcoded." Cannot reference the theme by
   construction.

None of the 15 residual rows are chromatic-and-unjustified except finding #1.

### 5b. Corner — 57 rows (`RoundedCornerShape(...)`/`CornerRadius(...)` literals)

| category | rows | verdict |
|---|---|---|
| Percent pills (`percent = 50` ×21, `100` ×2, `999.dp` ×2) | 25 | SANCTIONED |
| Hairline 1–3dp roundings (1.dp ×2, 2.dp ×9, 3.dp ×3) | 14 | SANCTIONED |
| `EpgScreen.kt:764` genre dot, 4.dp | 1 | SANCTIONED — documented exception |
| `AudioNowPlayingBar.kt:308` progress-bar half-height | 2 | SANCTIONED |
| `OwnTVAvatar.kt` canvas-art draw calls | 13 | SANCTIONED — pictorial constant |
| **sanctioned subtotal** | **55** | |
| `percent = 28` brand-mark shape | 2 | FINDING |
| **Total** | **57** | |

`GuideCore.kt` produces zero rows in this inventory, confirming its corner literals were
already migrated by the 2026-08-17 consolidation.

**The one corner finding:** `OwnTVShell.kt:1224` and `BrandLockup.kt:52` both write
`RoundedCornerShape(percent = 28)` for the wordmark "mark" glyph (a squircle-ish 28%-rounded
shape, distinct from the sanctioned 50%/100%/999dp "full pill" family). Exact-duplicate value,
same semantic role, 2 sites in 2 different files — each file defines its own local
`markShape` instead of sharing one. Very low severity (a consistent, deliberate design choice)
but not covered by any documented sanctioned exception — worth a shared
`Dimens.BrandMarkCornerPercent = 28` (or an exported `val` next to one canonical
`BrandLockup`).

### Reconciliation

Color: 122 + 15 = 137. Corner: 55 + 2 = 57. Both match their inventories exactly.

---

## 7. Unclassified appendix — residual-unexplained counts

Each extraction script also ran a **broad companion grep** — an intentionally wider,
unfiltered pattern — alongside its narrow, context-scoped extractor. The gap between the two
(`residual-unexplained` in `inventory-summary.txt`) is not a set of missed defects; it is
diagnostic instrumentation confirming nothing vanished silently. Per family:

| family | narrow (extracted) | broad companion | residual-unexplained | explanation |
|---|---|---|---|---|
| spacing | 1309 | 1931 | 622 | By design: the broad pattern counts **every** `N.dp` literal in scope, unfiltered — so this residual is dominated by literals that legitimately belong to the size/corner/shadow/border families (captured separately in their own inventories), not misses within spacing itself. Not further decomposed line-by-line beyond this documented explanation — **uninvestigated at the individual-line level**. |
| motion | 114 | 162 | 48 | The broad pattern matches any `tween(`/`delay(`/`animationSpec =` occurrence; the narrow extractor deliberately excludes `delay(...)`/`delayMillis=` calls in `*ViewModel.kt` files (a filename heuristic approximating "outside composables") and requires the specific call shapes documented in `inventory-summary.txt`. The residual is expected to be dominated by ViewModel-side delays and non-duration `animationSpec` arguments (e.g. spring specs without a literal `durationMillis`) — **uninvestigated at the individual-line level**; no re-audit of this residual was performed since Task 1's exclusions were reviewed and confirmed intentional. |
| alpha | 183 | 201 | 18 | Explained: the broad pattern searches for the bare text `alpha =`, while the narrow extractor requires a decimal `0.N`-prefixed literal (plus the `.alpha(N)`/`targetValue` variants documented in §Method). The 18-row gap is non-decimal `alpha =` assignments (e.g. `alpha = 1f`, or a bare variable reference) — deliberately excluded because they are not hardcoded literals to migrate, not a miss. |
| size | 473 | 1568 | 1095 | Same shape as spacing's residual, and the largest in the audit: the broad pattern counts every trigger-keyword occurrence unfiltered, including the same spacing/corner/shadow cross-family overlap described above (viewed from the size side of the boundary instead of the spacing side). The two families' broad counts double-count each other's legitimately-excluded literals by construction. **Uninvestigated at the individual-line level** beyond this documented boundary explanation; Task 1's round-3 fix (span-scoped, per-occurrence extraction) already eliminated the one confirmed *bug* in this area (19 lines that had been double-tagged across both TSVs — now verified zero overlap). |
| color | 137 | 137 | 0 | Fully explained — every `Color(0x...)` literal in scope is already 8-digit ARGB, so the broad (any-hex-length) and narrow (8-digit-only) patterns coincide exactly. No residual. |
| corner | 57 | 239 | 182 | Explained as a **counting-unit artifact, not missed literals**: the broad count is call-site *occurrences*, while the classified 57 is a *literal* count — a single `RoundedCornerShape(topStart=…, topEnd=…)` call site with multiple corner arguments contributes 1 to the broad count but multiple literals to the classified 57, which is why the naive "broad − narrow" arithmetic doesn't map to missed defects here. No additional unclassified corner literals were found; §6 accounts for all 57 already-extracted rows exactly. |

**Total residual-unexplained across all families: 622 + 48 + 18 + 1095 + 0 + 182 = 1965.**
None of this total represents literals known to exist and left unclassified within a family's
own inventory — every row that *was* extracted into a TSV is accounted for in §§2–6's
reconciliation lines (2273/2273). The residual figures instead describe the boundary between
families' extraction patterns (spacing/size's mutual, by-design overlap; expected/confirmed
exclusions for motion and alpha) or a counting-unit mismatch (corner). Where a family's
residual was not decomposed to individual `file:line` detail in this audit (motion, size,
spacing), it is flagged **uninvestigated** above rather than asserted as fully explained.

---

## 8. Phased remediation backlog

Each phase below is a proposal only and requires separate user approval before execution, per
the spec. Phases are sized like the 2026-08-17 corner-radius consolidation — one mapping
table, one gate (a screenshot/visual-diff pass) per phase — rather than one undifferentiated
sweep across all 2273 literals. Ordering follows the plan's guidance (spacing exact-matches →
motion tokens → alphas → sizes) with a fix-now mini-phase pulled to the front for the two real
defects, and spacing's larger new-token/snap work — the single biggest remaining chunk of
churn in the whole audit — scheduled last, after every other family's smaller, safer sweep is
done.

### Phase 0 — Fix now: defects (not consolidations)

These are bugs, not style drift, and are small enough to fix independently of the rest of the
backlog.

| step | issue | fix | sites |
|---|---|---|---|
| 0.1 | `Color(0xFFEF4444)` hardcoded error-red bypasses theme, doesn't respond to light/dark | Route through a themed error color, or formalize `0xFFEF4444` as a named `OwnTVColors`/`Dimens`-adjacent constant if a fixed non-reactive alert red is intended | `BackgroundImageChooserDialog.kt:165,173`, `DownloadStatusStrip.kt:79` (3 sites, 2 files) |
| 0.2 | `RoundedCornerShape(percent = 28)` wordmark "mark" shape duplicated independently in 2 files | Extract a shared `Dimens.BrandMarkCornerPercent = 28` (or an exported `val` next to one canonical `BrandLockup`) | `OwnTVShell.kt:1224`, `BrandLockup.kt:52` (2 sites, 2 files) |

Gate: visual confirmation the error-red still reads as an alert color in both themes; wordmark
renders identically before/after de-duplication.

### Phase 1 — Spacing exact-matches (546 rows, zero visual change)

Mechanical swap to tokens that already exist and produce **zero visual change** — the single
largest, lowest-risk win in the whole audit.

| literal | replacement | rows |
|---|---|---|
| `4.dp` | `Dimens.GapTiny` | 89 |
| `8.dp` | `Dimens.GapSmall` | 171 |
| `14.dp` | `Dimens.HeroGap` | 109 |
| `16.dp` (excl. `SubtitleOverlay.kt:103`) | `Dimens.GapMedium` | 142 |
| `24.dp` | `Dimens.GapLarge` | 29 |
| `32.dp` | `Dimens.ScreenPaddingH` | 6 |

Gate: a build + lint pass is sufficient (Δ0 by construction); no screenshot diff required.

### Phase 2 — Motion tokens (69 rows, zero visual change)

Mint the 6 named constants in `Animations.kt`, then mechanically replace each literal at its
sites. Every replacement is Δ0 — same literal value, just named.

| literal (call shape) | replacement | rows |
|---|---|---|
| `60` (`delay(...)`) | new `Animations.FocusSettleDelayMs` | 35 |
| `80` (`delay(...)`) | new `Animations.FocusSettleDelayLongMs` | 18 |
| `50` (`delay(...)`, excl. `MoveOrderOverlay.kt:66`) | new `Animations.FocusSettleDelayShortMs` | 7 |
| `140` (`ownTvTween(...)`) | new `Animations.MotionColorMs` | 4 |
| `160` (`ownTvTween(...)`) | new `Animations.MotionAccentBarMs` | 2 |
| `220` (`ownTvTween(...)`) | new `Animations.MotionRailMs` | 3 |

Gate: build pass; optionally spot-check the 3 `FocusSettleDelay*` sites for behavioral
regression given the delay values gate real focus-restore timing, not just visuals.

### Phase 3 — Alpha tokens (76 rows: 43 zero-delta + 33 small-delta)

| step | literal(s) | replacement | rows |
|---|---|---|---|
| 3.1 | `0.75f` (fullscreen scrim shape) | new `AlphaScrim` | 32 |
| 3.1 | `0.65f` (same shape, 3 independent dialogs) | new `AlphaScrimLight` | 3 |
| 3.1 | `0.82f` (side-panel fill) | new `AlphaPanelFill` | 2 |
| 3.1 | `0.5f` (`AsyncImage` blurred backdrop) | new `AlphaBlurredBackdrop` | 3 |
| 3.1 | `0.16f` (`focusedContainerColor`, 3 HUD controls) | new `AlphaHudFocusFill` | 3 |
| 3.2 | `0.7f` (same scrim shape) | `AlphaScrim` (Δ−0.05) | 30 |
| 3.2 | `0.8f` (same scrim shape) | `AlphaScrim` (Δ+0.05) | 2 |
| 3.2 | `0.78f` (same scrim shape) | `AlphaScrim` (Δ+0.03) | 1 |

Gate: step 3.1 is Δ0, build pass only. Step 3.2 changes the rendered scrim by ≤0.05 alpha —
sub-perceptual on a full-screen backdrop but worth a screenshot pass given the site count
(~33 dialogs).

### Phase 4 — Sizes (187 rows: 10 exact + 177 new-token/snap)

| step | literal(s) | replacement | rows |
|---|---|---|---|
| 4.1 | `.height(4.dp)` progress track/fill | `Dimens.PosterProgressHeight` (existing) | 10 |
| 4.2 | `dialogPanel(padding=28.dp)` | new `DialogPanelPadding` | 35 |
| 4.2 | `dialogPanel(corner=16.dp)` | new `DialogPanelCorner` | 15 |
| 4.2 | `.border(1.dp,...)` hairlines | new `HairlineWidth` | 12 |
| 4.2 | icon `.size/.height/.width(18.dp)` | new `IconSizeMedium` | 11 |
| 4.2 | `dialogPanel(width=480.dp)` | new `DialogPanelWidth` | 11 |
| 4.2 | `dialogPanel(width=560.dp)` | new `DialogPanelWidthWide` | 9 |
| 4.2 | 48dp touch targets | new `TouchTargetSize` | 8 |
| 4.2 | `dialogPanel(padding=18.dp)` | new `DialogPanelPaddingCompact` | 8 |
| 4.2 | icon `.size/.height/.width(20.dp)` | new `IconSizeLarge` | 8 |
| 4.2 | 44dp HUD touch targets | new `TouchTargetSizeCompact` | 6 |
| 4.2 | `.height(2.dp)` thin progress pairs | new `ThinProgressHeight` | 6 |
| 4.2 | `.border(2.dp,...)` selection rings | new `SelectionBorderWidth` | 6 |
| 4.2 | `widthIn(max=640.dp)` column caps | new `ContentColumnMaxWidth` | 4 |
| 4.2 | status/genre dot `.size(8.dp)` | new `StatusDotSize` | 4 |
| 4.2 | `letterSpacing = 8.sp` PIN display | new `Type.PinCodeLetterSpacing` | 3 |
| 4.3 | `dialogPanel(padding=24.dp)` | `DialogPanelPadding` (Δ−4) | 9 |
| 4.3 | `dialogPanel(corner=18.dp)` | `DialogPanelCorner` (Δ−2) | 3 |
| 4.3 | icon `22.dp` | `IconSizeLarge` (Δ−2) | 6 |
| 4.3 | icon `16.dp` | `IconSizeMedium` (Δ+2) | 5 |
| 4.3 | `dialogPanel(padding=16.dp)` | `DialogPanelPaddingCompact` (Δ−2) | 4 |
| 4.3 | `dialogPanel(padding=14.dp)` | `DialogPanelPaddingCompact` (Δ−4) | 4 |

Gate: step 4.1 is Δ0. Step 4.2 mints 15 new constants (14 in `Dimens.kt`, 1 in `Type.kt`) and
is Δ0 at every direct site. Step 4.3 (31 rows) carries deltas up to 4dp on icon sizes and
dialog padding/corner — worth a screenshot pass across the ~15 affected dialog/icon call
sites, smaller in scope than spacing's own snap phase.

### Phase 5 — Spacing new tokens + snap sweep (728 rows — largest remaining churn, scheduled last)

The largest single chunk of work in the audit by row count and mint count; deliberately
scheduled after the smaller families so the app benefits from motion/alpha/size consistency
before taking on the biggest visual-diff surface.

| step | literal(s) | replacement | rows |
|---|---|---|---|
| 5.1 | `2.dp` | new `Dimens.GapHairline` | 51 |
| 5.1 | `12.dp` | new `Dimens.GapCompact` | 202 |
| 5.1 | `20.dp` | new `Dimens.GapWide` | 73 |
| 5.1 | `horizontal = 40.dp, vertical = 28.dp` (paired) | new `Dimens.DetailPanelPaddingH`/`V` | 44 (22 sites × 2) |
| 5.2 | `1.dp` | `Dimens.GapHairline` | 2 |
| 5.2 | `3.dp` | `Dimens.GapTiny` | 12 |
| 5.2 | `5.dp` | `Dimens.GapTiny` | 9 |
| 5.2 | `6.dp` (excl. `SubtitleOverlay.kt:103`) | `Dimens.GapSmall` | 119 |
| 5.2 | `7.dp` | `Dimens.GapSmall` | 6 |
| 5.2 | `9.dp` | `Dimens.GapSmall` | 14 |
| 5.2 | `10.dp` | `Dimens.GapCompact` | 141 |
| 5.2 | `18.dp` (excl. `GuideCore.kt:238`) | `Dimens.GapWide` | 30 |
| 5.2 | `22.dp` | `Dimens.GapLarge` | 20 |
| 5.2 | `28.dp` (5 generic-rhythm sites only) | `Dimens.GapLarge` | 5 |

Gate: step 5.1 mints 5 new tokens and is Δ0 at every direct site (build pass only). Step 5.2
(358 rows across ~360 call sites) changes the rendered value by ≤4dp per site — worth its own
dedicated screenshot/visual-diff gate given the site count, the largest of any phase in this
backlog.

**Total rows addressed across all phases:** 5 (Phase 0) + 546 (Phase 1) + 69 (Phase 2) + 76
(Phase 3) + 187 (Phase 4) + 728 (Phase 5) = 1611 of 2273 audited literals (70.9%). The
remaining 662 rows stay as literals by design, each carrying a documented one-line reason in
§§2–6:

| family | rows staying literal | composition |
|---|---|---|
| spacing | 35 | 33 `INTENTIONAL` + 2 `SANCTIONED` |
| motion | 45 | 43 `INTENTIONAL` + 2 already-correct `MIGRATE-EXACT` (bare `ownTvTween()`, no action needed) |
| alpha | 107 | 91 `INTENTIONAL` + 16 `SANCTIONED` |
| size | 286 | 284 `INTENTIONAL` + 2 `SANCTIONED` |
| color | 134 | 122 `SANCTIONED` + 12 of the 15 findings (the 3 `0xFFEF4444` sites move to Phase 0) |
| corner | 55 | all `SANCTIONED` (the 2 `percent = 28` finding sites move to Phase 0) |
| **Total** | **662** | 35+45+107+286+134+55 = 662; 1611+662 = 2273 |

---

## 9. Closing recommendation — lint ratchet

This is a **recommendation only**; no lint code is proposed or written in this project, per
the spec.

Once the phases in §8 land, the value of this audit erodes the moment a new raw literal is
added back in — exactly the failure mode the existing `PluralsCandidate` lint check (fatal,
gating `lintStandardDebug` in CI per `CLAUDE.md`) prevents for hardcoded pluralizable strings.
The same pattern applies here: a **custom, fatal lint check** (or checks) that flags new raw
`.dp`/`.sp`/alpha-float/`tween`-duration literals in `ui/`, `features/`, and `player/` outside
an explicit allowlist, mirroring `PluralsCandidate`'s severity and scope discipline:

- **Scope precisely, not broadly.** `PluralsCandidate` doesn't flag every string — it flags
  the specific shape (a countable quantity concatenated with a literal string) that indicates
  the missing pattern. A design-token lint should be equally narrow: flag `N.dp`/`N.sp`
  literals inside the same trigger shapes this audit's extractors used
  (`padding(`/`.size(`/`.copy(alpha=`/`ownTvTween(`/`tween(`), not every numeric literal in
  the codebase (which would flag legitimate `INTENTIONAL` values too).
- **Carry the rubric's exemptions as the allowlist, not a suppression free-for-all.** The
  `SANCTIONED` list from `rubric.md` (SubtitleOverlay hand-tuned metrics, Player HUD black
  scrims, pictorial constants, FloatingRail mockup geometry, hairline 1–3dp roundings, percent
  pills, progress-bar half-heights, aspect-ratio-derived sizes) and the per-population
  `INTENTIONAL` reasons in §§2–5 are the seed list for exemptions — either by file/function
  annotation (`@Suppress` equivalent) or a lint-baseline file scoped to the exact sites already
  documented here, not a blanket directory exclusion.
- **Gate incrementally, after each phase, not all at once.** Turning the lint on fatally
  before Phase 5 lands would immediately fail CI on the 728 still-literal spacing rows.
  Recommended sequence: land Phases 0–5 first (or at minimum the zero-risk sub-steps of each),
  *then* flip the lint to fatal — same order the corner-radius consolidation and this backlog
  already establish, so the lint only ever blocks genuinely *new* drift, never re-litigates
  what this audit already classified `INTENTIONAL`/`SANCTIONED`.
- **One rule per family, not one monolith**, so a future contributor's lint failure message
  points at the specific scale that applies (e.g. "raw `.dp` literal in a `padding(...)` call
  — use `Dimens.Gap*`" vs. "raw alpha literal in `.copy(alpha=...)` — use
  `AlphaTokens.Alpha*`"), consistent with how this audit itself treated each family as a
  distinct population with its own scale and its own exemptions.
