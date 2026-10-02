#!/usr/bin/env python3
"""
Build 366-day Yearly Desert Fathers Quotes JSON and HTML Preview.
Fetches canonical Apophthegmata Patrum from Catena Bible tRPC API.
"""

import json
import os
import sys
import urllib.parse
import urllib.request

FATHER_AMHARIC = {
    "Abba Abraham": "አባ አብርሃም",
    "Abba Achilles": "አባ አኪላስ",
    "Abba Agathon": "አባ አጋቶን",
    "Abba Aio": "አባ አዮ",
    "Abba Alonius": "አባ አሎኒዎስ",
    "Abba Ammoes": "አባ አሞስ",
    "Abba Ammonas": "አባ አሞናስ",
    "Abba Ammonathas": "አባ አሞናታስ",
    "Abba Amoun": "አባ አሙን",
    "Abba Anoub": "አባ አኑብ",
    "Abba Antony the Great": "አባ እንጦንስ ዓቢይ",
    "Abba Apollo": "አባ አፖሎ",
    "Abba Apollos": "አባ አፖሎስ",
    "Abba Apphy": "አባ አፊ",
    "Abba Ares": "አባ አሬስ",
    "Abba Arsenius": "አባ አርሳኒዎስ",
    "Abba Benjamin": "አባ ቢንያም",
    "Abba Bessarion": "አባ ቢሳርዮን",
    "Abba Biares": "አባ ቢያሬስ",
    "Abba Carion": "አባ ካርዮን",
    "Abba Cassian": "አባ ካስያኖስ",
    "Abba Cheremon": "አባ ቼረሞን",
    "Abba Chomai": "አባ ቾማይ",
    "Abba Copres": "አባ ኮፕሬስ",
    "Abba Cronios": "አባ ክሮኒዎስ",
    "Abba Cyrus": "አባ ቂሮስ",
    "Abba Daniel": "አባ ዳንኤል",
    "Abba Dioscorus": "አባ ዲዮስቆሮስ",
    "Abba Doulas": "አባ ዱላስ",
    "Abba Elijah": "አባ ኤልያስ",
    "Abba Ephraim": "ቅዱስ ኤፍሬም",
    "Abba Epiphanius": "አባ ኤጲፋንዮስ",
    "Abba Eucharistus": "አባ አውካሪስቶስ",
    "Abba Eulogius": "አባ አውሎጊስ",
    "Abba Euprepius": "አባ አውጵረጵዮስ",
    "Abba Evagrius": "አባ ኤቫግሪዎስ",
    "Abba Felix": "አባ ፊሊክስ",
    "Abba Gelasius": "አባ ገላስዮስ",
    "Abba Gerontius": "አባ ጌሮንቲዎስ",
    "Abba Helladius": "አባ ህላዲዎስ",
    "Abba Heraclius": "አባ ሄራክሊዎስ",
    "Abba Hierax": "አባ ኢየራክስ",
    "Abba Hyperechios": "አባ ሁፔሬኪዮስ",
    "Abba Isaac of The Cells": "አባ ይስሐቅ ዘደሴት",
    "Abba Isaac of Thebes": "አባ ይስሐቅ ዘቴቤስ",
    "Abba Isaiah": "አባ ኢሳይያስ",
    "Abba Ischyrion": "አባ እስኪርዮን",
    "Abba Isidore of Pelusium": "አባ ኢሲዶሮስ ዘጲሉሲዮን",
    "Abba Isidore of Scete": "አባ ኢሲዶሮስ ዘአስቄጥስ",
    "Abba Isidore the Priest": "አባ ኢሲዶሮስ ካህን",
    "Abba John of The Cells": "አባ ዮሐንስ ዘደሴት",
    "Abba John the Coenobite": "አባ ዮሐንስ መነኮስ",
    "Abba John the Disciple of Paul": "አባ ዮሐንስ ደቀ ጳውሎስ",
    "Abba John the Dwarf": "አባ ዮሐንስ ሐጺር",
    "Abba John the Eunuch": "አባ ዮሐንስ ጃንደረባ",
    "Abba John the Persian": "አባ ዮሐንስ ዘፋርስ",
    "Abba John the Theban": "አባ ዮሐንስ ዘቴቤስ",
    "Abba Joseph of Panepho": "አባ ዮሴፍ ዘፓኔፎ",
    "Abba Joseph of Thebes": "አባ ዮሴፍ ዘቴቤስ",
    "Abba Longinus": "አባ ሎንጊኖስ",
    "Abba Lot": "አባ ሎጥ",
    "Abba Lucius": "አባ ሉክዮስ",
    "Abba Macarius of Alexandria": "አባ መቃርስ ዘእስክንድርያ",
    "Abba Macarius the Great": "አባ መቃርስ ዓቢይ",
    "Abba Mark": "አባ ማርቆስ",
    "Abba Mark the Egyptian": "አባ ማርቆስ ዘግብጽ",
    "Abba Matoes": "አባ ማቶስ",
    "Abba Megethius": "አባ መጌቲዎስ",
    "Abba Miles": "አባ ሚሌስ",
    "Abba Mios": "አባ ሚዮስ",
    "Abba Moses": "አባ ሙሴ ፀሊም",
    "Abba Motios": "አባ ሞቲዎስ",
    "Abba Netras": "አባ ኔትራስ",
    "Abba Nicetas": "አባ ኒቄጣስ",
    "Abba Nicon": "አባ ኒቆን",
    "Abba Nil": "አባ ኒል",
    "Abba Nisteros": "አባ ኒስቴሮስ",
    "Abba Nisteros the Coenobite": "አባ ኒስቴሮስ",
    "Abba Olympius": "አባ ኦሊምፒዎስ",
    "Abba Or": "አባ ሆር",
    "Abba Orsisius": "አባ ኦርሲሲዎስ",
    "Abba Pambo": "አባ ጳምቦ",
    "Abba Paphnutius": "አባ ጳፍኑቲዎስ",
    "Abba Paul of Thebes": "አባ ጳውሎስ ዘቴቤስ",
    "Abba Paul the Barber": "አባ ጳውሎስ ላጭ",
    "Abba Paul the Great": "አባ ጳውሊ ዓቢይ",
    "Abba Paul the Simple": "አባ ጳውሎስ የዋህ",
    "Abba Peter the Pionite": "አባ ጴጥሮስ ፒዮናዊ",
    "Abba Philagrius": "አባ ፊላግሪዎስ",
    "Abba Phortas": "አባ ፎርታስ",
    "Abba Pior": "አባ ፒዮር",
    "Abba Pistamon": "አባ ፒስታሞን",
    "Abba Pityrion": "አባ ፒቲርዮን",
    "Abba Poemen": "አባ ፖይመን",
    "Abba Psenthaisios": "አባ ፕሴንታይሲዮስ",
    "Abba Romanus": "አባ ሮማኖስ",
    "Abba Rufus": "አባ ሩፎስ",
    "Abba Sarmatas": "አባ ሳርማታስ",
    "Abba Serapion": "አባ ሴራፒዮን",
    "Abba Serinos": "አባ ሴሪኖስ",
    "Abba Silvanus": "አባ ሲልቫኖስ",
    "Abba Simon": "አባ ስምዖን",
    "Abba Sisoes": "አባ ሲሶይ",
    "Abba Sopatros": "አባ ሶፓትሮስ",
    "Abba The Roman": "አባ ሮማዊ",
    "Abba Theodore of Eleutheropolis": "አባ ቴዎድሮስ",
    "Abba Theodore of Enaton": "አባ ቴዎድሮስ ዘኤናቶን",
    "Abba Theodore of Pherme": "አባ ቴዎድሮስ ዘፈርሜ",
    "Abba Theodotus": "አባ ቴዎዶጦስ",
    "Abba Theonas": "አባ ቴዎናስ",
    "Abba Timothy": "አባ ጢሞቴዎስ",
    "Abba Tithoes": "አባ ቲቶስ",
    "Abba Xanthias": "አባ ዣንቲያስ",
    "Abba Xoios": "አባ ጾዮስ",
    "Abba Zachariah": "አባ ዘካርያስ",
    "Abba Zeno": "አባ ዜኖ",
    "Amma Sarah": "እመ አብርሃ ሳራ",
    "Amma Syncletica": "እመ አብርሃ ስንክሊቲካ",
    "Amma Theodora": "እመ አብርሃ ቴዎዶራ",
    "Augustine of Hippo": "ቅዱስ አውግስጢኖስ",
    "Gregory": "ቅዱስ ጎርጎርዮስ",
    "Hilarion": "ቅዱስ ሂላርዮን",
    "Spyridon": "ቅዱስ ስፓይሪዶን",
    "Theophilus": "ቴዎፍሎስ",
}

