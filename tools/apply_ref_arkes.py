#!/usr/bin/env python3
"""Apply verified Arke hymns from sinksar-ref/ to sources/sinksar/*_neat.json.

This updates the Arke lines in the Sinksar sources with the verified, clean
text from sinksar-ref/ while preserving:
- All narrative paragraphs and day headers
- Entry segmentation and entry IDs (content hashes remain 100% stable)
- Manuscript wordspace colons (፡) and liturgical punctuation (፤ and ።)
"""
from __future__ import annotations

import json
import re
from difflib import SequenceMatcher
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
REF_DIR = ROOT / "sinksar-ref"
NEAT_DIR = ROOT / "sources/sinksar"

MONTH_MAP = [
    ("መስከረም", "መስከረም_neat.json", "ስንክሳር ዘወርኀ መስከረም.json", 1),
    ("ጥቅምት", "ጥቅምት_neat.json", "ስንክሳር ዘወርኀ ጥቅምት.json", 2),
    ("ኅዳር", "ኅዳር_neat.json", "ስንክሳር ዘወርኀ ኅዳር.json", 3),
    ("ታኅሣሥ", "ታኅሣሥ_neat.json", "ስንክሳር ዘወርኃ ታኅሣሥ.json", 4),
    ("ጥር", "ጥር_neat.json", "ስንክሳር ዘወርኀ ጥር.json", 5),
    ("የካቲት", "የካቲት_neat.json", "ስንክሳር ዘወርኀ የካቲት.json", 6),
    ("መጋቢት", "መጋቢት_neat.json", "ስንክሳር ዘወርኀ መጋቢት.json", 7),
    ("ሚያዝያ", "ሚያዝያ_neat.json", "ስንክሳር ዘወርኀ ሚያዝያ.json", 8),
    ("ግንቦት", "ግንቦት_neat.json", "ስንክሳር ዘወርኀ ግንቦት.json", 9),
    ("ሰኔ", "ሰኔ_neat.json", "ስንክሳር ዘወርኀ ሰኔ.json", 10),
    ("ሐምሌ", "ሐምሌ_neat.json", "ስንክሳር ዘወርኀ ሐምሌ.json", 11),
    ("ነሐሴ", "ነሐሴ_neat.json", "ስንክሳር ዘወርኀ ነሐሴ.json", 12),
    ("ጳጉሜን", "ጳጒሜን_neat.json", "ስንክሳር ዘወርኀ ጳጒሜን.json", 13),
]

ARKE_PREFIXES = ("ሰላም", "ሰላመ", "ተዘኪረከ")

OVERRIDES = {
    (1, 11, 4): (11, 39),   # Benfzez martyrs
    (1, 27, 0): (27, 23),   # Eustathius
    (2, 6, 2): (6, 43),     # Habakkuk
    (4, 5, 3): (5, 46),     # Victor (መርዓዊ)
    (10, 20, 0): (20, 24),  # Church of Mary
    (12, 20, 0): (20, 24),  # Seven Sleepers (፯ተ)
}


def norm(t: str) -> str:
    return re.sub(r"[\s\W\d]+", "", t or "")


def sim(a: str, b: str) -> float:
    na = norm(a)
    nb = norm(b)
    if not na or not nb:
        return 0.0
    s = SequenceMatcher(None, na[:35], nb[:35]).ratio()
    if len(na) >= 20 and len(nb) >= 20:
        if na[:20] in nb or nb[:20] in na:
            s = max(s, 0.85)
    return s


def format_ref_arke(text: str) -> str:
    raw_strophes = [s.strip() for s in text.split("።") if s.strip()]
    if not raw_strophes:
        return ""
    formatted = []
    for i, st in enumerate(raw_strophes):
        st = st.rstrip("፤፣፡፦ ").strip()
        words = [w for w in re.split(r"[\s፡]+", st) if w]
        st_text = "፡".join(words)
        is_last = (i == len(raw_strophes) - 1)
        term = "።" if is_last else "፤"
        formatted.append(st_text + term)
    return " ".join(formatted)


