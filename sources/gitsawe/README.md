# Licensed Gitsawe source

> Reviewed 2026-09-16. This is source data for app 2.3.2 / 72; see [project status](../../docs/PROJECT_STATUS.md) and the [rights record](../../docs/CONTENT_RIGHTS.md). Commands and runtime paths below are relative to the repository root.

This directory preserves the maintainer-supplied source material used to build
Sinq's Gitsawe data.

- `gitsawe-master.json` is the complete licensed master transcription.
- `gitsawe-structure.json` is its structural reference/template.
- `months/` contains the 13 self-contained Part 1 base files regenerated from
  the master. These remain the stable input to the app's daily-data importer.
- `parts/` contains self-contained, source-preserving splits for Parts 2, 3 and 5:
  movable readings, Sunday/mezmur cycles,
  and the Bahre Hasab reference table.
- `app/src/main/assets/content/gitsawe/daily-gitsawe.json` is the normalized,
  app-facing fixed-calendar dataset; it is generated data, not the source of
  truth.

Keep the master and structure files intact. Run `tools/split_gitsawe_months.py`
to refresh the 13 base files from Part 1, then run
`tools/import_gitsawe_months.py` to normalize them into Sinq's existing daily
data model. Source text and app-specific normalization therefore remain
auditable.

Run `tools/split_gitsawe_parts.py` whenever Parts 2, 3 or 5 of the master change.

App-facing normalization currently runs as:

- `tools/import_gitsawe_part2.py` → `movable-weekday-gitsawe.json`
- `tools/import_gitsawe_part3.py` → `sunday-cycle-gitsawe.json`
- `tools/import_gitsawe_part5.py` → `bahre-hasab-reference.json`

Part 2 is calendar-matched through `BahreHasab`. Part 3 now has automatic selection through `GitsaweRepository.selectSundayCycle`, `BahreHasab` and `SundayCycleCalendar`, including printed date spans and seasonal weeks. The ordered source collection remains preserved. Rubrics and ambiguous rules still require [liturgical review](../../docs/LITURGICAL_REVIEW.md); implemented selectors are not a blanket approval of every source rule.

There is no `import_gitsawe_part4.py` in this checkout. Mahlet is now built by `tools/build_mahlet.py` from the editorial merge, Tsige sources and remaining Gitsawe-derived orders. No dedicated Athanasius runtime asset or reader route was found in this checkout; the former README feature claim is not retained. Do not describe Part 4 as a current independent importer.

Source snapshot SHA-256:

- `gitsawe-master.json`: `46d4f62739935aa7089b61a2cad0541fa6a6575398a590aacb317407899877b5`
- `gitsawe-structure.json`: `5d7a860f4d6b7f1b50a8f47065fc056d16584fee0b503a2c44acf7b3554623a8`

The current [NOTICE](../../NOTICE) records the maintainer's Gitsawe transcription under **CC BY-NC-ND 4.0**. It does not inherit the repository's Apache-2.0 source-code license. This supersedes the former private-permission wording; underlying provenance and approval questions remain in the rights record.
