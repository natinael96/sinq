#!/usr/bin/env python3
"""Build the ሌሎች መጻሕፍት shelf from the scanned church books in the repo root.

Ninety-one books, three million characters, scanned from printed copies. The
scans are the upstream: nothing here is edited by hand, so every correction
lives in this file as an asserted swap and is re-applied on every run.

Writes assets/content/books/index.json (the shelf) and one file per book, so a
reader loads only the book it opens rather than nine megabytes of everything.
"""
import hashlib, json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
# The scans, and the one book built from several of them by merge_seatat.py.
SOURCES = [ROOT / "sources/books", ROOT / "sources/generated"]
OUT = ROOT / "app/src/main/assets/content/books"

# The three unmerged መጽሐፈ ሰዓታት copies; tools/merge_seatat.py folds them into one.
SKIP = {
    "መጽሐፈ ሰዓታት በግዕዝ",
    "መጽሐፈ ሰዓታት በግዕዝና አማርኛ",
    "መጽሐፈ ሰዓታት ዘደብረ ዓባይ",
}

# The Church's own kinds, in the order a catechism lists them.
SHELVES = [
    ("zema",   "የዜማ መጻሕፍት",     "ማኅሌትና ሰዓታት የሚደረስባቸው"),
    ("melkie", "መልክአ ቅዱሳን",      "የቅዱሳን መልክእ"),
    # Empty since the ድርሳናት and the ቅዳሴ scans were taken out of the project.
    # Kept because shelf_of still routes ድርሳነ- and ቅዳሴ-titled books here, and a
    # book routed to a shelf this list does not name is dropped from the index
    # without a word. An empty shelf costs nothing: only shelves with books are
    # written out.
    ("dersan", "ድርሳናትና ተአምራት",   "የመላእክትና የጌታ ድርሳናት"),
    ("gedl",   "ገድላት",           "የቅዱሳን ገድል"),
    ("kidase", "የቅዳሴ መጻሕፍት",     "ሥርዓተ ቅዳሴ"),
    ("tselot", "የጸሎት መጻሕፍት",     "የነቢያት ጸሎት"),
]

# The printed collection every መልክእ on the shelf was scanned out of. The
# individual scans carry no provenance of their own, so it is recorded here
# rather than repeated into seventy-odd source files by hand.
MELKIE_SOURCE = "መልክዐ ጉባኤ"

# Books whose shelf the title alone does not give away.
SHELF_BY_TITLE = {
    "ማኅሌተ ጽጌ": "zema",
    "መጽሐፈ ሰዓታት": "zema",
    "ሰቆቃወ ድንግል": "zema",
}

# Title as the scan left it → the spelling to ship. Broken words in a title are
# worse than broken words in a page: the title is the only thing on the shelf.
TITLE_FIXES = {
    "መልክአ ቍርባን ።": "መልክአ ቍርባን",
}

# Words the scan ran together or split, seen while reading the corpus. Each is
# asserted: if a fix stops matching, the upstream changed and this must be read
# again rather than silently skipped.
CORRECTIONS = [
    ("በግዕ ዝና አማርኛ", "በግዕዝና አማርኛ"),
]

# ፪ማ = ፪ኛ ማዕረግ, the second tier of a stanza, and the bare ማ፡ the one after it.
# Both are the printer's marks for where a line of the stanza breaks, not words
# of the hymn: 89% of the lines either side of one rhyme with each other, and
# only one marked block in 1,292 opens a new ሰላም stanza against a 26% base rate.
# So they become the line break they stand for, and the mark itself goes.
# The bare form needs a space on both sides or it eats words ending in ማ (ሰማ፡).
TIER_MARK = re.compile(r"\s*(?:፪ማ|(?<=\s)ማ)[፡:]\s*")

# Word-initial የ is the Amharic genitive and never opens a Ge'ez word.
AMHARIC = re.compile(r"(?:^|\s)(?:የ|እን[ደዲ]|ስለ|ሲ|ብት)|ናቸው|ነው|ነበር|ዘንድ|ችሁ|ኛል|ናል|ሆይ|ጋር|ውስጥ")
GEEZ = re.compile(r"(?:^|\s)(?:ወ|ዘ|እም|ኀበ|ከመ|እስመ|እንዘ|ላዕለ|ኵሉ)|ውእቱ|ሆሙ|ኪያ")

