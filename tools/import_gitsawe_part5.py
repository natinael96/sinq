#!/usr/bin/env python3
"""Publish master Part 5's canonical joined Bahre Hasab table for the app."""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "content/gitsawe/parts/05-bahre-hasab-tables.json"
OUT = ROOT / "app/src/main/assets/content/gitsawe/bahre-hasab-reference.json"


def main():
    part = json.loads(SOURCE.read_text(encoding="utf-8"))["part"]
    table = part["unified_table"]
    if len(table["columns"]) != 17 or len(table["rows"]) != 15:
        raise SystemExit("Part 5 must be one 17-column, 15-row unified table")
    if any(len(row) != len(table["columns"]) for row in table["rows"]):
        raise SystemExit("Part 5 contains an incomplete row")
    document = {
        "title": table["title"],
        "note": table["note"],
        "sourcePages": table["source_scan_pages"],
        "columns": table["columns"],
        "rows": [{"values": row} for row in table["rows"]],
    }
    OUT.write_text(json.dumps(document, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print("Built the 15-row Bahre Hasab reference from master Part 5")


if __name__ == "__main__":
    main()
