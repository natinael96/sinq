#!/usr/bin/env python3
"""Subset the bundled fonts to the scripts Sinq actually renders.

WHY: Zemenay ships 2,740 glyphs of which the app can render 546 — the rest is
Latin Extended, IPA and symbol coverage that no Sinq screen will ever show.

WHAT IS KEPT — and this is the important part:

  * The **entire** Ethiopic block (U+1200–U+137F) and the Ethiopic Extended
    blocks, NOT merely the characters present in today's bundled content.
    Users type their own text — profile names, custom hour names, habit names —
    and subsetting to observed codepoints would render an unlucky name as
    tofu. The whole script stays.
  * ASCII, Latin-1 punctuation, and general punctuation, for the Gregorian
    dates, verse numbers and the few Latin words in the UI.

Everything else is dropped.

Requires fontTools:  pip install fonttools
Run:                 python3 tools/subset_fonts.py [--dry-run]

After running, rebuild and check the readers in every font before committing —
a subsetting mistake shows up as missing glyphs, which no test will catch.
"""
from __future__ import annotations

import argparse
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
FONT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "font")

# Unicode ranges to preserve, as (first, last) inclusive.
KEEP_RANGES = [
    (0x0020, 0x007E),   # ASCII printable
    (0x00A0, 0x00FF),   # Latin-1 supplement (degree signs, guillemets, ×)
    (0x2000, 0x206F),   # general punctuation — en dash, ·, ellipsis, quotes
    (0x20A0, 0x20BF),   # currency (harmless, tiny)
    (0x1200, 0x137F),   # Ethiopic — the whole block, deliberately
    (0x1380, 0x139F),   # Ethiopic Supplement
    (0x2D80, 0x2DDF),   # Ethiopic Extended
    (0xAB00, 0xAB2F),   # Ethiopic Extended-A
    (0x1E7E0, 0x1E7FF), # Ethiopic Extended-B
]


def unicodes() -> set[int]:
    out: set[int] = set()
    for lo, hi in KEEP_RANGES:
        out.update(range(lo, hi + 1))
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true", help="report sizes without writing")
    args = ap.parse_args()

    try:
        from fontTools import subset
        from fontTools.ttLib import TTFont
    except ImportError:
        print(
            "fontTools is required:  pip install fonttools\n"
            "Nothing was changed.",
            file=sys.stderr,
        )
        return 2

    keep = unicodes()
    total_before = total_after = 0

    for name in sorted(os.listdir(FONT_DIR)):
        if not name.endswith(".ttf"):
            continue
        path = os.path.join(FONT_DIR, name)
        before = os.path.getsize(path)
        total_before += before

        font = TTFont(path)
        have = set()
        for table in font["cmap"].tables:
            have.update(table.cmap.keys())
        target = have & keep

        options = subset.Options()
        options.layout_features = ["*"]     # keep shaping — Ethiopic needs its marks
        options.name_IDs = ["*"]            # keep the licence and family names
        options.notdef_outline = True
        options.recalc_bounds = True

        subsetter = subset.Subsetter(options=options)
        subsetter.populate(unicodes=target)
        subsetter.subset(font)

        if args.dry_run:
            tmp = path + ".tmp"
            font.save(tmp)
            after = os.path.getsize(tmp)
            os.remove(tmp)
        else:
            font.save(path)
            after = os.path.getsize(path)
        total_after += after

        print(
            f"  {name:<28} {before / 1024:6.0f} KB -> {after / 1024:6.0f} KB   "
            f"({len(have)} -> {len(target)} glyphs)"
        )

    saved = (total_before - total_after) / 1024
    verb = "would save" if args.dry_run else "saved"
    print(f"\n{verb} {saved:.0f} KB "
          f"({total_before / 1024:.0f} KB -> {total_after / 1024:.0f} KB)")
    if not args.dry_run:
        print("Rebuild and check every reading font on a device before committing.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
