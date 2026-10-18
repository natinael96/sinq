#!/usr/bin/env python3
"""Compile the bundled immutable text corpora into a pre-packaged SQLite asset.

Outputs: app/src/main/assets/database/sinq_corpus.db
Contains:
  * corpus_docs: normalized document records with raw haystack and pre-folded search strings
  * corpus_fts: SQLite FTS4 virtual table for fast full-text searching
"""
from __future__ import annotations

import json
import os
import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONTENT = ROOT / "app/src/main/assets/content"
DB_DIR = ROOT / "app/src/main/assets/database"
DB_PATH = DB_DIR / "sinq_corpus.db"

GEEZ_VALUE = {
    '፩': 1, '፪': 2, '፫': 3, '፬': 4, '፭': 5, '፮': 6, '፯': 7, '፰': 8, '፱': 9,
    '፲': 10, '፳': 20, '፴': 30, '፵': 40, '፶': 50, '፷': 60, '፸': 70, '፹': 80,
    '፺': 90, '፻': 100, '፼': 10000,
}

ONES = ["", "፩", "፪", "፫", "፬", "፭", "፮", "፯", "፰", "፱"]
TENS = ["", "፲", "፳", "፴", "፵", "፶", "፷", "፸", "፹", "፺"]


def geez_numeral(n: int) -> str:
    if n < 0:
        return str(n)
    if n >= 10000:
        count, rest = divmod(n, 10000)
        return (geez_numeral(count) if count != 1 else "") + "፼" + (geez_numeral(rest) if rest > 0 else "")
    if n >= 100:
        count, rest = divmod(n, 100)
        return (geez_numeral(count) if count != 1 else "") + "፻" + (geez_numeral(rest) if rest > 0 else "")
    return TENS[n // 10] + ONES[n % 10]


def parse_geez_numeral(s: str) -> int:
    if not s:
        return 0
    if s.isascii() and s.isdigit():
        return int(s)
    if '፼' in s:
        parts = s.split('፼', 1)
        high = parse_geez_numeral(parts[0]) if parts[0] else 1
        low = parse_geez_numeral(parts[1]) if parts[1] else 0
        return high * 10000 + low
    if '፻' in s:
        parts = s.split('፻', 1)
        high = parse_geez_numeral(parts[0]) if parts[0] else 1
        low = parse_geez_numeral(parts[1]) if parts[1] else 0
        return high * 100 + low
    total = 0
    for c in s:
        total += GEEZ_VALUE.get(c, 0)
    return total


def normalize_numerals(text: str) -> str:
    return re.sub(r'[፩-፼]+', lambda m: str(parse_geez_numeral(m.group(0))), text)


def fold_char(c: str) -> str:
    # Ha family: ሀ, ሐ, ኀ, ኸ (including 1st & 4th order equivalence)
    if c in ('ሀ', 'ሐ', 'ኀ', 'ሃ', 'ሓ', 'ኃ', 'ኻ', 'ኸ'):
        return 'ሀ'
    cp = ord(c)
    # ሐ series (0x1210..0x1217)
    if 0x1210 <= cp <= 0x1217:
        order = cp - 0x1210
        if order == 3:
            return 'ሀ'
        return chr(0x1200 + order)
    # ኀ series (0x1280..0x1287)
    if 0x1280 <= cp <= 0x1287:
        order = cp - 0x1280
        if order == 3:
            return 'ሀ'
        return chr(0x1200 + order)
    # ኸ series (0x12B8..0x12BE)
    if 0x12B8 <= cp <= 0x12BE:
        order = cp - 0x12B8
        if order == 3:
            return 'ሀ'
        return chr(0x1200 + order)

    # Glottals: አ, ዐ (including 1st & 4th order equivalence)
    if c in ('አ', 'ዐ', 'ዓ', 'ኣ'):
        return 'አ'
    # ዐ series (0x12D0..0x12D7)
    if 0x12D0 <= cp <= 0x12D7:
        order = cp - 0x12D0
        if order == 3:
            return 'አ'
        return chr(0x12A0 + order)

    # Sibilants: ሠ -> ሰ
    if 0x1220 <= cp <= 0x1227:
        return chr(0x1230 + (cp - 0x1220))
    # ፀ -> ጸ
    if 0x1340 <= cp <= 0x1347:
        return chr(0x1338 + (cp - 0x1340))

    # Labialized Velars
    if c == 'ኰ': return 'ኮ'
    if c == 'ጐ': return 'ጎ'
    if c == 'ቈ': return 'ቆ'
    if c == 'ኈ': return 'ሆ'
    if c == 'ኋ': return 'ዃ'

    # Wordspace
    if c == '፡': return ' '
    # Ethiopic punctuation stripped
    if c in ('።', '፣', '፤', '፥', '፦', '፧', '፨'):
        return ''

    return c


def fold(text: str) -> str:
    text = normalize_numerals(text)
    folded = ''.join(fold_char(c) for c in text)
    return re.sub(r'[ \t]+', ' ', folded).strip()


def build_corpus_database():
    DB_DIR.mkdir(parents=True, exist_ok=True)
    if DB_PATH.exists():
        DB_PATH.unlink()

    conn = sqlite3.connect(str(DB_PATH))
    cur = conn.cursor()

    cur.execute("PRAGMA page_size = 4096;")
    cur.execute("PRAGMA journal_mode = OFF;")
    cur.execute("PRAGMA synchronous = OFF;")

    cur.execute("""
    CREATE TABLE corpus_docs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        source TEXT NOT NULL,
        target_id TEXT NOT NULL,
        source_label TEXT NOT NULL,
        target_index INTEGER NOT NULL,
        title TEXT NOT NULL,
        route TEXT NOT NULL,
        haystack TEXT NOT NULL,
        folded TEXT NOT NULL,
        verse_starts TEXT,
        verse_numbers TEXT
    );
    """)

    cur.execute("""
    CREATE VIRTUAL TABLE corpus_fts USING fts4(
        content="corpus_docs",
        title,
        folded
    );
    """)

    docs: list[tuple] = []

    # 1. Scripture
    bible_meta_file = CONTENT / "bible/am-1980/meta.json"
    if bible_meta_file.exists():
        with open(bible_meta_file, "r", encoding="utf-8") as f:
            bible_meta = json.load(f)
        for b_entry in bible_meta.get("books", []):
            b_file_rel = b_entry.get("file", "")
            name_am = b_entry.get("name", "")
            key = b_entry.get("usfm_id", b_entry.get("id", "")).lower()
            book_file = CONTENT / f"bible/am-1980/{b_file_rel}"
            if not book_file.exists():
                continue
            with open(book_file, "r", encoding="utf-8") as f:
                book_data = json.load(f)
            for ch in book_data.get("chapters", []):
                chapter_num = ch.get("chapter", 1)
                verses = ch.get("verses", [])
                starts = []
                numbers = []
                text_parts = []
                cur_len = 0
                for i, v in enumerate(verses):
                    v_text = v.get("text", "")
                    if i > 0:
                        cur_len += 1
                    starts.append(cur_len)
                    numbers.append(v.get("n", i + 1))
                    text_parts.append(v_text)
                    cur_len += len(v_text)
                hay = " ".join(text_parts)
                title = f"{name_am} {geez_numeral(chapter_num)}"
                route = f"scripture/{key}/{chapter_num}"
                docs.append((
                    "SCRIPTURE",
                    key,
                    "መጽሐፍ ቅዱስ",
                    chapter_num,
                    title,
                    route,
                    hay,
                    fold(hay),
                    json.dumps(starts),
                    json.dumps(numbers),
                ))

    # 2. Synaxarium
    sinksar_dir = CONTENT / "sinksar"
    if sinksar_dir.exists():
        for m in range(1, 14):
            m_file = sinksar_dir / f"am-{m}.json"
            if not m_file.exists():
                continue
            with open(m_file, "r", encoding="utf-8") as f:
                m_data = json.load(f)
            for day in m_data.get("days", []):
                day_num = day.get("day", 1)
                for entry_idx, entry in enumerate(day.get("entries", [])):
                    paragraphs = entry.get("paragraphs", [])
                    para_texts = [p.get("text", "") for p in paragraphs if p.get("text")]
                    first_para = para_texts[0] if para_texts else ""
                    heading = first_para.split("።")[0].strip() if "።" in first_para else first_para[:100].strip()
                    body = " ".join(para_texts)
                    arke = entry.get("arke") or ""
                    hay = f"{body} {arke}".strip()
                    target_id = f"{m}-{day_num}-{entry_idx}"
                    route = f"synaxarium/{m}/{day_num}?entry={entry_idx}"
                    docs.append((
                        "SYNAXARIUM",
                        target_id,
                        "ስንክሳር",
                        day_num,
                        heading,
                        route,
                        hay,
                        fold(hay),
                        "[]",
                        "[]",
                    ))

    # 3. Books
    books_index_file = CONTENT / "books/index.json"
    if books_index_file.exists():
        with open(books_index_file, "r", encoding="utf-8") as f:
            b_index = json.load(f)
        for shelf in b_index.get("shelves", []):
            for b_meta in shelf.get("books", []):
                b_id = b_meta.get("id")
                b_file = CONTENT / f"books/{b_id}.json"
                if not b_file.exists():
                    continue
                with open(b_file, "r", encoding="utf-8") as f:
                    b_data = json.load(f)
                b_title = b_data.get("title", "")
                chapters = b_data.get("chapters", [])
                for ch in chapters:
                    ch_num = ch.get("number", 1)
                    ch_title = ch.get("title", "")
                    blocks = ch.get("blocks", [])
                    hay = " ".join(b.get("text", "") for b in blocks if b.get("text")).strip()
                    if not hay:
                        continue
                    title = f"{b_title} · {ch_title}" if ch_title and len(chapters) > 1 else b_title
                    route = f"book/{b_id}?ch={ch_num}"
                    docs.append((
                        "BOOK",
                        b_id,
                        "መጻሕፍት",
                        ch_num,
                        title,
                        route,
                        hay,
                        fold(hay),
                        "[]",
                        "[]",
                    ))

    # 4. Wudase
    wudase_file = CONTENT / "wudase/wudase.json"
    if wudase_file.exists():
        with open(wudase_file, "r", encoding="utf-8") as f:
            w_data = json.load(f)
        for sec in w_data.get("sections", []):
            s_id = sec.get("id", "")
            title = sec.get("titleAm") or sec.get("label", "")
            am_texts = sec.get("am", [])
            ge_texts = sec.get("ge", [])
            hay = " ".join(am_texts + ge_texts).strip()
            route = f"wudase?sec={s_id}"
            docs.append((
                "WUDASE",
                s_id,
                "ውዳሴ ማርያም",
                0,
                title,
                route,
                hay,
                fold(hay),
                "[]",
                "[]",
            ))

    # 5. Hours
    hours_files = [
        "morning.json", "terce.json", "sext.json", "none.json",
        "vespers.json", "compline.json", "veil.json", "midnight.json"
    ]
    for h_name in hours_files:
        h_file = CONTENT / h_name
        if not h_file.exists():
            continue
        with open(h_file, "r", encoding="utf-8") as f:
            h_data = json.load(f)
        h_id = h_data.get("id", h_file.stem)
        h_label = h_data.get("name", h_id)
        for idx, sec in enumerate(h_data.get("sections", [])):
            sec_title = sec.get("title", "")
            sec_sub = sec.get("subtitle") or ""
            verses = sec.get("verses", [])
            hay = f"{sec_title} {sec_sub} " + " ".join(verses)
            hay = hay.strip()
            docs.append((
                "HOUR",
                h_id,
                h_label,
                idx,
                sec_title,
                f"prayer/{h_id}?sec={idx}",
                hay,
                fold(hay),
                "[]",
                "[]",
            ))

    # 6. Psalter (Amharic & Ge'ez)
    for geez, sub_dir, label, target_id in [
        (False, "am-1980", "መዝሙረ ዳዊት", ""),
        (True, "gez-1980", "መዝሙረ ዳዊት (ግዕዝ)", "geez"),
    ]:
        psalms_file = CONTENT / f"bible/{sub_dir}/books/19-psalms.json"
        if psalms_file.exists():
            with open(psalms_file, "r", encoding="utf-8") as f:
                ps_data = json.load(f)
            for idx, ch in enumerate(ps_data.get("chapters", [])):
                ch_num = ch.get("chapter", idx + 1)
                v_texts = [v.get("text", "") for v in ch.get("verses", [])]
                hay = " ".join(v_texts).strip()
                title = f"{label} {geez_numeral(ch_num)}"
                route = f"psalter?section={idx}" + ("&geez=true" if geez else "")
                docs.append((
                    "PSALTER",
                    target_id,
                    label,
                    idx,
                    title,
                    route,
                    hay,
                    fold(hay),
                    "[]",
                    "[]",
                ))

    cur.executemany("""
    INSERT INTO corpus_docs (
        source, target_id, source_label, target_index, title, route,
        haystack, folded, verse_starts, verse_numbers
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    """, docs)

    cur.execute("""
    INSERT INTO corpus_fts (docid, title, folded)
    SELECT id, title, folded FROM corpus_docs;
    """)

    cur.execute("CREATE INDEX idx_corpus_source ON corpus_docs(source);")
    cur.execute("CREATE INDEX idx_corpus_target ON corpus_docs(target_id);")

    conn.commit()
    cur.execute("VACUUM;")
    conn.close()

    size_mb = DB_PATH.stat().st_size / (1024 * 1024)
    print(f"Compiled {len(docs)} documents into {DB_PATH.name} ({size_mb:.2f} MB)")


if __name__ == "__main__":
    build_corpus_database()
