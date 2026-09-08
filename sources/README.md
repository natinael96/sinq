# sources/

The upstream this repo builds from. Nothing here is read at runtime — these are
inputs to `tools/`, which write into `app/src/main/assets/content/`.

Nothing here is edited by hand either. When a scan is wrong, the fix goes into
the generator as an asserted swap, so it is re-applied every time the source is
re-imported and fails loudly if the upstream changes underneath it.

| Directory | What it is | Built by |
|---|---|---|
| `books/` | 93 scanned church books — the መልክእ hymns, ድርሳናት, ገድላት, ቅዳሴ, the chant books of ቅዱስ ያሬድ, and three copies of መጽሐፈ ሰዓታት | `build_books.py` |
| `sinksar/am/` | መጽሐፈ ስንክሳር በአማርኛ — 13 months | `build_sinksar.py` |
| `sinksar/ge/` | መጽሐፈ ስንክሳር በግእዝ — 13 months | `build_sinksar.py` |
| `gitsawe/` | the ግጻዌ lectionary masters, and the months and parts split out of them | `split_gitsawe_*.py`, `import_gitsawe_*.py` |
| `hours/` | `hour_mapping.json` — which psalms and gospels each hour of the Agpeya appoints | `extract_content.py` |
| `generated/` | intermediates one tool writes for another, e.g. the merged መጽሐፈ ሰዓታት | `merge_seatat.py` |

`extract_content.py` also needs the sibling repo `../80-weahadu` for the
scripture text; that one is not vendored here.

The scans in `books/` and `sinksar/` were made from available PDF scans of the
printed books by the Sinq maintainer. See NOTICE for the terms they are
released under.
