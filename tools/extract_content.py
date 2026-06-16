"""Generate the app's bundled prayer content from the 80-weahadu Amharic Bible.

Reads  : content/hour_mapping.json  +  ../80-weahadu/data/am/*.json
Writes : app/src/main/assets/content/<hourId>.json  +  manifest.json

Run from the repo root:  python tools/extract_content.py
"""

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BIBLE = ROOT.parent / "80-weahadu" / "data" / "am"
MAPPING = ROOT / "content" / "hour_mapping.json"
OUT = ROOT / "app" / "src" / "main" / "assets" / "content"

CONTENT_VERSION = 1

HOUR_META = {
    "morning":  {"order": 0, "name": "ጸሎተ ነግህ",        "translit": "Tselote Negh",    "timeHint": "ጠዋት"},
    "terce":    {"order": 1, "name": "ጸሎተ ሠለስት",       "translit": "Tselote Selest",  "timeHint": "ረፋድ"},
    "sext":     {"order": 2, "name": "ጸሎተ ቀትር",        "translit": "Tselote Ketr",    "timeHint": "እኩለ ቀን"},
    "none":     {"order": 3, "name": "ጸሎተ ተሰዓት",       "translit": "Tselote Tese'at", "timeHint": "ከሰዓት"},
    "vespers":  {"order": 4, "name": "ጸሎተ ሰርክ",        "translit": "Tselote Serk",    "timeHint": "ምሽት"},
    "compline": {"order": 5, "name": "ጸሎተ ንዋም",        "translit": "Tselote Nwam",    "timeHint": "ከመኝታ በፊት"},
    "midnight": {"order": 6, "name": "ጸሎተ መንፈቀ ሌሊት", "translit": "Menfeqe Lelit",   "timeHint": "እኩለ ሌሊት"},
    "veil":     {"order": 7, "name": "ጸሎተ ሥውር",        "translit": "Tselote Sewur",   "timeHint": "ከንዋም በኋላ"},
}

WATCH_NAMES = {"watch1": "ክፍል ፩", "watch2": "ክፍል ፪", "watch3": "ክፍል ፫"}

ONES = ["", "፩", "፪", "፫", "፬", "፭", "፮", "፯", "፰", "፱"]
TENS = ["", "፲", "፳", "፴", "፵", "፶", "፷", "፸", "፹", "፺"]


