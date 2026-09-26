#!/usr/bin/env python3
"""Build the bundled ውዳሴ ማርያም (Wudase Maryam) asset.

Two inputs, both vendored under sources/wudase/ — nothing is fetched at build
time:

  base.json
      The Ge'ez of every portion, and the Amharic of ይዌድስዋ መላእክት. This began
      as the community dataset at github.com/tecleet/wudase-mariam (prayers.json,
      no license file; the prayer itself is traditional, public-domain Ethiopian
      Orthodox liturgy), split into stanzas and then corrected word by word
      against the print. It is vendored rather than re-fetched because upstream
      has since been re-line-broken: refetching would silently replace the
      corrected text with a differently-paragraphed one. The corrections live in
      this file now, not in a table of string swaps.

  ውዳሴ ማርያም በአማርኛ.json
  የዘወትር ጸሎት በአማርኛ.json
      The Amharic the app ships: a chapter per portion, each a list of blocks.
      These replace base.json's Amharic wholesale for the portions they cover —
      they are the authority for the Amharic, and the readings they carry are
      not reconciled against base.json.

      Supplied as an extraction, and repaired in place: spaces that had fallen
      inside words (የእግዚአብሔር ን, ከ ኢየሱስ) were closed up, and four stanzas whose
      numeral had been swallowed by the stanza before them were split back out,
      so every chapter now runs ፩…n unbroken. Only breaks no Amharic word can
      have were touched; no reading was changed. Nothing is repaired at build
      time — what the files say is what ships.

ይዌድስዋ መላእክት is deliberately NOT taken from the ውዳሴ ማርያም file even though that
file carries it (chapter 9): the shipped one is the corrected, clause-separated
text, and it stays.

Output: app/src/main/assets/content/wudase/wudase.json.

Run:  python3 tools/build_wudase.py
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "sources", "wudase")
OUT = os.path.join(
    HERE, "..", "app", "src", "main", "assets", "content", "wudase", "wudase.json",
)

BASE = os.path.join(SRC, "base.json")

# Which chapter of which source file supplies each section's Amharic.
# Sections absent from this map keep base.json's Amharic — that is only
# yiwedsewa_melaekt, for the reason in the docstring.
AM_SOURCES = {
    "daily": ("የዘወትር ጸሎት በአማርኛ.json", 1),
    "monday": ("ውዳሴ ማርያም በአማርኛ.json", 1),
    "tuesday": ("ውዳሴ ማርያም በአማርኛ.json", 2),
    "wednesday": ("ውዳሴ ማርያም በአማርኛ.json", 3),
    "thursday": ("ውዳሴ ማርያም በአማርኛ.json", 4),
    "friday": ("ውዳሴ ማርያም በአማርኛ.json", 5),
    "saturday": ("ውዳሴ ማርያም በአማርኛ.json", 6),
    "sunday": ("ውዳሴ ማርያም በአማርኛ.json", 7),
    "anqetse_birhan": ("ውዳሴ ማርያም በአማርኛ.json", 8),
}


def chapter(filename, number):
    with open(os.path.join(SRC, filename), encoding="utf-8") as f:
        doc = json.load(f)
    for ch in doc["chapters"]:
        if ch["number"] == number:
            return ch["blocks"]
    raise SystemExit(f"{filename}: no chapter {number}")


def paragraphs(blocks):
    """One display paragraph per block.

    The stanza numeral is written into the line — "፩. …" — because that is how
    the reader already renders every other portion, and how the print sets it.

    The first block is the chapter's own name (የሰኞ ውዳሴ ማርያም, አንቀጸ ብርሃን) and is
    dropped: the screen prints the portion's title above the text already, so
    keeping it would show it twice. Any *later* unnumbered block is a heading
    inside the portion — የሃይማኖት መሠረት in the daily prayer — and is kept where it
    stands.
    """
    out = []
    for i, block in enumerate(blocks):
        text = " ".join(block["text"].split())
        if not text:
            continue
        if i == 0 and block["type"] == "paragraph":
            continue
        if block["type"] == "numbered_paragraph":
            out.append(f"{block['number']}. {text}")
        else:
            out.append(text)
    return out


def main():
    with open(BASE, encoding="utf-8") as f:
        base = json.load(f)

    sections = []
    for section in base["sections"]:
        am = section["am"]
        source = AM_SOURCES.get(section["id"])
        if source:
            am = paragraphs(chapter(*source))
            if not am:
                raise SystemExit(f"{section['id']}: override produced no text")
        sections.append({
            "id": section["id"],
            "weekday": section["weekday"],
            "label": section["label"],
            "titleAm": section["titleAm"],
            "titleGe": section["titleGe"],
            "am": am,
            "ge": section["ge"],
        })

    payload = {"contentVersion": base.get("contentVersion", 1), "sections": sections}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=1)

    for s in sections:
        mark = " (base)" if s["id"] not in AM_SOURCES else ""
        print(f"  {s['id']:<18} am={len(s['am']):>2} ge={len(s['ge']):>2}  {s['label']}{mark}")
    print(f"wrote {os.path.relpath(OUT, os.path.join(HERE, '..'))}")


if __name__ == "__main__":
    main()
