#!/usr/bin/env python3
"""
Extract the complete NKJV Bible from catenabible.com and compile it into JSON.
"""

import concurrent.futures
import json
import os
import random
import re
import sys
import time
import urllib.request
import urllib.error

# 66 canonical books from Catena Bible's NKJV edition
BOOKS = [
    {"key": "gn", "name": "Genesis", "chapters": 50, "testament": "OT"},
    {"key": "ex", "name": "Exodus", "chapters": 40, "testament": "OT"},
    {"key": "lv", "name": "Leviticus", "chapters": 27, "testament": "OT"},
    {"key": "nm", "name": "Numbers", "chapters": 36, "testament": "OT"},
    {"key": "dt", "name": "Deuteronomy", "chapters": 34, "testament": "OT"},
    {"key": "jo", "name": "Joshua", "chapters": 24, "testament": "OT"},
    {"key": "jgs", "name": "Judges", "chapters": 21, "testament": "OT"},
    {"key": "ru", "name": "Ruth", "chapters": 4, "testament": "OT"},
    {"key": "1sm", "name": "1 Samuel", "chapters": 31, "testament": "OT"},
    {"key": "2sm", "name": "2 Samuel", "chapters": 24, "testament": "OT"},
    {"key": "1kgs", "name": "1 Kings", "chapters": 22, "testament": "OT"},
    {"key": "2kgs", "name": "2 Kings", "chapters": 25, "testament": "OT"},
    {"key": "1chr", "name": "1 Chronicles", "chapters": 29, "testament": "OT"},
    {"key": "2chr", "name": "2 Chronicles", "chapters": 36, "testament": "OT"},
    {"key": "ezr", "name": "Ezra", "chapters": 10, "testament": "OT"},
    {"key": "neh", "name": "Nehemiah", "chapters": 13, "testament": "OT"},
    {"key": "est", "name": "Esther", "chapters": 10, "testament": "OT"},
    {"key": "jb", "name": "Job", "chapters": 42, "testament": "OT"},
    {"key": "ps", "name": "Psalms", "chapters": 150, "testament": "OT"},
    {"key": "prv", "name": "Proverbs", "chapters": 31, "testament": "OT"},
    {"key": "eccl", "name": "Ecclesiastes", "chapters": 12, "testament": "OT"},
    {"key": "sg", "name": "Song of Songs", "chapters": 8, "testament": "OT"},
    {"key": "is", "name": "Isaiah", "chapters": 66, "testament": "OT"},
    {"key": "jer", "name": "Jeremiah", "chapters": 52, "testament": "OT"},
    {"key": "lam", "name": "Lamentations", "chapters": 5, "testament": "OT"},
    {"key": "ez", "name": "Ezekiel", "chapters": 48, "testament": "OT"},
    {"key": "dn", "name": "Daniel", "chapters": 12, "testament": "OT"},
    {"key": "hos", "name": "Hosea", "chapters": 14, "testament": "OT"},
    {"key": "jl", "name": "Joel", "chapters": 3, "testament": "OT"},
    {"key": "am", "name": "Amos", "chapters": 9, "testament": "OT"},
    {"key": "ob", "name": "Obadiah", "chapters": 1, "testament": "OT"},
    {"key": "jon", "name": "Jonah", "chapters": 4, "testament": "OT"},
    {"key": "mi", "name": "Micah", "chapters": 7, "testament": "OT"},
    {"key": "na", "name": "Nahum", "chapters": 3, "testament": "OT"},
    {"key": "hb", "name": "Habakkuk", "chapters": 3, "testament": "OT"},
    {"key": "zep", "name": "Zephaniah", "chapters": 3, "testament": "OT"},
    {"key": "hg", "name": "Haggai", "chapters": 2, "testament": "OT"},
    {"key": "zec", "name": "Zechariah", "chapters": 14, "testament": "OT"},
    {"key": "mal", "name": "Malachi", "chapters": 4, "testament": "OT"},
    {"key": "mt", "name": "Matthew", "chapters": 28, "testament": "NT"},
    {"key": "mk", "name": "Mark", "chapters": 16, "testament": "NT"},
    {"key": "lk", "name": "Luke", "chapters": 24, "testament": "NT"},
    {"key": "jn", "name": "John", "chapters": 21, "testament": "NT"},
    {"key": "acts", "name": "Acts", "chapters": 28, "testament": "NT"},
    {"key": "rom", "name": "Romans", "chapters": 16, "testament": "NT"},
    {"key": "1cor", "name": "1 Corinthians", "chapters": 16, "testament": "NT"},
    {"key": "2cor", "name": "2 Corinthians", "chapters": 13, "testament": "NT"},
    {"key": "gal", "name": "Galatians", "chapters": 6, "testament": "NT"},
    {"key": "eph", "name": "Ephesians", "chapters": 6, "testament": "NT"},
    {"key": "phil", "name": "Philippians", "chapters": 4, "testament": "NT"},
    {"key": "col", "name": "Colossians", "chapters": 4, "testament": "NT"},
    {"key": "1thes", "name": "1 Thessalonians", "chapters": 5, "testament": "NT"},
    {"key": "2thes", "name": "2 Thessalonians", "chapters": 3, "testament": "NT"},
    {"key": "1tm", "name": "1 Timothy", "chapters": 6, "testament": "NT"},
    {"key": "2tm", "name": "2 Timothy", "chapters": 4, "testament": "NT"},
    {"key": "ti", "name": "Titus", "chapters": 3, "testament": "NT"},
    {"key": "phlm", "name": "Philemon", "chapters": 1, "testament": "NT"},
    {"key": "heb", "name": "Hebrews", "chapters": 13, "testament": "NT"},
    {"key": "jas", "name": "James", "chapters": 5, "testament": "NT"},
    {"key": "1pt", "name": "1 Peter", "chapters": 5, "testament": "NT"},
    {"key": "2pt", "name": "2 Peter", "chapters": 3, "testament": "NT"},
    {"key": "1jn", "name": "1 John", "chapters": 5, "testament": "NT"},
    {"key": "2jn", "name": "2 John", "chapters": 1, "testament": "NT"},
    {"key": "3jn", "name": "3 John", "chapters": 1, "testament": "NT"},
    {"key": "jude", "name": "Jude", "chapters": 1, "testament": "NT"},
    {"key": "rv", "name": "Revelation", "chapters": 22, "testament": "NT"},
]

