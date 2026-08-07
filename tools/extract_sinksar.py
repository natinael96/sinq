"""Normalize the Amharic ስንክሳር (synaxarium) into the app's bundled format.

Source: Sinksar.json from the open `gitsaweandsinksarbot` project (same family
as the Gitsawe data). Download it, then run:

    python tools/extract_sinksar.py path/to/Sinksar.json

Writes one file per Ethiopian month to app/src/main/assets/content/sinksar/,
each: { "month": N, "days": [ { "day": D, "entries": [ {title, text} ] } ] }.
The source's buggy per-day title is dropped; each commemoration keeps its own
title and cleaned narrative text.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app" / "src" / "main" / "assets" / "content" / "sinksar"

# Source month names, in Ethiopian calendar order -> month number 1..13.
MONTHS = [
    "Meskerem", "Tikimt", "Hidar", "Tahsas", "Tir", "Yekatit", "Megabit",
    "Miazia", "Ginbot", "Sene", "Hamle", "Nehasse", "Pagume",
]


def clean(s: str) -> str:
    """Trim each line, drop blanks, collapse runs of spaces — keep ❖ structure."""
    if not s:
        return ""
    s = s.replace(" ", " ")
    lines = [re.sub(r"[ \t]+", " ", ln).strip() for ln in s.split("\n")]
    return "\n".join(ln for ln in lines if ln).strip()


def main() -> None:
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("Sinksar.json")
    data = json.loads(src.read_text(encoding="utf-8"))
    OUT.mkdir(parents=True, exist_ok=True)

    manifest = []
    total_days = total_entries = total_chars = 0
    for idx, name in enumerate(MONTHS, start=1):
        block = data.get(name)
        if not block:
            print(f"  {name}: MISSING in source")
            continue
        days_out = []
        for day in block.get("days", []):
            entries = []
            for c in day.get("content", []):
                title = clean(c.get("title") or "")
                text = clean(c.get("main") or "")
                if not text and not title:
                    continue
                entries.append({"title": title, "text": text})
                total_chars += len(text)
            if entries:
                days_out.append({"day": int(day.get("id", len(days_out) + 1)), "entries": entries})
                total_entries += len(entries)
        days_out.sort(key=lambda d: d["day"])
        (OUT / f"{idx}.json").write_text(
            json.dumps({"month": idx, "days": days_out}, ensure_ascii=False, separators=(",", ":")),
            encoding="utf-8",
        )
        manifest.append({"month": idx, "name": name, "days": len(days_out)})
        total_days += len(days_out)
        print(f"  {idx:>2} {name:<10} {len(days_out):>2} days, "
              f"{sum(len(d['entries']) for d in days_out):>3} commemorations")

    (OUT / "manifest.json").write_text(
        json.dumps({"contentVersion": 1, "months": manifest}, ensure_ascii=False, indent=1),
        encoding="utf-8",
    )
    print(f"\nTOTAL {total_days} days, {total_entries} commemorations, "
          f"{total_chars:,} chars -> {OUT}")


if __name__ == "__main__":
    sys.exit(main())
