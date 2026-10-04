#!/usr/bin/env python3
"""
Generate Sinq-compatible offline assets for en-nkjv in:
app/src/main/assets/content/bible/en-nkjv/books/
"""

import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CANON_PATH = os.path.join(ROOT, "app", "src", "main", "assets", "content", "bible", "canon.json")
NKJV_JSON_PATH = os.path.join(ROOT, "nkjv_bible.json")
OUT_DIR = os.path.join(ROOT, "app", "src", "main", "assets", "content", "bible", "en-nkjv", "books")

SLUG_TO_CATENA = {
    'genesis': 'gn', 'exodus': 'ex', 'leviticus': 'lv',
    'numbers': 'nm', 'deuteronomy': 'dt', 'joshua': 'jo',
    'judges': 'jgs', 'ruth': 'ru', '1-samuel': '1sm',
    '2-samuel': '2sm', '1-kings': '1kgs', '2-kings': '2kgs',
    '1-chronicles': '1chr', '2-chronicles': '2chr', 'ezra': 'ezr',
    'nehemiah': 'neh', 'esther': 'est', 'job': 'jb',
    'psalms': 'ps', 'proverbs': 'prv', 'ecclesiastes': 'eccl',
    'song-of-solomon': 'sg', 'isaiah': 'is', 'jeremiah': 'jer',
    'lamentations': 'lam', 'ezekiel': 'ez', 'daniel': 'dn',
    'hosea': 'hos', 'joel': 'jl', 'amos': 'am',
    'obadiah': 'ob', 'jonah': 'jon', 'micah': 'mi',
    'nahum': 'na', 'habakkuk': 'hb', 'zephaniah': 'zep',
    'haggai': 'hg', 'zechariah': 'zec', 'malachi': 'mal',
    'matthew': 'mt', 'mark': 'mk', 'luke': 'lk', 'john': 'jn',
    'acts': 'acts', 'romans': 'rom', '1-corinthians': '1cor',
    '2-corinthians': '2cor', 'galatians': 'gal', 'ephesians': 'eph',
    'philippians': 'phil', 'colossians': 'col',
    '1-thessalonians': '1thes', '2-thessalonians': '2thes',
    '1-timothy': '1tm', '2-timothy': '2tm', 'titus': 'ti',
    'philemon': 'phlm', 'hebrews': 'heb', 'james': 'jas',
    '1-peter': '1pt', '2-peter': '2pt', '1-john': '1jn',
    '2-john': '2jn', '3-john': '3jn', 'jude': 'jude',
    'revelation': 'rv'
}

# Computus LXX to Masoretic Psalm mapping (from CatenaLink.kt)
PSALM_TO_MASORETIC = [
    1, 2, 3, 4, 5, 6, 7, 8, 10, 11,
    12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
    22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
    32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
    42, 43, 44, 45, 46, 47, 48, 49, 50, 51,
    52, 53, 54, 55, 56, 57, 58, 59, 60, 61,
    62, 63, 64, 65, 66, 67, 68, 69, 70, 71,
    72, 73, 74, 75, 76, 77, 78, 79, 80, 81,
    82, 83, 84, 85, 86, 87, 88, 89, 90, 91,
    92, 93, 94, 95, 96, 97, 98, 99, 100, 101,
    102, 103, 104, 105, 106, 107, 108, 109, 110, 111,
    112, 113, 114, 116, 116, 117, 118, 119, 120, 121,
    122, 123, 124, 125, 126, 127, 128, 129, 130, 131,
    132, 133, 134, 135, 136, 137, 138, 139, 140, 141,
    142, 143, 144, 145, 146, 147, 147, 148, 149, 150
]

def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    with open(CANON_PATH, "r", encoding="utf-8") as f:
        canon = json.load(f)

    with open(NKJV_JSON_PATH, "r", encoding="utf-8") as f:
        nkjv = json.load(f)

    nkjv_books_by_key = {b["key"]: b for b in nkjv["books"]}

    written_count = 0
    total_verses = 0

    for c in canon:
        slug = c["slug"]
        cat_key = SLUG_TO_CATENA.get(slug)
        if not cat_key or cat_key not in nkjv_books_by_key:
            continue

        nkjv_b = nkjv_books_by_key[cat_key]
        out_filename = os.path.basename(c["file"])
        out_filepath = os.path.join(OUT_DIR, out_filename)

        # Build book JSON
        book_obj = {
            "edition": "en-nkjv",
            "book": slug,
            "usfm_id": c["id"],
            "order": c["order"],
            "chapters": []
        }

        if slug == "psalms":
            # Build 150 LXX chapters for Psalms
            # PSALM_TO_MASORETIC maps LXX index (0-based) to Masoretic chapter (1-based)
            masoretic_chapters = {ch["chapter"]: ch for ch in nkjv_b["chapters"]}
            for lxx_num in range(1, 151):
                mas_num = PSALM_TO_MASORETIC[lxx_num - 1]
                mas_ch = masoretic_chapters.get(mas_num)
                ch_verses = []
                ch_headings = []
                if mas_ch:
                    for v in mas_ch["verses"]:
                        ch_verses.append({"n": v["verse"], "t": v["text"]})
                        total_verses += 1
                        if "headings" in v:
                            for h in v["headings"]:
                                ch_headings.append({"before": v["verse"], "text": h})

                book_obj["chapters"].append({
                    "n": lxx_num,
                    "verses": ch_verses,
                    "headings": ch_headings
                })
        else:
            for ch in nkjv_b["chapters"]:
                ch_verses = []
                ch_headings = []
                for v in ch["verses"]:
                    ch_verses.append({"n": v["verse"], "t": v["text"]})
                    total_verses += 1
                    if "headings" in v:
                        for h in v["headings"]:
                            ch_headings.append({"before": v["verse"], "text": h})

                book_obj["chapters"].append({
                    "n": ch["chapter"],
                    "verses": ch_verses,
                    "headings": ch_headings
                })

        with open(out_filepath, "w", encoding="utf-8") as out_f:
            json.dump(book_obj, out_f, separators=(',', ':'), ensure_ascii=False)
        written_count += 1

    print(f"Generated {written_count} book files in {OUT_DIR}.")
    print(f"Total verses processed: {total_verses}")

if __name__ == "__main__":
    main()
