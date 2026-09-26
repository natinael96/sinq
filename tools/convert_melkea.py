#!/usr/bin/env python3
"""Convert Melkea source books from raw blocks into a strophic liturgical poetry schema.

Each Malke'a (መልክእ) is a canonical strophic poem addressed to the bodily members,
attributes, and virtues of our Lord, the Virgin Mary, angels, or saints.
Traditionally each strophe (ስንኝ) consists of 5 rhythmic rhyming lines (በ፭ ቤት የሚቋጠር)
starting with 'ሰላም ለ...', concluded by a closing prayer (ማኅተም / መደምደሚያ).

This script reads each `sources/books/መልክአ *.json` and transforms it into the structured schema:
- `id`: clean unique slug (e.g. `malke_mikael`)
- `title`: multilingual titles (Ge'ez and English)
- `dedicated_to`: the honored saint / angel / Lord
- `category`: "malke"
- `sections`:
    - `prelude`: opening prayer / introduction (መቅድም)
    - `body`: canonical strophes with target anatomy, incipit, Ge'ez text, and 5-line meter
    - `conclusion`: closing prayers / petitions (ማኅተም / መደምደሚያ)
"""

import json
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC_BOOKS = ROOT / "sources/books"

MELKEA_METADATA = {
    "መልክአ ሀብተ ማርያም": {
        "slug": "malke_habte_maryam",
        "en": "Malke'a Habte Maryam",
        "dedicated_to": "አቡነ ሀብተ ማርያም",
    },
    "መልክአ ልደታ": {
        "slug": "malke_lidata",
        "en": "Malke'a Lidata",
        "dedicated_to": "ልደታ ለማርያም",
    },
    "መልክአ ሐራ ድንግል": {
        "slug": "malke_hara_dingil",
        "en": "Malke'a Hara Dingil",
        "dedicated_to": "ሐራ ድንግል",
    },
    "መልክአ መርቆሬዎስ": {
        "slug": "malke_merqorewos",
        "en": "Malke'a Merqorewos",
        "dedicated_to": "ቅዱስ መርቆሬዎስ",
    },
    "መልክአ መድኃኔ ዓለም": {
        "slug": "malke_medhanealem",
        "en": "Malke'a Medhanealem",
        "dedicated_to": "መድኃኔ ዓለም",
    },
    "መልክአ ሚካኤል": {
        "slug": "malke_mikael",
        "en": "Malke'a Mika'el",
        "dedicated_to": "ቅዱስ ሚካኤል",
    },
    "መልክአ ማርቆስ": {
        "slug": "malke_marqos",
        "en": "Malke'a Marqos",
        "dedicated_to": "ቅዱስ ማርቆስ ወንጌላዊ",
    },
    "መልክአ ማርያም": {
        "slug": "malke_maryam",
        "en": "Malke'a Maryam",
        "dedicated_to": "እግዝእትነ ማርያም ድንግል",
    },
    "መልክአ ሥላሴ": {
        "slug": "malke_selassie",
        "en": "Malke'a Selassie",
        "dedicated_to": "ቅድስት ሥላሴ",
    },
    "መልክአ ሩፋኤል": {
        "slug": "malke_rufael",
        "en": "Malke'a Rufa'el",
        "dedicated_to": "ቅዱስ ሩፋኤል",
    },
    "መልክአ ቂርቆስ": {
        "slug": "malke_qirqos",
        "en": "Malke'a Qirqos",
        "dedicated_to": "ቅዱስ ቂርቆስ",
    },
    "መልክአ ቍርባን ።": {
        "slug": "malke_qurban",
        "en": "Malke'a Qurban",
        "dedicated_to": "ቅዱስ ቍርባን",
    },
    "መልክአ ተክለ ሃይማኖት": {
        "slug": "malke_teklehaymanot",
        "en": "Malke'a Tekle Haymanot",
        "dedicated_to": "አቡነ ተክለ ሃይማኖት",
    },
    "መልክአ አማኑኤል": {
        "slug": "malke_amanuel",
        "en": "Malke'a Amanuel",
        "dedicated_to": "አማኑኤል",
    },
    "መልክአ አረጋዊ": {
        "slug": "malke_aregawi",
        "en": "Malke'a Aregawi",
        "dedicated_to": "አቡነ አረጋዊ (ዘሚካኤል)",
    },
    "መልክአ አርሴማ ቅድስት": {
        "slug": "malke_arsema",
        "en": "Malke'a Arsema",
        "dedicated_to": "ቅድስት አርሴማ",
    },
    "መልክአ አባ ጊዮርጊስ ዘጋሥጫ": {
        "slug": "malke_abba_giyorgis_zegasicha",
        "en": "Malke'a Abba Giyorgis ZeGasicha",
        "dedicated_to": "አባ ጊዮርጊስ ዘጋሥጫ",
    },
    "መልክአ ኢየሱስ": {
        "slug": "malke_iyesus",
        "en": "Malke'a Iyesus",
        "dedicated_to": "እግዚእነ ኢየሱስ ክርስቶስ",
    },
    "መልክአ ኢያቄም": {
        "slug": "malke_iyaqem",
        "en": "Malke'a Iyaqem",
        "dedicated_to": "ጻድቅ ኢያቄም",
    },
    "መልክአ ኪዳነ ምሕረት": {
        "slug": "malke_kidane_mihret",
        "en": "Malke'a Kidane Mihret",
        "dedicated_to": "ኪዳነ ምሕረት",
    },
    "መልክአ ካህናተ ሰማይ": {
        "slug": "malke_kahinate_semay",
        "en": "Malke'a Kahinate Semay",
        "dedicated_to": "ሃያ አራቱ ካህናተ ሰማይ",
    },
    "መልክአ ክርስቶስ ሠምራ": {
        "slug": "malke_kristos_semra",
        "en": "Malke'a Kristos Semra",
        "dedicated_to": "ቅድስት ክርስቶስ ሠምራ",
    },
    "መልክአ ዑራኤል": {
        "slug": "malke_urael",
        "en": "Malke'a Ura'el",
        "dedicated_to": "ቅዱስ ዑራኤል",
    },
    "መልክአ ያሬድ": {
        "slug": "malke_yared",
        "en": "Malke'a Yared",
        "dedicated_to": "ቅዱስ ያሬድ ማኅሌታይ",
    },
    "መልክአ ዮሐንስ መጥምቅ": {
        "slug": "malke_yohannes_metmiq",
        "en": "Malke'a Yohannes Metmiq",
        "dedicated_to": "ቅዱስ ዮሐንስ መጥምቅ",
    },
    "መልክአ ዮሐንስ ወልደ ነጎድጓድ": {
        "slug": "malke_yohannes_welde_negodgwad",
        "en": "Malke'a Yohannes Welde Negodgwad",
        "dedicated_to": "ቅዱስ ዮሐንስ ወልደ ነጎድጓድ",
    },
    "መልክአ ዮሐንስ ፍቁረ እግዚእ": {
        "slug": "malke_yohannes_fiqure_egzi",
        "en": "Malke'a Yohannes Fiqure Egzi",
        "dedicated_to": "ቅዱስ ዮሐንስ ፍቁረ እግዚእ",
    },
    "መልክአ ዮሐንስ": {
        "slug": "malke_yohannes",
        "en": "Malke'a Yohannes",
        "dedicated_to": "ቅዱስ ዮሐንስ",
    },
    "መልክአ ገብረ መንፈስ ቅዱስ": {
        "slug": "malke_gebre_menfes_kiddus",
        "en": "Malke'a Gebre Menfes Kiddus",
        "dedicated_to": "አቡነ ገብረ መንፈስ ቅዱስ",
    },
    "መልክአ ገብርኤል": {
        "slug": "malke_gabriel",
        "en": "Malke'a Gabriel",
        "dedicated_to": "ቅዱስ ገብርኤል",
    },
    "መልክአ ጊዮርጊስ": {
        "slug": "malke_giyorgis",
        "en": "Malke'a Giyorgis",
        "dedicated_to": "ቅዱስ ጊዮርጊስ ሰማዕት",
    },
    "መልክአ ፍልሰታ": {
        "slug": "malke_filseta",
        "en": "Malke'a Filseta",
        "dedicated_to": "ፍልሰታ ለማርያም",
    },
}

