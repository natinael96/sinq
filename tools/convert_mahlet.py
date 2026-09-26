#!/usr/bin/env python3
"""Convert Mahlet monthly files from nested orders to structured services and hymns schema.

Transforming sources/mahlet/months/*.json into clean domain-driven structures:
- `month_number`: 1 to 13
- `month_name`: Ethiopian month name ("መስከረም", etc.)
- `season`: Ethiopian liturgical season ("መፀው", "በጋ", "ጸደይ", "ክረምት")
- `feasts`: array of feast objects:
    - `id`: descriptive feast slug (e.g. `meskerem_01_awde_amet`)
    - `day`: day of month (or null if movable/undated)
    - `name`: feast name
    - `calendar`: "fixed" or "movable"
    - `services`: dictionary of liturgical services (e.g. `wazema`, `mahlet`, `angergari_order`):
        - `title`: canonical service title
        - `hymns`: ordered list of chants/hymns:
            - `id`: clean hymn identifier
            - `form`: liturgical movement form (ዋዜማ, ነግሥ, ሰላም, ዚቅ, ምልጣን, እስመ ለዓለም, ወረብ, መመሪያ)
            - `melody_mode`: musical mode (ግዕዝ, ዕዝል, አራራይ)
            - `lyrics`: {"gez": chant text}
            - `text`: chant text (string)
            - `is_alternative`: boolean (whether this is an 'or' / ወይም option)
            - `is_rubric`: boolean (performance direction)
        - `versions`: alternative editions from independent manuscripts/posts
"""

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MONTHS_DIR = ROOT / "sources/mahlet/months"

MONTH_SEASONS = {
    1: "መፀው",
    2: "መፀው",
    3: "መፀው",
    4: "በጋ",
    5: "በጋ",
    6: "በጋ",
    7: "ጸደይ",
    8: "ጸደይ",
    9: "ጸደይ",
    10: "ክረምት",
    11: "ክረምት",
    12: "ክረምት",
    13: "ክረምት",
}

MONTH_SLUGS = {
    1: "meskerem",
    2: "tikimt",
    3: "hidar",
    4: "tahsas",
    5: "tir",
    6: "yekatit",
    7: "megabit",
    8: "miyazya",
    9: "ginbot",
    10: "sene",
    11: "hamle",
    12: "nehase",
    13: "pagumen",
}

# Mode detection from text keywords
MODE_PATTERNS = [
    (re.compile(r"\bበግዕዝ\b"), "ግዕዝ"),
    (re.compile(r"\bበዕዝል\b"), "ዕዝል"),
    (re.compile(r"\bበአራራይ\b"), "አራራይ"),
]


def detect_melody_mode(text: str) -> str | None:
    for pat, mode in MODE_PATTERNS:
        if pat.search(text):
            return mode
    return None


def transform_chant(c: dict, idx: int, prefix: str) -> dict:
    text = c.get("text", "")
    form = c.get("form") or ""
    is_rubric = (form == "መመሪያ") or (len(text) < 40 and any(k in text for k in ("በቁም", "ከከበሮ", "መርግድ")))
    is_alt = bool(c.get("alternative_to") is not None or c.get("is_alternative"))
    mode = detect_melody_mode(text)

    hymn = {
        "id": f"{prefix}_{idx:02d}",
        "form": form,
        "lyrics": {
            "gez": text,
        },
        "text": text,
        "is_alternative": is_alt,
        "is_rubric": is_rubric,
    }
    if mode:
        hymn["melody_mode"] = mode
    if c.get("title"):
        hymn["title"] = c["title"]
    return hymn


def transform_version(v: dict, prefix: str) -> dict:
    chants = v.get("chants", [])
    hymns = [transform_chant(c, i + 1, prefix) for i, c in enumerate(chants) if isinstance(c, dict)]
    return {
        "id": v.get("id", ""),
        "title": v.get("title"),
        "sources": v.get("sources", []),
        "hymns": hymns,
        "chants": chants,  # Kept for backward compatibility with flatten()
    }


def convert_month_file(path: Path):
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    month_num = data.get("month_number") or 1
    month_name = data.get("month") or "መስከረም"
    month_slug = MONTH_SLUGS.get(month_num, f"m{month_num}")

    feasts = []
    for f_idx, feast in enumerate(data.get("feasts", []), 1):
        day = feast.get("day")
        name = feast.get("name", "").strip()
        day_str = f"{day:02d}" if day else "movable"
        clean_name = re.sub(r"[^\w]+", "_", name).strip("_")
        feast_slug = f"{month_slug}_{day_str}_{clean_name}"

        services = {}
        # Support both 'orders' (old) and 'services' (new)
        raw_orders = feast.get("orders") or feast.get("services") or {}

        for s_key, s_data in raw_orders.items():
            book_info = s_data.get("book") or {}
            book_chants = book_info.get("chants") or book_info.get("hymns") or []

            hymns = [
                transform_chant(c, i + 1, f"{feast_slug}_{s_key}")
                for i, c in enumerate(book_chants)
                if isinstance(c, dict)
            ]

            versions = [
                transform_version(v, f"{feast_slug}_{s_key}_v{v_idx + 1}")
                for v_idx, v in enumerate(s_data.get("versions", []))
                if isinstance(v, dict)
            ]

            service_obj = {
                "title": book_info.get("title") or f"ሥርዓተ {s_key}",
                "hymns": hymns,
                "chants": book_chants,  # Backward compatibility
                "versions": versions,
            }
            if book_info and book_chants:
                service_obj["book"] = {
                    "title": book_info.get("title"),
                    "hymns": hymns,
                    "chants": book_chants,
                }

            services[s_key] = service_obj

        feasts.append({
            "id": feast_slug,
            "day": day,
            "name": name,
            "calendar": feast.get("calendar", "fixed"),
            "origin": feast.get("origin"),
            "services": services,
            "orders": services,  # Backward compatibility alias
            "telegram_names": feast.get("telegram_names", []),
        })

    new_doc = {
        "month_number": month_num,
        "month_name": month_name,
        "season": MONTH_SEASONS.get(month_num, "መፀው"),
        "summary": f"ሥርዓተ ማኅሌት ዘወርኃ {month_name} — የ፲፫ቱ በዓላትና ሰንበታት ማኅሌታት።",
        "feasts": feasts,
    }

    with open(path, "w", encoding="utf-8") as f:
        json.dump(new_doc, f, ensure_ascii=False, indent=2)


def main():
    files = sorted(MONTHS_DIR.glob("*.json"))
    print(f"Converting {len(files)} Mahlet month files...")
    for path in files:
        convert_month_file(path)
        print(f"  ✓ Converted {path.name}")
    print("\nAll Mahlet monthly files successfully converted.")


if __name__ == "__main__":
    main()
