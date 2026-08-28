#!/usr/bin/env python3
"""Normalize master Part 2 into Sinq's existing SeasonalEntry model."""

import json
import sys
from pathlib import Path

from import_gitsawe_months import convert_service


ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "content/gitsawe/parts/02-movable-feasts-and-fasts.json"
OUT = ROOT / "app/src/main/assets/content/gitsawe/movable-weekday-gitsawe.json"

DAY_PART = {
    "ሰኑይ": 1, "ሠሉስ": 2, "ረቡዕ": 3, "ሐሙስ": 4,
    "ዓርብ": 5, "ቀዳሚት": 6,
}


def classification(index, title):
    if index <= 3:
        season, week = "neneweTsom", 1
    elif index <= 9:
        season, week = "heraclius", 1
    elif index <= 15:
        season, week = "abiyTsom", 1
    elif index <= 21:
        season, week = "abiyTsom", 2
    elif index <= 27:
        season, week = "abiyTsom", 3
    elif index <= 33:
        season, week = "abiyTsom", 4
    elif index <= 39:
        season, week = "abiyTsom", 5
    elif index <= 45:
        season, week = "abiyTsom", 6
    elif index == 46:
        return "rikbeKahnat", None, 3
    elif index == 47:
        return "erget", None, 4
    elif index == 48:
        return "apostlesFast", None, 1
    else:
        # The source does not anchor በዕለተ ምህላ to a computable date.
        return "supplication", None, None
    part = next((value for word, value in DAY_PART.items() if word in title), None)
    if part is None:
        raise SystemExit(f"Part 2 section {index} has no recognizable weekday: {title}")
    return season, week, part


def main():
    source = json.loads(SOURCE.read_text(encoding="utf-8"))
    sections = source["part"]["extracted_sections"]
    if len(sections) != 49:
        raise SystemExit("Part 2 must contain exactly 49 sections")
    entries = []
    for index, section in enumerate(sections, 1):
        title = section["section_title"].strip()
        season, week, part = classification(index, title)
        entry = {
            "season": season,
            "week": week,
            "part": part,
            "raw": title,
            "movable": True,
            "title": title,
            "sourcePages": section["source_scan_pages"],
        }
        for source_key, target_key in (("ዘነግህ", "negh"), ("ዘቅዳሴ", "kidassie"), ("ዘሠርክ", "serk")):
            service = section["services"].get(source_key)
            if service:
                entry[target_key] = convert_service(service)
        entries.append({key: value for key, value in entry.items() if value is not None})
    OUT.write_text(json.dumps(entries, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"Built {len(entries)} movable weekday entries from master Part 2")


if __name__ == "__main__":
    if len(sys.argv) != 1:
        raise SystemExit("usage: import_gitsawe_part2.py")
    main()
