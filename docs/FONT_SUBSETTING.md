# Font subsetting

> Reviewed 2026-09-16. The dry run still stops because `fontTools` is unavailable in this environment; no font assets were changed. See [project status](PROJECT_STATUS.md) for verification scope.

`tools/subset_fonts.py` is available for optional font-size optimization. It requires `fontTools` and processes every `.ttf` in `app/src/main/res/font/`, including the newer Waldba font. No subsetting was performed during this documentation audit.

## What the fonts actually carry

The following glyph counts are retained from the original analysis; they were not remeasured in this audit. File sizes still match those original five assets approximately:

| Font | Size | Glyphs | Needed | Droppable |
|---|---:|---:|---:|---:|
| `zemenay.ttf` | 383 KB | 2,740 | 546 | **2,194** |
| `noto_sans_ethiopic.ttf` | 1,116 KB | 860 | 580 | 280 |
| `abyssinica_sil.ttf` | 262 KB | 862 | 629 | 233 |
| `ethiopic_abay_light.ttf` | 158 KB | 735 | 603 | 132 |
| `bela_bereka.ttf` | 77 KB | 385 | 385 | 0 |

Zemenay is the outlier: it carries 432 Latin-Extended glyphs, 176 IPA, and
~1,470 further symbols that no Sinq screen will ever show. Bela Bereka is
already tight and would not benefit.

Waldba is also bundled: **628,580 bytes (about 614 KiB)**. It was not part of the original glyph-count analysis, so no droppable-glyph count is asserted for it. Review it as well before applying the tool. Licenses and reader-font roles are recorded in [CONTENT_RIGHTS.md](CONTENT_RIGHTS.md).

## The rule that matters

**Keep the entire Ethiopic block, not merely the characters in today's content.**

Subsetting to observed codepoints would be a trap: users type their own text —
profile name, Christian name, custom hour names, habit names — and any Ethiopic
character they choose must render. `tools/subset_fonts.py` preserves
U+1200–U+137F and the Ethiopic Extended blocks whole, plus ASCII and general
punctuation for dates and numerals, and drops the rest.

## Running it

```bash
pip install fonttools
python3 tools/subset_fonts.py --dry-run   # report sizes, change nothing
python3 tools/subset_fonts.py             # rewrite the fonts in place
```

Then rebuild and **look at every reading font on a device**. A subsetting
mistake shows up as missing glyphs (tofu), which no unit test will catch.
