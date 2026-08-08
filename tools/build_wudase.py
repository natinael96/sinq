#!/usr/bin/env python3
"""Build the bundled ውዳሴ ማርያም (Wudase Maryam) asset from the community dataset.

Source: https://github.com/tecleet/wudase-mariam  (prayers.json — no license file;
the underlying prayer is traditional, public-domain Ethiopian Orthodox liturgy).
We keep the Ge'ez (ge) and Amharic (am) text, split into numbered stanzas, and
drop the sparse English. Output: app/src/main/assets/content/wudase/wudase.json.

Run:  python3 tools/build_wudase.py
"""
import json
import os
import re
import urllib.request

SRC = "https://raw.githubusercontent.com/tecleet/wudase-mariam/main/prayers.json"
OUT = os.path.join(
    os.path.dirname(__file__),
    "..", "app", "src", "main", "assets", "content", "wudase", "wudase.json",
)
ATTRIBUTION = "ጽሑፍ፦ github.com/tecleet/wudase-mariam · ትውፊታዊ የኦርቶዶክስ ተዋሕዶ ጸሎት"

# The sections we ship, in liturgical order: the daily opening prayer first,
# then the weekday portions, then the appended prayers.
# weekday: 1=Mon … 7=Sun for the day cycle; 0 = not day-keyed.
# split: "numeral" breaks on ፩.-style stanza markers; "para" on blank lines
# (the daily prayer has meaningful unnumbered sub-paragraphs to preserve).
SECTIONS = [
    ("daily", 0, "ዘወትር ጸሎት", "para"),
    ("monday", 1, "ሰኞ", "numeral"),
    ("tuesday", 2, "ማክሰኞ", "numeral"),
    ("wednesday", 3, "ረቡዕ", "numeral"),
    ("thursday", 4, "ሐሙስ", "numeral"),
    ("friday", 5, "ዓርብ", "numeral"),
    ("saturday", 6, "ቅዳሜ", "numeral"),
    ("sunday", 7, "እሑድ", "numeral"),
    ("yiwedsewa_melaekt", 0, "ይወድስዋ መላእክት", "numeral"),
    ("anqetse_birhan", 0, "አንቀጸ ብርሃን", "numeral"),
]

_NUM = r"[፩-፼]"
# Split right before a stanza marker: a Ge'ez numeral group followed by "." / "።".
_STANZA = re.compile(r"(?=(?<!\S)" + _NUM + r"+[.።] )")


def stanzas(text, split):
    """Clean the escape-mangled source text into a list of display paragraphs."""
    if not text:
        return []
    # The source double-encodes some whitespace as the two characters "\t"/"\n".
    text = text.replace("\\t", " ").replace("\t", " ")
    if split == "para":
        text = text.replace("\\n", "\n")
        parts = re.split(r"\n\s*\n", text)
    else:
        text = text.replace("\\n", " ").replace("\n", " ")
        parts = _STANZA.split(text)
    return [re.sub(r"\s+", " ", p).strip() for p in parts if p.strip()]


def main():
    with urllib.request.urlopen(SRC) as r:
        data = json.load(r)

    out_sections = []
    for key, weekday, label, split in SECTIONS:
        node = data[key]
        out_sections.append({
            "id": key,
            "weekday": weekday,
            "label": label,
            "titleAm": node["title"].get("am", "").strip(),
            "titleGe": node["title"].get("ge", "").strip(),
            "am": stanzas(node["content"].get("am", ""), split),
            "ge": stanzas(node["content"].get("ge", ""), split),
        })

    payload = {
        "contentVersion": 1,
        "attribution": ATTRIBUTION,
        "sections": out_sections,
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=1)

    for s in out_sections:
        print(f"  {s['id']:<18} am={len(s['am']):>2} ge={len(s['ge']):>2}  {s['label']}")
    print(f"wrote {os.path.relpath(OUT)}")


if __name__ == "__main__":
    main()
