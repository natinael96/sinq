# Sinq content structure

Reviewed **2026-09-16** against the bundled assets for **2.3.2 / 72**. This replaces the pre-implementation section skeleton. For provenance see [sources](../sources/README.md) and [rights](CONTENT_RIGHTS.md).

## Source and runtime boundaries

- `sources/`: source transcriptions, mappings, editorial merges and generated intermediates; not loaded by the Android app.
- `tools/`: corpus-specific extraction, merging, normalization and validation.
- `app/src/main/assets/content/`: runtime JSON assets consumed by repositories.
- `tools/section-ids.json`: compatibility snapshot for permanent core section IDs.
- `docs/CONTENT_TRACKER.csv`: historical manual-entry skeleton; it is not a current completeness report.

The built-in hours retain the Agpeya psalm/gospel mapping. The fuller Ethiopian መጽሐፈ ሰዓታት is a separate library book; its presence does not mean all its prayers have been inserted into the eight built-in hours.

## Hours and Psalter

`manifest.json` declares `contentVersion` and eight `{id, file}` entries. Each hour file contains metadata and ordered sections with stable IDs and verse text. Exact field contracts are in `model/Models.kt` and the generated JSON.

| Hour ID | Bundled name | Sections | Verse entries |
|---|---|---:|---:|
| `morning` | ጸሎተ ነግህ | 21 | 223 |
| `terce` | ጸሎተ ሠለስት | 15 | 166 |
| `sext` | ጸሎተ ቀትር | 14 | 154 |
| `none` | ጸሎተ ተሰዓት | 14 | 154 |
| `vespers` | ጸሎተ ሰርክ | 14 | 120 |
| `compline` | ጸሎተ ንዋም | 14 | 126 |
| `midnight` | ጸሎተ መንፈቀ ሌሊት | 56 | 481 |
| `veil` | ሌሊት 9 ሰዓት | 33 | 328 |

There are **181 hour sections** plus **150 Psalter sections**, totaling **331 permanent section IDs** across nine core files. Verse-entry totals count appearances inside each hour, including repeated passages; they are not counts of unique Bible verses.

Examples of actual IDs include `morning_ps1`, `terce_ps50`, `midnight_watch1_ps3` and `veil_ps4`. Use the snapshot and actual assets rather than inventing names from the old manual-entry template. Midnight watch labels and Psalm 118 stanza boundaries are generated; Psalm 118 contains 22 eight-verse stanzas.

`tools/extract_content.py` reads `sources/hours/hour_mapping.json` and the sibling `80-weahadu/data/am` corpus. The separate Scripture generator uses newer edition directories. Preserve this distinction when reproducing the hours.

## Other runtime collections

| Asset area | Structure and consumer |
|---|---|
| `bible/` | Catalog/canon plus edition book data; `ScriptureRepository`. Full `am-1980`, Psalms-only `gez-1980`. Source metadata, headings and noninteger verse identifiers are preserved. |
| `gitsawe/` | Fixed daily offices, movable weekdays, seasonal/monthly records, Sunday cycle and reference data; `GitsaweRepository`. Calendar selectors and provenance are distinct from reading text. |
| `sinksar/` | Amharic and Ge'ez editions, 366 dates each; `SynaxariumRepository`. Day entries retain hymns and any source readings. |
| `wudase/` | Daily opening, weekday portions and appended prayers in Amharic/Ge'ez; `WudaseRepository`. |
| `books/` | Curated shelf index and books with chapters/blocks; `BookRepository`. |
| `mahlet/` | Index plus `m0.json` through `m13.json`; orders, parts and alternative editions; `MahletRepository`. Month 0 is undated material. |
| `reading/` | Generated reading plans and chapter assignments; `ReadingPlanRepository`. User progress is stored separately. |
| `nisiha/`, other devotional assets | Preparation and supporting reference data; consult the corresponding Kotlin models/repositories. |

See [project status](PROJECT_STATUS.md#content-inventory) for measured corpus counts. Source-book, canonical-book, feast, order and edition counts describe different units and must not be interchanged.

## Editing and validation rules

1. Identify the source and generator for the corpus before changing text.
2. Preserve original source snapshots and document explicit corrections in transformation logic. Editable mapping/configuration files are distinct from verbatim source transcriptions.
3. Keep unknown dates/forms unset and alternatives separate; do not infer sacred text or calendar rules to fill a gap.
4. Regenerate only the affected assets with the required source dependencies available.
5. Run `python3 tools/validate_content.py` and relevant Kotlin content/selection tests.
6. Review the generated diff, source references and stable IDs. An intentional compatibility change needs migration handling, not simply a new ID snapshot.
7. Record human liturgical approval separately in [LITURGICAL_REVIEW.md](LITURGICAL_REVIEW.md).
