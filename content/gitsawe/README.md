# Licensed Gitsawe source

This directory preserves the maintainer-supplied source material used to build
Sinq's Gitsawe data.

- `gitsawe-master.json` is the complete licensed master transcription.
- `gitsawe-structure.json` is its structural reference/template.
- `months/` contains the 13 self-contained Part 1 base files regenerated from
  the master. These remain the stable input to the app's daily-data importer.
- `parts/` contains self-contained, source-preserving splits for Parts 2–5:
  movable readings, Sunday/mezmur cycles, the Athanasius funeral lectionary,
  and the Bahre Hasab reference table.
- `app/src/main/assets/content/gitsawe/daily-gitsawe.json` is the normalized,
  app-facing fixed-calendar dataset; it is generated data, not the source of
  truth.

Keep the master and structure files intact. Run `tools/split_gitsawe_months.py`
to refresh the 13 base files from Part 1, then run
`tools/import_gitsawe_months.py` to normalize them into Sinq's existing daily
data model. Source text and app-specific normalization therefore remain
auditable.

Run `tools/split_gitsawe_parts.py` whenever Parts 2–5 of the master change.

App-facing normalization currently runs as:

- `tools/import_gitsawe_part2.py` → `movable-weekday-gitsawe.json`
- `tools/import_gitsawe_part3.py` → `sunday-cycle-gitsawe.json`
- `tools/import_gitsawe_part4.py` → `athanasius.json`
- `tools/import_gitsawe_part5.py` → `bahre-hasab-reference.json`

Part 2 is calendar-matched through `BahreHasab`. Part 3 is exposed by the
repository as an ordered, lossless collection; automatic selection is added
only after each printed period rule is classified, because the part mixes fixed
dates, movable seasons, continuations, and rubrics.

Source snapshot SHA-256:

- `gitsawe-master.json`: `46d4f62739935aa7089b61a2cad0541fa6a6575398a590aacb317407899877b5`
- `gitsawe-structure.json`: `5d7a860f4d6b7f1b50a8f47065fc056d16584fee0b503a2c44acf7b3554623a8`

These files are separately licensed to the Sinq maintainer. They do not inherit
the repository's Apache-2.0 source-code licence; forks and redistributors must
obtain their own permission.
