#!/usr/bin/env python3
"""Normalize all master Part 3 rows without guessing their calendar selector."""

import json
from pathlib import Path

from import_gitsawe_months import convert_service


ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "content/gitsawe/parts/03-sunday-cycle-and-mezmur.json"
OUT = ROOT / "app/src/main/assets/content/gitsawe/sunday-cycle-gitsawe.json"

# Only rules whose printed heading gives an unambiguous selector are activated.
# Continuation rows share their parent selector. Unlisted rows remain browseable.
FIXED = {
    1: (1, 1, 6), 2: (1, 7, 7), 3: (1, 7, 13), 4: (1, 16, 16),
    5: (1, 17, 23), 6: (1, 24, 24), 7: (1, 25, 25),
    14: (2, 17, 17), 15: (2, 12, 12), 16: (2, 21, 21),
    27: (4, 23, 23), 71: (10, 12, 12), 72: (10, 17, 17),
    73: (10, 17, 24), 74: (10, 24, 24), 84: (12, 6, 6),
}
MOVABLE = {
    39: ("abiyTsom", 1), 40: ("abiyTsom", 1),
    41: ("abiyTsom", 2), 42: ("abiyTsom", 3), 43: ("abiyTsom", 4),
    44: ("abiyTsom", 5), 45: ("abiyTsom", 6), 46: ("abiyTsom", 7),
    **{i: ("abiyTsom", 8) for i in range(47, 56)},
    57: ("tnsae", 1), 58: ("tnsae", 2), 59: ("tnsae", 2),
    60: ("tnsae", 3), 61: ("tnsae", 3), 62: ("tnsae", 4),
    63: ("tnsae", 5), 64: ("tnsae", 6), 65: ("tnsae", 7),
    66: ("tnsae", 8), 67: ("tnsae", 9), 68: ("tnsae", 10),
    69: ("tnsae", 11), 70: ("tnsae", 12),
}


def main():
    sections = json.loads(SOURCE.read_text(encoding="utf-8"))["part"]["extracted_sections"]
    if len(sections) != 91:
        raise SystemExit("Part 3 must contain exactly 91 ordered sections")
    entries = []
    for index, section in enumerate(sections, 1):
        readings = dict(section["readings"])
        # The Resurrection row prints three alternative Gospels together.
        for alt_index, gospel in enumerate(readings.pop("ወንጌላት", []), 1):
            readings[f"ወንጌል_ዓዲ_{alt_index}"] = gospel
        morning_psalm = readings.pop("ዘነግህ ምስባክ", None)
        rubrics = []
        for key in ("note", "ዘቅዳሴ", "ምንባብ"):
            value = readings.pop(key, None)
            if isinstance(value, str) and value.strip():
                rubrics.append(value.strip())
        title = next((section.get(key) for key in ("period", "section_title", "heading_as_printed", "hymn") if section.get(key)), f"Part 3 section {index}")
        entry = {
            "index": index,
            "title": title.strip(),
            "period": section.get("period"),
            "heading": section.get("heading_as_printed") or section.get("section_title"),
            "mezmur": section.get("hymn"),
            "gize": section.get("gize"),
            "rubric": " ".join(rubrics) or None,
            "reviewNotes": section.get("review_notes"),
            "sourcePages": section["source_scan_pages"],
            "kidassie": convert_service(readings) if readings else None,
            "negh": convert_service({"ምስባክ": morning_psalm}) if morning_psalm else None,
        }
        if index in FIXED:
            entry["monthNum"], entry["fromDay"], entry["toDay"] = FIXED[index]
        if index in MOVABLE:
            entry["season"], entry["week"] = MOVABLE[index]
        entries.append({key: value for key, value in entry.items() if value is not None})
    OUT.write_text(json.dumps(entries, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"Built {len(entries)} ordered Sunday-cycle entries from master Part 3")


if __name__ == "__main__":
    main()
