#!/usr/bin/env python3
"""Normalize all master Part 3 rows and attach the calendar selector each rule prints."""

import json
from pathlib import Path

from import_gitsawe_months import convert_service


ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "content/gitsawe/parts/03-sunday-cycle-and-mezmur.json"
OUT = ROOT / "app/src/main/assets/content/gitsawe/sunday-cycle-gitsawe.json"

# Every row carries the selector its printed heading implies. Fixed dates are
# (month, fromDay, toDay); SPANS adds further dates the same rubric names.
# Seasons are computus keys (BahreHasab) or fixed-anchored Sunday seasons
# (SundayCycleCalendar) — see that file for how each week number is counted.
# Continuation rows share their parent selector.
FIXED = {
    1: (1, 1, 6), 2: (1, 7, 7), 3: (1, 7, 13), 4: (1, 16, 16),
    5: (1, 17, 23), 6: (1, 24, 24), 7: (1, 25, 25),
    14: (2, 17, 17), 15: (2, 12, 12), 16: (2, 21, 21),
    17: (2, 3, 3),                       # ዕንባቆም — "ጥቅምት ወይም ኅዳር ፫" (see SPANS)
    # Scan 390 prints "ለታኅሣሥ ፳፰ ቀን መርዓዊ" (አማኑኤል's monthly day); the master's ፳፫ is a misread.
    27: (4, 28, 28),
    31: (5, 11, 11),                     # ጥምቀት on a Sunday: read the day's own
    35: (6, 8, 8),                       # ፮ተኛ — ስምዖን on a Sunday
    71: (10, 12, 12), 72: (10, 17, 17),
    73: (10, 17, 24), 74: (10, 24, 24),
    77: (11, 5, 5),                      # ፫ተኛ — ሐዋርያት on a Sunday
    80: (11, 19, 19),                    # ፮ተኛ — ቂርቆስ on a Sunday
    84: (12, 6, 6),                      # ፲ኛ — ነሐሴ ፮ on a Sunday
    86: (12, 13, 13),                    # ደብረ ታቦር on a Sunday
    91: (13, 1, 6),                      # ፲፭ኛ — a Sunday in ጳጉሜን
}
SPANS = {
    17: [(3, 3, 3)],
}
MOVABLE = {
    # ዘመነ ጽጌ, መስከረም ፳፮ – ኅዳር ፭: six numbered Sundays.
    **{i: ("tsige", i - 7) for i in range(8, 14)},
    # ዘመነ አስተምህሮ, ኅዳር ፮ – ታኅሣሥ ፮: five numbered Sundays (19 continues 18).
    18: ("astemhro", 1), 19: ("astemhro", 1), 20: ("astemhro", 2),
    21: ("astemhro", 3), 22: ("astemhro", 4), 23: ("astemhro", 5),
    24: ("sibket", None), 25: ("birhan", None), 26: ("nolawi", None),
    # ዘመነ ልደት: week 0 is ልደት itself; the rest are the printed ordinals.
    28: ("lidet", 0), 29: ("lidet", 1), 30: ("lidet", 2), 32: ("lidet", 3),
    33: ("lidet", 4), 34: ("lidet", 5), 36: ("lidet", 7), 37: ("lidet", 8), 38: ("lidet", 9),
    39: ("abiyTsom", 1), 40: ("abiyTsom", 1),
    41: ("abiyTsom", 2), 42: ("abiyTsom", 3), 43: ("abiyTsom", 4),
    44: ("abiyTsom", 5), 45: ("abiyTsom", 6), 46: ("abiyTsom", 7),
    **{i: ("abiyTsom", 8) for i in range(47, 56)},
    56: ("holy_saturday", None),
    57: ("tnsae", 1), 58: ("tnsae", 2), 59: ("tnsae", 2),
    60: ("tnsae", 3), 61: ("tnsae", 3), 62: ("tnsae", 4),
    63: ("tnsae", 5), 64: ("tnsae", 6), 65: ("tnsae", 7),
    66: ("tnsae", 8), 67: ("tnsae", 9), 68: ("tnsae", 10),
    69: ("tnsae", 11), 70: ("tnsae", 12),
    # ዘመነ ክረምት: the book's printed hymn ordinal ፩ኛ–፲፬ኛ (90 continues 89).
    75: ("kremt", 1), 76: ("kremt", 2), 78: ("kremt", 4), 79: ("kremt", 5),
    81: ("kremt", 7), 82: ("kremt", 8), 83: ("kremt", 9), 85: ("kremt", 11),
    87: ("kremt", 12), 88: ("kremt", 13), 89: ("kremt", 14), 90: ("kremt", 14),
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
        if index in SPANS:
            entry["spans"] = [
                {"monthNum": m, "fromDay": a, "toDay": b} for m, a, b in SPANS[index]
            ]
        if index in MOVABLE:
            entry["season"], entry["week"] = MOVABLE[index]
        entries.append({key: value for key, value in entry.items() if value is not None})
    OUT.write_text(json.dumps(entries, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"Built {len(entries)} ordered Sunday-cycle entries from master Part 3")


if __name__ == "__main__":
    main()
