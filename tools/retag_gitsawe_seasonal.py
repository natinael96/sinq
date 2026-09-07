#!/usr/bin/env python3
"""Align the npm-derived seasonal Sunday rows with the season keys the app can date.

`restructure_gitsawe.py` kept the package's own keys. Four of them named a
season the app never resolved (genaTsom, the ሰኔ astemhro, zere_demena) or
overloaded `part` as a Sunday ordinal. This maps them onto the keys used by
`SundayCycleCalendar` and Part 3, so the same window selects both files.
Idempotent: rows already carrying the new keys are left alone.
"""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PATH = ROOT / "app/src/main/assets/content/gitsawe/seasonal-gitsawe.json"


def retag(entry):
    season, week, part = entry.get("season"), entry.get("week"), entry.get("part")
    if season == "genaTsom":
        if week == 1:            # ፩ኛ–፭ኛ printed as parts: the አስተምህሮ Sundays
            entry.update(season="astemhro", week=part, part=None)
        else:                    # ስብከት / ብርሃን / ኖላዊ, the three Sundays before ልደት
            entry.update(season={2: "sibket", 3: "birhan", 4: "nolawi"}[week], week=None)
    elif season == "astemhro" and "ሰኔ" in (entry.get("title") or ""):
        entry["season"] = "seneAstemhro"
    elif season == "zere_demena":
        entry["season"] = "zere"
    return entry


def main():
    rows = json.loads(PATH.read_text(encoding="utf-8"))
    rows = [retag(dict(r)) for r in rows]
    PATH.write_text(json.dumps(rows, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"Retagged {len(rows)} seasonal rows")


if __name__ == "__main__":
    main()