def apply_ref_arkes():
    total_ref_arkes = 0
    total_replaced_lines = 0

    for name, neat_name, ref_name, m_num in MONTH_MAP:
        ref_path = REF_DIR / ref_name
        neat_path = NEAT_DIR / neat_name

        with open(ref_path, encoding="utf-8") as f:
            ref_data = json.load(f)
        with open(neat_path, encoding="utf-8") as f:
            neat_data = json.load(f)

        matched_neat_lines = set()
        replacements = {}  # (day_num, line_idx) -> formatted_arke

        # Pass 1: Explicit overrides
        for (om_num, o_rday, o_ridx), (o_tday, o_tline) in OVERRIDES.items():
            if om_num == m_num:
                r_blocks = [
                    b["text"].strip()
                    for b in ref_data["chapters"][o_rday - 1]["blocks"]
                    if b.get("text", "").strip().startswith(ARKE_PREFIXES)
                ]
                replacements[(o_tday, o_tline)] = format_ref_arke(r_blocks[o_ridx])
                matched_neat_lines.add((o_tday, o_tline))

        # Pass 2: Map each ref arke
        for day_idx in range(len(ref_data["chapters"])):
            day_num = day_idx + 1
            ref_c = ref_data["chapters"][day_idx]
            neat_d = neat_data["days"][day_idx]

            ref_arkes = [
                b["text"].strip()
                for b in ref_c["blocks"]
                if b.get("text", "").strip().startswith(ARKE_PREFIXES)
            ]

            for r_idx, ra in enumerate(ref_arkes):
                total_ref_arkes += 1
                if (m_num, day_num, r_idx) in OVERRIDES:
                    continue

                # Search on the same day first
                neat_lines = [
                    (idx, l["amharic"].strip())
                    for idx, l in enumerate(neat_d["lines"])
                    if l.get("amharic", "").strip().startswith(ARKE_PREFIXES)
                ]
                best_idx = -1
                best_score = -1
                for idx, nl in neat_lines:
                    if (day_num, idx) in matched_neat_lines:
                        continue
                    s = sim(ra, nl)
                    if s > best_score:
                        best_score = s
                        best_idx = idx

                if best_score > 0.4 and best_idx != -1:
                    matched_neat_lines.add((day_num, best_idx))
                    replacements[(day_num, best_idx)] = format_ref_arke(ra)
                    continue

                # Search across whole month
                best_m_day = -1
                best_m_idx = -1
                best_m_score = -1
                for d in neat_data["days"]:
                    d_num = d["day"]
                    for idx, l in enumerate(d["lines"]):
                        if (d_num, idx) in matched_neat_lines:
                            continue
                        nl = l.get("amharic", "").strip()
                        if nl.startswith(ARKE_PREFIXES):
                            s = sim(ra, nl)
                            if s > best_m_score:
                                best_m_score = s
                                best_m_day = d_num
                                best_m_idx = idx

                if best_m_score > 0.4 and best_m_idx != -1:
                    matched_neat_lines.add((best_m_day, best_m_idx))
                    replacements[(best_m_day, best_m_idx)] = format_ref_arke(ra)
                else:
                    raise RuntimeError(f"Unmatched ref arke in {name} day {day_num}: {ra[:50]}")

        # Pass 3: Also update duplicate lines on the same day with the clean text
        for (d_num, l_idx), new_txt in list(replacements.items()):
            d = neat_data["days"][d_num - 1]
            orig_txt = d["lines"][l_idx]["amharic"]
            for o_idx, ol in enumerate(d["lines"]):
                if (d_num, o_idx) not in matched_neat_lines and (d_num, o_idx) not in replacements:
                    ot = ol.get("amharic", "").strip()
                    if ot.startswith(ARKE_PREFIXES) and sim(ot, orig_txt) > 0.8:
                        replacements[(d_num, o_idx)] = new_txt

        # Apply replacements to neat data
        for (d_num, l_idx), new_txt in replacements.items():
            line_dict = neat_data["days"][d_num - 1]["lines"][l_idx]
            line_dict["amharic"] = new_txt
            line_dict["geez"] = new_txt

        # Save neat file
        with open(neat_path, "w", encoding="utf-8") as f:
            json.dump(neat_data, f, ensure_ascii=False, indent=2)

        total_replaced_lines += len(replacements)
        print(f"{name:8s} (Month {m_num:2d}): updated {len(replacements)} arke lines")

    print(f"\nSuccessfully applied {total_ref_arkes} ref arkes across {total_replaced_lines} neat lines.")


if __name__ == "__main__":
    apply_ref_arkes()
