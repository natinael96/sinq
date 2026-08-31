#!/usr/bin/env python3
"""Derive the monthly commemorations (ወርኀዊ በዓላት) from the bundled ስንክሳር.

Every synaxarium day carries an explicit "📌 ወርኀዊ በዓላት" block listing the
saints commemorated on that day of EVERY Ethiopian month. Taking the block
across all twelve 30-day months and keeping the names that agree gives a
monthly calendar that is traceable to bundled content rather than authored
from memory — which matters, because a vow (ስዕለት) is anchored to these days.

Writes app/src/main/assets/content/holidays/monthly.json:
    [{"day": 12, "primary": "ቅዱስ ሚካኤል ሊቀ መላእክት", "also": [...]}, ...]

Idempotent: re-running regenerates the same file from the same sources.
"""
import json
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SINKSAR = ROOT / "app/src/main/assets/content/sinksar"
OUT = ROOT / "app/src/main/assets/content/holidays/monthly.json"

MONTHLY_MARKER = "ወርኀዊ በዓላት"
# Ge'ez numeral / arabic list prefixes: "፩.ቅዱስ ሚካኤል" -> "ቅዱስ ሚካኤል"
PREFIX = re.compile(r"^[፩-፼0-9]+\s*[.።)]\s*")
# How many of the twelve months must list a name for it to count as monthly.
AGREEMENT = 0.8


def month_lists(day):
    """The ወርኀዊ በዓላት name lists for `day`, one per 30-day month that has it."""
    out = []
    for month in range(1, 13):          # ጳጉሜ (13) is short and carries no cycle
        data = json.loads((SINKSAR / f"{month}.json").read_text(encoding="utf-8"))
        for entry in data["days"]:
            if entry["day"] != day:
                continue
            for item in entry["entries"]:
                if MONTHLY_MARKER not in item.get("title", ""):
                    continue
                names = [
                    PREFIX.sub("", line).strip()
                    for line in item.get("text", "").split("\n")
                    if line.strip()
                ]
                if names:
                    out.append(names)
                # One block per month: a couple of days carry the list twice,
                # and counting it twice would skew the agreement threshold.
                break
    return out


def main():
    holidays = []
    for day in range(1, 31):
        lists = month_lists(day)
        if not lists:
            raise SystemExit(f"no ወርኀዊ በዓላት block found for day {day}")
        # The day's own name is the one the sources put first most often.
        primary = Counter(names[0] for names in lists).most_common(1)[0][0]
        # Everything else the months agree on, in descending agreement.
        counts = Counter(name for names in lists for name in set(names))
        # Every name the months agree on — not a top-N slice. Capping the
        # list at four silently dropped ቅዱስ ጊዮርጊስ from ቀን ፳፫, which is
        # exactly the kind of name someone anchors a ስዕለት to.
        also = [
            name
            for name, seen in counts.most_common()
            if seen >= len(lists) * AGREEMENT and name != primary
        ]
        holidays.append({"day": day, "primary": primary, "also": also, "months": len(lists)})
        print(f"{day:2d} ({len(lists)}/12) {primary}")

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(
        json.dumps(holidays, ensure_ascii=False, indent=1) + "\n", encoding="utf-8"
    )
    print(f"\nwrote {OUT.relative_to(ROOT)} ({OUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
