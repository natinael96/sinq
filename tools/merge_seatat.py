#!/usr/bin/env python3
"""Merge the three scanned recensions of መጽሐፈ ሰዓታት into one book.

The Ge'ez-only copy and the bilingual copy are the same 15-chapter recension;
the bilingual one carries the Amharic beside the Ge'ez for the chapters that
were translated at all.  So the bilingual copy is the spine, the Ge'ez copy
fills the lines it dropped, and every block is tagged ግዕዝ or አማርኛ.

ዘደብረ ዓባይ is a different, fuller recension.  Only the offices it alone carries
are appended, each marked with its provenance, so the reader always knows which
book a chapter came from.
"""
import json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC  = ROOT / "sources/books"
OUT  = ROOT / "sources/generated"
GEZ  = SRC / "መጽሐፈ ሰዓታት በግዕዝ.json"
BI   = SRC / "መጽሐፈ ሰዓታት በግዕዝና አማርኛ.json"
ABAY = SRC / "መጽሐፈ ሰዓታት ዘደብረ ዓባይ.json"

ABAY_SOURCE = "ዘደብረ ዓባይ"

# Where the scans came from: the printed መጽሐፈ ሰዓታት the Ethiopian Orthodox
# Tewahedo Church itself publishes, as the PDF on its own site. Recorded here
# and in NOTICE; not written into the shipped book, where a URL beside three
# copy names would be noise a reader cannot use.
ORIGIN = "https://www.ethiopianorthodox.org/amharic/holybooks/Metsehafeseatat.pdf"

# Offices ዘደብረ ዓባይ alone carries, by its own chapter number.  Measured: each of
# these overlaps the bilingual book by less than a tenth of its lines.
ABAY_ONLY = {
    7:  "መልክአ ፍልሰታ",
    8:  "በሌሊት አንሥኡ እደዊክሙ",
    16: "መልክዐ ውዳሴ ዘ፯ቱ ዕለታት",
    17: "ጸሎተ ኪዳን",
    18: "ሊጦን ዘ፯ቱ ዕለታት ወዘበዓላት",
    19: "መስተብቍዕ ካልእ",
    20: "ዘይነግሥ",
    21: "ፍትሐት ዘወልድ ወበእንተ ቅድሳት",
    22: "በእንተ ብፁዕ ወብፅዕት",
    23: "በእንተ ትምህርተ ኅቡአት",
    24: "ሥርዓት",
}

# ዘደብረ ዓባይ's መቅድመ ተአምራት is six times the length of the one in the other two
# copies, so that chapter is taken from there instead and marked as such.
# Keyed by the shared title, not by a chapter number — the three books number
# their chapters differently.
ABAY_FULLER = {"መቅድመ ተአምራት": 14}

# 25 የባሕር ሐሳብ ሠንጠረዥ is 91 blocks and 14 characters — the ባሕረ ሓሳብ table was a
# grid the scan could not carry.  15 ምልጣን is 2 blocks and 14 characters.
DROP_ABAY = {25}
DROP_SHARED = {15}

# Chapter titles as the scan left them → the spelling to ship.
TITLE_FIXES = {
    "መጽሐፈ ሰዓታት በግዕ ዝና አማርኛ": "መቅድም",
    "መስቀብቍዕ": "መስተብቍዕ ዘጽባሕ",
    "ለኖኅ ሐመሩ።": "ለኖኅ ሐመሩ",
    "መቅድም ምስለ ተአምራት": "መቅድመ ተአምራት",
    "እሴብሕ ፀጋኪ": "እሴብሕ ጸጋኪ",
    "ተፈሥሒ": "ተፈሥሒ ማርያም",
    "ዘይነግሥ።": "ዘይነግሥ",
    "ፍትሐት ዘወልድ ወበእንተ ቅድሳት።": "ፍትሐት ዘወልድ ወበእንተ ቅድሳት",
}

# Word-initial የ is the Amharic genitive and never opens a Ge'ez word; the rest
# are Amharic verb and copula endings.  Used only where the Ge'ez copy is silent.
AMHARIC = re.compile(r"(?:^|\s)(?:የ|እን[ደዲ]|ስለ|ሲ|ብት)|ናቸው|ነው|ነበር|ዘንድ|ችሁ|ኛል|ናል|ሆይ|ጋር|ውስጥ")
GEEZ    = re.compile(r"(?:^|\s)(?:ወ|ዘ|እም|ኀበ|ከመ|እስመ|እንዘ|ላዕለ|ኵሉ)|ውእቱ|ሆሙ|ኪያ")


def norm(s):
    return re.sub(r"[\s።፡፣፤፥.›]+", "", s or "")


