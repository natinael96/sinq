#!/usr/bin/env python3
"""Pair ማኅሌተ ጽጌ and ሰቆቃወ ድንግል with their Amharic.

Both are already on the ሌሎች መጻሕፍት shelf in Ge'ez — byte for byte the same text
as the new scans. What the new folder adds is the Amharic, and the two are worth
merging rather than shelving apart: the hymn and its rendering are one book, read
together, the way መጽሐፈ ሰዓታት already is.

The two books pair differently, so each is done its own way and asserted:

  ማኅሌተ ጽጌ      the Amharic carries the verse numbers, so they pair by number.
  ሰቆቃወ ድንግል    the Amharic carries none, and there are 57 of it against 58
                Ge'ez verses. One is missing, and nothing in the text says which,
                so a positional pairing would be right until the gap and wrong
                after it. Its Amharic is appended whole instead, under its own
                heading — the reader gets both texts and is told nothing false
                about which line renders which.

Writes into sources/generated/, which tools/build_books.py then shelves.
"""
import json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "sources/tsige"
OUT = ROOT / "sources/generated"

GEEZ_NUM = re.compile(r"^([፩-፼ወ]+)\s+")
ARABIC_NUM = re.compile(r"^(\d+)\s+")

GEEZ_VALUE = {"፩": 1, "፪": 2, "፫": 3, "፬": 4, "፭": 5, "፮": 6, "፯": 7, "፰": 8,
              "፱": 9, "፲": 10, "፳": 20, "፴": 30, "፵": 40, "፶": 50, "፷": 60,
              "፸": 70, "፹": 80, "፺": 90, "፻": 100}


def geez_int(s):
    total = run = 0
    for c in s.replace("ወ", ""):
        v = GEEZ_VALUE.get(c, 0)
        if v == 100:
            run = (run or 1) * 100
            total += run
            run = 0
        else:
            run += v
    return total + run


def load(name):
    return json.loads((SRC / f"{name}.json").read_text(encoding="utf-8"))


def blocks(doc):
    return [(b.get("type", "paragraph"), (b.get("text") or "").strip())
            for c in doc["chapters"] for b in c.get("blocks", []) if (b.get("text") or "").strip()]


def geez_verses(doc):
    """Numbered Ge'ez verses, and the unnumbered prefaces before the first."""
    preface, verses = [], {}
    for kind, text in blocks(doc):
        m = GEEZ_NUM.match(text)
        if m:
            verses[geez_int(m.group(1))] = text[m.end():].strip()
        elif not verses:
            preface.append(text)
    return preface, verses


def amharic_by_number(doc):
    """Amharic keyed by its own Arabic verse number; continuation lines append."""
    out, current = {}, None
    for _, text in blocks(doc):
        m = ARABIC_NUM.match(text)
        # A continuation line can open with a digit too, so a number only starts
        # a new verse when it advances the count. Requiring exactly the next
        # number was too strict: the scan skips a few, and every verse after the
        # first gap was then swallowed into the one before it.
        if m and (current is None or int(m.group(1)) > current):
            current = int(m.group(1))
            out[current] = text[m.end():].strip()
        elif current is not None:
            out[current] += " " + text
    return out


def merge_by_number(title, geez_doc, amharic_doc):
    preface, verses = geez_verses(geez_doc)
    amharic = amharic_by_number(amharic_doc)
    paired = sum(1 for n in verses if n in amharic)
    if paired < len(verses) * 0.9:
        sys.exit(f"{title}: only {paired} of {len(verses)} verses found an Amharic "
                 f"counterpart — the numbering no longer lines up")
    out = [{"type": "heading", "text": title, "level": 1}]
    out += [{"type": "paragraph", "text": t, "lang": "amh"} for t in preface]
    for n in sorted(verses):
        out.append({"type": "paragraph", "text": f"{geez_numeral(n)} {verses[n]}", "lang": "gez"})
        if n in amharic:
            out.append({"type": "paragraph", "text": amharic[n], "lang": "amh"})
    return out, paired, len(verses)


def merge_appended(title, geez_doc, amharic_doc, amharic_heading):
    """The two texts in one book, one after the other rather than interleaved."""
    preface, verses = geez_verses(geez_doc)
    amharic = [t for kind, t in blocks(amharic_doc)]
    out = [{"type": "heading", "text": title, "level": 1}]
    out += [{"type": "paragraph", "text": t, "lang": "gez"} for t in preface]
    for n in sorted(verses):
        out.append({"type": "paragraph", "text": f"{geez_numeral(n)} {verses[n]}", "lang": "gez"})
    out.append({"type": "heading", "text": amharic_heading, "level": 2})
    out += [{"type": "paragraph", "text": t, "lang": "amh"} for t in amharic]
    return out, len(amharic), len(verses)


def geez_numeral(n):
    if n <= 0:
        return ""
    out, rest = "", n
    if rest >= 100:
        hundreds = rest // 100
        out += ("" if hundreds == 1 else geez_numeral(hundreds)) + "፻"
        rest %= 100
    tens, ones = divmod(rest, 10)
    return out + ("፲፳፴፵፶፷፸፹፺"[tens - 1] if tens else "") + ("፩፪፫፬፭፮፯፰፱"[ones - 1] if ones else "")


def write(title, blocks_out):
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f"{title}.json").write_text(
        json.dumps({"title": title, "category": "የዜማ መጻሕፍት",
                    "chapters": [{"number": 1, "title": title, "blocks": blocks_out}]},
                   ensure_ascii=False, indent=1), encoding="utf-8")


def main():
    b, paired, total = merge_by_number(
        "ማኅሌተ ጽጌ", load("ማኅሌተ ጽጌ በግእዝ"), load("ማኅሌተ ጽጌ በዐማርኛ"))
    write("ማኅሌተ ጽጌ", b)
    print("ማኅሌተ ጽጌ:    %d of %d verses paired by number" % (paired, total))

    b, amharic, total = merge_appended(
        "ሰቆቃወ ድንግል", load("ሰቆቃወ ድንግል በግእዝ"), load("ሰቆቃወ ድንግል በአማርኛ"), "ትርጓሜ በአማርኛ")
    write("ሰቆቃወ ድንግል", b)
    print("ሰቆቃወ ድንግል: %d Ge'ez verses, %d Amharic blocks appended "
          "(one verse short, so not interleaved)" % (total, amharic))


if __name__ == "__main__":
    main()
