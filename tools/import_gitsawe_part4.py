#!/usr/bin/env python3
"""Normalize master Part 4 as a separately selected Athanasius collection."""

import json
from pathlib import Path

from import_gitsawe_months import convert_service


ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "content/gitsawe/parts/04-athanasius-funeral-lectionary.json"
OUT = ROOT / "app/src/main/assets/content/gitsawe/athanasius.json"
MEMORIAL_DAY = {20: 3, 21: 7, 22: 12, 23: 30, 24: 40}


def category(index):
    if index <= 10:
        return "person"
    if index <= 17:
        return "riteChapter"
    if index <= 19:
        return "burialPrayer"
    return "memorial"


def main():
    sections = json.loads(SOURCE.read_text(encoding="utf-8"))["part"]["extracted_sections"]
    if len(sections) != 25:
        raise SystemExit("Part 4 must contain exactly 25 sections")
    entries = []
    for index, section in enumerate(sections, 1):
        source_readings = dict(section["readings"])
        supplication = source_readings.pop("መስተበቍዕ", None)
        entry = {
            "index": index,
            "title": section["section_title"].strip(),
            "category": category(index),
            "memorialDay": MEMORIAL_DAY.get(index),
            "observance": "80th, 100th, half-year, annual, or any memorial" if index == 25 else None,
            "supplication": supplication.strip() if isinstance(supplication, str) else None,
            "sourcePages": section["source_scan_pages"],
            "kidassie": convert_service(source_readings),
        }
        entries.append({key: value for key, value in entry.items() if value is not None})
    OUT.write_text(json.dumps(entries, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"Built {len(entries)} Athanasius entries from master Part 4")


if __name__ == "__main__":
    main()