def probes(text):
    n = norm(text)
    if len(n) < 30:
        return []
    mid = len(n) // 2
    return [p for p in (n[:24], n[mid - 12:mid + 12], n[-24:]) if p]


def contains(haystack, text):
    """Is this block's text somewhere in that chapter? Segmentation-proof."""
    p = probes(text)
    return bool(p) and any(x in haystack for x in p)


def language(text, geez_text):
    """ግዕዝ if the Ge'ez-only copy has this line; otherwise judge it lexically."""
    if contains(geez_text, text):
        return "gez"
    am, gz = len(AMHARIC.findall(text)), len(GEEZ.findall(text))
    return "amh" if am > gz else "gez"


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def chapter_text(ch):
    return norm(" ".join(b.get("text", "") for b in ch["blocks"]))


def clean(block, lang=None, source=None):
    out = {"type": block.get("type", "paragraph"), "text": block.get("text", "")}
    if block.get("level"):
        out["level"] = block["level"]
    if lang:
        out["lang"] = lang
    if source:
        out["source"] = source
    return out


def title_of(ch):
    t = ch.get("title", "").strip()
    return TITLE_FIXES.get(t, t)


def merge():
    gez, bi, abay = load(GEZ), load(BI), load(ABAY)
    gez_by_title = {title_of(c): chapter_text(c) for c in gez["chapters"]}
    abay_by_num = {c["number"]: c for c in abay["chapters"]}

    chapters = []
    for ch in bi["chapters"]:
        if ch["number"] in DROP_SHARED:
            continue
        title = title_of(ch)

        if title in ABAY_FULLER:
            src = abay_by_num[ABAY_FULLER[title]]
            blocks = [clean(b, language(b.get("text", ""), ""))
                      for b in src["blocks"] if b.get("text", "").strip()]
            chapters.append({"title": title, "source": ABAY_SOURCE, "blocks": blocks})
            continue

        geez_text = gez_by_title.get(title, "")
        blocks = [clean(b, language(b.get("text", ""), geez_text))
                  for b in ch["blocks"] if b.get("text", "").strip()]

        # Lines the bilingual scan dropped: fold them back in from the Ge'ez copy,
        # after the block they follow there, so the order of the office survives.
        here = norm(" ".join(b["text"] for b in blocks))
        for src in gez["chapters"]:
            if title_of(src) != title:
                continue
            pending = []
            for b in src["blocks"]:
                t = b.get("text", "").strip()
                if not t:
                    continue
                if contains(here, t):
                    if pending:
                        at = next((i for i, x in enumerate(blocks)
                                   if contains(norm(x["text"]), t)), len(blocks))
                        for k, p in enumerate(pending):
                            blocks.insert(at + k, clean(p, "gez"))
                        pending = []
                elif probes(t):
                    pending.append(b)
            blocks.extend(clean(p, "gez") for p in pending)

        chapters.append({"title": title, "source": None, "blocks": blocks})

    for num, title in sorted(ABAY_ONLY.items()):
        if num in DROP_ABAY:
            continue
        src = abay_by_num[num]
        blocks = [clean(b, language(b.get("text", ""), "")) 
                  for b in src["blocks"] if b.get("text", "").strip()]
        chapters.append({"title": title, "source": ABAY_SOURCE, "blocks": blocks})

    for i, c in enumerate(chapters, 1):
        c["number"] = i

    return {
        "title": "መጽሐፈ ሰዓታት",
        "category": "የዜማ መጻሕፍት",
        "sources": [
            {"name": "በግዕዝና አማርኛ", "role": "spine"},
            {"name": "በግዕዝ", "role": "fills the lines the bilingual scan dropped"},
            {"name": ABAY_SOURCE, "role": "the offices it alone carries"},
        ],
        "chapters": chapters,
    }


def main():
    book = merge()
    OUT.mkdir(parents=True, exist_ok=True)
    out = OUT / "መጽሐፈ ሰዓታት.json"
    out.write_text(json.dumps(book, ensure_ascii=False, indent=1), encoding="utf-8")

    gez = amh = 0
    print("%-3s %-28s %-12s %6s %6s %6s" % ("#", "chapter", "source", "blocks", "ግዕዝ", "አማርኛ"))
    for c in book["chapters"]:
        g = sum(1 for b in c["blocks"] if b.get("lang") == "gez")
        a = sum(1 for b in c["blocks"] if b.get("lang") == "amh")
        gez += g; amh += a
        print("%-3d %-28s %-12s %6d %6d %6d"
              % (c["number"], c["title"][:26], c["source"] or "ሁለቱም", len(c["blocks"]), g, a))
    print("\n%d chapters, %d blocks — %d ግዕዝ, %d አማርኛ" %
          (len(book["chapters"]), gez + amh, gez, amh))
    print("wrote", out.name)


if __name__ == "__main__":
    main()
