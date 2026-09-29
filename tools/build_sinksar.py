#!/usr/bin/env python3
"""Build the bundled ስንክሳር from the scanned editions in sources/sinksar/.

Two editions, thirteen months each, 366 days. Reads the parallel aligned
transcriptions in sources/sinksar/*_neat.json and outputs the structured
JSON files into app/src/main/assets/content/sinksar/.

Shape per day:
    day       day of the Ethiopian month (1..30, or 1..6 for ጳጉሜን)
    header    the ስንክሳር ዘወርኀ … / አመ … ለ… line
    entries   one per commemoration: numbered paragraphs, closed by its አርኬ
"""
from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "sources/sinksar"
OUT = ROOT / "app/src/main/assets/content/sinksar"

CONTENT_VERSION = 2

MONTHS = [
    "መስከረም", "ጥቅምት", "ኅዳር", "ታኅሣሥ", "ጥር", "የካቲት",
    "መጋቢት", "ሚያዝያ", "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜን"
]
MONTH_ALIASES = {"ጳጉሜን": ("ጳጒሜን", "ጳጉሜ", "ጳጒሜ")}
DAYS_IN = {13: 6}
EDITIONS = [("am", "አማርኛ"), ("ge", "ግእዝ")]

GEEZ_VALUE = {
    "፩": 1, "፪": 2, "፫": 3, "፬": 4, "፭": 5, "፮": 6, "፯": 7, "፰": 8, "፱": 9,
    "፲": 10, "፳": 20, "፴": 30, "፵": 40, "፶": 50, "፷": 60, "፸": 70, "፹": 80,
    "፺": 90, "፻": 100, "፼": 10000,
}

JUNK = re.compile(
    r"[\u2190-\u21FF\u2600-\u27BF\u2B00-\u2BFF\uFE00-\uFE0F\U0001F000-\U0001FAFF<>]"
)
NUM_RE = re.compile(r"^(?:(\d+)\s+)?([፩-፼0-9]+)\s*[፡:]\s*(.*)", re.DOTALL)


def parse_num(s: str) -> int:
    if not s:
        return 0
    if s.isascii() and s.isdigit():
        return int(s)
    total = run = 0
    for c in s:
        v = GEEZ_VALUE.get(c, 0)
        if v == 100:
            run = (run or 1) * 100
            total += run
            run = 0
        else:
            run += v
    return total + run


