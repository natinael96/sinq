#!/usr/bin/env python3
"""Regenerate the 13 authoritative month files from the licensed master.

Each file carries the book metadata and its season, so it stands alone without
needing gitsawe-master.json alongside it.
"""
import json, os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
D = ROOT / 'content/gitsawe'
OUT = D / 'months'
os.makedirs(OUT, exist_ok=True)

SLUG = {'መስከረም':'meskerem','ጥቅምት':'tikimt','ኅዳር':'hidar','ታኅሣሥ':'tahsas',
        'ጥር':'tir','የካቲት':'yekatit','መጋቢት':'megabit','ሚያዝያ':'miyazya',
        'ግንቦት':'ginbot','ሰኔ':'sene','ሐምሌ':'hamle','ነሐሴ':'nehase','ጳጉሜን':'paguemen'}
GV = {'፩':1,'፪':2,'፫':3,'፬':4,'፭':5,'፮':6,'፯':7,'፰':8,'፱':9,'፲':10,'፳':20,'፴':30}
def g2i(s): return sum(GV.get(c,0) for c in s)

ABOUT = {
  "what_this_is":
    "One month of the ግጻዌ (lectionary) from መጽሐፈ ግጻዌ ወመዝሙር ከነምልክቱ — the readings "
    "appointed for each day of the Ethiopian Orthodox Tewahedo liturgical year. "
    "Transcribed from page scans; there was no text layer, so every page was read visually.",

  "how_to_find_a_day":
    "days[] is sorted by day_number, which is a Ge'ez numeral (፩=1 … ፴=30). "
    "Use the geez_numerals map below to convert. Each day carries its commemoration "
    "and up to three services.",

  "the_three_services": {
    "ዘነግህ":  "matins — the morning office",
    "ዘቅዳሴ":  "the Divine Liturgy; the fullest block, carrying the epistles and Acts",
    "ዘሠርክ":  "vespers — the evening office",
    "note":   "A day may legitimately have fewer than three. Absence was recorded, not invented."
  },

  "reading_types": {
    "ምስባክ":            "the psalm verses chanted before the gospel; `verses` holds the Ge'ez text",
    "ወንጌል":            "the gospel reading; `incipit` is its opening words",
    "epistles_and_acts": "Pauline epistle, catholic epistle, and Acts — `reading_type` names which",
    "ቅዳሴ":             "the anaphora appointed for that day (a name, not a reading)",
    "ምስባክ_ዓዲ / ወንጌል_ዓዲ": "a second ምስባክ or gospel introduced in the book by ዓዲ ('again')"
  },

  "citation_format": {
    "ም·":     "ምዕራፍ — chapter",
    "ቍ·":     "ቍጥር — verse",
    "ፍ፡ም፡":   "ፍጻሜ ምዕራፍ — 'to the end of the chapter'",
    "example": "'ም· ፰ ቍ· ፲፱ – ፳፯' = chapter 8, verses 19–27",
    "for ምስባክ": "the psalm number comes FIRST, before ቍ· — e.g. '፻፲፰ ቍ· ፹፮ – ፹፯' is Ps 118:86-87"
  },

  "read_this_before_trusting_a_citation": [
    "Everything is transcribed AS PRINTED. Nothing was normalised or corrected — "
    "malformed citations, missing ቍ·, descending ranges and duplicated marks are in the book.",
    "The incipit is the reliable key. It is always legible and identifies the reading "
    "unambiguously. The Ge'ez numerals are the fragile part — at scan resolution "
    "፰/፳/፷ share one form, as do ፴/፵ and ፮/፯.",
    "This edition's verse numbering runs 1–2 LOWER than the Greek/LXX numbering in many "
    "places. A one-verse discrepancy against a modern Bible is usually the book's own "
    "convention, not an error.",
    "Some citations are genuinely impossible as printed (a verse beyond the chapter's "
    "length). Those are the book's misprints; check_citations.py in the parent directory "
    "enumerates them."
  ],

  "provenance_fields": {
    "source_scan_pages": "which scanned page(s) this day was read from",
    "scan_pages / printed_pages": "the month's range; printed_page = scan_page − 3",
    "review_notes": "present on some days where a reading was uncertain or the print was odd"
  },

  "geez_numerals": {"፩":1,"፪":2,"፫":3,"፬":4,"፭":5,"፮":6,"፯":7,"፰":8,"፱":9,
                    "፲":10,"፳":20,"፴":30,"፵":40,"፶":50,"፷":60,"፸":70,"፹":80,"፺":90,"፻":100,
                    "note":"compound: ፲፬=14, ፳፭=25; ፻ multiplies what precedes it, so ፪፻=200"}
}

master = json.loads((D / 'gitsawe-master.json').read_text(encoding='utf-8'))
book = dict(master['book'])
part1 = [p for p in master['parts'] if p.get('part') == 1][0]

rows = []
for season in part1['seasons']:
    for m in season['months']:
        idx = m['index']
        days = sorted(m['days'], key=lambda d: g2i(d['day_number']))
        doc = {
            'README': ABOUT,
            'book': book,
            'part': {'part': 1, 'title': part1['title']},
            'season': season['season'],
            'month': m['month'],
            'month_index': idx,
            'scan_pages': m['scan_pages'],
            'printed_pages': m['printed_pages'],
            'day_count': m['day_count'],
            'days_extracted': len(days),
            'status': m['status'],
            'days': days,
        }
        name = f"{idx:02d}-{SLUG[m['month']]}.json"
        with open(OUT / name, 'w', encoding='utf-8') as f:
            json.dump(doc, f, ensure_ascii=False, indent=2); f.write('\n')
        nums = [g2i(d['day_number']) for d in days]
        missing = [i for i in range(1, m['day_count']+1) if i not in nums]
        thin = [d['day_number'] for d in days if len(d.get('services') or {}) < 3]
        rows.append((name, m['month'], season['season'], len(days), m['day_count'],
                     missing, thin, os.path.getsize(OUT / name)))

print(f"{'file':22} {'month':8} {'season':12} days   size    issues")
for name, mo, se, n, tot, missing, thin, size in rows:
    issues = []
    if missing: issues.append(f'missing {missing}')
    if thin: issues.append(f'thin {thin}')
    print(f"  {name:20} {mo:8} {se:12} {n:>2}/{tot:<2} {size/1024:>6.0f}K  {'; '.join(issues) or 'clean'}")
print(f"\nwrote {len(rows)} files to months/")
