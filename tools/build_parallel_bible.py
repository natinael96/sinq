#!/usr/bin/env python3
"""
Build bilingual English (NKJV) + Amharic (1980 EC) parallel dataset and
generate a side-by-side, line-by-line interactive preview HTML.
"""

import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FULL_JSON_PATH = os.path.join(ROOT, "nkjv_bible.json")
PARALLEL_JSON_PATH = os.path.join(ROOT, "nkjv_amharic_parallel.json")
OUTPUT_HTML_PATH = os.path.join(ROOT, "preview_nkjv.html")

# Map of app slugs to Catena keys
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

MAS_TO_LXX = {}
for lxx_0, mas in enumerate(PSALM_TO_MASORETIC):
    if mas not in MAS_TO_LXX:
        MAS_TO_LXX[mas] = lxx_0 + 1

# Ge'ez numerals conversion
GEEZ_DIGITS = ["", "፩", "፪", "፫", "፬", "፭", "፮", "፯", "፰", "፱"]
GEEZ_TENS = ["", "፲", "፳", "፴", "፵", "፶", "፷", "፸", "፹", "፺"]

def to_geez(n):
    if n <= 0:
        return str(n)
    if n < 10:
        return GEEZ_DIGITS[n]
    if n < 100:
        return GEEZ_TENS[n // 10] + GEEZ_DIGITS[n % 10]
    if n == 100:
        return "፻"
    if n < 1000:
        c = n // 100
        rem = n % 100
        prefix = ("" if c == 1 else to_geez(c)) + "፻"
        return prefix + (to_geez(rem) if rem else "")
    return str(n)


def load_amharic_corpus():
    canon_path = os.path.join(ROOT, "app", "src", "main", "assets", "content", "bible", "canon.json")
    with open(canon_path, "r", encoding="utf-8") as f:
        canon = json.load(f)

    am_corpus = {}
    for c in canon:
        slug = c["slug"]
        if slug in SLUG_TO_CATENA:
            cat_key = SLUG_TO_CATENA[slug]
            filepath = os.path.join(ROOT, "app", "src", "main", "assets", "content", "bible", "am-1980", "books", c["file"])
            if os.path.exists(filepath):
                with open(filepath, "r", encoding="utf-8") as bf:
                    bdata = json.load(bf)
                    am_corpus[cat_key] = {
                        "name_am": c.get("name_am") or bdata.get("name") or c.get("name_en"),
                        "chapters": {ch["n"]: ch for ch in bdata.get("chapters", [])}
                    }
    return am_corpus


def main():
    print("Loading NKJV Bible JSON...")
    with open(FULL_JSON_PATH, "r", encoding="utf-8") as f:
        nkjv_bible = json.load(f)

    print("Loading Amharic 1980 EC Bible corpus...")
    am_corpus = load_amharic_corpus()

    print("Building unified parallel dataset (English NKJV + Amharic 1980)...")
    parallel_bible = {
        "translation_en": "nkjv",
        "translation_am": "am-1980",
        "title": "NKJV & Amharic 1980 Parallel Bible",
        "total_books": len(nkjv_bible["books"]),
        "total_chapters": nkjv_bible["total_chapters"],
        "books": []
    }

    total_aligned_verses = 0

    for b in nkjv_bible["books"]:
        cat_key = b["key"]
        am_book = am_corpus.get(cat_key, {})
        am_chapters = am_book.get("chapters", {})

        p_book = {
            "key": cat_key,
            "name_en": b["name"],
            "name_am": am_book.get("name_am", b["name"]),
            "testament": b["testament"],
            "chapter_count": b["chapter_count"],
            "chapters": []
        }

        for ch in b["chapters"]:
            ch_num = ch["chapter"]
            # For Psalms, align by liturgical content: Masoretic chapter -> LXX chapter
            if cat_key == "ps":
                am_ch_num = MAS_TO_LXX.get(ch_num, ch_num)
            else:
                am_ch_num = ch_num

            am_ch = am_chapters.get(am_ch_num, {})
            am_verses_list = am_ch.get("verses", [])
            am_v_map = {v["n"]: v for v in am_verses_list}

            p_ch = {
                "chapter": ch_num,
                "am_chapter": am_ch_num,
                "verses": []
            }

            for v in ch["verses"]:
                v_num = v["verse"]
                am_v = am_v_map.get(v_num)

                p_verse = {
                    "verse": v_num,
                    "geez": to_geez(v_num),
                    "en": v["text"],
                    "am": am_v.get("t", "") if am_v else "",
                }
                if "headings" in v:
                    p_verse["headings"] = v["headings"]
                if "footnotes" in v:
                    p_verse["footnotes"] = v["footnotes"]
                if "verse_parts" in v:
                    p_verse["verse_parts"] = v["verse_parts"]

                p_ch["verses"].append(p_verse)
                total_aligned_verses += 1

            p_book["chapters"].append(p_ch)

        parallel_bible["books"].append(p_book)

    parallel_bible["total_verses"] = total_aligned_verses

    print(f"Writing parallel JSON to {PARALLEL_JSON_PATH}...")
    with open(PARALLEL_JSON_PATH, "w", encoding="utf-8") as f:
        json.dump(parallel_bible, f, ensure_ascii=False, indent=2)

    par_size_mb = os.path.getsize(PARALLEL_JSON_PATH) / (1024 * 1024)
    print(f"Parallel JSON written: {par_size_mb:.2f} MB (Total verses: {total_aligned_verses})")

    # Minify for embedded browser preview
    print("Generating interactive side-by-side preview HTML...")
    light_parallel = {
        "title": parallel_bible["title"],
        "books": []
    }
    for b in parallel_bible["books"]:
        lb = {
            "key": b["key"],
            "name_en": b["name_en"],
            "name_am": b["name_am"],
            "testament": b["testament"],
            "chapter_count": b["chapter_count"],
            "chapters": []
        }
        for ch in b["chapters"]:
            lch = {
                "chapter": ch["chapter"],
                "am_chapter": ch["am_chapter"],
                "verses": []
            }
            for v in ch["verses"]:
                lv = {
                    "v": v["verse"],
                    "g": v["geez"],
                    "en": v["en"],
                    "am": v["am"]
                }
                if "headings" in v:
                    lv["h"] = v["headings"]
                if "footnotes" in v:
                    lv["fn"] = v["footnotes"]
                if "verse_parts" in v:
                    lv["vp"] = v["verse_parts"]
                lch["verses"].append(lv)
            lb["chapters"].append(lch)
        light_parallel["books"].append(lb)

    json_data_str = json.dumps(light_parallel, separators=(',', ':'), ensure_ascii=False)

    html_code = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>English (NKJV) & Amharic (1980) Side-by-Side Parallel Bible</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Noto+Serif+Ethiopic:wght@400;600;700&family=Cardo:ital,wght@0,400;0,700;1,400&family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
  <style>
    :root {{
      --bg: #0d1117;
      --card-bg: #161b22;
      --card-border: #30363d;
      --row-hover: #1c222b;
      --row-alt: rgba(22, 27, 34, 0.4);
      --text-en: #e6edf3;
      --text-am: #f0f6fc;
      --text-muted: #8b949e;
      --accent-gold: #e8c46b;
      --accent-green: #3fb950;
      --accent-blue: #58a6ff;
      --jesus-red: #ff7b72;
      --divine-name: #f2cc60;
      --font-en: "Cardo", Georgia, serif;
      --font-am: "Noto Serif Ethiopic", "Abyssinica SIL", "Nyala", "Kefa", serif;
      --font-ui: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      --font-mono: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
    }}

    * {{ box-sizing: border-box; margin: 0; padding: 0; }}

    body {{
      background-color: var(--bg);
      color: var(--text-en);
      font-family: var(--font-ui);
      line-height: 1.6;
    }}

    header {{
      background: linear-gradient(180deg, #161f28 0%, #111822 100%);
      border-bottom: 1px solid var(--card-border);
      padding: 16px 28px;
      position: sticky;
      top: 0;
      z-index: 100;
    }}

    .header-top {{
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }}

    .badge-sinq {{
      background: #0e3b31;
      color: #e8c46b;
      padding: 4px 10px;
      border-radius: 6px;
      font-weight: 700;
      font-size: 13px;
      border: 1px solid rgba(232, 196, 107, 0.3);
    }}

    h1 {{
      font-size: 19px;
      font-weight: 700;
      color: #fff;
    }}

    .controls-row {{
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-top: 14px;
    }}

    .nav-group {{
      display: flex;
      align-items: center;
      gap: 8px;
    }}

    select, button.btn-ui, input.search-input {{
      background: #21262d;
      color: #e6edf3;
      border: 1px solid var(--card-border);
      padding: 7px 12px;
      border-radius: 6px;
      font-size: 13px;
      font-weight: 500;
      outline: none;
      cursor: pointer;
      transition: all 0.15s ease;
    }}

    select:focus, input.search-input:focus {{
      border-color: var(--accent-blue);
    }}

    button.btn-ui:hover:not(:disabled) {{
      background: #30363d;
      border-color: #8b949e;
    }}

    button.btn-ui:disabled {{
      opacity: 0.4;
      cursor: not-allowed;
    }}

    button.btn-ui.active {{
      background: var(--accent-gold);
      color: #000;
      border-color: var(--accent-gold);
      font-weight: 700;
    }}

    .tag {{
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 600;
      background: #21262d;
      border: 1px solid var(--card-border);
    }}

    .tag.gold {{ color: #e8c46b; border-color: rgba(232, 196, 107, 0.3); }}
    .tag.green {{ color: #3fb950; border-color: rgba(63, 185, 80, 0.3); }}
    .tag.purple {{ color: #d2a8ff; border-color: rgba(210, 168, 255, 0.3); }}

    main {{
      max-width: 1480px;
      margin: 0 auto;
      padding: 20px 24px 64px 24px;
    }}

    /* Column Headers */
    .bilingual-header {{
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      background: #11161d;
      border: 1px solid var(--card-border);
      padding: 14px 24px;
      border-radius: 8px 8px 0 0;
      font-weight: 700;
      font-size: 15px;
      position: sticky;
      top: 110px;
      z-index: 50;
    }}

    .bilingual-header.stacked-mode {{
      display: none;
    }}

    .col-title-en {{
      color: var(--accent-gold);
      display: flex;
      align-items: center;
      gap: 8px;
    }}

    .col-title-am {{
      color: #58a6ff;
      display: flex;
      align-items: center;
      gap: 8px;
    }}

    /* Verse Container */
    .verses-container {{
      border: 1px solid var(--card-border);
      border-top: none;
      background: var(--card-bg);
      border-radius: 0 0 8px 8px;
      overflow: hidden;
    }}

    /* Parallel Row (Side by Side) */
    .parallel-row {{
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      padding: 16px 24px;
      border-bottom: 1px solid rgba(48, 54, 61, 0.5);
      transition: background 0.15s ease;
    }}

    .parallel-row:nth-child(even) {{
      background: var(--row-alt);
    }}

    .parallel-row:hover {{
      background: var(--row-hover);
    }}

    /* Stacked Interlinear Mode */
    .verses-container.stacked-mode .parallel-row {{
      display: block;
      padding: 18px 28px;
    }}

    .verses-container.stacked-mode .parallel-row .verse-cell-am {{
      margin-top: 10px;
      padding-top: 10px;
      border-top: 1px dashed rgba(48, 54, 61, 0.6);
    }}

    .verse-cell-en {{
      font-family: var(--font-en);
      font-size: 18px;
      line-height: 1.8;
      color: var(--text-en);
    }}

    .verse-cell-am {{
      font-family: var(--font-am);
      font-size: 18px;
      line-height: 1.9;
      color: var(--text-am);
    }}

    .verse-num-badge {{
      display: inline-block;
      font-family: var(--font-ui);
      font-size: 12px;
      font-weight: 700;
      color: #000;
      background: var(--accent-gold);
      border-radius: 4px;
      padding: 1px 6px;
      margin-right: 8px;
      vertical-align: baseline;
      user-select: none;
    }}

    .geez-num-badge {{
      display: inline-block;
      font-family: var(--font-am);
      font-size: 13px;
      font-weight: 700;
      color: #fff;
      background: #1f6feb;
      border-radius: 4px;
      padding: 1px 7px;
      margin-right: 8px;
      vertical-align: baseline;
      user-select: none;
    }}

    /* Liturgical Typography */
    .words-of-jesus {{
      color: var(--jesus-red);
      font-weight: 500;
    }}

    .divine-name {{
      font-variant: small-caps;
      letter-spacing: 0.6px;
      color: var(--divine-name);
    }}

    .italic-text {{
      font-style: italic;
      color: #c9d1d9;
    }}

    .footnote-ref {{
      display: inline-block;
      background: #21262d;
      color: #8b949e;
      border: 1px solid #30363d;
      border-radius: 4px;
      padding: 0 4px;
      font-size: 11px;
      font-family: var(--font-mono);
      cursor: help;
      margin: 0 2px;
      position: relative;
    }}

    .footnote-ref:hover::after {{
      content: attr(data-tooltip);
      position: absolute;
      bottom: 125%;
      left: 50%;
      transform: translateX(-50%);
      background: #1f242c;
      color: #f0f6fc;
      border: 1px solid var(--accent-gold);
      padding: 6px 10px;
      border-radius: 6px;
      font-size: 12px;
      white-space: nowrap;
      z-index: 50;
      box-shadow: 0 4px 14px rgba(0,0,0,0.6);
    }}

    .section-heading-bar {{
      background: rgba(232, 196, 107, 0.08);
      border-top: 1px solid rgba(232, 196, 107, 0.25);
      border-bottom: 1px solid rgba(232, 196, 107, 0.25);
      padding: 12px 24px;
      color: var(--accent-gold);
      font-weight: 700;
      font-size: 16px;
      font-family: var(--font-ui);
      letter-spacing: 0.3px;
    }}

    /* JSON & Search Viewers */
    .view-panel {{
      display: none;
    }}

    .view-panel.active {{
      display: block;
    }}

    .json-split-grid {{
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      margin-top: 16px;
    }}

    .json-pane {{
      background: #090d13;
      border: 1px solid var(--card-border);
      border-radius: 8px;
      padding: 16px;
      height: 680px;
      overflow-y: auto;
      font-family: var(--font-mono);
      font-size: 12px;
      line-height: 1.6;
      white-space: pre-wrap;
      word-break: break-all;
    }}

    .search-result-row {{
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 8px;
      padding: 14px 18px;
      margin-bottom: 12px;
      cursor: pointer;
      transition: all 0.15s;
    }}

    .search-result-row:hover {{
      border-color: var(--accent-blue);
      background: #1c222b;
    }}

    .code-hl-key {{ color: #79c0ff; }}
    .code-hl-str {{ color: #a5d6ff; }}
    .code-hl-num {{ color: #ffa657; }}
    .code-hl-bool {{ color: #ff7b72; }}
  </style>
</head>
<body>

<header>
  <div class="header-top">
    <div style="display: flex; align-items: center; gap: 12px;">
      <span class="badge-sinq">ስንቅ (Sinq)</span>
      <h1>English (NKJV) & Amharic (1980 EC) Side-by-Side Parallel Bible</h1>
    </div>
    <div style="display: flex; gap: 8px;">
      <button id="tabBtnParallel" class="btn-ui active" onclick="switchView('parallel')">📖 Bilingual Reader</button>
      <button id="tabBtnJson" class="btn-ui" onclick="switchView('json')">🧩 Raw JSON (Side-by-Side)</button>
      <button id="tabBtnSearch" class="btn-ui" onclick="switchView('search')">🔍 Global Search (EN & AM)</button>
    </div>
  </div>

  <div class="controls-row">
    <div class="nav-group">
      <span style="font-size: 13px; color: var(--text-muted); font-weight: 600;">BOOK:</span>
      <select id="bookSelector" onchange="onBookChanged()"></select>
    </div>

    <div class="nav-group">
      <span style="font-size: 13px; color: var(--text-muted); font-weight: 600;">CHAPTER:</span>
      <select id="chapterSelector" onchange="onChapterChanged()"></select>
    </div>

    <div class="nav-group">
      <button id="btnPrev" class="btn-ui" onclick="stepChapter(-1)">◄ Prev</button>
      <button id="btnNext" class="btn-ui" onclick="stepChapter(1)">Next ►</button>
    </div>

    <div class="nav-group" style="margin-left: auto;">
      <span style="font-size: 13px; color: var(--text-muted);">Layout:</span>
      <button id="layoutSideBySide" class="btn-ui active" onclick="setLayoutMode('side')">Dual Columns (Side-by-Side)</button>
      <button id="layoutStacked" class="btn-ui" onclick="setLayoutMode('stacked')">Interlinear (Line-by-Line)</button>
    </div>
  </div>
</header>

<main>
  <!-- VIEW 1: BILINGUAL READER -->
  <div id="viewParallel" class="view-panel active">
    <div id="bilingualHeader" class="bilingual-header">
      <div class="col-title-en">
        <span>English (NKJV)</span>
        <span id="headerEnTitle" class="tag gold">Genesis 1</span>
      </div>
      <div class="col-title-am">
        <span>አማርኛ (1980 ዓ.ም)</span>
        <span id="headerAmTitle" class="tag green">ኦሪት ዘፍጥረት ፩</span>
      </div>
    </div>

    <div id="versesContainer" class="verses-container"></div>
  </div>

  <!-- VIEW 2: JSON SIDE-BY-SIDE -->
  <div id="viewJson" class="view-panel">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
      <div style="font-size: 14px; font-weight: 600; color: var(--accent-gold);">
        Raw Chapter Payloads (Left: English NKJV | Right: Amharic 1980 EC)
      </div>
      <button class="btn-ui" onclick="copyParallelJson()">Copy Chapter JSON</button>
    </div>
    <div class="json-split-grid">
      <div id="jsonEnPane" class="json-pane"></div>
      <div id="jsonAmPane" class="json-pane"></div>
    </div>
  </div>

  <!-- VIEW 3: GLOBAL SEARCH -->
  <div id="viewSearch" class="view-panel">
    <div style="display: flex; gap: 10px; margin-bottom: 16px;">
      <input type="text" id="searchInput" class="search-input" style="flex: 1; padding: 10px 14px; font-size: 15px;" placeholder="Search in English (e.g. 'shepherd', 'in the beginning') OR Amharic (e.g. 'እረኛዬ', 'በመዠመሪያ')..." onkeydown="if(event.key==='Enter') runGlobalSearch()">
      <button class="btn-ui active" style="padding: 10px 20px;" onclick="runGlobalSearch()">Search 31,102 Verses</button>
    </div>
    <div id="searchMeta" style="font-size: 13px; color: var(--text-muted); margin-bottom: 12px;"></div>
    <div id="searchResultsList"></div>
  </div>
</main>

<script>
window.PARALLEL_DATA = {json_data_str};

let currentBookIdx = 0;
let currentChapterIdx = 0;
let currentLayout = 'side';

function init() {{
  populateBookSelect();
  loadChapter();
}}

function populateBookSelect() {{
  const sel = document.getElementById('bookSelector');
  sel.innerHTML = '';

  const otGroup = document.createElement('optgroup');
  otGroup.label = "Old Testament (ብሉይ ኪዳን - 39 Books)";
  const ntGroup = document.createElement('optgroup');
  ntGroup.label = "New Testament (ሐዲስ ኪዳን - 27 Books)";

  window.PARALLEL_DATA.books.forEach((b, idx) => {{
    const opt = document.createElement('option');
    opt.value = idx;
    opt.textContent = `${{b.name_en}} / ${{b.name_am}} (${{b.chapter_count}} chs)`;
    if (b.testament === 'OT') {{
      otGroup.appendChild(opt);
    }} else {{
      ntGroup.appendChild(opt);
    }}
  }});

  sel.appendChild(otGroup);
  sel.appendChild(ntGroup);
  sel.value = currentBookIdx;
  populateChapterSelect();
}}

function populateChapterSelect() {{
  const sel = document.getElementById('chapterSelector');
  sel.innerHTML = '';
  const book = window.PARALLEL_DATA.books[currentBookIdx];

  book.chapters.forEach((ch, idx) => {{
    const opt = document.createElement('option');
    opt.value = idx;
    if (book.key === 'ps') {{
      opt.textContent = `Psalm ${{ch.chapter}} / መዝሙር ${{toGeez(ch.am_chapter)}} (${{toGeez(ch.chapter)}})`;
    }} else {{
      opt.textContent = `Chapter ${{ch.chapter}} / ምዕራፍ ${{toGeez(ch.chapter)}}`;
    }}
    sel.appendChild(opt);
  }});
  sel.value = currentChapterIdx;
}}

function onBookChanged() {{
  currentBookIdx = parseInt(document.getElementById('bookSelector').value, 10);
  currentChapterIdx = 0;
  populateChapterSelect();
  loadChapter();
}}

function onChapterChanged() {{
  currentChapterIdx = parseInt(document.getElementById('chapterSelector').value, 10);
  loadChapter();
}}

function stepChapter(delta) {{
  const book = window.PARALLEL_DATA.books[currentBookIdx];
  const newIdx = currentChapterIdx + delta;

  if (newIdx >= 0 && newIdx < book.chapters.length) {{
    currentChapterIdx = newIdx;
    document.getElementById('chapterSelector').value = currentChapterIdx;
    loadChapter();
  }} else if (delta > 0 && currentBookIdx + 1 < window.PARALLEL_DATA.books.length) {{
    currentBookIdx++;
    currentChapterIdx = 0;
    document.getElementById('bookSelector').value = currentBookIdx;
    populateChapterSelect();
    loadChapter();
  }} else if (delta < 0 && currentBookIdx - 1 >= 0) {{
    currentBookIdx--;
    const prevBook = window.PARALLEL_DATA.books[currentBookIdx];
    currentChapterIdx = prevBook.chapters.length - 1;
    document.getElementById('bookSelector').value = currentBookIdx;
    populateChapterSelect();
    loadChapter();
  }}
}}

function loadChapter() {{
  const book = window.PARALLEL_DATA.books[currentBookIdx];
  const ch = book.chapters[currentChapterIdx];

  document.getElementById('headerEnTitle').textContent = `${{book.name_en}} ${{ch.chapter}} (${{ch.verses.length}} verses)`;
  if (book.key === 'ps') {{
    document.getElementById('headerAmTitle').textContent = `${{book.name_am}} መዝሙር ${{toGeez(ch.am_chapter)}} (LXX ፳፪)`;
  }} else {{
    document.getElementById('headerAmTitle').textContent = `${{book.name_am}} ምዕራፍ ${{toGeez(ch.chapter)}}`;
  }}

  document.getElementById('btnPrev').disabled = (currentBookIdx === 0 && currentChapterIdx === 0);
  document.getElementById('btnNext').disabled = (currentBookIdx === window.PARALLEL_DATA.books.length - 1 && currentChapterIdx === book.chapters.length - 1);

  renderParallelVerses(ch);
  renderJsonView(book, ch);
}}

function renderParallelVerses(ch) {{
  const container = document.getElementById('versesContainer');
  let html = '';

  ch.verses.forEach(v => {{
    if (v.h && v.h.length) {{
      v.h.forEach(heading => {{
        html += `<div class="section-heading-bar">${{escapeHtml(heading)}}</div>`;
      }});
    }}

    // English cell content
    let enContent = `<span class="verse-num-badge">${{v.v}}</span>`;
    if (v.vp && v.vp.length) {{
      v.vp.forEach(part => {{
        const style = part.style || 'NONE';
        const txt = part.text || '';
        if (style === 'WORDS_OF_JESUS') {{
          enContent += `<span class="words-of-jesus">${{escapeHtml(txt)}}</span>`;
        }} else if (style === 'DIVINE_NAME') {{
          enContent += `<span class="divine-name">${{escapeHtml(txt)}}</span>`;
        }} else if (style === 'ITALIC') {{
          enContent += `<span class="italic-text">${{escapeHtml(txt)}}</span>`;
        }} else if (style === 'FOOTNOTE') {{
          enContent += `<span class="footnote-ref" data-tooltip="${{escapeHtml(txt)}}">fn</span>`;
        }} else if (style === 'LINE_BREAK') {{
          enContent += `<br/>`;
        }} else {{
          enContent += escapeHtml(txt);
        }}
      }});
    }} else {{
      enContent += escapeHtml(v.en);
    }}

    // Amharic cell content
    const amContent = `<span class="geez-num-badge">${{v.g}}</span>` + escapeHtml(v.am);

    html += `<div class="parallel-row" id="verse-row-${{v.v}}">
      <div class="verse-cell-en">${{enContent}}</div>
      <div class="verse-cell-am">${{amContent}}</div>
    </div>`;
  }});

  container.innerHTML = html;
}}

function renderJsonView(book, ch) {{
  const enObj = {{
    book: book.name_en,
    key: book.key,
    chapter: ch.chapter,
    verses: ch.verses.map(v => ({{
      verse: v.v,
      text: v.en,
      headings: v.h || undefined,
      footnotes: v.fn || undefined,
      verse_parts: v.vp || undefined
    }}))
  }};

  const amObj = {{
    book: book.name_am,
    key: book.key,
    chapter: ch.am_chapter || ch.chapter,
    chapter_geez: toGeez(ch.am_chapter || ch.chapter),
    verses: ch.verses.map(v => ({{
      verse: v.v,
      geez: v.g,
      text: v.am
    }}))
  }};

  document.getElementById('jsonEnPane').innerHTML = syntaxHighlight(enObj);
  document.getElementById('jsonAmPane').innerHTML = syntaxHighlight(amObj);
}}

function setLayoutMode(mode) {{
  currentLayout = mode;
  const header = document.getElementById('bilingualHeader');
  const container = document.getElementById('versesContainer');
  const btnSide = document.getElementById('layoutSideBySide');
  const btnStack = document.getElementById('layoutStacked');

  if (mode === 'stacked') {{
    header.classList.add('stacked-mode');
    container.classList.add('stacked-mode');
    btnSide.classList.remove('active');
    btnStack.classList.add('active');
  }} else {{
    header.classList.remove('stacked-mode');
    container.classList.remove('stacked-mode');
    btnSide.classList.add('active');
    btnStack.classList.remove('active');
  }}
}}

function switchView(viewName) {{
  document.getElementById('tabBtnParallel').classList.remove('active');
  document.getElementById('tabBtnJson').classList.remove('active');
  document.getElementById('tabBtnSearch').classList.remove('active');

  document.getElementById('viewParallel').classList.remove('active');
  document.getElementById('viewJson').classList.remove('active');
  document.getElementById('viewSearch').classList.remove('active');

  if (viewName === 'parallel') {{
    document.getElementById('tabBtnParallel').classList.add('active');
    document.getElementById('viewParallel').classList.add('active');
  }} else if (viewName === 'json') {{
    document.getElementById('tabBtnJson').classList.add('active');
    document.getElementById('viewJson').classList.add('active');
  }} else if (viewName === 'search') {{
    document.getElementById('tabBtnSearch').classList.add('active');
    document.getElementById('viewSearch').classList.add('active');
  }}
}}

function toGeez(n) {{
  const DIGITS = ["", "፩", "፪", "፫", "፬", "፭", "፮", "፯", "፰", "፱"];
  const TENS = ["", "፲", "፳", "፴", "፵", "፶", "፷", "፸", "፹", "፺"];
  if (n <= 0) return String(n);
  if (n < 10) return DIGITS[n];
  if (n < 100) return TENS[Math.floor(n / 10)] + DIGITS[n % 10];
  if (n === 100) return "፻";
  if (n < 1000) {{
    const c = Math.floor(n / 100);
    const rem = n % 100;
    const prefix = (c === 1 ? "" : toGeez(c)) + "፻";
    return prefix + (rem ? toGeez(rem) : "");
  }}
  return String(n);
}}

function escapeHtml(str) {{
  return (str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}}

function syntaxHighlight(jsonObj) {{
  const str = JSON.stringify(jsonObj, null, 2);
  return str.replace(/("(\\\\u[a-zA-Z0-9]{{4}}|\\\\[^u]|[^\\\\"])*"(\\s*:)?|\\b(true|false|null)\\b|-?\\d+(?:\\.\\d*)?(?:[eE][+\\-]?\\d+)?)/g, function (match) {{
    let cls = 'code-hl-num';
    if (/^"/.test(match)) {{
      if (/:$/.test(match)) {{
        cls = 'code-hl-key';
      }} else {{
        cls = 'code-hl-str';
      }}
    }} else if (/true|false/.test(match)) {{
      cls = 'code-hl-bool';
    }} else if (/null/.test(match)) {{
      cls = 'code-hl-bool';
    }}
    return '<span class="' + cls + '">' + match + '</span>';
  }});
}}

function copyParallelJson() {{
  const book = window.PARALLEL_DATA.books[currentBookIdx];
  const ch = book.chapters[currentChapterIdx];
  navigator.clipboard.writeText(JSON.stringify(ch, null, 2)).then(() => {{
    alert("Copied bilingual chapter JSON to clipboard!");
  }});
}}

function runGlobalSearch() {{
  const q = document.getElementById('searchInput').value.trim().toLowerCase();
  if (!q) return;

  const resultsDiv = document.getElementById('searchResultsList');
  resultsDiv.innerHTML = '<div style="color: var(--text-muted);">Searching across all 31,102 verses...</div>';

  const matches = [];
  const MAX = 60;

  for (let b = 0; b < window.PARALLEL_DATA.books.length; b++) {{
    const book = window.PARALLEL_DATA.books[b];
    for (let c = 0; c < book.chapters.length; c++) {{
      const ch = book.chapters[c];
      for (let v = 0; v < ch.verses.length; v++) {{
        const verse = ch.verses[v];
        if (verse.en.toLowerCase().includes(q) || verse.am.includes(q)) {{
          matches.push({{
            bIdx: b,
            cIdx: c,
            bookEn: book.name_en,
            bookAm: book.name_am,
            chNum: ch.chapter,
            vNum: verse.v,
            geez: verse.g,
            en: verse.en,
            am: verse.am
          }});
          if (matches.length >= MAX) break;
        }}
      }}
      if (matches.length >= MAX) break;
    }}
    if (matches.length >= MAX) break;
  }}

  document.getElementById('searchMeta').textContent = `Found ${{matches.length}}${{matches.length >= MAX ? '+' : ''}} matching verses:`;

  if (matches.length === 0) {{
    resultsDiv.innerHTML = '<div style="color: var(--text-muted);">No matching verses found.</div>';
    return;
  }}

  let html = '';
  matches.forEach(m => {{
    html += `<div class="search-result-row" onclick="jumpToVerse(${{m.bIdx}}, ${{m.cIdx}}, ${{m.vNum}})">
      <div style="font-size: 14px; font-weight: 700; color: var(--accent-gold); margin-bottom: 4px;">
        ${{m.bookEn}} ${{m.chNum}}:${{m.vNum}} &bull; ${{m.bookAm}} ${{toGeez(m.chNum)}}፥${{m.geez}}
      </div>
      <div style="font-family: var(--font-en); font-size: 15px; margin-bottom: 4px;">${{escapeHtml(m.en)}}</div>
      <div style="font-family: var(--font-am); font-size: 15px; color: #58a6ff;">${{escapeHtml(m.am)}}</div>
    </div>`;
  }});

  resultsDiv.innerHTML = html;
}}

function jumpToVerse(bIdx, cIdx, vNum) {{
  currentBookIdx = bIdx;
  currentChapterIdx = cIdx;
  document.getElementById('bookSelector').value = currentBookIdx;
  populateChapterSelect();
  document.getElementById('chapterSelector').value = currentChapterIdx;
  loadChapter();

  switchView('parallel');
  setTimeout(() => {{
    const el = document.getElementById(`verse-row-${{vNum}}`);
    if (el) {{
      el.scrollIntoView({{ behavior: 'smooth', block: 'center' }});
      el.style.background = 'rgba(232, 196, 107, 0.2)';
      setTimeout(() => el.style.background = '', 2000);
    }}
  }}, 100);
}}

// Initialize
init();
</script>

</body>
</html>
"""

    print(f"Writing bilingual preview HTML to {OUTPUT_HTML_PATH}...")
    with open(OUTPUT_HTML_PATH, "w", encoding="utf-8") as f:
        f.write(html_code)

    preview_size_mb = os.path.getsize(OUTPUT_HTML_PATH) / (1024 * 1024)
    print(f"Bilingual preview generated: {preview_size_mb:.2f} MB")

if __name__ == "__main__":
    main()
