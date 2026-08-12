#!/usr/bin/env python3
"""One-off maintenance: strip source artifacts from the bundled ስንክሳር JSON.

The upstream text carried three kinds of noise that are not content:
  * C0 control characters (stray backspaces) left by the original export
  * `<b>`-family markup, including malformed `< b >` and `<b<` variants
  * lone angle brackets left behind once those tags are removed

The reader already strips all of this at render time (see
ui/gitsawe/SynaxariumText.kt), so removing it from the data changes nothing on
screen — it just makes the stored text match what is displayed, and lets the
release-gate validator stay strict about malformed text.

Run:  python3 tools/clean_sinksar_artifacts.py
"""
from __future__ import annotations

import json
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
DIR = os.path.join(ROOT, "app", "src", "main", "assets", "content", "sinksar")

# Keep \n (paragraph separator) and \t; drop the rest of C0 plus U+FFFD.
CONTROL = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f�]")
B_TAG = re.compile(r"<\s*/?\s*b\s*[<>]")
ANGLE = re.compile(r"[<>]")


def clean(text: str) -> str:
    text = CONTROL.sub(" ", text)
    text = B_TAG.sub(" ", text)
    text = ANGLE.sub(" ", text)
    # Collapse the runs of spaces the removals leave, per line.
    return "\n".join(re.sub(r"[ \t]+", " ", line).strip() for line in text.split("\n"))


def main() -> None:
    changed_files = 0
    changed_fields = 0
    for m in range(1, 14):
        path = os.path.join(DIR, f"{m}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        touched = False
        for day in data.get("days", []):
            for entry in day.get("entries", []):
                for key in ("title", "text"):
                    before = entry.get(key, "")
                    after = clean(before)
                    if after != before:
                        entry[key] = after
                        touched = True
                        changed_fields += 1
        if touched:
            with open(path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
            changed_files += 1
            print(f"  cleaned {m}.json")
    print(f"{changed_fields} field(s) cleaned across {changed_files} file(s)")


if __name__ == "__main__":
    main()
