#!/usr/bin/env python3
"""One-time migration: renumber both bundled psalm editions to the Ge'ez (LXX)
psalm numbering that the ግጻዌ lectionary cites. NOT idempotent — it detects the
original shapes and refuses to run twice.

Why: the ግጻዌ data cites psalms by the Ethiopian/Ge'ez (LXX) numbers, but the
bundled 1980 diglot editions arrived in other numberings:

- gez-1980: the true Ge'ez psalter text, but with Ge'ez psalm 9 split into file
  chapters 9+10 (following the Amharic column's Masoretic split), pushing every
  later psalm one number up (ending at 151). The edition's own section headings
  record the real number: "በግእዙ መዝሙር ፻፶ (፻፶፩)".
- am-1980: pure Masoretic numbering (1-150; 9/10 and 114/115 separate, 116 and
  147 unsplit), so most citations land one psalm early.

After this script both files carry Ge'ez psalm N at chapter n == N (1..150):

- gez: merge chapters 9+10 back into 9, shift 11..151 down to 10..150, drop the
  now-redundant "በግእዙ መዝሙር X (Y)" numbering cross-notes.
- am: rebuild along LXX boundaries — merge Mas 9+10 -> 9 and Mas 114+115 -> 113,
  split Mas 116 -> 114 (v1-9) + 115 (v10-19) and Mas 147 -> 146 (v1-11) +
  147 (v12-20), shift the stretches in between, renumber verses, and rewrite
  the "መዝሙር (N)" psalm-label headings to the new numbers.
"""
import copy
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BIBLE = ROOT / "app/src/main/assets/content/bible"

GEEZ_ONES = ["", "፩", "፪", "፫", "፬", "፭", "፮", "፯", "፰", "፱"]
GEEZ_TENS = ["", "፲", "፳", "፴", "፵", "፶", "፷", "፸", "፹", "፺"]


