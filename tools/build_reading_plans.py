#!/usr/bin/env python3
"""Generate the bundled ንባብ reading plans.

The plan reads the whole canon, in Ethiopian canonical order: all 81 books, the
New Testament and the Psalter included. It used to read only what the ግጻዌ does
not — the Old Testament and the deuterocanon — on the grounds that the
lectionary already carries 88.5% of the New Testament. That made the plan a
supplement to the ግጻዌ rather than a way through the Bible, and someone who
finished it had still never read Matthew straight through. Overlap with the
day's ግጻዌ is the price, and it is the right one: the two are read differently.

Days are packed to a VERSE budget rather than a chapter count. EOTC chapter
divisions are wildly uneven — ሄኖክ averages 37 verses a chapter against
ዘሌዋውያን's 32 — so a chapter-count plan lurches between five-minute days and
twenty-minute ones. Chapters are never split across days.
"""
import json, os, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BIBLE = os.path.join(ROOT, "app/src/main/assets/content/bible")
OUT = os.path.join(ROOT, "app/src/main/assets/content/reading/plans.json")

# The nine broader-canon books — ሲኖዶስ, ክሌመንጦስ and the rest: church order and
# history rather than Scripture read in the same sense. They are part of the 81
# and the app bundles them, so "everything" includes them.
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
    """Everything bundled, in canonical order — the whole 81."""
    return list(books)


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


def psalter_units(books):
    """The 150 psalms, one entry each, in order."""
    psa = next((b for b in books if b["id"] == "PSA"), None)
    if not psa:
        raise SystemExit("psalms are not in the bundle")
    counts = chapter_verses(psa["slug"], psa["order"])
    return [(psa["slug"], i, v) for i, v in enumerate(counts, start=1)]


def build_psalter(u):
    """One psalm a day, which is how the Psalter is prayed through.

    Not packed to a verse budget like the other tracks. A psalm is a unit of
    prayer, not a quantity of text: መዝሙር ፻፲፰ runs to 176 verses and መዝሙር ፻፲፮
    to two, and both are a day's ዳዊት. The average day is 16 verses, about a
    minute.

    It exists because the other tracks leave the Psalter out — it is "prayed in
    the hours" — but only 77 of the 150 psalms appear whole in an hour, and the
    ግጻዌ cites the rest in ምስባክ fragments of a verse or two. 698 psalm verses
    are in no hour and in no citation, and this is the track that reaches them.
    """
    readings = [{"d": i + 1, "r": [{"b": slug, "c": ch, "to": ch}]}
                for i, (slug, ch, _) in enumerate(u)]
    return {
        "id": "psalter", "title": "የዳዊት ንባብ", "subtitle": "በየቀኑ አንድ መዝሙር",
        "days": len(readings), "withGitsawe": True,
        "versesADay": verses_a_day([[(slug, ch)] for slug, ch, _ in u], u), "readings": readings,
    }


def verses_a_day(packed, u):
    """The middle day's verse count — what the plan actually asks of a day.

    Chapters are the wrong unit to advertise a plan in: these are packed by
    verses, so the chapter count runs from one to sixteen while the reading
    stays the same length. The app shows this instead, and the minutes it
    implies.
    """
    counts = {(slug, ch): v for slug, ch, v in u}
    per = sorted(sum(counts.get(x, 0) for x in day) for day in packed if day)
    return per[len(per) // 2] if per else 0


def build(u, days, plan_id, title, subtitle):
    packed = pack(u, days)
    readings = [{"d": i + 1, "r": to_readings(day)} for i, day in enumerate(packed) if day]
    return {
        "id": plan_id, "title": title, "subtitle": subtitle,
        "days": len(readings), "withGitsawe": True,
        "versesADay": verses_a_day(packed, u), "readings": readings,
    }


def main():
    books, _ = load()
    core = corpus(books)
    u = units(core)
    total_ch, total_v = len(u), sum(v for _, _, v in u)
    print(f"corpus: {len(core)} books, {total_ch} chapters, {total_v} verses")

    plans = [
        build(u, 360, "annual", "ዓመታዊ ንባብ", "መጽሐፍ ቅዱስ በሙሉ፣ በአንድ ዓመት"),
        build(u, 180, "half", "የስድስት ወር ንባብ", "መጽሐፍ ቅዱስ በሙሉ፣ በስድስት ወር"),
        build_psalter(psalter_units(books)),
    ]
    # The psalter track reads its own corpus, so each plan is checked against
    # the one it was built from rather than against the shared total.
    psalter_ch = len(psalter_units(books))
    psalter_v = sum(v for _, _, v in psalter_units(books))
    for p in plans:
        want_ch, want_v = (psalter_ch, psalter_v) if p["id"] == "psalter" else (total_ch, total_v)
        ch = sum(len(range(r["c"], r["to"] + 1)) for d in p["readings"] for r in d["r"])
        assert ch == want_ch, f"{p['id']}: {ch} chapters packed, expected {want_ch}"
        longest = max(len([1 for r in d["r"] for _ in range(r["c"], r["to"] + 1)]) for d in p["readings"])
        print(f"  {p['id']:8s} {p['days']:3d} days  {ch} ch  ~{ch / p['days']:.2f} ch/day  "
              f"~{want_v / p['days']:.0f} v/day  longest day {longest} ch")

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    json.dump({"contentVersion": 1, "plans": plans},
              open(OUT, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    print(f"wrote {OUT} ({os.path.getsize(OUT) / 1024:.0f} KB)")


if __name__ == "__main__":
    main()