CACHE_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".cache", "catena_nkjv")


def parse_rsc_content(raw_text):
    """Extract chapter JSON object from Next.js RSC payload."""
    for line in raw_text.split("\n"):
        if '"bookKey":' in line:
            idx = line.find('{"bookKey":')
            if idx != -1:
                chunk = line[idx:]
                for end in range(len(chunk), 0, -1):
                    try:
                        return json.loads(chunk[:end])
                    except ValueError:
                        pass
    return None


def fetch_chapter(book_key, chapter, max_retries=5):
    """Fetch a single chapter from Catena Bible RSC endpoint."""
    os.makedirs(CACHE_DIR, exist_ok=True)
    cache_file = os.path.join(CACHE_DIR, f"{book_key}_{chapter}.json")

    if os.path.exists(cache_file):
        try:
            with open(cache_file, "r", encoding="utf-8") as f:
                data = json.load(f)
                if data and "paragraphs" in data:
                    return data
        except Exception:
            pass

    url = f"https://catenabible.com/bible/nkjv/{book_key}/{chapter}"
    headers = {
        "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "RSC": "1",
    }

    for attempt in range(max_retries):
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=15) as resp:
                content = resp.read().decode("utf-8", errors="ignore")
                parsed = parse_rsc_content(content)
                if parsed and "paragraphs" in parsed:
                    with open(cache_file, "w", encoding="utf-8") as f:
                        json.dump(parsed, f, ensure_ascii=False)
                    return parsed
        except Exception as e:
            if attempt == max_retries - 1:
                print(f"Error fetching {book_key} {chapter}: {e}", file=sys.stderr)
                return None
            time.sleep((attempt + 1) * 1.5 + random.uniform(0.1, 0.5))

    return None


def process_chapter(raw_chapter):
    """
    Transform raw chapter structure into clean, standard format.
    Extracts headings, clean verse text, footnotes, and verse parts.
    """
    headings = []
    verses = []
    
    # Track current active headings before verses
    pending_headings = []

    for p in raw_chapter.get("paragraphs", []):
        ptype = p.get("type")
        if ptype == "section_header":
            txt = p.get("text", "").strip()
            if txt:
                pending_headings.append(txt)
        elif ptype == "section_paragraph":
            verses_list = p.get("verses_list", [])
            for v in verses_list:
                vnum = v.get("num_int") or int(v.get("num_str") or v.get("num_string") or 0)
                vparts = v.get("verse_parts", [])
                
                # Build clean text excluding FOOTNOTE items
                text_pieces = []
                footnotes = []
                
                for part in vparts:
                    pstyle = part.get("style", "NONE")
                    ptext = part.get("text", "")
                    if pstyle == "FOOTNOTE":
                        ftext = ptext.strip()
                        if ftext:
                            footnotes.append(ftext)
                    elif pstyle == "LINE_BREAK":
                        text_pieces.append(" ")
                    else:
                        text_pieces.append(ptext)

                clean_text = "".join(text_pieces)
                # Normalize repeated whitespace
                clean_text = re.sub(r"[ \t]+", " ", clean_text).strip()

                verse_entry = {
                    "verse": vnum,
                    "text": clean_text,
                }
                if pending_headings:
                    verse_entry["headings"] = list(pending_headings)
                    pending_headings.clear()
                if footnotes:
                    verse_entry["footnotes"] = footnotes
                if vparts:
                    verse_entry["verse_parts"] = vparts

                verses.append(verse_entry)

    return {
        "chapter": raw_chapter.get("chapter"),
        "verse_count": len(verses),
        "verses": verses,
    }