def geez_numeral(n: int) -> str:
    if n <= 0:
        return str(n)
    out = ""
    if n >= 100:
        h = n // 100
        out += (geez_numeral(h) if h > 1 else "") + "፻"
        n %= 100
    out += GEEZ_TENS[n // 10] + GEEZ_ONES[n % 10]
    return out


def renumber_verses(verses, start=1):
    for i, v in enumerate(verses):
        v["n"] = start + i
        v["alt"] = geez_numeral(start + i)
    return verses


def merge(first, second):
    """Append second chapter's verses/headings onto first, numbering continuously."""
    merged = copy.deepcopy(first)
    offset = len(first["verses"])
    merged["verses"] = renumber_verses(
        copy.deepcopy(first["verses"]) + copy.deepcopy(second["verses"])
    )
    for h in second.get("headings", []):
        h = copy.deepcopy(h)
        h["before"] = (h.get("before") or 1) + offset
        merged.setdefault("headings", []).append(h)
    return merged


def split(chapter, first_len):
    """Split one chapter's verses into two, each renumbered from 1."""
    a, b = copy.deepcopy(chapter), copy.deepcopy(chapter)
    a["verses"] = renumber_verses(copy.deepcopy(chapter["verses"][:first_len]))
    b["verses"] = renumber_verses(copy.deepcopy(chapter["verses"][first_len:]))
    a["headings"] = [h for h in chapter.get("headings", []) if (h.get("before") or 1) <= first_len]
    b["headings"] = []
    for h in chapter.get("headings", []):
        if (h.get("before") or 1) > first_len:
            h = copy.deepcopy(h)
            h["before"] = h["before"] - first_len
            b["headings"].append(h)
    return a, b


def fix_geez():
    path = BIBLE / "gez-1980/books/19-psalms.json"
    data = json.loads(path.read_text())
    chapters = {c["n"]: c for c in data["chapters"]}
    if len(data["chapters"]) != 151:
        sys.exit(f"gez edition has {len(data['chapters'])} chapters — already migrated?")

    ch9 = merge(chapters[9], chapters[10])
    out = []
    for n in range(1, 151):
        src = ch9 if n == 9 else chapters[n if n < 9 else n + 1]
        c = copy.deepcopy(src)
        c["n"] = n
        # The "በግእዙ መዝሙር X (Y)" cross-notes recorded the true number while the
        # file was shifted; with n now the true number they are stale noise.
        c["headings"] = [
            h for h in c.get("headings", [])
            if not (h.get("text") or "").startswith("በግእዙ መዝሙር")
        ]
        out.append(c)
    # Ge'ez 9 continues into the old chapter 10's incipit — keep it as a plain
    # heading at the verse where that half begins.
    old10_incipit = "ለምንት እግዚኦ ቆምከ እምርሑቅ።"
    for h in out[8].get("headings", []):
        if old10_incipit in (h.get("text") or ""):
            h["text"] = old10_incipit
            h["before"] = len(chapters[9]["verses"]) + 1
    data["chapters"] = out
    path.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")))
    return out


def relabel(chapter, n):
    """Rewrite the printed "መዝሙር (N)" psalm-label heading to the new number."""
    labeled = False
    for h in chapter.get("headings", []):
        text = h.get("text") or ""
        if text.startswith("መዝሙር (") and (h.get("before") or 1) == 1 and not labeled:
            h["text"] = f"መዝሙር ({geez_numeral(n)})"
            labeled = True
        elif text.startswith("መዝሙር ("):
            h["text"] = ""  # mid-psalm duplicate label from a merged chapter
    chapter["headings"] = [h for h in chapter.get("headings", []) if h.get("text")]
    if not labeled:
        chapter.setdefault("headings", []).insert(
            0, {"style": "d", "kind": "descriptive", "text": f"መዝሙር ({geez_numeral(n)})", "before": 1},
        )
    return chapter


def fix_amharic():
    path = BIBLE / "am-1980/books/19-psalms.json"
    data = json.loads(path.read_text())
    mas = {c["n"]: c for c in data["chapters"]}
    if len(data["chapters"]) != 150 or len(mas[147]["verses"]) != 20:
        sys.exit("am edition shape unexpected — already migrated?")

    lxx = {}
    for n in range(1, 9):
        lxx[n] = mas[n]                       # LXX 1-8 = Mas 1-8
    lxx[9] = merge(mas[9], mas[10])           # LXX 9 = Mas 9+10
    for n in range(10, 113):
        lxx[n] = mas[n + 1]                   # LXX 10-112 = Mas 11-113
    lxx[113] = merge(mas[114], mas[115])      # LXX 113 = Mas 114+115
    lxx[114], lxx[115] = split(mas[116], 9)   # LXX 114/115 = Mas 116:1-9 / 10-19
    for n in range(116, 146):
        lxx[n] = mas[n + 1]                   # LXX 116-145 = Mas 117-146
    lxx[146], lxx[147] = split(mas[147], 11)  # LXX 146/147 = Mas 147:1-11 / 12-20
    for n in range(148, 151):
        lxx[n] = mas[n]                       # LXX 148-150 = Mas 148-150

    out = []
    for n in range(1, 151):
        c = copy.deepcopy(lxx[n])
        c["n"] = n
        out.append(relabel(c, n))
    data["chapters"] = out
    path.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")))
    return out


def main():
    gz = fix_geez()
    am = fix_amharic()
    # Both editions must now agree chapter-for-chapter.
    for a, g in zip(am, gz):
        assert a["n"] == g["n"]
        if abs(len(a["verses"]) - len(g["verses"])) > 3:
            print(f"WARN psalm {a['n']}: am {len(a['verses'])} vs gez {len(g['verses'])} verses")
    print(f"done: am {len(am)} chapters / {sum(len(c['verses']) for c in am)} verses, "
          f"gez {len(gz)} chapters / {sum(len(c['verses']) for c in gz)} verses")


if __name__ == "__main__":
    main()
