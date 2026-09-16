# Source inputs

Reviewed **2026-09-16** for app **2.3.2 / 72**. These files feed the generators under `tools/`; Android reads their outputs from `app/src/main/assets/content/`, not from this directory.

Preserve raw transcriptions and their provenance. Put scan corrections in explicit generator transformations, preferably with assertions. Mapping/configuration files such as `hours/hour_mapping.json` are editable inputs; the old blanket claim that nothing here may be edited was too broad.

| Directory | Current role | Tool |
|---|---|---|
| `books/` | 37 source JSON files used with curated/merged intermediates to build the church-book shelf | `build_books.py` |
| `sinksar/am/`, `sinksar/ge/` | Thirteen source months per Synaxarium edition | `build_sinksar.py` |
| `gitsawe/` | Preserved lectionary master, structure, month/part splits and supporting material | `split_gitsawe_*.py`, four `import_gitsawe_*.py` scripts |
| `hours/` | Built-in Agpeya psalm/gospel mapping | `extract_content.py` |
| `generated/` | Merged book intermediates | `merge_seatat.py`, `merge_tsige.py`, `merge_tselot_nebiyat.py` |
| `mahlet/` | Book-first editorial merge, raw snapshots and held-out review material | `build_mahlet.py` consumes the checked-in merge |
| `tsige/` | Seasonal Tsige orders and supporting source texts | `build_mahlet.py`, `merge_tsige.py` |

## External inputs and normal builds

- `extract_content.py` requires `../80-weahadu/data/am` for the legacy prayer-hour extraction.
- `extract_bible_editions.py` requires the sibling repository's `data/am-1980` and `data/gez-1980`; it bundles full Amharic Scripture and Ge'ez Psalms.
- `build_wudase.py` downloads `prayers.json` from its recorded upstream URL and therefore needs network access when regenerating that corpus.
- The original Mahlet merge script is external to this checkout; see [Mahlet source notes](mahlet/README.md). The app builder is included here.

A normal Gradle build uses committed runtime assets and does not require these regeneration dependencies. Do not run every generator as a build prerequisite.

After a source change, run the appropriate generator and `python3 tools/validate_content.py`, inspect the generated diff and run relevant content tests. Preserve section IDs and source alternatives. [Content structure](../docs/CONTENT_STRUCTURE.md) documents the contracts; [project status](../docs/PROJECT_STATUS.md) records measured output counts.

Licenses are recorded in [NOTICE](../NOTICE) and [CONTENT_RIGHTS.md](../docs/CONTENT_RIGHTS.md), independently of the source-code license.
