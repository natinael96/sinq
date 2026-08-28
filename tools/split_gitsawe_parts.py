#!/usr/bin/env python3
"""Split licensed Gitsawe master Parts 2–5 into self-contained source JSON.

These are source-preserving splits, not app-model normalization. Each output
retains the complete original part object, its scan provenance, and a concise
machine-readable explanation of how that collection is organized.
"""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "content/gitsawe/gitsawe-master.json"
OUT = ROOT / "content/gitsawe/parts"

FILES = {
    2: "02-movable-feasts-and-fasts.json",
    3: "03-sunday-cycle-and-mezmur.json",
    4: "04-athanasius-funeral-lectionary.json",
    5: "05-bahre-hasab-tables.json",
}

ABOUT = {
    2: {
        "collection": "movable_feasts_and_fasts",
        "selection_basis": "date relative to Fasika through the Ethiopian computus",
        "content_shape": "49 titled sections; each has services and source_scan_pages",
        "app_model_target": "SeasonalEntry with existing GitsaweService fields",
        "notes": [
            "This is not a fixed Ethiopian month/day calendar.",
            "It includes weekday readings for Nineveh, Heraclius, Great Lent, and later movable observances.",
            "48 sections contain ዘቅዳሴ only; one also contains ዘነግህ.",
        ],
    },
    3: {
        "collection": "sunday_cycle_and_mezmur",
        "selection_basis": "Sunday within a printed fixed-date range or movable liturgical season",
        "content_shape": "91 ordered sections combining period rules, mezmur headings, readings, and provenance",
        "app_model_target": "MonthlyEntry for fixed Sunday ranges; SeasonalEntry for movable Sunday cycles",
        "notes": [
            "The printed book has no explicit chapter headings; the 11 chapter ranges are derived routing metadata.",
            "The flat extracted_sections order is canonical and must be preserved.",
            "Some sections are continuations or instructions and intentionally lack a period or hymn field.",
            "Reading shapes vary; note-only and partial-service sections must not be invented into full services.",
        ],
    },
    4: {
        "collection": "athanasius_funeral_lectionary",
        "selection_basis": "funeral rite, person who died, rite chapter, or memorial day",
        "content_shape": "25 titled funeral and memorial reading sections with provenance",
        "app_model_target": "existing GitsaweService reading structure in a separately selected collection",
        "notes": [
            "This is not calendar-driven daily or seasonal Gitsawe.",
            "It covers clergy, monastics, adults, children, childbirth, rite chapters, burial prayers, and memorial days.",
            "The መስተበቍዕ supplication is source content and must remain distinct from scripture readings.",
        ],
    },
    5: {
        "collection": "bahre_hasab_reference_table",
        "selection_basis": "Ethiopian year",
        "content_shape": "one 15-row table printed across three scan-page column fragments",
        "app_model_target": "reference/validation data for BahreHasab; not a GitsaweService",
        "notes": [
            "unified_table is canonical for use: corresponding rows from scans 425–427 are already joined.",
            "extracted_sections preserves the three page fragments for auditability.",
            "The table covers Ethiopian years 2001–2015 and must not be treated as an evergreen calendar lookup.",
        ],
    },
}


def validate(part):
    number = part["part"]
    sections = part.get("extracted_sections", [])
    start, end = part["scan_pages"]
    pages = {
        page
        for section in sections
        for page in section.get("source_scan_pages", [])
    }
    expected_pages = set(range(start, end + 1))
    if pages != expected_pages:
        raise SystemExit(f"Part {number}: provenance does not cover scans {start}–{end}")

    if number == 5:
        table = part.get("unified_table", {})
        if len(table.get("rows", [])) != 15 or table.get("source_scan_pages") != [425, 426, 427]:
            raise SystemExit("Part 5: unified Bahre Hasab table is incomplete")
    elif len(sections) != part.get("sections_extracted"):
        raise SystemExit(f"Part {number}: extracted-section count is inconsistent")


def main():
    master = json.loads(SOURCE.read_text(encoding="utf-8"))
    parts = {part["part"]: part for part in master["parts"]}
    if set(FILES) - set(parts):
        raise SystemExit("master is missing one or more of Parts 2–5")

    OUT.mkdir(parents=True, exist_ok=True)
    for number, filename in FILES.items():
        part = parts[number]
        validate(part)
        document = {
            "README": ABOUT[number],
            "book": master["book"],
            "part": part,
        }
        path = OUT / filename
        path.write_text(json.dumps(document, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"Part {number}: {filename} ({len(part.get('extracted_sections', []))} extracted sections)")


if __name__ == "__main__":
    main()