GEEZ_NUMERALS = [
    "", "፩", "፪", "፫", "፬", "፭", "፮", "፯", "፰", "፱", "፲",
    "፲፩", "፲፪", "፲፫", "፲፬", "፲፭", "፲፮", "፲፯", "፲፰", "፲፱", "፳",
    "፳፩", "፳፪", "፳፫", "፳፬", "፳፭", "፳፮", "፳፯", "፳፰", "፳፱", "፴",
    "፴፩", "፴፪", "፴፫", "፴፬", "፴፭", "፴፮", "፴፯", "፴፰", "፴፱", "፵",
    "፵፩", "፵፪", "፵፫", "፵፬", "፵፭", "፵፮", "፵፯", "፵፰", "፵፱", "፶"
]


def to_geez(n: int) -> str:
    """Convert integer to Ge'ez numeral representation up to 999."""
    if n <= 0:
        return ""
    if n < len(GEEZ_NUMERALS) and GEEZ_NUMERALS[n]:
        return GEEZ_NUMERALS[n]
    hundreds = n // 100
    remainder = n % 100
    tens = (remainder // 10) * 10
    ones = remainder % 10

    h_str = ""
    if hundreds == 1:
        h_str = "፻"
    elif hundreds > 1:
        h_str = (GEEZ_NUMERALS[hundreds] if hundreds < len(GEEZ_NUMERALS) else str(hundreds)) + "፻"

    t_map = {10: "፲", 20: "፳", 30: "፴", 40: "፵", 50: "፶", 60: "፷", 70: "፸", 80: "፹", 90: "፺"}
    o_map = {1: "፩", 2: "፪", 3: "፫", 4: "፬", 5: "፭", 6: "፮", 7: "፯", 8: "፰", 9: "፱"}

    rem_str = t_map.get(tens, "") + o_map.get(ones, "")
    return h_str + rem_str


def fetch_all_quotes() -> list:
    print("Fetching all Desert Father quotes from Catena API...")
    headers = {"User-Agent": "Mozilla/5.0"}
    all_quotes = []
    seen = set()

    for offset in range(0, 900, 100):
        params = {"offset": offset, "limit": 100}
        url = "https://catenabible.com/api/catena/trpc/quotes.list?input=" + urllib.parse.quote(json.dumps(params))
        req = urllib.request.Request(url, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                data = json.loads(resp.read().decode("utf-8"))["result"]["data"]["results"]
                for q in data:
                    txt = q.get("quote_text", "").strip()
                    if txt and txt not in seen:
                        seen.add(txt)
                        all_quotes.append(q)
                print(f"  Offset {offset}: collected {len(all_quotes)} unique quotes")
        except Exception as e:
            print(f"Error at offset {offset}: {e}")
            break

    return all_quotes


def build_yearly_dataset(quotes: list) -> list:
    """Build multi-year corpus of all Desert Fathers sayings with sequential day and Ge'ez indexing."""
    dataset = []
    for idx, q in enumerate(quotes, start=1):
        author_en = q.get("author_name", "Desert Father").strip()
        author_am = FATHER_AMHARIC.get(author_en, author_en)
        quote_text = q.get("quote_text", "").strip()

        dataset.append({
            "day": idx,
            "day_geez": to_geez(idx),
            "author_en": author_en,
            "author_am": author_am,
            "quote": quote_text,
            "quote_id": q.get("quote_id", str(idx)),
        })

    return dataset


def generate_html_preview(yearly: list, out_path: str):
    html_content = f"""<!DOCTYPE html>
<html lang="am">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Sinq — 366-Day Desert Fathers Quotes Preview (የአበው ምክር)</title>
  <style>
    @font-face {{
      font-family: 'AbyssinicaSIL';
      src: url('../app/src/main/res/font/abyssinica_sil.ttf') format('truetype');
    }}
    :root {{
      --bg: #F4F0E8;
      --card-bg: #FFFFFF;
      --border: #DDD4C5;
      --primary: #0E3B31;
      --gold: #C49A45;
      --text: #1C2320;
      --text-muted: #6A736F;
    }}
    [data-theme="dark"] {{
      --bg: #0F1513;
      --card-bg: #151D1A;
      --border: #25332D;
      --primary: #255C50;
      --gold: #E8C46B;
      --text: #EAE6DF;
      --text-muted: #8E9894;
    }}
    * {{ box-sizing: border-box; margin: 0; padding: 0; }}
    body {{
      background: var(--bg);
      color: var(--text);
      font-family: 'AbyssinicaSIL', system-ui, sans-serif;
      padding: 24px;
      line-height: 1.6;
    }}
    .header {{
      max-width: 1000px;
      margin: 0 auto 24px;
      padding-bottom: 16px;
      border-bottom: 1px solid var(--border);
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;
    }}
    .title h1 {{
      font-size: 20px;
      color: var(--primary);
    }}
    [data-theme="dark"] .title h1 {{ color: var(--gold); }}
    .title p {{
      font-size: 13px;
      color: var(--text-muted);
    }}
    .search-box {{
      padding: 8px 14px;
      border-radius: 20px;
      border: 1px solid var(--border);
      background: var(--card-bg);
      color: var(--text);
      font-family: inherit;
      font-size: 14px;
      width: 260px;
    }}
    .theme-btn {{
      padding: 6px 14px;
      border-radius: 20px;
      border: 1px solid var(--border);
      background: var(--card-bg);
      color: var(--text);
      cursor: pointer;
      font-size: 13px;
    }}
    .stats-bar {{
      max-width: 1000px;
      margin: 0 auto 20px;
      font-size: 13px;
      color: var(--text-muted);
    }}
    .quotes-grid {{
      max-width: 1000px;
      margin: 0 auto;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 16px;
    }}
    .quote-card {{
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      position: relative;
      transition: transform 0.15s, border-color 0.15s;
    }}
    .quote-card:hover {{
      border-color: var(--gold);
      transform: translateY(-2px);
    }}
    .card-top {{
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
    }}
    .day-badge {{
      background: var(--gold);
      color: #0F1513;
      font-weight: 700;
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 6px;
    }}
    .author-name {{
      font-size: 13px;
      font-weight: 700;
      color: var(--primary);
    }}
    [data-theme="dark"] .author-name {{ color: var(--gold); }}
    .quote-body {{
      font-size: 14px;
      line-height: 1.6;
      color: var(--text);
      margin-bottom: 12px;
    }}
    .card-footer {{
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 11px;
      color: var(--text-muted);
      border-top: 1px dashed var(--border);
      padding-top: 8px;
    }}
  </style>
</head>
<body data-theme="light">
  <div class="header">
    <div class="title">
      <h1>☦ የአበው ምክር (Desert Fathers Sayings)</h1>
      <p>Apophthegmata Patrum · ዜና አበው — ዓመታዊ ዑደት (Multi-Year Daily Contemplation)</p>
    </div>
    <div style="display: flex; gap: 8px; align-items: center;">
      <input type="text" class="search-box" id="search" placeholder="ፈልግ (አባት ወይም ቃል)..." oninput="filterQuotes()">
      <button class="theme-btn" onclick="toggleTheme()">☀️ / 🌙</button>
    </div>
  </div>

  <div class="stats-bar" id="stats">
    ድምር፦ ፰፻፵፬ ምክሮች (844 Monastic Sayings · Multi-Year Continuous Cycle)
  </div>

  <div class="quotes-grid" id="grid">
"""

    for item in yearly:
        html_content += f"""
    <div class="quote-card" data-search="{item['author_am']} {item['author_en']} {item['quote']}">
      <div>
        <div class="card-top">
          <span class="day-badge">ምክር {item['day_geez']} (#{item['day']})</span>
          <span class="author-name">{item['author_am']}</span>
        </div>
        <div class="quote-body">«{item['quote']}»</div>
      </div>
      <div class="card-footer">
        <span>{item['author_en']}</span>
        <span>#{item['quote_id']}</span>
      </div>
    </div>
"""

    html_content += """
  </div>

  <script>
    function toggleTheme() {
      const b = document.body;
      b.setAttribute('data-theme', b.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
    }
    function filterQuotes() {
      const q = document.getElementById('search').value.toLowerCase();
      const cards = document.querySelectorAll('.quote-card');
      let visible = 0;
      cards.forEach(card => {
        const text = card.getAttribute('data-search').toLowerCase();
        if (text.includes(q)) {
          card.style.display = 'flex';
          visible++;
        } else {
          card.style.display = 'none';
        }
      });
      document.getElementById('stats').textContent = `የተገኙ፦ ${visible} ምክሮች`;
    }
  </script>
</body>
</html>
"""
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(html_content)
    print(f"Generated HTML preview: {out_path}")


def main():
    quotes = fetch_all_quotes()
    print(f"Total unique quotes fetched: {len(quotes)}")

    yearly = build_yearly_dataset(quotes)
    print(f"Built full dataset with {len(yearly)} sayings.")

    # 1. Output to app assets
    asset_path = "app/src/main/assets/content/daily_quotes.json"
    with open(asset_path, "w", encoding="utf-8") as f:
        json.dump(yearly, f, ensure_ascii=False, indent=2)
    print(f"Saved asset dataset: {asset_path}")

    # 2. Output to docs
    json_path = "docs/yearly_desert_fathers_quotes.json"
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(yearly, f, ensure_ascii=False, indent=2)
    print(f"Saved JSON dataset: {json_path}")

    # 3. Output HTML preview
    html_path = "docs/daily-quotes-preview.html"
    generate_html_preview(yearly, html_path)


if __name__ == "__main__":
    main()
