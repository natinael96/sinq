#!/usr/bin/env python3
"""Extract and structure the English Synaxarium (Wallis Budge translation)
from synaxarium-english.pdf into Sinq's SynaxariumMonth and SynaxariumDay schema.

Outputs:
  - app/src/main/assets/content/sinksar/en-{1..13}.json
  - /home/natinael/Desktop/afterq/SYNAXARIUM ENGLISH JSON/en-{1..13}.json
  - /home/natinael/Desktop/afterq/SYNAXARIUM ENGLISH JSON/synaxarium_english_master.json
"""
from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path

PDF_PATH = Path("/home/natinael/Desktop/afterq/synaxarium-english.pdf")
SCRATCH_TXT = Path("/home/natinael/.gemini/antigravity-cli/brain/cba7c609-303e-4920-a438-a814bae02143/scratch/synaxarium_english_full.txt")
AFTERQ_DIR = Path("/home/natinael/Desktop/afterq/SYNAXARIUM ENGLISH JSON")
ASSETS_DIR = Path("/mnt/data/project/sinq/app/src/main/assets/content/sinksar")

MONTHS = [
    ("Meskerem", 30),
    ("Tekemt", 30),
    ("Hedar", 30),
    ("Tahsas", 30),
    ("Tir", 30),
    ("Yekatit", 30),
    ("Megabit", 30),
    ("Miyazia", 30),
    ("Ginbot", 30),
    ("Senne", 30),
    ("Hamle", 30),
    ("Nehasse", 30),
    ("Pagumen", 6),
]

NORM = {
    'MESKEREM': 1, 'MASKARAM': 1,
    'TEKEMT': 2, 'TIKIMT': 2,
    'HEDAR': 3, 'HIDAR': 3,
    'TAHISAS': 4, 'TAHSAS': 4,
    'TIR': 5, 'TER': 5,
    'YEKATIT': 6,
    'MEGABIT': 7, 'MAGABIT': 7,
    'MIYAZIA': 8, 'MIAZIA': 8,
    'GINBOT': 9,
    'SENNE': 10, 'SENE': 10,
    'HAMLE': 11,
    'NEHASSE': 12, 'NAHASSE': 12, 'NEHASE': 12,
    'PAGUMEN': 13, 'PAGUEMEN': 13, 'PAGUME': 13
}

ENTRY_START_RE = re.compile(
    r'(?:^|\n)[ \t]*('
    r'This\s+blessed\s+month\b'
    r'|(?:And\s+)?(?:On|on)\s+this\s+(?:blessed\s+)?day\b'
    r'|And\s+if\s+the\s+month\s+of\s+Paguemen\b'
    r'|Salutation\s+(?:to\s+)?[A-Z‘\"][^\n]*'
    r')'
)


def entry_id(text: str) -> str:
    key = re.sub(r'[\s\.,;:\-\"\'“”‘’]+', '', text)[:80]
    return hashlib.sha1(key.encode('utf-8')).hexdigest()[:8]


def reflow_paragraphs(text: str) -> list[str]:
    lines = [l.strip() for l in text.split('\n') if l.strip()]
    paragraphs = []
    current_para = []

    for i, line in enumerate(lines):
        current_para.append(line)
        is_sentence_end = line.endswith(('.', '.”', '?”', '!”', '\"', '”', '!)', '.)'))
        is_short = len(line) < 76

        next_is_para_start = False
        if i + 1 < len(lines):
            next_line = lines[i + 1]
            if re.match(r'^(?:On this day|And on this day|Salutation to|Glory be to God)\b', next_line, re.I):
                next_is_para_start = True

        if (is_sentence_end and is_short) or next_is_para_start or i == len(lines) - 1:
            p_text = ' '.join(current_para)
            p_text = re.sub(r'\s+', ' ', p_text).strip()
            if p_text:
                paragraphs.append(p_text)
            current_para = []

    if current_para:
        p_text = ' '.join(current_para)
        p_text = re.sub(r'\s+', ' ', p_text).strip()
        if p_text:
            paragraphs.append(p_text)

    return paragraphs


