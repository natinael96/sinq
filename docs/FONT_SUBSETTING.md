# Font subsetting

Measured, not yet applied — `tools/subset_fonts.py` requires `fontTools`, which
was not available on the machine where this was analysed.

## What the fonts actually carry

Glyph counts from each font's `cmap`, against what Sinq can render:

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
