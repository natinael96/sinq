"""Fill missing daily ግጻዌ entries from the compiled month JSON files.

Existing entries are deliberately preserved: they contain translations and
synaxarium notes which are not present in the scan transcription. Their missing
evening office is added from the transcription. New entries retain the Ge'ez
incipits, misbak text, scripture references, all three offices, and the printed
citation as provenance.

Usage:
  python3 tools/import_gitsawe_months.py /path/to/new/json/months
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/assets/content/gitsawe/daily-gitsawe.json"

DIGITS = {"፩": 1, "፪": 2, "፫": 3, "፬": 4, "፭": 5, "፮": 6, "፯": 7,
          "፰": 8, "፱": 9, "፲": 10, "፳": 20, "፴": 30, "፵": 40,
          "፶": 50, "፷": 60, "፸": 70, "፹": 80, "፺": 90}


def geez_number(value):
    """Parse the additive/multiplicative Ethiopic numbers used in the source."""
    value = re.sub(r"[^፩-፼]", "", value or "")
    if not value:
        return None
    total = group = 0
    for char in value:
        if char in DIGITS:
            group += DIGITS[char]
        elif char == "፻":
            group = (group or 1) * 100
        elif char == "፼":
            total = (total + (group or 1)) * 10_000
            group = 0
    return total + group


def title_for(raw, fallback=None):
    raw = (raw or "").replace("ዓዲ", "").replace("ዘቅዳሴ", "").strip()
    if fallback == "psalm" or "መዝ" in raw or "ምስ" in raw:
        return "መዝሙረ ዳዊት"
    names = (
        (("ማቴ",), "የማቴዎስ ወንጌል"), (("ማር",), "የማርቆስ ወንጌል"),
        (("ሉቃ",), "የሉቃስ ወንጌል"), (("ዮሐ",), "የዮሐንስ ወንጌል"),
        (("ግብ",), "ግብረ ሐዋርያት"), (("ሮሜ",), "ወደ ሮሜ ሰዎች"),
        (("ቆሮ", "ቆር"), "ወደ ቆሮንቶስ ሰዎች"), (("ገላ", "ጌላ"), "ወደ ገላትያ ሰዎች"),
        (("ኤፌ", "ፌሶ"), "ወደ ኤፌሶን ሰዎች"), (("ፊልጵ", "ፈልጽ"), "ወደ ፊልጵስዩስ ሰዎች"),
        (("ቈላ", "ቄላ", "ቴላ"), "ወደ ቆላስይስ ሰዎች"), (("ተሰሎ", "ተስሎ"), "ወደ ተሰሎንቄ ሰዎች"),
        (("ጢሞ",), "ወደ ጢሞቴዎስ"), (("ቲቶ",), "ወደ ቲቶ"),
        (("ፊልሞ",), "ወደ ፊልሞና"), (("ዕብ",), "ወደ ዕብራውያን"),
        (("ያዕ",), "የያዕቆብ መልእክት"), (("ጴጥ",), "የጴጥሮስ መልእክት"),
        (("ይሁዳ",), "የይሁዳ መልእክት"), (("ራእ", "ራዕ"), "የዮሐንስ ራእይ"),
    )
    for needles, title in names:
        if any(n in raw for n in needles):
            suffix = geez_number(raw)
            if fallback != "gospel" and suffix in (1, 2, 3) and any(n in raw for n in ("ቆሮ", "ቆር", "ተሰሎ", "ጢሞ", "ዮሐ", "ጴጥ")):
                return f"{title} {suffix}"
            return title
    return None


def verse_ref(citation, book, psalm=False):
    citation = citation or ""
    clean = citation.replace("ỻ", "")
    if psalm:
        before, _, after = re.sub(r"[·፡፤]", " ", clean).partition("ቍ")
        chapter = geez_number(before)
    else:
        normalized = re.sub(r"[·፡፤]", " ", clean)
        before, _, after = normalized.partition("ቍ")
        chapter = geez_number(before.partition("ም")[2])
    nums = [geez_number(x) for x in re.findall(r"[፩-፼]+", after)]
    nums = [x for x in nums if x is not None]
    ref = {"bookTitle": book, "chapter": chapter, "citation": citation.strip()}
    if nums:
        ref["start"] = nums[0]
        if len(nums) > 1:
            ref["end"] = nums[-1]
    return ref


def valid_chapter(book, chapter):
    if chapter is None:
        return False
    limits = {
        "መዝሙረ ዳዊት": 150, "የማቴዎስ ወንጌል": 28, "የማርቆስ ወንጌል": 16,
        "የሉቃስ ወንጌል": 24, "የዮሐንስ ወንጌል": 21, "ግብረ ሐዋርያት": 28,
        "ወደ ሮሜ ሰዎች": 16, "ወደ ገላትያ ሰዎች": 6, "ወደ ኤፌሶን ሰዎች": 6,
        "ወደ ፊልጵስዩስ ሰዎች": 4, "ወደ ቆላስይስ ሰዎች": 4,
        "ወደ ቲቶ": 3, "ወደ ፊልሞና": 1, "ወደ ዕብራውያን": 13,
        "የያዕቆብ መልእክት": 5, "የይሁዳ መልእክት": 1, "የዮሐንስ ራእይ": 22,
    }
    if book.startswith("ወደ ቆሮንቶስ"):
        limit = 16 if book.endswith(" 1") else 13
    elif book.startswith("ወደ ተሰሎንቄ"):
        limit = 5 if book.endswith(" 1") else 3
    elif book.startswith("ወደ ጢሞቴዎስ"):
        limit = 6 if book.endswith(" 1") else 4
    elif book.startswith("የጴጥሮስ"):
        limit = 5 if book.endswith(" 1") else 3
    elif book.startswith("የዮሐንስ ወንጌል"):
        limit = 21
    elif book.startswith("የዮሐንስ መልእክት"):
        limit = {" 1": 5, " 2": 1, " 3": 1}.get(book[-2:], 5)
    else:
        limit = limits.get(book)
    return limit is not None and 1 <= chapter <= limit


def reading(item, kind, forced_title=None):
    is_psalm = kind == "msbak"
    book = forced_title or title_for(item.get("book"), "psalm" if is_psalm else "gospel")
    text = " ".join(item.get("verses", [])) if is_psalm else item.get("incipit")
    result = {"text": {"geez": text.strip()} if text else None,
              "citation": (item.get("chapter_verse") or "").strip() or None}
    if book:
        ref = verse_ref(item.get("chapter_verse"), book, is_psalm)
        if valid_chapter(book, ref.get("chapter")):
            result["verse"] = ref
    return {k: v for k, v in result.items() if v is not None}


def convert_service(source):
    out = {"msbak": [], "wengel": [], "firstDeacon": [], "secondDeacon": [],
           "secondKahn": [], "kidassie": []}
    for key, item in source.items():
        if not isinstance(item, dict):
            continue
        if "ምስባክ" in key:
            out["msbak"].append(reading(item, "msbak"))
        elif "ወንጌ" in key or "ወንጌ" in key.replace("ל", "ል"):
            out["wengel"].append(reading(item, "gospel"))
    slots = ("firstDeacon", "secondDeacon", "secondKahn")
    for slot, item in zip(slots, source.get("epistles_and_acts", [])):
        out[slot].append(reading(item, "epistle", title_for(item.get("reading_type"))))
    kidassie = source.get("ቅዳሴ")
    if kidassie:
        out["kidassie"] = [kidassie.strip()]
    return {k: v for k, v in out.items() if v}


def main(months_dir):
    existing = json.loads(OUT.read_text(encoding="utf-8"))
    by_date = {entry["date"]: entry for entry in existing}
    added = []
    enriched = []
    for path in sorted(months_dir.glob("*.json")):
        month = json.loads(path.read_text(encoding="utf-8"))
        month_num = month["month_index"]
        for day in month["days"]:
            day_num = geez_number(day["day_number"])
            key = f"{day_num:02d}-{month_num:02d}"
            if key in by_date:
                services = day.get("services", {})
                imported = any(
                    reading.get("citation") or (reading.get("verse") or {}).get("citation")
                    for office in ("negh", "kidassie")
                    for role in ("msbak", "wengel", "firstDeacon", "secondDeacon", "secondKahn")
                    for reading in by_date[key].get(office, {}).get(role, [])
                )
                if imported:
                    for source_key, target_key in (("ዘነግህ", "negh"), ("ዘቅዳሴ", "kidassie"), ("ዘሠርክ", "serk")):
                        if source_key in services:
                            by_date[key][target_key] = convert_service(services[source_key])
                if "ዘሠርክ" in services:
                    by_date[key]["serk"] = convert_service(services["ዘሠርክ"])
                    enriched.append(key)
                continue
            services = day.get("services", {})
            entry = {"date": key, "title": day.get("commemoration", "").strip()}
            for source_key, target_key in (("ዘነግህ", "negh"), ("ዘቅዳሴ", "kidassie"), ("ዘሠርክ", "serk")):
                if source_key in services:
                    entry[target_key] = convert_service(services[source_key])
            by_date[key] = entry
            added.append(key)
    merged = sorted(by_date.values(), key=lambda e: tuple(map(int, reversed(e["date"].split("-")))))
    OUT.write_text(json.dumps(merged, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"Added {len(added)} entries and enriched {len(enriched)} with ሠርክ; "
          f"daily coverage is now {len(merged)} days")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: import_gitsawe_months.py /path/to/json/months")
    main(Path(sys.argv[1]))
