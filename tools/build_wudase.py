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
# No attribution line is emitted: the page carries the prayer and nothing else.
# The digitization's provenance is credited in NOTICE and README instead.

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

# Corrections applied on top of the upstream text, keyed by section id.
#
# The community dataset is a transcription and carries a few readings that
# differ from the printed ውዳሴ ማርያም. Each entry here is an exact string swap,
# asserted to match once, so a rebuild that no longer needs a fix fails loudly
# instead of silently dropping it. Nothing is rewritten wholesale: the prayer is
# the source's, and these are the specific words the user corrected against the
# book.
CORRECTIONS = {
    # The seven weekday portions, checked word by word against the independent
    # transcription at EOTCOpenSource/store (prayers/wudasie-maryam). The prayer
    # is the same in both; these are the places our source is wrong about where
    # a word ends. 22 defects in 2,936 words.
    #
    # A space dropped into the middle of a word — the worst of them splits the
    # name of God.
    "monday": [
        ("የፈረደበት ንም", "የፈረደበትንም"),
        ("ያለወንድ", "ያለ ወንድ"),
        ("ቤተልሔም", "ቤተ ልሔም"),
    ],
    "tuesday": [
        ("እግዚ አብሔር", "እግዚአብሔር"),
        ("የእር ሱን", "የእርሱን"),
        ("የቆ ሙትንም", "የቆሙትንም"),
    ],
    "wednesday": [
        ("ፍጽም ትና", "ፍጽምትና"),
        ("ባለሟ ልነትን", "ባለሟልነትን"),
        ("ስለተገለጠልን", "ስለ ተገለጠልን"),
    ],
    "thursday": [
        ("ድን ግልናዋ", "ድንግልናዋ"),
        ("አልተ ለወጠም", "አልተለወጠም"),
        ("ከወለደ ችው", "ከወለደችው"),
        ("የሚሆ ነው", "የሚሆነው"),
        ("ዘለዓ ለም", "ዘለዓለም"),
        ("እንደ ሰጡ", "እንደሰጡ"),
        # Two words run together with no space at all.
        ("ያዳነንየክርስቶስ", "ያዳነን የክርስቶስ"),
        ("በስሙያስተማሩለትን", "በስሙ ያስተማሩለትን"),
        ("ዳዊትግን", "ዳዊት ግን"),
        ("ስለመውደድ", "ስለ መውደድ"),
        ("ስለእግዚአብሔር", "ስለ እግዚአብሔር"),
        # A letter lost along with the space: ሕማም, not ህማም; መንግሥተ, not መንግስተ.
        ("ያለህማም", "ያለ ሕማም"),
        ("ስለመንግስተ", "ስለ መንግሥተ"),
        ("ቤተልሔም", "ቤተ ልሔም"),
    ],
    "friday": [
        ("ፈጥሮና ልና", "ፈጥሮናልና"),
        ("ዳግ መኛ", "ዳግመኛ"),
        ("ማርያምሆይ", "ማርያም ሆይ"),
    ],
    "sunday": [
        ("መጀመሪያስሙን", "መጀመሪያ ስሙን"),
        ("ያለመለወጥም", "ያለ መለወጥም"),
        ("ሁልጊዜ", "ሁል ጊዜ"),
        ("የህግጽላት", "የሕግ ጽላት"),
    ],
    "yiwedsewa_melaekt": [
        # ድንኳን is addressed to her, so the participle agrees with "you".
        ("የተሸለመች ድንኳን", "የተሸለምሽ ድንኳን"),
        ("የሁሉ እመቤት ማርያም", "እመቤታችን ማርያም"),
        ("የሁሉ ፍቅረኛ ማርያም", "የሁሉ ሰላም ማርያም"),
    ],
}

# Sections whose clause-per-line praise formulas are separated by ፤ in the
# printed book and run together in the source. Splitting on the formula is
# safe because it is the refrain the whole paragraph is built from.
CLAUSE_SEPARATED = {"yiwedsewa_melaekt": "ምስጋና ይገባሻል"}

# Fixes that only make sense once the clauses are separated, because they are
# about where the ፤ lands rather than about a word.
AFTER_SEPARATION = {
    "yiwedsewa_melaekt": [
        # "አላት" closes the angel's greeting; splitting on the refrain alone puts
        # it at the head of the next clause, where it reads as him saying the
        # second praise rather than the first.
        ("ይገባሻል፤ አላት ", "ይገባሻል አላት፤ "),
        # Two titles, not one: sister of the angels, mother of all the people.
        ("የመላእክት እኅት የሕዝቡ", "የመላእክት እኅት፤ የሕዝቡ"),
    ],
}


def correct(key, paragraphs):
    """Apply the printed book's readings to one section's paragraphs."""
    fixes = CORRECTIONS.get(key, [])
    out = []
    for para in paragraphs:
        for old, new in fixes:
            if old in para:
                para = para.replace(old, new)
        out.append(para)
    for old, _ in fixes:
        if any(old in p for p in out):
            raise SystemExit(f"{key}: correction for {old!r} did not apply")
    return out


def separate_clauses(key, paragraphs):
    """Put ፤ between the repeated praise clauses, as the book prints them."""
    refrain = CLAUSE_SEPARATED.get(key)
    if not refrain:
        return paragraphs
    after = AFTER_SEPARATION.get(key, [])
    out = []
    for para in paragraphs:
        if para.count(refrain) < 3:          # not one of the litany paragraphs
            out.append(para)
            continue
        parts = [p.strip() for p in para.split(refrain) if p.strip()]
        joined = f" {refrain}፤ ".join(parts)
        para = re.sub(r"\s+", " ", joined).strip() + f" {refrain}።"
        for old, new in after:
            para = para.replace(old, new)
        out.append(para)
    return out


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
            "am": separate_clauses(key, correct(key, stanzas(node["content"].get("am", ""), split))),
            "ge": stanzas(node["content"].get("ge", ""), split),
        })

    payload = {
        "contentVersion": 1,
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