def clean(text: str) -> str:
    if not text:
        return ""
    text = JUNK.sub("", text)
    # Replace single Ethiopic wordspace (፡) with standard whitespace,
    # preserving double punctuation like ፡፡
    text = re.sub(r"(?<!፡)፡(?!፡)", " ", text)
    # Normalize spacing around punctuation: attach directly to preceding word
    text = re.sub(r"\s*([።፣፤፥፦])\s*", r"\1 ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def entry_id(text: str) -> str:
    return hashlib.sha1(re.sub(r"\s+", "", text)[:80].encode("utf-8")).hexdigest()[:8]


def month_file(directory: Path, name: str) -> Path | None:
    wanted = {name, *MONTH_ALIASES.get(name, ())}
    for path in sorted(directory.glob("*_neat.json")):
        stem_name = path.stem.replace("_neat", "").strip()
        if stem_name in wanted:
            return path
    return None


def parse_day(day_data: dict, edition: str) -> dict:
    day_num = day_data["day"]
    lines = day_data.get("lines", [])
    if not lines:
        sys.exit(f"Empty lines for day {day_num}")

    l0_curr = lines[0].get("amharic" if edition == "am" else "geez")
    l0_oth = lines[0].get("geez" if edition == "am" else "amharic")
    header = clean(l0_curr or l0_oth or "")

    entries = []
    curr_paras = []
    curr_other = []
    curr_arke = None

    def close_entry():
        nonlocal curr_paras, curr_other, curr_arke
        # If this edition has no narrative paragraphs for this commemoration,
        # fall back to the other edition's paragraphs so commemorations stay complete.
        paras_to_use = curr_paras if curr_paras else curr_other
        if paras_to_use or curr_arke:
            first = paras_to_use[0]["text"] if paras_to_use else (curr_arke or "")
            eid = entry_id(first)
            entry_dict = {
                "id": eid,
                "paragraphs": paras_to_use,
            }
            if curr_arke:
                entry_dict["arke"] = curr_arke
            entries.append(entry_dict)
        curr_paras, curr_other, curr_arke = [], [], None

    for l in lines[1:]:
        am_raw = (l.get("amharic") or "").strip()
        ge_raw = (l.get("geez") or "").strip()

        is_arke = (
            am_raw.startswith("ሰላም") or ge_raw.startswith("ሰላም")
            or am_raw.startswith("ሰላመ") or ge_raw.startswith("ሰላመ")
            or am_raw.startswith("ተዘኪረከ") or ge_raw.startswith("ተዘኪረከ")
        )

        if is_arke:
            # Arkes are Ge'ez hymns; use whichever field is non-empty
            arke_raw = (am_raw if edition == "am" else ge_raw) or (ge_raw if edition == "am" else am_raw)
            arke_clean = clean(arke_raw)
            if curr_arke:
                curr_arke = f"{curr_arke}\n{arke_clean}"
            else:
                curr_arke = arke_clean
        else:
            raw_curr = am_raw if edition == "am" else ge_raw
            raw_oth = ge_raw if edition == "am" else am_raw

            m_curr = NUM_RE.match(raw_curr) if raw_curr else None
            m_oth = NUM_RE.match(raw_oth) if raw_oth else None

            if curr_arke:
                close_entry()

            if raw_curr:
                num_val = (
                    parse_num(m_curr.group(2))
                    if m_curr
                    else (parse_num(m_oth.group(2)) if m_oth else 0)
                )
                body = clean(m_curr.group(3) if m_curr else raw_curr)
                curr_paras.append({"n": num_val, "text": body})

            if raw_oth:
                num_val_oth = (
                    parse_num(m_oth.group(2))
                    if m_oth
                    else (parse_num(m_curr.group(2)) if m_curr else 0)
                )
                body_oth = clean(m_oth.group(3) if m_oth else raw_oth)
                curr_other.append({"n": num_val_oth, "text": body_oth})

    close_entry()

    # Disambiguate entry ids within the day in case of duplicate content hash
    seen_ids = set()
    for idx, e in enumerate(entries):
        if e["id"] in seen_ids:
            e["id"] = hashlib.sha1(f"{e['id']}_{idx}".encode("utf-8")).hexdigest()[:8]
        seen_ids.add(e["id"])

    return {
        "day": day_num,
        "header": header,
        "entries": entries,
    }


def build_edition(code: str) -> list[dict]:
    months = []
    for num, name in enumerate(MONTHS, 1):
        path = month_file(SRC, name)
        if path is None:
            sys.exit(f"no scan for {name} in {SRC.relative_to(ROOT)}")
        data = json.loads(path.read_text(encoding="utf-8"))
        days = [parse_day(d, code) for d in data.get("days", [])]
        expected = DAYS_IN.get(num, 30)
        if len(days) != expected:
            sys.exit(f"{code}/{name}: {len(days)} days, expected {expected}")
        months.append({
            "month": num,
            "name": name,
            "edition": code,
            "contentVersion": CONTENT_VERSION,
            "days": days,
        })
    return months


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for stale in OUT.glob("*.json"):
        stale.unlink()

    manifest = {"contentVersion": CONTENT_VERSION, "editions": [], "months": []}
    for code, label in EDITIONS:
        months = build_edition(code)
        entries = arke = paras = chars = 0
        for m in months:
            (OUT / f"{code}-{m['month']}.json").write_text(
                json.dumps(m, ensure_ascii=False, separators=(",", ":")),
                encoding="utf-8",
            )
            for d in m["days"]:
                entries += len(d["entries"])
                arke += sum(1 for e in d["entries"] if e.get("arke"))
                paras += sum(len(e["paragraphs"]) for e in d["entries"])
                chars += sum(len(p["text"]) for e in d["entries"] for p in e["paragraphs"])
                chars += sum(len(e.get("arke", "")) for e in d["entries"])

        manifest["editions"].append({"code": code, "name": label})
        if code == "am":
            manifest["months"] = [
                {"month": m["month"], "name": m["name"], "days": len(m["days"])}
                for m in months
            ]
        print(f"{label} ({code}): {entries} entries, {paras} paragraphs, {arke} አርኬ, {chars} chars")

    (OUT / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    size = sum(f.stat().st_size for f in OUT.glob("*.json"))
    print(f"\nwrote {len(list(OUT.glob('*.json')))} files, {size / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