def geez(n: int) -> str:
    """Ge'ez numerals for 1..199 (enough for psalms and verse refs)."""
    if n >= 100:
        return "፻" + geez(n - 100) if n > 100 else "፻"
    return TENS[n // 10] + ONES[n % 10]


def load_book(filename: str) -> dict:
    with open(BIBLE / filename, encoding="utf-8") as f:
        return json.load(f)


def chapter(book: dict, num: int) -> dict:
    for ch in book["chapters"]:
        if int(ch["chapter"]) == num:
            return ch
    raise KeyError(f"{book['book_name_en']} has no chapter {num}")


def chapter_verses(ch: dict) -> list[dict]:
    out = []
    for sec in ch["sections"]:
        out.extend(sec["verses"])
    return out


def psalm_section(psalms_book: dict, hour_id: str, order: int, num: int) -> dict:
    ch = chapter(psalms_book, num)
    superscription = (ch["sections"][0].get("title") or "").strip() or None
    verses = [v["text"].strip() for v in chapter_verses(ch)]
    return {
        "id": f"{hour_id}_ps{num}",
        "orderIndex": order,
        "type": "psalm",
        "title": f"መዝሙር {geez(num)}",
        "subtitle": superscription,
        "reference": f"Ps {num}",
        "firstVerse": 1,
        "verses": verses,
    }


def psalm118_stanzas(psalms_book: dict, hour_id: str, order: int, from_st: int, to_st: int) -> list[dict]:
    ch = chapter(psalms_book, 118)
    verses = chapter_verses(ch)
    assert len(verses) == 176, f"Psalm 118 expected 176 verses, got {len(verses)}"
    sections = []
    for st in range(from_st, to_st + 1):
        chunk = verses[(st - 1) * 8: st * 8]
        sections.append({
            "id": f"{hour_id}_ps118_s{st}",
            "orderIndex": order,
            "type": "psalm",
            "title": f"መዝሙር ፻፲፰ — {geez(st)}",
            "subtitle": None,
            "reference": f"Ps 118:{(st - 1) * 8 + 1}-{st * 8}",
            "firstVerse": (st - 1) * 8 + 1,
            "verses": [v["text"].strip() for v in chunk],
        })
        order += 1
    return sections


def gospel_section(books: dict, hour_id: str, order: int, ref: dict, idx: int) -> dict:
    book = books[ref["book"]]
    ch = chapter(book, ref["chapter"])
    lo, hi = ref["fromVerse"], ref["toVerse"]
    verses = [v["text"].strip() for v in chapter_verses(ch) if lo <= int(v["verse"]) <= hi]
    assert verses, f"empty gospel range {ref}"
    ref_str = f"{book['book_name_am']} {geez(ref['chapter'])}፥{geez(lo)}–{geez(hi)}"
    return {
        "id": f"{hour_id}_gospel_{idx}",
        "orderIndex": order,
        "type": "gospel",
        "title": "ወንጌል",
        "subtitle": ref_str,
        "reference": f"{book['book_name_en']} {ref['chapter']}:{lo}-{hi}",
        "firstVerse": lo,
        "verses": verses,
    }


def build_hour(hour: dict, books: dict) -> dict:
    hour_id = hour["id"]
    psalms_book = books["psalms"]
    sections = []
    order = 0

    def add_psalms(nums, part=None):
        nonlocal order
        for n in nums:
            s = psalm_section(psalms_book, hour_id, order, n)
            if part:
                s["part"] = part
                s["id"] = f"{hour_id}_{part_key}_ps{n}"
            sections.append(s)
            order += 1

    if "watches" in hour:
        for w in hour["watches"]:
            part = WATCH_NAMES[w["name"]]
            part_key = w["name"]
            for n in w["psalms"]:
                s = psalm_section(psalms_book, hour_id, order, n)
                s["part"] = part
                s["id"] = f"{hour_id}_{part_key}_ps{n}"
                sections.append(s)
                order += 1
            if "psalm118Stanzas" in w:
                st = w["psalm118Stanzas"]
                for s in psalm118_stanzas(psalms_book, f"{hour_id}_{part_key}", order, st["from"], st["to"]):
                    s["part"] = part
                    sections.append(s)
                    order += 1
            for i, ref in enumerate(w["gospel"]):
                s = gospel_section(books, f"{hour_id}_{part_key}", order, ref, i)
                s["part"] = part
                sections.append(s)
                order += 1
    else:
        for n in hour["psalms"]:
            sections.append(psalm_section(psalms_book, hour_id, order, n))
            order += 1
        if "psalm118Stanzas" in hour:
            st = hour["psalm118Stanzas"]
            for s in psalm118_stanzas(psalms_book, hour_id, order, st["from"], st["to"]):
                sections.append(s)
                order += 1
        for i, ref in enumerate(hour["gospel"]):
            sections.append(gospel_section(books, hour_id, order, ref, i))
            order += 1

    meta = HOUR_META[hour_id]
    return {
        "id": hour_id,
        "orderIndex": meta["order"],
        "name": meta["name"],
        "transliteration": meta["translit"],
        "timeHint": meta["timeHint"],
        "sections": sections,
    }


def main() -> None:
    mapping = json.loads(MAPPING.read_text(encoding="utf-8"))
    books = {key: load_book(fn) for key, fn in mapping["books"].items()}

    OUT.mkdir(parents=True, exist_ok=True)
    manifest_hours = []
    total_sections = 0
    total_chars = 0

    for hour in mapping["hours"]:
        data = build_hour(hour, books)
        out_file = OUT / f"{hour['id']}.json"
        out_file.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        n_sec = len(data["sections"])
        n_chars = sum(len(v) for s in data["sections"] for v in s["verses"])
        total_sections += n_sec
        total_chars += n_chars
        manifest_hours.append({"id": hour["id"], "file": f"{hour['id']}.json"})
        print(f"{hour['id']:<10} {n_sec:>3} sections  {n_chars:>7} chars")

    manifest = {"contentVersion": CONTENT_VERSION, "hours": manifest_hours}
    (OUT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\nTOTAL      {total_sections} sections  {total_chars} chars -> {OUT}")

    # Full Psalter (all 150) so users can add any psalm to any hour.
    psalter = build_psalter(books)
    (OUT / "psalms.json").write_text(json.dumps(psalter, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"PSALTER    {len(psalter['psalms'])} psalms -> psalms.json")


def build_psalter(books: dict) -> dict:
    psalms_book = books["psalms"]
    out = []
    for ch in psalms_book["chapters"]:
        num = int(ch["chapter"])
        superscription = (ch["sections"][0].get("title") or "").strip() or None
        verses = [v["text"].strip() for v in chapter_verses(ch)]
        out.append({
            "id": f"ps_{num}",
            "number": num,
            "orderIndex": num,
            "type": "psalm",
            "title": f"መዝሙር {geez(num)}",
            "subtitle": superscription,
            "reference": f"Ps {num}",
            "firstVerse": 1,
            "verses": verses,
        })
    return {"psalms": out}


if __name__ == "__main__":
    sys.exit(main())
