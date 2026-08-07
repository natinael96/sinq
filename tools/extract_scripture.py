"""Generate the app's bundled New Testament from the 80-weahadu Amharic Bible.

Reads  : ../80-weahadu/legacy/am/<n>-<book>.json  (the same 1962-translation
         export the Psalter was built from — matches its orthography exactly)
Writes : app/src/main/assets/content/scripture/<key>.json  + nt-manifest.json

The NT backs the ግጻዌ (Gitsawe) reading links: a Gospel/Epistle reference in the
lectionary opens the passage here. Text is never hand-edited — regenerate with:
    python tools/extract_scripture.py
"""

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LEGACY = ROOT.parent / "80-weahadu" / "legacy" / "am"
OUT = ROOT / "app" / "src" / "main" / "assets" / "content" / "scripture"

# The 27-book New Testament, in canon order. (number, key, Amharic name, English
# name) — Amharic names are the clean canonical forms; the source file for James
# carries a typo (ያዕቆን) and Acts a variant (የሐዋርያት ሥራ), corrected here.
BOOKS = [
    (55, "matthew", "የማቴዎስ ወንጌል", "Matthew"),
    (56, "mark", "የማርቆስ ወንጌል", "Mark"),
    (57, "luke", "የሉቃስ ወንጌል", "Luke"),
    (58, "john", "የዮሐንስ ወንጌል", "John"),
    (59, "acts", "የሐዋርያት ሥራ", "Acts"),
    (60, "romans", "ወደ ሮሜ ሰዎች", "Romans"),
    (61, "1-corinthians", "፩ኛ ወደ ቆሮንቶስ ሰዎች", "1 Corinthians"),
    (62, "2-corinthians", "፪ኛ ወደ ቆሮንቶስ ሰዎች", "2 Corinthians"),
    (63, "galatians", "ወደ ገላትያ ሰዎች", "Galatians"),
    (64, "ephesians", "ወደ ኤፌሶን ሰዎች", "Ephesians"),
    (65, "philippians", "ወደ ፊልጵስዩስ ሰዎች", "Philippians"),
    (66, "colossians", "ወደ ቈላስይስ ሰዎች", "Colossians"),
    (67, "1-thessalonians", "፩ኛ ወደ ተሰሎንቄ ሰዎች", "1 Thessalonians"),
    (68, "2-thessalonians", "፪ኛ ወደ ተሰሎንቄ ሰዎች", "2 Thessalonians"),
    (69, "1-timothy", "፩ኛ ወደ ጢሞቴዎስ", "1 Timothy"),
    (70, "2-timothy", "፪ኛ ወደ ጢሞቴዎስ", "2 Timothy"),
    (71, "titus", "ወደ ቲቶ", "Titus"),
    (72, "philemon", "ወደ ፊልሞና", "Philemon"),
    (73, "hebrews", "ወደ ዕብራውያን", "Hebrews"),
    (74, "1-peter", "፩ኛ የጴጥሮስ መልእክት", "1 Peter"),
    (75, "2-peter", "፪ኛ የጴጥሮስ መልእክት", "2 Peter"),
    (76, "1-john", "፩ኛ የዮሐንስ መልእክት", "1 John"),
    (77, "2-john", "፪ኛ የዮሐንስ መልእክት", "2 John"),
    (78, "3-john", "፫ኛ የዮሐንስ መልእክት", "3 John"),
    (79, "james", "የያዕቆብ መልእክት", "James"),
    (80, "jude", "የይሁዳ መልእክት", "Jude"),
    (81, "revelation", "የዮሐንስ ራዕይ", "Revelation"),
]


def find_source(number: int) -> Path:
    matches = sorted(LEGACY.glob(f"{number}-*.json"))
    if not matches:
        raise FileNotFoundError(f"no legacy/am source for book {number}")
    return matches[0]


def chapter_verses(ch: dict) -> list[dict]:
    """Flatten a chapter's sections into ordered {n, text} verses."""
    out = []
    for sec in ch["sections"]:
        for v in sec["verses"]:
            text = (v.get("text") or "").strip()
            out.append({"n": int(v["verse"]), "text": text})
    return out


def build_book(number: int, key: str, name_am: str, name_en: str) -> tuple[dict, dict]:
    raw = json.loads(find_source(number).read_text(encoding="utf-8"))
    chapters = []
    for ch in raw["chapters"]:
        chapters.append({"chapter": int(ch["chapter"]), "verses": chapter_verses(ch)})
    chapters.sort(key=lambda c: c["chapter"])
    book = {
        "number": number,
        "key": key,
        "nameAm": name_am,
        "nameEn": name_en,
        "chapters": chapters,
    }
    meta = {
        "number": number,
        "key": key,
        "nameAm": name_am,
        "nameEn": name_en,
        "chapters": len(chapters),
    }
    return book, meta


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    metas = []
    total_verses = 0
    total_chars = 0
    for number, key, name_am, name_en in BOOKS:
        book, meta = build_book(number, key, name_am, name_en)
        (OUT / f"{key}.json").write_text(
            json.dumps(book, ensure_ascii=False, separators=(",", ":")), encoding="utf-8"
        )
        metas.append(meta)
        nv = sum(len(c["verses"]) for c in book["chapters"])
        nc = sum(len(v["text"]) for c in book["chapters"] for v in c["verses"])
        total_verses += nv
        total_chars += nc
        print(f"{key:<16} {meta['chapters']:>3} ch  {nv:>5} v  {nc:>7} chars")

    manifest = {"contentVersion": 1, "books": metas}
    (OUT / "nt-manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=1), encoding="utf-8"
    )
    print(f"\nTOTAL  {len(metas)} books  {total_verses} verses  {total_chars} chars -> {OUT}")


if __name__ == "__main__":
    sys.exit(main())
