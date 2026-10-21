#!/usr/bin/env python3
"""
tools/check_font_coverage.py

Verifies Unicode glyph coverage of bundled TrueType fonts against all characters
appearing in the Sinq content corpus (app/src/main/assets/content/), including
the full range of Ge'ez numerals (፩-፼) and liturgical punctuation marks (፨ ፠ ፡ ። ፣ ፤ ፥ ፦ ፧).

Uses pure Python standard library (no external fonttools dependency required).
"""

import os
import sys
import json
import struct
import argparse

ETHIOPIC_NUMERALS = [chr(cp) for cp in range(0x1369, 0x137D)]
LITURGICAL_MARKS = [chr(cp) for cp in range(0x1360, 0x1369)]

def parse_font_cmap(ttf_path):
    """Parses format 4 and format 12 cmap subtables from a TrueType font file."""
    with open(ttf_path, 'rb') as f:
        data = f.read()

    if len(data) < 12:
        return set()

    sfnt_version, num_tables = struct.unpack('>IH', data[:6])
    tables = {}
    for i in range(num_tables):
        entry = data[12 + i * 16 : 28 + i * 16]
        tag, checksum, offset, length = struct.unpack('>4sIII', entry)
        tables[tag] = (offset, length)

    if b'cmap' not in tables:
        return set()

    cmap_offset, cmap_length = tables[b'cmap']
    version, num_cmap_tables = struct.unpack('>HH', data[cmap_offset : cmap_offset + 4])
    codepoints = set()

    for i in range(num_cmap_tables):
        p_id, e_id, sub_offset = struct.unpack('>HHI', data[cmap_offset + 4 + i * 8 : cmap_offset + 12 + i * 8])
        abs_sub_offset = cmap_offset + sub_offset
        if abs_sub_offset + 2 > len(data):
            continue
        fmt, = struct.unpack('>H', data[abs_sub_offset : abs_sub_offset + 2])

        if fmt == 4:
            if abs_sub_offset + 14 > len(data):
                continue
            length, lang, seg_count_x2, search_range, entry_selector, range_shift = struct.unpack(
                '>HHHHHH', data[abs_sub_offset + 2 : abs_sub_offset + 14]
            )
            seg_count = seg_count_x2 // 2
            end_code_offset = abs_sub_offset + 14
            end_codes = struct.unpack(f'>{seg_count}H', data[end_code_offset : end_code_offset + seg_count * 2])
            start_code_offset = end_code_offset + seg_count * 2 + 2
            start_codes = struct.unpack(f'>{seg_count}H', data[start_code_offset : start_code_offset + seg_count * 2])
            id_delta_offset = start_code_offset + seg_count * 2
            id_deltas = struct.unpack(f'>{seg_count}h', data[id_delta_offset : id_delta_offset + seg_count * 2])
            id_range_offset = id_delta_offset + seg_count * 2
            id_range_offsets = struct.unpack(f'>{seg_count}H', data[id_range_offset : id_range_offset + seg_count * 2])

            for idx, (start, end, delta, ro) in enumerate(zip(start_codes, end_codes, id_deltas, id_range_offsets)):
                if start == 0xFFFF:
                    continue
                for cp in range(start, end + 1):
                    if ro == 0:
                        glyph_id = (cp + delta) & 0xFFFF
                    else:
                        glyph_offset = id_range_offset + idx * 2 + ro + (cp - start) * 2
                        if glyph_offset + 2 <= len(data):
                            glyph_id, = struct.unpack('>H', data[glyph_offset : glyph_offset + 2])
                        else:
                            glyph_id = 0
                    if glyph_id > 0:
                        codepoints.add(cp)

        elif fmt == 12:
            if abs_sub_offset + 16 > len(data):
                continue
            fmt12, reserved, length, lang, num_groups = struct.unpack(
                '>HHIII', data[abs_sub_offset : abs_sub_offset + 16]
            )
            groups_offset = abs_sub_offset + 16
            for g in range(num_groups):
                rec = data[groups_offset + g * 12 : groups_offset + (g + 1) * 12]
                if len(rec) < 12:
                    break
                start_c, end_c, glyph = struct.unpack('>III', rec)
                for cp in range(start_c, end_c + 1):
                    codepoints.add(cp)

    return codepoints