# A ማሳሰቢያ with nothing after the marker is a heading the scan lost the text of.
# The notice itself is kept wherever it still says something — "ማሳሰቢያ፦ ቅደም ተከሉ
# እንደ ግብረ ሕማማቱ ነው" tells the singer which order to follow, which is exactly the
# kind of thing a chant book is read for.
EMPTY_NOTICE = re.compile(r"^ማሳሰቢያ[፦:：]?\s*$")

# ሀ ሐ ኀ ኸ · ሰ ሠ · አ ዐ · ጸ ፀ sound alike and the scans spell them both ways.
# This mirrors AmharicSearch.foldChar series for series — each family folds to
# one consonant keeping its vowel order — so a key built here matches a name
# folded in the app. Used only for matching, never for display.
FOLD_SERIES = [(0x1220, 0x1230), (0x1210, 0x1200), (0x1280, 0x1200),
               (0x12B8, 0x1200), (0x12D0, 0x12A0), (0x1340, 0x1338)]


def fold(s):
    out = []
    for c in s:
        cp = ord(c)
        for start, target in FOLD_SERIES:
            if start <= cp <= start + 7:
                c = chr(target + cp - start)
                break
        out.append(c)
    return re.sub(r"\s+", " ", "".join(out)).strip()


def shelf_of(title):
    if title in SHELF_BY_TITLE:
        return SHELF_BY_TITLE[title]
    if title.startswith(("መልክአ", "መልክዐ", "መልክእ")):
        return "melkie"
    if title.startswith("ድርሳነ"):
        return "dersan"
    if title.startswith("ገድለ"):
        return "gedl"
    if "ቅዳሴ" in title:
        return "kidase"
    if title.startswith("ጸሎት"):
        return "tselot"
    return "melkie"


def language(text):
    am, gz = len(AMHARIC.findall(text)), len(GEEZ.findall(text))
    return "amh" if am > gz else "gez"


def clean_text(raw):
    """Strip the tier marks, mend the scan, and normalise the whitespace."""
    t = TIER_MARK.sub("\n", raw or "")
    t = re.sub(r"[ \t]+", " ", t)
    t = re.sub(r"\n\s*\n+", "\n", t)
    return t.strip()


def book_id(title):
    """Stable across runs and across books being added or renumbered."""
    return hashlib.sha1(title.encode("utf-8")).hexdigest()[:10]