# Regex to detect salutations to a member/attribute
SELAM_RE = re.compile(
    r"^(?:ሰ\s*ላ\s*ም|ሰላም)\s*(?:ዕብል|እብል)?\s*ለ([^\s፡]+(?:\s+[^\s፡]+)?)"
)


def extract_target(raw_text: str) -> str:
    """Extract and normalize the anatomical member/target from the strophe incipit."""
    m = SELAM_RE.match(raw_text)
    if not m:
        return ""
    target = m.group(1).strip()
    target = re.sub(r"[፡።፤\.,]+$", "", target).strip()
    return target


def split_into_lines(text: str) -> list[str]:
    """Split a 5-line Ge'ez strophe by Ethiopic punctuation markers."""
    parts = [p.strip() for p in re.split(r"[።፤]", text) if p.strip()]
    lines = []
    for p in parts:
        lines.append(p + "፤")
    if lines:
        lines[-1] = lines[-1][:-1] + "።"
    return lines


def convert_file(path: Path) -> dict:
    with open(path, "r", encoding="utf-8") as f:
        old_data = json.load(f)

    title = old_data.get("title", path.stem).strip()
    meta = MELKEA_METADATA.get(title)
    if not meta:
        clean_title = re.sub(r"[።\s]+$", "", title)
        meta = MELKEA_METADATA.get(clean_title, {
            "slug": re.sub(r"[^\w]+", "_", clean_title),
            "en": clean_title,
            "dedicated_to": clean_title,
        })

    blocks = old_data.get("chapters", [{}])[0].get("blocks", [])

    prelude_stanzas = []
    strophes = []
    conclusion_stanzas = []

    strophe_indices = []
    for idx, b in enumerate(blocks):
        txt = b.get("text", "").strip()
        if not txt or b.get("type") == "heading":
            continue
        if SELAM_RE.search(txt):
            strophe_indices.append(idx)

    min_strophe_idx = strophe_indices[0] if strophe_indices else len(blocks)
    max_strophe_idx = strophe_indices[-1] if strophe_indices else -1

    strophe_num = 1
    for idx, b in enumerate(blocks):
        txt = b.get("text", "").strip()
        if not txt or b.get("type") == "heading":
            continue

        if idx < min_strophe_idx:
            prelude_stanzas.append({
                "type": "paragraph",
                "text": {"gez": txt},
            })
        elif idx > max_strophe_idx:
            conclusion_stanzas.append({
                "type": "prayer",
                "text": {"gez": txt},
            })
        else:
            if SELAM_RE.search(txt):
                target = extract_target(txt)
                lines = split_into_lines(txt)
                incipit_words = txt.split()[:4]
                strophes.append({
                    "index": strophe_num,
                    "target": target,
                    "incipit": " ".join(incipit_words),
                    "text": {"gez": txt},
                    "lines": lines,
                })
                strophe_num += 1
            else:
                strophes.append({
                    "index": strophe_num,
                    "target": "ማኅሌት",
                    "incipit": " ".join(txt.split()[:4]),
                    "text": {"gez": txt},
                    "lines": split_into_lines(txt),
                })
                strophe_num += 1

    sections = []
    if prelude_stanzas:
        sections.append({
            "id": "prelude",
            "type": "prelude",
            "title": "መቅድም",
            "stanzas": prelude_stanzas,
        })

    sections.append({
        "id": "body",
        "type": "body",
        "title": "ስንኞች (አካላት)",
        "strophes": strophes,
    })

    if conclusion_stanzas:
        sections.append({
            "id": "conclusion",
            "type": "conclusion",
            "title": "ማኅተም (ጸሎት)",
            "stanzas": conclusion_stanzas,
        })

    new_doc = {
        "id": meta["slug"],
        "title": {
            "gez": title,
            "en": meta["en"],
        },
        "dedicated_to": meta["dedicated_to"],
        "category": "malke",
        "collection": "መልክዐ ጉባኤ",
        "summary": f"{title} — {meta['dedicated_to']}ን የሚመለከት ባለ ፭ መስመር ስንኞች ያሉት መልክእ።",
        "sections": sections,
    }
    return new_doc


def main():
    files = sorted(SRC_BOOKS.glob("መልክአ*.json"))
    print(f"Converting {len(files)} Melkea books...")
    for path in files:
        converted = convert_file(path)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(converted, f, ensure_ascii=False, indent=2)
        n_strophes = sum(len(s.get("strophes", [])) for s in converted["sections"])
        print(f"  ✓ {converted['title']['gez']:<25} ({converted['id']}): {n_strophes} strophes")
    print(f"\nAll {len(files)} Melkea books converted successfully to structured schema.")


if __name__ == "__main__":
    main()