def collect_corpus_characters(content_dir):
    """Collects every distinct character present across all JSON files in content_dir."""
    used_chars = set()

    def walk_json(obj):
        if isinstance(obj, str):
            used_chars.update(obj)
        elif isinstance(obj, list):
            for item in obj:
                walk_json(item)
        elif isinstance(obj, dict):
            for k, v in obj.items():
                used_chars.update(k)
                walk_json(v)

    file_count = 0
    for root, _, files in os.walk(content_dir):
        for f in files:
            if f.endswith('.json'):
                path = os.path.join(root, f)
                file_count += 1
                with open(path, 'r', encoding='utf-8') as fp:
                    try:
                        walk_json(json.load(fp))
                    except Exception as e:
                        print(f"Warning: could not parse {path}: {e}", file=sys.stderr)

    return used_chars, file_count

def main():
    parser = argparse.ArgumentParser(description="Verify font glyph coverage against Sinq corpus")
    parser.add_argument("--content-dir", default="app/src/main/assets/content", help="Path to assets/content directory")
    parser.add_argument("--font-dir", default="app/src/main/res/font", help="Path to res/font directory")
    args = parser.parse_args()

    if not os.path.isdir(args.content_dir):
        print(f"Error: Content directory '{args.content_dir}' not found.", file=sys.stderr)
        return 1

    if not os.path.isdir(args.font_dir):
        print(f"Error: Font directory '{args.font_dir}' not found.", file=sys.stderr)
        return 1

    print(f"Collecting characters from '{args.content_dir}'...")
    used_chars, file_count = collect_corpus_characters(args.content_dir)
    ethiopic_chars = {c for c in used_chars if '\u1200' <= c <= '\u139F' or '\u2D80' <= c <= '\u2DDF' or '\uAB00' <= c <= '\uAB2F'}
    print(f"Scanned {file_count} JSON files: {len(used_chars)} unique characters ({len(ethiopic_chars)} Ethiopic syllables/marks).")

    # Verify baseline requirements exist in content
    missing_nums_in_content = [c for c in ETHIOPIC_NUMERALS if c not in used_chars]
    missing_marks_in_content = [c for c in LITURGICAL_MARKS if c not in used_chars]
    print(f"Ge'ez numerals in corpus: {len(ETHIOPIC_NUMERALS) - len(missing_nums_in_content)} / {len(ETHIOPIC_NUMERALS)}")
    print(f"Liturgical marks in corpus: {len(LITURGICAL_MARKS) - len(missing_marks_in_content)} / {len(LITURGICAL_MARKS)}")

    # Audit each font in font_dir
    fonts = sorted([f for f in os.listdir(args.font_dir) if f.endswith('.ttf') or f.endswith('.otf')])
    has_error = False

    print("\n--- Auditing Bundled Fonts ---")
    for font_name in fonts:
        font_path = os.path.join(args.font_dir, font_name)
        cps = parse_font_cmap(font_path)

        # Check coverage
        missing_numerals = [c for c in ETHIOPIC_NUMERALS if ord(c) not in cps]
        missing_marks = [c for c in LITURGICAL_MARKS if ord(c) not in cps]
        missing_content_ethiopic = [c for c in ethiopic_chars if ord(c) not in cps]

        status = "PASS"
        notes = []

        # Baseline scripture and UI fonts must have 100% full coverage
        is_baseline_font = font_name in ["abyssinica_sil.ttf", "noto_sans_ethiopic.ttf", "ethiopic_abay_light.ttf"]
        if is_baseline_font:
            if missing_numerals or missing_marks or missing_content_ethiopic:
                status = "FAIL"
                has_error = True
                notes.append(f"Baseline font missing {len(missing_content_ethiopic)} content glyphs")
        else:
            if missing_numerals or missing_marks:
                status = "WARN"
                notes.append(f"Missing numerals/marks: {len(missing_numerals)} num, {len(missing_marks)} marks")
            if missing_content_ethiopic:
                notes.append(f"{len(missing_content_ethiopic)} Ethiopic chars missing (requires fallback)")

        print(f"[{status}] {font_name} ({len(cps)} glyphs)")
        print(f"  Ge'ez numerals: {len(ETHIOPIC_NUMERALS) - len(missing_numerals)}/{len(ETHIOPIC_NUMERALS)} supported")
        print(f"  Liturgical marks: {len(LITURGICAL_MARKS) - len(missing_marks)}/{len(LITURGICAL_MARKS)} supported")
        print(f"  Corpus Ethiopic coverage: {len(ethiopic_chars) - len(missing_content_ethiopic)}/{len(ethiopic_chars)} chars")
        if notes:
            for note in notes:
                print(f"  * {note}")

    if has_error:
        print("\nFont coverage check FAILED.")
        return 1
    else:
        print("\nFont coverage check PASSED successfully.")
        return 0

if __name__ == '__main__':
    sys.exit(main())