def build():
    applied = {before: 0 for before, _ in CORRECTIONS}
    books, seen_ids = [], {}

    # A generated file shadows a raw scan of the same name. ጸሎት ነቢያት በአማርኛ is
    # the case: the scan stops at fifteen chapters and merge_tselot_nebiyat.py
    # writes the same title with twenty, so the title alone cannot tell them
    # apart the way SKIP does for the ሰዓታት copies. Same title, same id — the
    # replacement keeps every bookmark that pointed at the shorter book.
    generated = {p.stem for p in (ROOT / "sources/generated").glob("*.json")}
    shadowed = set()
    for path in sorted(p for d in SOURCES for p in d.glob("*.json")):
        raw_title = path.stem
        if raw_title in SKIP:
            continue
        if path.parent.name == "books" and raw_title in generated:
            shadowed.add(raw_title)
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        if "chapters" not in data:
            continue

        title = TITLE_FIXES.get(raw_title, raw_title)
        bid = book_id(title)
        if bid in seen_ids:
            sys.exit(f"id collision: {title} and {seen_ids[bid]}")
        seen_ids[bid] = title

        chapters, chars, blocks_n, amh = [], 0, 0, 0
        for ch in data["chapters"]:
            blocks = []
            for b in ch.get("blocks", []):
                text = b.get("text", "")
                for before, after in CORRECTIONS:
                    if before in text:
                        text = text.replace(before, after)
                        applied[before] += 1
                text = clean_text(text)
                # An empty table_row is a cell the scan could not read; an empty
                # paragraph is nothing at all. Neither belongs in a reader.
                if not text or EMPTY_NOTICE.match(text):
                    continue
                block = {"type": b.get("type", "paragraph"), "text": text}
                if b.get("level"):
                    block["level"] = b["level"]
                lang = b.get("lang") or language(text)
                block["lang"] = lang
                if lang == "amh":
                    amh += 1
                chars += len(text)
                blocks.append(block)
            if not blocks:
                continue
            ch_title = ch.get("title", "").strip()
            for before, after in CORRECTIONS:
                if before in ch_title:
                    ch_title = ch_title.replace(before, after)
                    applied[before] += 1
            entry = {"number": len(chapters) + 1, "title": ch_title, "blocks": blocks}
            if ch.get("source"):
                entry["source"] = ch["source"]
            chapters.append(entry)
            blocks_n += len(blocks)

        if not chapters:
            continue
        # A book whose chapters are all untitled reads better as one run of text
        # than as a chapter list with nothing to pick from.
        titled = sum(1 for c in chapters if c["title"])
        books.append({
            "id": bid,
            "title": title,
            "shelf": shelf_of(title),
            "chapters": chapters,
            "meta": {
                "id": bid,
                "title": title,
                "shelf": shelf_of(title),
                "chapterCount": len(chapters),
                "blockCount": blocks_n,
                "charCount": chars,
                "lang": "amh" if amh > blocks_n * 0.6 else ("mixed" if amh > blocks_n * 0.05 else "gez"),
                "titledChapters": titled,
                # What a ማኅሌት part name has to match to become a door into it.
                # The full title, ካልዕ included: a part named መልክአ ሚካኤል means the
                # first recension, and folding the suffix away let the second one
                # answer to the first one's name.
                "key": fold(title),
                # Where the scan came from. A merged book names the copies it
                # was folded from; every መልክእ came out of the same printed
                # collection, መልክዐ ጉባኤ, which the individual scans do not say
                # on their own — so the shelf says it for them.
                "sources": (
                    [s["name"] for s in data.get("sources", [])]
                    or ([MELKIE_SOURCE] if shelf_of(title) == "melkie" else [])
                    or None
                ),
            },
        })

    # A book named "X ካልዕ" is a second recension of X. Each points at the other,
    # so a reader who opens one is told the other exists — the shelf lists them
    # adjacently but says nothing about their being the same hymn twice.
    by_title = {b["title"]: b for b in books}
    for b in books:
        base = re.sub(r"\s+ካልዕ$", "", b["title"])
        if base == b["title"] or base not in by_title:
            continue
        other = by_title[base]
        b["meta"]["variantOf"] = {"id": other["id"], "title": other["title"]}
        other["meta"].setdefault("variants", []).append({"id": b["id"], "title": b["title"]})

    # Every generated book that claims to replace a scan must actually have one
    # to replace; a stray file in sources/generated/ would otherwise ship as a
    # second copy under a name nothing else knows.
    orphans = generated - shadowed - {"መጽሐፈ ሰዓታት", "ማኅሌተ ጽጌ", "ሰቆቃወ ድንግል"}
    if orphans:
        sys.exit(f"generated with nothing to replace: {sorted(orphans)}")
    if shadowed:
        print("shadowed by sources/generated/: " + ", ".join(sorted(shadowed)))

    missing = [b for b, n in applied.items() if n == 0]
    if missing:
        sys.exit("correction no longer matches upstream: " + "; ".join(missing))
    return books


def main():
    books = build()
    OUT.mkdir(parents=True, exist_ok=True)
    for stale in OUT.glob("*.json"):
        stale.unlink()

    by_shelf = {}
    for b in books:
        payload = {"id": b["id"], "title": b["title"], "shelf": b["shelf"],
                   "chapters": b["chapters"]}
        (OUT / f"{b['id']}.json").write_text(
            json.dumps(payload, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        by_shelf.setdefault(b["shelf"], []).append(b["meta"])

    shelves = []
    for key, name, subtitle in SHELVES:
        items = sorted(by_shelf.get(key, []), key=lambda m: m["title"])
        if items:
            shelves.append({"key": key, "name": name, "subtitle": subtitle, "books": items})
    (OUT / "index.json").write_text(
        json.dumps({"contentVersion": 1, "shelves": shelves},
                   ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    total_chars = sum(b["meta"]["charCount"] for b in books)
    size = sum(f.stat().st_size for f in OUT.glob("*.json"))
    for sh in shelves:
        print("%-16s %3d books  %9d chars" %
              (sh["name"], len(sh["books"]), sum(b["charCount"] for b in sh["books"])))
    print("\n%d books, %d chapters, %d chars, %.1f MB on disk" %
          (len(books), sum(b["meta"]["chapterCount"] for b in books), total_chars, size / 1e6))


if __name__ == "__main__":
    main()
