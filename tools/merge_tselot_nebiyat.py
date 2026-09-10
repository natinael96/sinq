#!/usr/bin/env python3
"""Give the Amharic ጸሎት ነቢያት its last five chapters.

The printed ዳዊት ends with fifteen canticles — ጸሎት ነቢያት — and then መኃልየ መኃልይ,
the Song of Songs, in five readings. The Ge'ez scan carries all twenty. The
Amharic scan stops at fifteen. But the five it lacks are scripture the app
already bundles, so they are built from the Bible rather than re-scanned.

Where each reading begins and ends is read off the Ge'ez: መሐልይ ፩ opens on 1:1
and closes on 1:17, ፪ closes on the "till he please" refrain at 3:5, ፫ closes
on "tell him I am sick of love" at 5:8, ፬ on the refrain at 8:4, ፭ runs to the
end. One thing the Ge'ez scan does NOT carry is 2:1–7 — ፩ ends at 1:17 and ፪
opens at 2:8, so a leaf was lost. The Amharic is built from a complete text and
does not reproduce that gap: ፪ starts at 2:1.

Writes sources/generated/, where build_books.py lets it shadow the raw scan.
"""
import json, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "sources/books/ጸሎት ነቢያት በአማርኛ.json"
GEEZ = ROOT / "sources/books/ጸሎት ነቢያት በግእዝ.json"
SONG = ROOT / "app/src/main/assets/content/bible/am-1980/books/22-song-of-solomon.json"
OUT = ROOT / "sources/generated/ጸሎት ነቢያት በአማርኛ.json"

# (from_chapter, from_verse, to_chapter, to_verse), inclusive, one per መሐልይ.
READINGS = [
    (1, 1, 1, 17),
    (2, 1, 3, 5),
    (3, 6, 5, 8),
    (5, 9, 8, 4),
    (8, 5, 8, 14),
]
ABBR = "መኃል"


def geez_numeral(n):
    if n <= 0:
        return ""
    out, rest = "", n
    if rest >= 100:
        h = rest // 100
        out += ("" if h == 1 else geez_numeral(h)) + "፻"
        rest %= 100
    tens, ones = divmod(rest, 10)
    return out + ("፲፳፴፵፶፷፸፹፺"[tens - 1] if tens else "") + ("፩፪፫፬፭፮፯፰፱"[ones - 1] if ones else "")


def span(fc, fv, tc, tv):
    """"(መኃል ፪፥፩ - ፫፥፭)" in the style the scan uses for its own references."""
    if fc == tc:
        return f"({ABBR} {geez_numeral(fc)}፥{geez_numeral(fv)} - {geez_numeral(tv)})"
    return f"({ABBR} {geez_numeral(fc)}፥{geez_numeral(fv)} - {geez_numeral(tc)}፥{geez_numeral(tv)})"


def main():
    raw = json.loads(RAW.read_text(encoding="utf-8"))
    geez = json.loads(GEEZ.read_text(encoding="utf-8"))
    song = json.loads(SONG.read_text(encoding="utf-8"))

    # The premise, asserted: fifteen there, twenty here, and exactly five to add.
    if len(raw["chapters"]) != 15:
        sys.exit(f"Amharic scan has {len(raw['chapters'])} chapters, expected 15")
    if len(geez["chapters"]) != 20:
        sys.exit(f"Ge'ez scan has {len(geez['chapters'])} chapters, expected 20")
    if [c["title"] for c in geez["chapters"][15:]] != [f"መሐልይ {geez_numeral(i)}" for i in range(1, 6)]:
        sys.exit("the Ge'ez's last five are no longer መሐልይ ፩–፭")
    chapters = {c["chapter"] if "chapter" in c else i + 1: c["verses"]
                for i, c in enumerate(song["chapters"])}
    if len(chapters) != 8 or sum(len(v) for v in chapters.values()) != 117:
        sys.exit("Song of Songs is not 8 chapters / 117 verses — the edition changed")

    covered = []
    added = []
    for i, (fc, fv, tc, tv) in enumerate(READINGS, 1):
        blocks = [{"type": "heading", "level": 1,
                   "text": f"መኃልየ መኃልይ {geez_numeral(i)} {span(fc, fv, tc, tv)}"}]
        for ch in range(fc, tc + 1):
            for v in chapters[ch]:
                n = v["n"]
                if (ch == fc and n < fv) or (ch == tc and n > tv):
                    continue
                blocks.append({"type": "paragraph", "text": v["t"].strip()})
                covered.append((ch, n))
        added.append({"number": 15 + i, "title": blocks[0]["text"], "blocks": blocks})

    # Every verse once, none twice, none dropped — the whole point of building
    # from the Bible instead of from a scan with a missing leaf.
    expect = [(ch, v["n"]) for ch in range(1, 9) for v in chapters[ch]]
    if covered != expect:
        missing = sorted(set(expect) - set(covered))
        doubled = sorted({x for x in covered if covered.count(x) > 1})
        sys.exit(f"readings do not tile the book: missing {missing[:5]}, doubled {doubled[:5]}")

    out = {"title": raw["title"], "category": raw["category"],
           "chapters": raw["chapters"] + added}
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=1), encoding="utf-8")
    for c in added:
        print("  %2d %-42s %3d verses" % (c["number"], c["title"], len(c["blocks"]) - 1))
    print("20 chapters → %s" % OUT.relative_to(ROOT))


if __name__ == "__main__":
    main()
