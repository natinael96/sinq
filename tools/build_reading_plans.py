#!/usr/bin/env python3
"""Generate the bundled ንባብ reading plans.

The plan reads what the ግጻዌ does not: the Old Testament and the deuterocanon,
in Ethiopian canonical order. The lectionary already carries 88.5% of the New
Testament and every psalm it needs, so neither is in the plan corpus.

Days are packed to a VERSE budget rather than a chapter count. EOTC chapter
divisions are wildly uneven — ሄኖክ averages 37 verses a chapter against
ዘሌዋውያን's 32 — so a chapter-count plan lurches between five-minute days and
twenty-minute ones. Chapters are never split across days.
"""
import json, os, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BIBLE = os.path.join(ROOT, "app/src/main/assets/content/bible")
OUT = os.path.join(ROOT, "app/src/main/assets/content/reading/plans.json")

# The nine broader-canon books: church order and history rather than Scripture
# read in the same sense. Available as their own track, never in the default.
BROADER = {"LAO", "OTH", "XXA", "XXB", "XXC", "XXD", "XXE", "XXF", "XXG"}


def load():
    canon = json.load(open(os.path.join(BIBLE, "canon.json"), encoding="utf-8"))
    meta = json.load(open(os.path.join(BIBLE, "am-1980/meta.json"), encoding="utf-8"))
    by_id = {b["id"]: b for b in meta["books"]}
    books = []
    for c in sorted(canon, key=lambda x: x["order"]):
        b = by_id.get(c["id"])
        if not b:                      # in canon.json but not bundled
            continue
        books.append({
            "id": c["id"], "slug": c["slug"], "testament": c["testament"],
            "order": c["order"], "chapters": b["chapters"], "verses": b["verses"],
        })
    return books, json.load(open(os.path.join(BIBLE, "am-1980/meta.json"), encoding="utf-8"))


def chapter_verses(slug, order):
    """Verse count per chapter, so packing can be honest about day length."""
    path = os.path.join(BIBLE, "am-1980/books", f"{order:02d}-{slug}.json")
    data = json.load(open(path, encoding="utf-8"))
    return [len(ch.get("verses", [])) for ch in data.get("chapters", [])]


def corpus(books):
    """OT + deuterocanon, minus Psalms, minus the broader canon."""
    out = []
    for b in books:
        if b["id"] in BROADER:
            continue
        if b["testament"] not in ("old", "deuterocanonical"):
            continue
        if b["id"] == "PSA":           # prayed in the hours; its own cycle
            continue
        out.append(b)
    return out


def units(books):
    """Flat list of (slug, chapter, verses) in canonical order."""
    u = []
    for b in books:
        counts = chapter_verses(b["slug"], b["order"])
        if len(counts) != b["chapters"]:
            print(f"  ! {b['slug']}: meta says {b['chapters']} chapters, file has {len(counts)}")
        for i, v in enumerate(counts, start=1):
            u.append((b["slug"], i, v))
    return u


def pack(u, days):
    """Greedy pack to a per-day verse budget, never splitting a chapter.

    A chapter joins the current day when it does not overshoot the budget, or
    when the day is still empty (a single chapter longer than the budget has to
    go somewhere). The budget is recomputed from what is left, so a long book
    early on does not push the whole tail into the final week.
    """
    total = sum(v for _, _, v in u)
    out, i, n = [], 0, len(u)
    for d in range(days):
        remaining_days = days - d
        left = sum(v for _, _, v in u[i:])
        if left <= 0:
            break
        budget = left / remaining_days
        day, got = [], 0
        while i < n:
            slug, ch, v = u[i]
            if day and got + v > budget * 1.35:
                break
            day.append((slug, ch))
            got += v
            i += 1
            # Leave at least one chapter for each remaining day.
            if (n - i) <= (remaining_days - 1):
                break
            if got >= budget:
                break
        out.append(day)
    # Anything left over (rounding) joins the final day rather than vanishing.
    while i < n:
        slug, ch, _ = u[i]
        out[-1].append((slug, ch))
        i += 1
    return out


def to_readings(day):
    """Collapse consecutive chapters of one book into {b, c, to}."""
    r = []
    for slug, ch in day:
        if r and r[-1]["b"] == slug and r[-1]["to"] == ch - 1:
            r[-1]["to"] = ch
        else:
            r.append({"b": slug, "c": ch, "to": ch})
    return r


def build(u, days, plan_id, title, subtitle):
    packed = pack(u, days)
    readings = [{"d": i + 1, "r": to_readings(day)} for i, day in enumerate(packed) if day]
    return {
        "id": plan_id, "title": title, "subtitle": subtitle,
        "days": len(readings), "withGitsawe": True, "readings": readings,
    }


def main():
    books, _ = load()
    core = corpus(books)
    u = units(core)
    total_ch, total_v = len(u), sum(v for _, _, v in u)
    print(f"corpus: {len(core)} books, {total_ch} chapters, {total_v} verses")

    plans = [
        build(u, 360, "annual", "ዓመታዊ ንባብ", "ግጻዌው የማያነብልዎት"),
        build(u, 180, "half", "የስድስት ወር ንባብ", "በስድስት ወር"),
    ]
    for p in plans:
        ch = sum(len(range(r["c"], r["to"] + 1)) for d in p["readings"] for r in d["r"])
        assert ch == total_ch, f"{p['id']}: {ch} chapters packed, expected {total_ch}"
        longest = max(len([1 for r in d["r"] for _ in range(r["c"], r["to"] + 1)]) for d in p["readings"])
        print(f"  {p['id']:7s} {p['days']:3d} days  {ch} ch  ~{ch / p['days']:.2f} ch/day  "
              f"~{total_v / p['days']:.0f} v/day  longest day {longest} ch")

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    json.dump({"contentVersion": 1, "plans": plans},
              open(OUT, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    print(f"wrote {OUT} ({os.path.getsize(OUT) / 1024:.0f} KB)")


if __name__ == "__main__":
    main()
