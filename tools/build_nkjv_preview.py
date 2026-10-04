#!/usr/bin/env python3
"""
Generate a complete, fully-wired preview HTML embedding the entire NKJV Bible
for both nkjv_bible.json and nkjv_bible_compact.json across all 66 books and 1,189 chapters.
"""

import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FULL_JSON_PATH = os.path.join(ROOT, "nkjv_bible.json")
COMPACT_JSON_PATH = os.path.join(ROOT, "nkjv_bible_compact.json")
OUTPUT_HTML_PATH = os.path.join(ROOT, "preview_nkjv.html")

def main():
    print("Loading full and compact datasets...")
    with open(FULL_JSON_PATH, "r", encoding="utf-8") as f:
        full_data = json.load(f)

    with open(COMPACT_JSON_PATH, "r", encoding="utf-8") as f:
        compact_data = json.load(f)

    full_size_mb = os.path.getsize(FULL_JSON_PATH) / (1024 * 1024)
    compact_size_mb = os.path.getsize(COMPACT_JSON_PATH) / (1024 * 1024)

    print("Minifying JSON data for embedded script...")
    full_min = json.dumps(full_data, separators=(',', ':'), ensure_ascii=False)
    compact_min = json.dumps(compact_data, separators=(',', ':'), ensure_ascii=False)

    html_template = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Complete NKJV Bible — Both JSONs & Interactive Reader</title>
  <style>
    :root {
      --bg: #0d1117;
      --card-bg: #161b22;
      --card-border: #30363d;
      --text: #e6edf3;
      --text-muted: #8b949e;
      --accent-gold: #d29922;
      --accent-green: #3fb950;
      --accent-blue: #58a6ff;
      --jesus-red: #ff7b72;
      --divine-name: #f2cc60;
      --tag-bg: #21262d;
      --code-bg: #090d13;
      --font-serif: "Cardo", "Georgia", "Times New Roman", serif;
      --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      --font-mono: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
    }

    * { box-sizing: border-box; margin: 0; padding: 0; }

    body {
      background-color: var(--bg);
      color: var(--text);
      font-family: var(--font-sans);
      line-height: 1.5;
    }

    header {
      background: linear-gradient(180deg, #161f28 0%, #111822 100%);
      border-bottom: 1px solid var(--card-border);
      padding: 16px 28px;
      position: sticky;
      top: 0;
      z-index: 100;
    }

    .header-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }

    .logo-group {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .badge-sinq {
      background: #0e3b31;
      color: #e8c46b;
      padding: 4px 10px;
      border-radius: 6px;
      font-weight: 700;
      font-size: 13px;
      border: 1px solid rgba(232, 196, 107, 0.3);
    }

    h1 {
      font-size: 20px;
      font-weight: 700;
      color: #fff;
    }

    .nav-tabs {
      display: flex;
      gap: 8px;
      margin-top: 14px;
      border-bottom: 1px solid var(--card-border);
      padding-bottom: 6px;
    }

    .nav-btn {
      background: transparent;
      border: none;
      color: var(--text-muted);
      padding: 8px 16px;
      font-size: 14px;
      font-weight: 600;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .nav-btn:hover {
      color: var(--text);
      background: rgba(255, 255, 255, 0.05);
    }

    .nav-btn.active {
      color: #fff;
      background: var(--card-border);
    }

    main {
      max-width: 1440px;
      margin: 0 auto;
      padding: 24px 28px 64px 28px;
    }

    /* Universal Chapter Navigator Bar */
    .master-nav-bar {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      padding: 14px 20px;
      border-radius: 8px;
      margin-bottom: 20px;
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 14px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.25);
    }

    .nav-group {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .nav-label {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    select, input[type="text"] {
      background: #21262d;
      color: var(--text);
      border: 1px solid var(--card-border);
      padding: 8px 14px;
      border-radius: 6px;
      font-size: 14px;
      outline: none;
      cursor: pointer;
    }

    select:focus, input[type="text"]:focus {
      border-color: var(--accent-blue);
    }

    .btn-step {
      background: #21262d;
      border: 1px solid var(--card-border);
      color: var(--text);
      padding: 8px 12px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;
      font-weight: 600;
      transition: all 0.15s;
    }

    .btn-step:hover:not(:disabled) {
      background: #30363d;
      border-color: #8b949e;
    }

    .btn-step:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    .chapter-meta {
      margin-left: auto;
      font-size: 13px;
      color: var(--text-muted);
      display: flex;
      align-items: center;
      gap: 10px;
    }

    /* Metric Cards */
    .metric-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 14px;
      margin-bottom: 24px;
    }

    .metric-card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 8px;
      padding: 16px 18px;
    }

    .metric-title {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.8px;
      color: var(--text-muted);
      margin-bottom: 4px;
    }

    .metric-val {
      font-size: 24px;
      font-weight: 700;
      color: #fff;
    }

    .metric-sub {
      font-size: 12px;
      color: var(--text-muted);
      margin-top: 2px;
    }

    .section-panel {
      display: none;
    }

    .section-panel.active {
      display: block;
    }

    /* Split viewer */
    .split-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
    }

    @media (max-width: 960px) {
      .split-grid { grid-template-columns: 1fr; }
    }

    .pane-card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      height: 720px;
    }

    .pane-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 18px;
      border-bottom: 1px solid var(--card-border);
      background: #11161d;
      border-radius: 8px 8px 0 0;
    }

    .pane-title {
      font-size: 14px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .tag {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-family: var(--font-mono);
      font-weight: 600;
      background: var(--tag-bg);
      border: 1px solid var(--card-border);
    }

    .tag.green { color: #3fb950; border-color: rgba(63, 185, 80, 0.3); }
    .tag.gold { color: #d29922; border-color: rgba(210, 153, 34, 0.3); }
    .tag.blue { color: #58a6ff; border-color: rgba(88, 166, 255, 0.3); }

    .code-container {
      background: var(--code-bg);
      padding: 16px;
      overflow-y: auto;
      flex-grow: 1;
      font-family: var(--font-mono);
      font-size: 12px;
      line-height: 1.6;
      color: #e6edf3;
      white-space: pre-wrap;
      word-break: break-all;
    }

    /* Reader view */
    .reader-card {
      background: #0f141c;
      border: 1px solid var(--card-border);
      border-radius: 8px;
      padding: 40px 52px;
      max-width: 920px;
      margin: 0 auto;
      font-family: var(--font-serif);
      font-size: 19px;
      line-height: 1.85;
      box-shadow: 0 8px 32px rgba(0,0,0,0.5);
    }

    @media (max-width: 640px) {
      .reader-card { padding: 24px 20px; font-size: 17px; }
    }

    .reader-title {
      font-family: var(--font-sans);
      text-align: center;
      font-size: 26px;
      font-weight: 700;
      color: #fff;
      margin-bottom: 24px;
    }

    .reader-heading {
      font-family: var(--font-sans);
      font-size: 18px;
      font-weight: 700;
      color: #e8c46b;
      margin: 32px 0 14px 0;
      border-bottom: 1px solid rgba(232, 196, 107, 0.25);
      padding-bottom: 6px;
    }

    .verse-num {
      font-size: 13px;
      font-family: var(--font-sans);
      color: var(--accent-gold);
      font-weight: 700;
      vertical-align: super;
      margin-right: 4px;
      user-select: none;
    }

    .words-of-jesus {
      color: #ff7b72;
      font-weight: 500;
    }

    .divine-name {
      font-variant: small-caps;
      letter-spacing: 0.6px;
      color: #f2cc60;
    }

    .italic-text {
      font-style: italic;
      color: #c9d1d9;
    }

    .footnote-ref {
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
    }

    .footnote-ref:hover::after {
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
    }

    /* Search Tab */
    .search-bar {
      display: flex;
      gap: 10px;
      margin-bottom: 20px;
    }

    .search-input {
      flex: 1;
      padding: 12px 16px;
      font-size: 15px;
    }

    .search-results {
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-height: 650px;
      overflow-y: auto;
    }

    .search-item {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      padding: 14px 18px;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.15s;
    }

    .search-item:hover {
      border-color: var(--accent-blue);
      background: #1c212a;
    }

    .search-ref {
      font-weight: 700;
      color: var(--accent-gold);
      font-size: 14px;
      margin-bottom: 4px;
    }

    .search-text {
      font-size: 14px;
      color: var(--text);
      line-height: 1.5;
    }

    .code-hl-key { color: #79c0ff; }
    .code-hl-str { color: #a5d6ff; }
    .code-hl-num { color: #ffa657; }
    .code-hl-bool { color: #ff7b72; }
  </style>
</head>
<body>

<header>
  <div class="header-top">
    <div class="logo-group">
      <span class="badge-sinq">Sinq Liturgical</span>
      <h1>NKJV Bible Complete (Both JSONs Fully Wired)</h1>
    </div>
    <div style="font-size: 13px; color: var(--text-muted);">
      Source: <a href="https://catenabible.com" target="_blank" style="color: var(--accent-blue); text-decoration: none;">catenabible.com</a>
    </div>
  </div>

  <div class="nav-tabs">
    <button class="nav-btn active" onclick="switchTab('reader')">Live Typography Reader</button>
    <button class="nav-btn" onclick="switchTab('split')">Side-by-Side JSON Inspector</button>
    <button class="nav-btn" onclick="switchTab('search')">Search All 31,102 Verses</button>
    <button class="nav-btn" onclick="switchTab('overview')">Overview & Metrics</button>
  </div>
</header>

<main>
  <!-- Universal Book & Chapter Navigator -->
  <div class="master-nav-bar">
    <div class="nav-group">
      <span class="nav-label">Book:</span>
      <select id="bookSelector" onchange="onBookChanged()"></select>
    </div>

    <div class="nav-group">
      <span class="nav-label">Chapter:</span>
      <select id="chapterSelector" onchange="onChapterChanged()"></select>
    </div>

    <div class="nav-group">
      <button id="btnPrev" class="btn-step" onclick="stepChapter(-1)">◄ Prev</button>
      <button id="btnNext" class="btn-step" onclick="stepChapter(1)">Next ►</button>
    </div>

    <div class="chapter-meta">
      <span id="currentMetaTag" class="tag blue">Genesis 1</span>
      <span id="verseCountBadge" class="tag green">31 Verses</span>
    </div>
  </div>

  <!-- TAB 1: READER -->
  <section id="tab-reader" class="section-panel active">
    <div style="text-align: center; margin-bottom: 16px; font-size: 13px; color: var(--text-muted);">
      Typography Legend: 
      <span class="words-of-jesus" style="margin-left: 6px;">● Words of Jesus (Red Letter)</span> &bull; 
      <span class="divine-name" style="margin-left: 6px;">● Lord (Divine Name)</span> &bull; 
      <span class="italic-text" style="margin-left: 6px;">● Supplied Words (Italics)</span> &bull; 
      <span style="color: #8b949e; margin-left: 6px;">● [fn] Footnotes</span>
    </div>
    <div id="readerContainer" class="reader-card"></div>
  </section>

  <!-- TAB 2: SPLIT JSON INSPECTOR -->
  <section id="tab-split" class="section-panel">
    <div class="split-grid">
      <!-- Full JSON Pane -->
      <div class="pane-card">
        <div class="pane-header">
          <div class="pane-title">
            <span class="tag gold">Rich Typography</span>
            <span>nkjv_bible.json</span>
          </div>
          <button class="btn-step" onclick="copyPane('codeFull')">Copy Chapter JSON</button>
        </div>
        <div id="codeFull" class="code-container"></div>
      </div>

      <!-- Compact JSON Pane -->
      <div class="pane-card">
        <div class="pane-header">
          <div class="pane-title">
            <span class="tag green">Clean Text</span>
            <span>nkjv_bible_compact.json</span>
          </div>
          <button class="btn-step" onclick="copyPane('codeCompact')">Copy Chapter JSON</button>
        </div>
        <div id="codeCompact" class="code-container"></div>
      </div>
    </div>
  </section>

  <!-- TAB 3: GLOBAL SEARCH -->
  <section id="tab-search" class="section-panel">
    <div class="search-bar">
      <input type="text" id="searchInput" class="search-input" placeholder="Search keyword (e.g. 'shepherd', 'light', 'love') or reference (e.g. 'John 3:16', 'Ps 23:1')..." onkeydown="if(event.key==='Enter') executeSearch()">
      <button class="btn-step" style="padding: 12px 24px; font-size: 14px;" onclick="executeSearch()">Search</button>
    </div>
    <div id="searchResultsCount" style="font-size: 13px; color: var(--text-muted); margin-bottom: 12px;"></div>
    <div id="searchResults" class="search-results"></div>
  </section>

  <!-- TAB 4: OVERVIEW -->
  <section id="tab-overview" class="section-panel">
    <div class="metric-grid">
      <div class="metric-card">
        <div class="metric-title">Total Books</div>
        <div class="metric-val">66</div>
        <div class="metric-sub">39 OT &bull; 27 NT (All Canon)</div>
      </div>
      <div class="metric-card">
        <div class="metric-title">Total Chapters</div>
        <div class="metric-val">1,189</div>
        <div class="metric-sub">100% Embedded in this page</div>
      </div>
      <div class="metric-card">
        <div class="metric-title">Total Verses</div>
        <div class="metric-val">31,102</div>
        <div class="metric-sub">Full Scripture text</div>
      </div>
      <div class="metric-card">
        <div class="metric-title">Rich JSON</div>
        <div class="metric-val">__FULL_SIZE_MB__ MB</div>
        <div class="metric-sub">nkjv_bible.json</div>
      </div>
      <div class="metric-card">
        <div class="metric-title">Compact JSON</div>
        <div class="metric-val">__COMPACT_SIZE_MB__ MB</div>
        <div class="metric-sub">nkjv_bible_compact.json</div>
      </div>
    </div>

    <div style="background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 8px; padding: 20px;">
      <h3 style="margin-bottom: 12px; color: var(--accent-gold);">Python Quick Example</h3>
      <div class="code-container" style="max-height: 240px; border-radius: 6px;">import json

# Load the clean compact file for lightning-fast search
with open("nkjv_bible_compact.json") as f:
    bible = json.load(f)

print(f"Loaded {bible['total_books']} books, {bible['total_verses']} verses.")

# Access Genesis 1:1
gen1_1 = bible["books"][0]["chapters"][0]["verses"][0]
print(gen1_1["text"])
# Output: In the beginning God created the heavens and the earth.</div>
    </div>
  </section>
</main>

<!-- Embedded Complete Datasets -->
<script>
window.FULL_BIBLE = __FULL_DATA_JSON__;
window.COMPACT_BIBLE = __COMPACT_DATA_JSON__;

let currentBookIndex = 0;
let currentChapterIndex = 0;

function init() {
  populateBookSelector();
  loadCurrentChapter();
}

function populateBookSelector() {
  const sel = document.getElementById('bookSelector');
  sel.innerHTML = '';

  const otGroup = document.createElement('optgroup');
  otGroup.label = "Old Testament (39 Books)";
  const ntGroup = document.createElement('optgroup');
  ntGroup.label = "New Testament (27 Books)";

  window.FULL_BIBLE.books.forEach((b, idx) => {
    const opt = document.createElement('option');
    opt.value = idx;
    opt.textContent = `${b.name} (${b.chapter_count} chs)`;
    if (b.testament === 'OT') {
      otGroup.appendChild(opt);
    } else {
      ntGroup.appendChild(opt);
    }
  });

  sel.appendChild(otGroup);
  sel.appendChild(ntGroup);
  sel.value = currentBookIndex;
  populateChapterSelector();
}

function populateChapterSelector() {
  const sel = document.getElementById('chapterSelector');
  sel.innerHTML = '';
  const book = window.FULL_BIBLE.books[currentBookIndex];

  book.chapters.forEach((ch, idx) => {
    const opt = document.createElement('option');
    opt.value = idx;
    opt.textContent = `Chapter ${ch.chapter}`;
    sel.appendChild(opt);
  });

  sel.value = currentChapterIndex;
}

function onBookChanged() {
  currentBookIndex = parseInt(document.getElementById('bookSelector').value, 10);
  currentChapterIndex = 0;
  populateChapterSelector();
  loadCurrentChapter();
}

function onChapterChanged() {
  currentChapterIndex = parseInt(document.getElementById('chapterSelector').value, 10);
  loadCurrentChapter();
}

function stepChapter(delta) {
  const book = window.FULL_BIBLE.books[currentBookIndex];
  const newCh = currentChapterIndex + delta;

  if (newCh >= 0 && newCh < book.chapters.length) {
    currentChapterIndex = newCh;
    document.getElementById('chapterSelector').value = currentChapterIndex;
    loadCurrentChapter();
  } else if (delta > 0 && currentBookIndex + 1 < window.FULL_BIBLE.books.length) {
    currentBookIndex++;
    currentChapterIndex = 0;
    document.getElementById('bookSelector').value = currentBookIndex;
    populateChapterSelector();
    loadCurrentChapter();
  } else if (delta < 0 && currentBookIndex - 1 >= 0) {
    currentBookIndex--;
    const prevBook = window.FULL_BIBLE.books[currentBookIndex];
    currentChapterIndex = prevBook.chapters.length - 1;
    document.getElementById('bookSelector').value = currentBookIndex;
    populateChapterSelector();
    loadCurrentChapter();
  }
}

function loadCurrentChapter() {
  const fullBook = window.FULL_BIBLE.books[currentBookIndex];
  const fullCh = fullBook.chapters[currentChapterIndex];
  const compBook = window.COMPACT_BIBLE.books[currentBookIndex];
  const compCh = compBook.chapters[currentChapterIndex];

  document.getElementById('currentMetaTag').textContent = `${fullBook.name} ${fullCh.chapter}`;
  document.getElementById('verseCountBadge').textContent = `${fullCh.verses.length} Verses`;

  // Update Prev / Next button states
  document.getElementById('btnPrev').disabled = (currentBookIndex === 0 && currentChapterIndex === 0);
  document.getElementById('btnNext').disabled = (currentBookIndex === window.FULL_BIBLE.books.length - 1 && currentChapterIndex === fullBook.chapters.length - 1);

  renderReader(fullBook, fullCh);
  renderSplitCode(fullBook, fullCh, compBook, compCh);
}

function renderReader(book, ch) {
  const container = document.getElementById('readerContainer');
  let html = `<div class="reader-title">${book.name} ${ch.chapter}</div>`;

  ch.verses.forEach(v => {
    if (v.headings && v.headings.length) {
      v.headings.forEach(h => {
        html += `<div class="reader-heading">${escapeHtml(h)}</div>`;
      });
    }

    html += `<span class="verse-num">${v.verse}</span>`;

    if (v.verse_parts && v.verse_parts.length) {
      v.verse_parts.forEach(part => {
        const style = part.style || 'NONE';
        const txt = part.text || '';
        if (style === 'WORDS_OF_JESUS') {
          html += `<span class="words-of-jesus">${escapeHtml(txt)}</span>`;
        } else if (style === 'DIVINE_NAME') {
          html += `<span class="divine-name">${escapeHtml(txt)}</span>`;
        } else if (style === 'ITALIC') {
          html += `<span class="italic-text">${escapeHtml(txt)}</span>`;
        } else if (style === 'FOOTNOTE') {
          html += `<span class="footnote-ref" data-tooltip="${escapeHtml(txt)}">fn</span>`;
        } else if (style === 'LINE_BREAK') {
          html += `<br/>`;
        } else {
          html += escapeHtml(txt);
        }
      });
    } else {
      html += escapeHtml(v.text);
    }
    html += ' ';
  });

  container.innerHTML = html;
}

function renderSplitCode(fullBook, fullCh, compBook, compCh) {
  const fullPayload = {
    book: fullBook.name,
    key: fullBook.key,
    chapter: fullCh.chapter,
    verse_count: fullCh.verses.length,
    verses: fullCh.verses
  };

  const compPayload = {
    book: compBook.name,
    key: compBook.key,
    chapter: compCh.chapter,
    verse_count: compCh.verses.length,
    verses: compCh.verses
  };

  document.getElementById('codeFull').innerHTML = syntaxHighlight(fullPayload);
  document.getElementById('codeCompact').innerHTML = syntaxHighlight(compPayload);
}

function syntaxHighlight(jsonObj) {
  const str = JSON.stringify(jsonObj, null, 2);
  return str.replace(/("(\\\\u[a-zA-Z0-9]{4}|\\\\[^u]|[^\\\\"])*"(\\s*:)?|\\b(true|false|null)\\b|-?\\d+(?:\\.\\d*)?(?:[eE][+\\-]?\\d+)?)/g, function (match) {
    let cls = 'code-hl-num';
    if (/^"/.test(match)) {
      if (/:$/.test(match)) {
        cls = 'code-hl-key';
      } else {
        cls = 'code-hl-str';
      }
    } else if (/true|false/.test(match)) {
      cls = 'code-hl-bool';
    } else if (/null/.test(match)) {
      cls = 'code-hl-bool';
    }
    return '<span class="' + cls + '">' + match + '</span>';
  });
}

function escapeHtml(str) {
  return (str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function switchTab(tabId) {
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.section-panel').forEach(p => p.classList.remove('active'));

  event.target.classList.add('active');
  document.getElementById('tab-' + tabId).classList.add('active');
}

function copyPane(elementId) {
  const text = document.getElementById(elementId).innerText;
  navigator.clipboard.writeText(text).then(() => {
    alert("Copied chapter JSON to clipboard!");
  });
}

// Global search across all 31,102 verses
function executeSearch() {
  const query = document.getElementById('searchInput').value.trim().toLowerCase();
  if (!query) return;

  const resultsDiv = document.getElementById('searchResults');
  resultsDiv.innerHTML = '<div style="color: var(--text-muted); font-size: 14px;">Searching...</div>';

  const matches = [];
  const MAX_MATCHES = 100;

  for (let bIdx = 0; bIdx < window.COMPACT_BIBLE.books.length; bIdx++) {
    const book = window.COMPACT_BIBLE.books[bIdx];
    for (let cIdx = 0; cIdx < book.chapters.length; cIdx++) {
      const ch = book.chapters[cIdx];
      for (let vIdx = 0; vIdx < ch.verses.length; vIdx++) {
        const v = ch.verses[vIdx];
        if (v.text.toLowerCase().includes(query)) {
          matches.push({
            bookIndex: bIdx,
            chapterIndex: cIdx,
            bookName: book.name,
            chapter: ch.chapter,
            verse: v.verse,
            text: v.text
          });
          if (matches.length >= MAX_MATCHES) break;
        }
      }
      if (matches.length >= MAX_MATCHES) break;
    }
    if (matches.length >= MAX_MATCHES) break;
  }

  document.getElementById('searchResultsCount').textContent = `Found ${matches.length}${matches.length >= MAX_MATCHES ? '+' : ''} matching verses for "${query}":`;

  if (matches.length === 0) {
    resultsDiv.innerHTML = '<div style="color: var(--text-muted); font-size: 14px;">No verses found matching that query.</div>';
    return;
  }

  let html = '';
  matches.forEach(m => {
    // Highlight matched term
    const re = new RegExp(`(${escapeRegex(query)})`, 'gi');
    const highlighted = escapeHtml(m.text).replace(re, '<mark style="background: #e8c46b; color: #000; border-radius: 2px; padding: 0 2px;">$1</mark>');

    html += `<div class="search-item" onclick="jumpToVerse(${m.bookIndex}, ${m.chapterIndex})">
      <div class="search-ref">${m.bookName} ${m.chapter}:${m.verse}</div>
      <div class="search-text">${highlighted}</div>
    </div>`;
  });

  resultsDiv.innerHTML = html;
}

function escapeRegex(str) {
  return str.replace(/[-\\/\\\\^$*+?.()|[\\]{}]/g, '\\\\$&');
}

function jumpToVerse(bIdx, cIdx) {
  currentBookIndex = bIdx;
  currentChapterIndex = cIdx;
  document.getElementById('bookSelector').value = currentBookIndex;
  populateChapterSelector();
  document.getElementById('chapterSelector').value = currentChapterIndex;
  loadCurrentChapter();

  // Switch to reader view
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.section-panel').forEach(p => p.classList.remove('active'));
  document.querySelector('.nav-btn').classList.add('active');
  document.getElementById('tab-reader').classList.add('active');
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Start
init();
</script>

</body>
</html>
"""

    print("Replacing placeholders...")
    html_output = html_template.replace("__FULL_SIZE_MB__", f"{full_size_mb:.2f}")
    html_output = html_output.replace("__COMPACT_SIZE_MB__", f"{compact_size_mb:.2f}")
    html_output = html_output.replace("__FULL_DATA_JSON__", full_min)
    html_output = html_output.replace("__COMPACT_DATA_JSON__", compact_min)

    print(f"Writing complete HTML to {OUTPUT_HTML_PATH}...")
    with open(OUTPUT_HTML_PATH, "w", encoding="utf-8") as f:
        f.write(html_output)

    final_size_mb = os.path.getsize(OUTPUT_HTML_PATH) / (1024 * 1024)
    print(f"Done! {OUTPUT_HTML_PATH} created successfully ({final_size_mb:.2f} MB).")

if __name__ == "__main__":
    main()
