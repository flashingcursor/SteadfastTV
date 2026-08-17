# Design-Token Audit — Design Spec

**Date:** 2026-08-17
**Status:** approved (user pre-approved the presented design verbatim)
**Deliverable:** report only — no remediation in this project.

## Goal

Produce a complete, provable inventory of hardcoded design values in the UI layer that should be
design tokens, classified and packaged as a remediation backlog. This is the same audit-first
motion as the 2026-08-12 design-compliance audit and the 2026-08-17 corner-radius consolidation:
deterministic extraction first, judgment second, directed sweeps later (separately approved).

## Scope

- **Code:** `app/src/main/java/tv/own/owntv/` — `ui/`, `features/`, `player/` only (`core/` and
  `di/` carry no design values). Main source set only; no tests, no resources.
- **Families:**
  1. **Spacing & padding** — raw `.dp` literals in `padding`/`Arrangement.spacedBy`/`offset`/
     gaps/insets vs the `Gap*` scale. Largest population.
  2. **Animation timing** — `ownTvTween(N)` duration literals, `tween(N)`, delay constants,
     spring specs at call sites.
  3. **Alphas & translucency** — `.copy(alpha = N)`, `Color.White/Black.copy(...)` rims/scrims/
     fills, literal alpha floats.
  4. **Sizes, elevations, text** — `Modifier.size/height/width(N.dp)` on components (icons,
     avatars, bars), `shadow(elevation = N.dp)`, `sp` literals outside the typography scale.
  5. **Residual re-checks** — colors (`Color(0x...)` literals vs the achromatic palette /
     sanctioned pictorial sets) and corner radii (vs the 2026-08-17 token scale), expected to be
     small after this week's sweeps.

## Method

1. **Extraction scripts** (Python, session scratchpad; not committed to the repo): one per
   family, each emitting a TSV inventory — `family, value, file, line, code snippet`. Regex-based
   over the scoped tree; the point is exhaustive, reproducible counts, not AST perfection.
   Multi-line call forms must be handled (grep with context where needed).
2. **Classification pass** over each inventory, bucket-by-bucket (group identical values first —
   judgment is per-population, not per-site):
   - `MIGRATE-EXACT` — literal equals an existing token's value; mechanical swap.
   - `MIGRATE-NEW` — cluster of near-identical values that wants a new named token (propose it).
   - `INTENTIONAL` — geometric/derived values (half-heights on progress bars, aspect-driven
     sizes) that must stay literal.
   - `SANCTIONED` — the settled design-language exceptions: SubtitleOverlay hand-tuned metrics,
     player HUD plain-black scrims, pictorial constants (WeatherGlyph, HudPictorial, GenreColor,
     avatar palette/art), mockup-literal rail geometry constants, hairline 1–3dp roundings,
     percent pills. These are re-affirmed, not re-litigated.
3. **Token-scheme proposals** per family where `MIGRATE-NEW` clusters exist — e.g. whether the
   `Gap*` scale needs intermediate steps; named motion tokens (`MotionFast/MotionMedium/…`) for
   the 140/160/220 tween family; named alpha tokens for scrim/rim/fill translucency.

## Deliverable

`docs/superpowers/reports/2026-08-17-token-audit.md`, containing per family:

- Total literal count, distinct-value histogram, classification table with counts.
- Every `MIGRATE-*` site listed (file:line) or, for very large uniform populations, listed by
  file with per-file counts.
- Proposed token additions (names, values, home: `Dimens.kt` / theme files).
- A **phased remediation backlog**: each phase a directed sweep sized like the radius migration
  (one mapping table, one gate), ordered by visual payoff vs churn. Phases are proposals only —
  each needs separate user approval before execution.
- Closing recommendation: a lint-ratchet follow-up (custom checks in the spirit of the fatal
  `PluralsCandidate`) to keep new literals out once families are migrated — recommendation only,
  no lint code in this project.

## Constraints & invariants

- **No production code changes.** The only repo artifact is the report (and this spec). `[skip ci]`
  applies to the report commit.
- Sanctioned exceptions from the design-language rulings are classified as such, never flagged as
  defects.
- Counts in the report must come from the extraction scripts (reproducible), not estimates.
- Git hygiene as always: explicit-path staging only.

## Risks

- **Regex misses on exotic call forms** (multi-line, named-argument permutations). Mitigation:
  each script's residual check greps the broad pattern and reports what the narrow pattern didn't
  capture; unexplained residuals go in the report's "unclassified" appendix rather than being
  dropped silently.
- **Judgment drift on INTENTIONAL vs MIGRATE.** Mitigation: classification is per-population with
  the written rulings as the lens, and every INTENTIONAL bucket carries its one-line reason in
  the report.