def main():
    if not SCRATCH_TXT.exists():
        import subprocess
        subprocess.run(['pdftotext', str(PDF_PATH), str(SCRATCH_TXT)], check=True)

    with open(SCRATCH_TXT) as f:
        raw = f.read()

    # Clean running headers, form feeds, and page numbers
    cleaned = re.sub(r'\n+(?:\d{1,4}\s*\n+)?\x0c[^\n]*\n+', '\n', raw)

    day_pattern = re.compile(r'^[ \t]*([A-Za-z]+)\s+(\d{1,2})\s*\n[ \t]*\(([A-Za-z]+ \d{1,2})\)', re.MULTILINE)
    matches = list(day_pattern.finditer(cleaned))

    days_raw = []
    for i, m in enumerate(matches):
        m_idx = NORM.get(m.group(1).upper())
        if not m_idx:
            continue
        d_num = int(m.group(2))
        # Deduplicate accidental repetition in source (e.g. Senne 24)
        if days_raw and days_raw[-1]['month'] == m_idx and days_raw[-1]['day'] == d_num:
            continue
        start = m.start()
        end = matches[i + 1].start() if i + 1 < len(matches) else len(cleaned)
        days_raw.append({
            'month': m_idx,
            'day': d_num,
            'gregorian': m.group(3),
            'text': cleaned[start:end].strip()
        })

    print(f"Collected {len(days_raw)} raw days.")
    assert len(days_raw) == 366, f"Expected 366 days, got {len(days_raw)}"

    # Group by month
    months_data = {}
    for m_idx in range(1, 14):
        m_name, _ = MONTHS[m_idx - 1]
        months_data[m_idx] = {
            "month": m_idx,
            "name": m_name,
            "edition": "en",
            "contentVersion": 2,
            "days": []
        }

    total_entries = 0
    total_paras = 0

    for d in days_raw:
        m_idx = d['month']
        d_num = d['day']
        g_date = d['gregorian']
        raw_text = d['text']
        m_name = MONTHS[m_idx - 1][0]

        dox_m = re.search(r'IN THE NAME OF.*?AMEN\.?', raw_text, re.I | re.DOTALL)
        body = raw_text[dox_m.end():].strip() if dox_m else raw_text

        splits = list(ENTRY_START_RE.finditer(body))
        entry_chunks = []
        if splits:
            pre = body[:splits[0].start()].strip()
            if pre and pre != '!':
                entry_chunks.append(pre)
            for i, sm in enumerate(splits):
                s_start = sm.start()
                s_end = splits[i + 1].start() if i + 1 < len(splits) else len(body)
                chunk = body[s_start:s_end].strip()
                if chunk:
                    entry_chunks.append(chunk)
        else:
            entry_chunks.append(body)

        entries = []
        para_counter = 1
        for chunk in entry_chunks:
            # Check for Salutation at end of chunk
            salut_m = re.search(r'\n?[ \t]*(Salutation\s+(?:to\s+)?[A-Z‘\"].*)$', chunk, re.DOTALL | re.IGNORECASE)
            arke_text = None
            narrative = chunk
            if salut_m:
                salut_part = salut_m.group(1).strip()
                narrative = chunk[:salut_m.start()].strip()
                salut_lines = [s.strip() for s in salut_part.split('\n') if s.strip()]
                arke_text = '\n'.join(salut_lines)

            if not narrative and arke_text:
                narrative = arke_text
                arke_text = None

            paras_text = reflow_paragraphs(narrative)
            if not paras_text and arke_text:
                paras_text = [arke_text]
                arke_text = None

            paras = []
            for pt in paras_text:
                paras.append({'n': para_counter, 'text': pt})
                para_counter += 1

            if not paras:
                continue

            eid = entry_id(paras[0]['text'])
            entries.append({
                'id': eid,
                'paragraphs': paras,
                'arke': arke_text
            })

        total_entries += len(entries)
        total_paras += sum(len(e['paragraphs']) for e in entries)

        day_obj = {
            'day': d_num,
            'header': f"Synaxarium: Month of {m_name} {d_num:02d} ({g_date})",
            'entries': entries
        }
        months_data[m_idx]["days"].append(day_obj)

    print(f"Processed 366 days, {total_entries} entries, {total_paras} paragraphs.")

    # Write output to Desktop/afterq and to Sinq assets
    AFTERQ_DIR.mkdir(parents=True, exist_ok=True)
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)

    master_list = []
    for m_idx in range(1, 14):
        m_data = months_data[m_idx]
        expected_days = MONTHS[m_idx - 1][1]
        assert len(m_data["days"]) == expected_days, f"Month {m_idx} expected {expected_days} days, got {len(m_data['days'])}"

        # 1. Desktop afterq
        out_afterq = AFTERQ_DIR / f"en-{m_idx}.json"
        with open(out_afterq, "w", encoding="utf-8") as f:
            json.dump(m_data, f, ensure_ascii=False, indent=2)

        # 2. Sinq assets (compact)
        out_assets = ASSETS_DIR / f"en-{m_idx}.json"
        with open(out_assets, "w", encoding="utf-8") as f:
            json.dump(m_data, f, ensure_ascii=False, separators=(',', ':'))

        master_list.append(m_data)

    master_path = AFTERQ_DIR / "synaxarium_english_master.json"
    with open(master_path, "w", encoding="utf-8") as f:
        json.dump(master_list, f, ensure_ascii=False, indent=2)

    # 3. Update manifest.json in Sinq
    manifest_path = ASSETS_DIR / "manifest.json"
    with open(manifest_path, "r", encoding="utf-8") as f:
        manifest = json.load(f)

    # Check if 'en' is in editions
    ed_codes = [ed["code"] for ed in manifest["editions"]]
    if "en" not in ed_codes:
        manifest["editions"].append({"code": "en", "name": "English"})
        with open(manifest_path, "w", encoding="utf-8") as f:
            json.dump(manifest, f, ensure_ascii=False, separators=(',', ':'))
        print("Updated manifest.json with English edition.")

    print(f"Successfully generated all 13 months into {AFTERQ_DIR} and {ASSETS_DIR}")


if __name__ == "__main__":
    main()
