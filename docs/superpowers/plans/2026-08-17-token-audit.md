# Design-Token Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce `docs/superpowers/reports/2026-08-17-token-audit.md` — a complete, script-backed inventory of hardcoded design values in the UI layer, classified (MIGRATE-EXACT / MIGRATE-NEW / INTENTIONAL / SANCTIONED) with proposed tokens and a phased remediation backlog.

**Architecture:** Extraction scripts (Python, in the audit workspace, never committed) emit per-family TSV inventories; classification passes work population-by-population over those TSVs; the report is assembled from the classified sections. No production code changes — the report (and spec) are the only repo artifacts.

**Tech Stack:** Python 3 (regex extraction), the existing design rulings as the classification rubric.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-17-token-audit-design.md` governs. Report only — ZERO production code changes.
- Scope: `app/src/main/java/tv/own/owntv/` limited to `ui/`, `features/`, `player/`. Main source set only.
- Counts in the report MUST be script-reproduced, never estimated. Unexplained extraction residuals go to an "unclassified" appendix, never silently dropped.
- Sanctioned exceptions (design-language rulings; listed in Task 2 rubric) are classified SANCTIONED, not flagged as defects.
- Existing token values for exact-match tests: corners XSmall 4 / Small 6 / Medium 8 / CardCorner 10 / PanelCorner 11 / Large 12; gaps Tiny 4 / Small 8 / Medium 16 / Large 24; RailTopGap 8; FocusBorderWidth 2.5; motion durations currently literal (140/160/220 via ownTvTween; default ownTvTween() = check its default in ui/theme).
- Work happens in the plan's SDD workspace scratch dir (git-ignored). Report committed with `[skip ci]`. Git hygiene: explicit-path staging only; NEVER `git add -A`/`commit -am`.

---

### Task 1: Extraction scripts + raw inventories

**Files:**
- Create (workspace, NOT committed): `<workspace>/extract.py`, `<workspace>/inventory-{spacing,motion,alpha,size,color,corner}.tsv`, `<workspace>/inventory-summary.txt`

**Interfaces — Produces:** TSV rows `family<TAB>value<TAB>path<TAB>line<TAB>snippet` (snippet = the trimmed source line, max 160 chars). `inventory-summary.txt` = per-family total + distinct-value histogram. Tasks 2–4 consume these files verbatim.

- [ ] **Step 1:** Write `extract.py` scanning only `app/src/main/java/tv/own/owntv/{ui,features,player}/**/*.kt`, with one extractor per family:
  - `spacing`: `.dp` literals appearing inside `padding(...)`, `Arrangement.spacedBy(...)`, `offset(...)`, `PaddingValues(...)`, `Spacer(...)`/`width|height(N.dp)` where the receiver line also contains `Spacer` — capture the numeric value. Exclude lines already using `Dimens.`.
  - `motion`: numeric args of `ownTvTween(N)`, `tween(N`, `delayMillis = N`, `delay(N)` inside composables, `animationSpec = ... N` literals; also bare `ownTvTween()` (defaulted) counted separately.
  - `alpha`: `alpha = 0.N`, `.copy(alpha = 0.N`, `Color.White.copy(...)`/`Color.Black.copy(...)` capturing the float.
  - `size`: `Modifier.size(N.dp`, `.height(N.dp`, `.width(N.dp` (excluding rows already counted as spacing/Spacer), `shadow(elevation = N.dp`, `N.sp` literals.
  - `color`: `Color(0x...)` literals (8-digit) — full list, no filtering.
  - `corner`: `RoundedCornerShape(N.dp|N)` literals and `CornerRadius(` literals (post-consolidation residual check).
  Each extractor ALSO runs a broad companion grep (e.g. any `.dp` literal) and writes the count difference to the summary as `residual-unexplained` per family.
- [ ] **Step 2:** Run it; verify spot-truth on 5 known sites (rail padding 9dp, scrim 0.45f, tween 220, avatar 48dp, LiveBadge 0xCCDC3232) appear in the right TSVs. Write `inventory-summary.txt`.
- [ ] **Step 3:** Report back totals per family (no commit — workspace artifacts only).

### Task 2: Classify spacing + build the rubric appendix

**Files:**
- Create (workspace): `<workspace>/section-spacing.md`, `<workspace>/rubric.md`

**Interfaces — Consumes:** `inventory-spacing.tsv`. **Produces:** `rubric.md` (the classification rubric all later tasks reuse verbatim) and `section-spacing.md` (report-ready).

- [ ] **Step 1:** Write `rubric.md`: the four buckets with definitions from the spec, plus the sanctioned list verbatim: SubtitleOverlay hand-tuned 24/30/Medium metrics; player HUD plain-black scrims; pictorial constants (WeatherGlyph, HudPictorial, GenreColor, OwnTVAvatar palette/canvas art); FloatingRail mockup-literal geometry constants (file-local vals); hairline 1–3dp roundings; percent pills / 999.dp; progress-bar half-height radii; aspect-ratio-derived sizes.
- [ ] **Step 2:** Group `inventory-spacing.tsv` by value; classify each population: values ∈ {4,8,16,24} → MIGRATE-EXACT (Gap*); adjacent clusters (e.g. 6, 10, 12, 14, 18, 20) → propose MIGRATE-NEW tokens or snap-to-Gap recommendations with per-value site counts; one-off geometric values → INTENTIONAL with one-line reasons.
- [ ] **Step 3:** Write `section-spacing.md`: histogram table, classification table with counts, proposed token additions (names + values + rationale), the family's remediation-sweep description (mapping table like the radius sweep), and file:line lists for MIGRATE populations (per-file counts acceptable above 30 sites per value).

### Task 3: Classify motion + alpha

**Files:**
- Create (workspace): `<workspace>/section-motion.md`, `<workspace>/section-alpha.md`

**Interfaces — Consumes:** `inventory-motion.tsv`, `inventory-alpha.tsv`, `rubric.md`.

- [ ] **Step 1 (motion):** Histogram the durations. Propose named motion tokens in `ui/theme` (e.g. `MotionQuick = 140`, `MotionFocus = 160`, `MotionShell = 220` — final names/values from the actual histogram; the ownTvTween() default counts as its own row). Classify every literal against them; springs and one-off choreography (e.g. marquee) judged per population.
- [ ] **Step 2 (alpha):** Histogram alphas. Cluster into semantic roles observed in code (scrim ~0.45, panel fill 0.82, white rims 0.12/0.14, separator 0.25, ring 0.7, disabled/hint tiers) and propose named alpha tokens; classify. HUD plain-black scrims = SANCTIONED per rubric.
- [ ] **Step 3:** Write both section files, same table format as Task 2.

### Task 4: Classify sizes/elevation/text + color & corner residuals

**Files:**
- Create (workspace): `<workspace>/section-size.md`, `<workspace>/section-residuals.md`

**Interfaces — Consumes:** `inventory-size.tsv`, `inventory-color.tsv`, `inventory-corner.tsv`, `rubric.md`.

- [ ] **Step 1 (size):** Separate component sizes (icons, avatars, bars, tiles) from layout dimensions (panel widths already in Dimens). Flag repeated magnitudes (24dp icons, 48dp avatar/touch targets, elevations 10/14) as MIGRATE-NEW candidates; aspect-derived and one-off geometry INTENTIONAL. Stray `.sp` literals: compare against the typography scale; SubtitleOverlay = SANCTIONED.
- [ ] **Step 2 (residuals):** Colors: classify every `Color(0x...)` literal — expected mostly SANCTIONED pictorial or already-neutralized theme files; anything else = finding. Corners: expect only the documented exceptions (hairlines, pills, avatar art, EpgScreen 4dp dot); anything else = finding.
- [ ] **Step 3:** Write both section files.

### Task 5: Assemble the report + final review + commit

**Files:**
- Create: `docs/superpowers/reports/2026-08-17-token-audit.md`

- [ ] **Step 1:** Assemble: intro (scope, method, reproducibility note pointing at the spec), the five family sections from the workspace, the unclassified appendix (all `residual-unexplained` counts with explanations or "uninvestigated" flags), the **phased backlog** (each phase = one directed sweep with its mapping table, ordered by visual payoff vs churn — recommend order: spacing exact-matches → motion tokens → alphas → sizes), and the closing lint-ratchet recommendation (custom lint in the spirit of the fatal PluralsCandidate; recommendation only).
- [ ] **Step 2:** Cross-check every count in the report against `inventory-summary.txt`; fix mismatches.
- [ ] **Step 3:** Commit the report alone: `git add docs/superpowers/reports/2026-08-17-token-audit.md && git commit -m "docs: design-token audit report [skip ci]"`.

## Self-Review

1. **Spec coverage:** families 1–5 → Tasks 2/3/3/4/4; method §1 → Task 1; method §2 rubric → Task 2 Step 1; method §3 proposals → Tasks 2–4; deliverable structure + backlog + lint reco → Task 5; constraints (no code, reproducible counts, residual appendix) → Global Constraints + Task 1 Step 1 + Task 5 Step 1. No gaps.
2. **Placeholder scan:** token names in Tasks 3–4 are explicitly "final names/values from the actual histogram" — that is the task's judgment output, not a placeholder; extraction patterns are concrete. Clean.
3. **Type consistency:** TSV schema defined once in Task 1 and consumed by name in Tasks 2–4; rubric produced in Task 2, consumed in 3–4. Consistent.