def main():
    total_chapters = sum(b["chapters"] for b in BOOKS)
    print(f"Starting NKJV Bible extraction from catenabible.com...")
    print(f"Total books: {len(BOOKS)}, Total chapters: {total_chapters}")

    tasks = []
    for b in BOOKS:
        for ch in range(1, b["chapters"] + 1):
            tasks.append((b["key"], ch))

    print(f"Prepared {len(tasks)} chapter tasks. Fetching concurrently...")
    start_time = time.time()
    raw_data_map = {}
    completed_count = 0

    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        future_to_task = {
            executor.submit(fetch_chapter, b_key, ch): (b_key, ch)
            for b_key, ch in tasks
        }
        for future in concurrent.futures.as_completed(future_to_task):
            task = future_to_task[future]
            res = future.result()
            if res:
                raw_data_map[task] = res
            else:
                print(f"FAILED to fetch {task[0]} {task[1]}", file=sys.stderr)
            completed_count += 1
            if completed_count % 50 == 0 or completed_count == total_chapters:
                elapsed = time.time() - start_time
                pct = (completed_count / total_chapters) * 100
                print(f"Progress: {completed_count}/{total_chapters} chapters ({pct:.1f}%) in {elapsed:.1f}s")

    print(f"All downloads finished. Successfully fetched: {len(raw_data_map)}/{total_chapters} chapters.")

    # Assemble structured Bible JSON
    bible = {
        "translation": "nkjv",
        "name": "New King James Version",
        "source": "catenabible.com",
        "retrieved_at": time.strftime("%Y-%m-%d %H:%M:%SZ", time.gmtime()),
        "books": []
    }

    total_verses = 0

    for b in BOOKS:
        b_key = b["key"]
        book_entry = {
            "key": b_key,
            "name": b["name"],
            "testament": b["testament"],
            "chapter_count": b["chapters"],
            "chapters": []
        }

        for ch in range(1, b["chapters"] + 1):
            raw_ch = raw_data_map.get((b_key, ch))
            if not raw_ch:
                print(f"Missing chapter data for {b_key} {ch}!", file=sys.stderr)
                continue
            ch_data = process_chapter(raw_ch)
            total_verses += ch_data["verse_count"]
            book_entry["chapters"].append(ch_data)

        bible["books"].append(book_entry)

    bible["total_books"] = len(bible["books"])
    bible["total_chapters"] = total_chapters
    bible["total_verses"] = total_verses

    output_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "nkjv_bible.json")
    print(f"Writing complete NKJV Bible to {output_path} (Total verses: {total_verses})...")
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(bible, f, ensure_ascii=False, indent=2)

    # Also write a lightweight compact version (without verse_parts, just clean text & headings)
    compact_bible = {
        "translation": "nkjv",
        "name": "New King James Version",
        "source": "catenabible.com",
        "retrieved_at": bible["retrieved_at"],
        "total_books": bible["total_books"],
        "total_chapters": bible["total_chapters"],
        "total_verses": total_verses,
        "books": []
    }

    for b in bible["books"]:
        compact_b = {
            "key": b["key"],
            "name": b["name"],
            "testament": b["testament"],
            "chapter_count": b["chapter_count"],
            "chapters": []
        }
        for ch in b["chapters"]:
            compact_ch = {
                "chapter": ch["chapter"],
                "verses": []
            }
            for v in ch["verses"]:
                v_obj = {
                    "verse": v["verse"],
                    "text": v["text"],
                }
                if "headings" in v:
                    v_obj["headings"] = v["headings"]
                if "footnotes" in v:
                    v_obj["footnotes"] = v["footnotes"]
                compact_ch["verses"].append(v_obj)
            compact_b["chapters"].append(compact_ch)
        compact_bible["books"].append(compact_b)

    compact_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "nkjv_bible_compact.json")
    print(f"Writing compact NKJV Bible to {compact_path}...")
    with open(compact_path, "w", encoding="utf-8") as f:
        json.dump(compact_bible, f, ensure_ascii=False, indent=2)

    print("Extraction complete!")
    print(f"  Full JSON: {output_path} ({os.path.getsize(output_path) / (1024*1024):.2f} MB)")
    print(f"  Compact JSON: {compact_path} ({os.path.getsize(compact_path) / (1024*1024):.2f} MB)")


if __name__ == "__main__":
    main()
