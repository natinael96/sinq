#!/usr/bin/env python3
"""
Fetch Daily Readings and Daily Patristic Quotes from catenabible.com (Coptic Edition).

Uses standard library urllib (no Playwright, no Chromium, no dependencies).
Catena Bible provides a direct JSON tRPC API for both the daily Coptic lectionary
and Patristic quotes.

Run:
    python3 tools/fetch_catena_daily.py

Output:
    catena_daily.json (contains coptic_readings, daily_quote, and metadata)
"""

import datetime
import json
import urllib.parse
import urllib.request
import sys


HEADERS = {
    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
    "Accept": "application/json",
}


def fetch_coptic_readings(date: datetime.date) -> dict:
    """Fetch Coptic Orthodox lectionary (Katameros) for the given date."""
    params = {
        "religion": "coptic",
        "month": date.month,
        "day": date.day,
        "year": date.year,
    }
    encoded = urllib.parse.quote(json.dumps(params))
    url = f"https://catenabible.com/api/catena/trpc/lectionary.day?input={encoded}"
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=15) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        return res.get("result", {}).get("data", {}).get("data", {})


def fetch_daily_quote() -> dict:
    """Fetch Catena Bible's official daily quote."""
    url = "https://catenabible.com/api/quote"
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode("utf-8"))


def fetch_patristic_quotes(limit: int = 50) -> list:
    """Fetch Desert Father / Patristic quotes list for offline bundling."""
    params = {"page": 1, "limit": limit}
    encoded = urllib.parse.quote(json.dumps(params))
    url = f"https://catenabible.com/api/catena/trpc/quotes.list?input={encoded}"
    req = urllib.request.Request(url, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            return data.get("result", {}).get("data", {}).get("results", [])
    except Exception:
        return []


def main():
    today = datetime.date.today()
    print(f"Fetching Catena Coptic readings and daily quote for {today}...")

    # 1. Coptic Daily Readings
    try:
        coptic_data = fetch_coptic_readings(today)
    except Exception as e:
        print(f"Warning: Failed to fetch Coptic lectionary: {e}", file=sys.stderr)
        coptic_data = {}

    # 2. Daily Quote
    try:
        quote_payload = fetch_daily_quote()
        quote_data = quote_payload.get("quote", {})
    except Exception as e:
        print(f"Warning: Failed to fetch daily quote: {e}", file=sys.stderr)
        quote_data = {}

    # 3. Assemble unified output
    readings = coptic_data.get("readings", {})
    verses_summary = []
    for service, sections in readings.items():
        if isinstance(sections, dict):
            for sect, items in sections.items():
                if isinstance(items, list):
                    for item in items:
                        bk = item.get("book", "")
                        ch = item.get("chapter", "")
                        sv = item.get("startVerse", "")
                        ev = item.get("endVerse", "")
                        label = f"{service.capitalize()} {sect}: {bk} {ch}:{sv}" + (f"-{ev}" if ev and ev != sv else "")
                        verses_summary.append({
                            "service": service,
                            "section": sect,
                            "reference": label,
                            "book": bk,
                            "chapter": ch,
                            "startVerse": sv,
                            "endVerse": ev,
                            "url": f"https://catenabible.com/{bk}/{ch}/{sv}",
                        })

    result = {
        "fetched_at": datetime.datetime.now().isoformat(timespec="seconds"),
        "coptic_calendar": {
            "gregorian_date": str(today),
            "coptic_date_code": coptic_data.get("coptic_date", ""),
            "coptic_local_date": coptic_data.get("local_date", ""),
        },
        "daily_quote": {
            "father": quote_data.get("father", ""),
            "quote": quote_data.get("quote", ""),
            "id": quote_data.get("id", ""),
        },
        "verses": verses_summary,
        "raw_lectionary": coptic_data,
    }

    out_file = "catena_daily.json"
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print(f"\n✓ Saved {out_file}")
    print(f"• Coptic Date: {result['coptic_calendar']['coptic_local_date']}")
    if quote_data.get("father"):
        print(f"• Daily Quote ({quote_data['father']}): \"{quote_data['quote'][:80]}...\"")
    print(f"• Coptic Readings Found: {len(verses_summary)}")
    for v in verses_summary:
        print(f"  - {v['reference']} -> {v['url']}")


if __name__ == "__main__":
    main()
