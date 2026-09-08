#!/usr/bin/env python3
"""Build the ሥርዓተ ማኅሌት corpus from the scanned chant books and the ግጻዌ.

Three sources, merged:

  sources/zema/ሥርዓተ ማኅሌት ዘዓበይት በዓላት.json
      The spine. 95 orders over all thirteen months, each headed with its date.
      The ግጻዌ's ማኅሌት stops after ሚያዝያ; this reaches ጳጉሜን.

  sources/tsige/*አቋቋም*.json
      ዘመነ ጽጌ, one order per date its Sundays can fall on. The season floats, so
      the ግጻዌ could only give it as "fourth week"; these give ጥቅምት ፲፰, and the
      app's own calendar knows which applies this year.

  sources/gitsawe/mahlets.json
      Only the orders the scanned book does not carry — six of its thirty-seven,
      measured, not assumed. Each keeps its source.

Never edited by hand; corrections live below as asserted swaps.
"""
import hashlib, json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ZEMA = ROOT / "sources/zema"
TSIGE = ROOT / "sources/tsige"
GITSAWE = ROOT / "sources/gitsawe/mahlets.json"
OUT = ROOT / "app/src/main/assets/content/mahlet"

CONTENT_VERSION = 1

MONTHS = ["መስከረም", "ጥቅምት", "ኅዳር", "ታኅሣሥ", "ጥር", "የካቲት", "መጋቢት",
          "ሚያዝያ", "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜን"]
MONTH_ALIASES = {"ጳጉሜን": ("ጳጒሜን", "ጳጉሜ"), "ታኅሣሥ": ("ታኅሳስ", "ታኅሣስ", "ታህሳስ")}

VIGIL, MAHLET = "vigil", "mahlet"

# "ሥርዓተ ዋዜማ ዘጥቅምት አቡነ ገብረ መንፈስ ቅዱስ ጥቅምት ፬" — the kind, then the feast, then
# the date. The leading digit is a stray page number on the first heading only.
ORDER_HEAD = re.compile(r"^\s*\d*\s*ሥርዓተ\s+(ዋዜማ|ማኅሌት)\s+(.+)$")
MONTH_HEAD = re.compile(r"ዘወር[ኃኀ]\s")

# "አመ ፲ወ፩ ለጥቅምት (ጥቅምት ፲፩ እሑድ ሲውል)" — the ጽጌ orders are keyed by the date whose
# falling on a Sunday appoints them. The ወ is the joiner in a compound numeral
# (፲ወ፩ is eleven) and sits outside the ፩-፼ block, so it has to be admitted here
# or every date past ten is read as no date at all.
TSIGE_HEAD = re.compile(r"^አመ\s+([፩-፼ወ]+)\s*ሁ?\s+ለ(\S+)")

GEEZ_VALUE = {"፩": 1, "፪": 2, "፫": 3, "፬": 4, "፭": 5, "፮": 6, "፯": 7, "፰": 8,
              "፱": 9, "፲": 10, "፳": 20, "፴": 30, "፵": 40, "፶": 50, "፷": 60,
              "፸": 70, "፹": 80, "፺": 90, "፻": 100}

FOLD_SERIES = [(0x1220, 0x1230), (0x1210, 0x1200), (0x1280, 0x1200),
               (0x12B8, 0x1200), (0x12D0, 0x12A0), (0x1340, 0x1338)]

# Part names too long or too rare to be caught by frequency, and one the scan
# writes as a parenthetical. Asserted, so a re-scan that drops them is reported.
EXTRA_PART_NAMES = ["(ሥርዓተ ነግሥ )", "ዓዲ (ወይም)", "በ፭ ለእግዚአብሔር ምድር በምልዓ"]

# A part name never runs longer than this; past it the block is a verse that
# happens to open with a name.
PART_NAME_MAX = 45


def fold(s):
    out = []
    for c in s:
        cp = ord(c)
        for start, target in FOLD_SERIES:
            if start <= cp <= start + 7:
                c = chr(target + cp - start)
                break
        out.append(c)
    return re.sub(r"[\s()፡።፣፤፥]+", "", "".join(out))


def geez_int(s):
    """The ወ joining a compound numeral carries no value of its own."""
    total = run = 0
    s = s.replace("ወ", "")
    for c in s:
        v = GEEZ_VALUE.get(c, 0)
        if v == 100:
            run = (run or 1) * 100
            total += run
            run = 0
        else:
            run += v
    return total + run


def month_number(name):
    for i, m in enumerate(MONTHS, 1):
        if name == m or name in MONTH_ALIASES.get(m, ()):
            return i
    return None


def order_id(*parts):
    return hashlib.sha1("|".join(parts).encode("utf-8")).hexdigest()[:10]


def load(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def part_vocabulary():
    """Every name a part of a ማኅሌት goes by, from both books.

    The scanned book alternates a part's name with its verse, but not strictly
    enough to split on length: a third of the transitions repeat, because some
    verses are short and some names are long. So the split is made on a
    vocabulary — the ግጻዌ's own part names, plus the names the scanned book uses
    often enough to be names rather than text.
    """
    vocab = {fold(p["key"]) for order in gitsawe_orders() for p in order["detail"]}
    counts = {}
    for chapter in load(ZEMA / "ሥርዓተ ማኅሌት ዘዓበይት በዓላት.json")["chapters"]:
        for b in chapter["blocks"]:
            t = (b.get("text") or "").strip()
            if not t or ORDER_HEAD.match(t) or MONTH_HEAD.search(t) or len(t) > 40:
                continue
            counts[fold(t)] = counts.get(fold(t), 0) + 1
    vocab |= {k for k, n in counts.items() if n >= 3}
    for name in EXTRA_PART_NAMES:
        if fold(name) not in vocab:
            sys.exit(f"part name no longer in the scan: {name!r}")
        vocab.add(fold(name))
    return vocab


_gitsawe = None


def gitsawe_orders():
    global _gitsawe
    if _gitsawe is None:
        found = []

        def walk(o):
            if isinstance(o, dict):
                if isinstance(o.get("detail"), list) and o.get("title"):
                    found.append(o)
                for v in o.values():
                    walk(v)
            elif isinstance(o, list):
                for v in o:
                    walk(v)

        walk(load(GITSAWE))
        _gitsawe = found
    return _gitsawe


def split_parts(blocks, vocab):
    """Blocks into (name, verse) pairs. A verse with no name keeps an empty one."""
    parts, current = [], None
    for text in blocks:
        if fold(text) in vocab and len(text) <= PART_NAME_MAX:
            current = {"key": text.strip("() "), "verse": ""}
            parts.append(current)
        elif current is not None and not current["verse"]:
            current["verse"] = text
        elif current is not None:
            current["verse"] += "\n" + text
        else:
            # 2.6% of verses open an order before any name is given. They are
            # kept, unnamed, rather than dropped — the text is the point.
            current = {"key": "", "verse": text}
            parts.append(current)
    return [p for p in parts if p["verse"]]


def parse_date(tail):
    """The month and day a heading ends on, if it names one."""
    m = re.search(r"(\S+)\s+([፩-፼]+)\s*$", tail)
    if not m:
        return None, None, tail
    num = month_number(m.group(1))
    if num is None:
        return None, None, tail
    return num, geez_int(m.group(2)), tail[: m.start()].strip()


def build_spine(vocab, report):
    orders = []
    for chapter in load(ZEMA / "ሥርዓተ ማኅሌት ዘዓበይት በዓላት.json")["chapters"]:
        month = None
        head = None
        buffer = []

        def flush():
            if head is None:
                return
            parts = split_parts(buffer, vocab)
            if not parts:
                report["empty"] += 1
                return
            kind, feast, mo, day = head
            orders.append({
                "id": order_id("zema", kind, feast, str(mo), str(day)),
                "kind": kind,
                "feast": feast,
                "month": mo if mo else month,
                "day": day,
                "source": None,
                "parts": parts,
            })

        for b in chapter["blocks"]:
            t = (b.get("text") or "").strip()
            if not t:
                continue
            # "ሥርዓተ ማኅሌት ዘወርኃ ጥቅምት" is the month's own heading, and it matches
            # ORDER_HEAD too, so it has to be caught first or every month opens
            # with an order that has no parts under it.
            if MONTH_HEAD.search(t):
                num = month_number(t.split()[-1])
                if num:
                    month = num
                continue
            m = ORDER_HEAD.match(t)
            if m:
                flush()
                buffer = []
                kind = VIGIL if m.group(1) == "ዋዜማ" else MAHLET
                mo, day, feast = parse_date(m.group(2).strip())
                feast = re.sub(r"^ዘ", "", feast).strip()
                head = (kind, feast, mo, day)
                continue
            buffer.append(t)
        flush()
    return orders


def build_tsige(vocab, report):
    orders = []
    for path in sorted(TSIGE.glob("*አቋቋም*.json")):
        for chapter in load(path)["chapters"]:
            title = (chapter.get("title") or "").strip()
            m = TSIGE_HEAD.match(title)
            if not m:
                # Each file opens with the season's general order, undated. It
                # is the one to fall back on when no date matches, so it is kept
                # rather than skipped along with the empty trailing chapter.
                if not title or not chapter.get("blocks"):
                    continue
                blocks = [(b.get("text") or "").strip()
                          for b in chapter["blocks"]
                          if (b.get("text") or "").strip()
                          and (b.get("text") or "").strip() != title]
                parts = split_parts(blocks, vocab)
                if parts:
                    orders.append({
                        "id": order_id("tsige-base", title),
                        "kind": MAHLET, "feast": title,
                        "month": None, "day": None, "season": "tsige",
                        "source": "ማኅሌተ ጽጌ አቋቋም", "parts": parts,
                    })
                continue
            day, month = geez_int(m.group(1)), month_number(m.group(2))
            if month is None:
                report["unmapped"].append(title)
                continue
            # The chapter repeats its own title as its first block; kept, it
            # became an unnamed part whose whole text was the heading above it.
            blocks = [(b.get("text") or "").strip()
                      for b in chapter["blocks"]
                      if (b.get("text") or "").strip()
                      and (b.get("text") or "").strip() != title]
            parts = split_parts(blocks, vocab)
            if not parts:
                report["empty"] += 1
                continue
            orders.append({
                "id": order_id("tsige", str(month), str(day)),
                "kind": MAHLET,
                "feast": title,
                "month": month,
                "day": day,
                "season": "tsige",
                # This order is appointed when this date falls on a Sunday.
                "whenSunday": True,
                "source": "ማኅሌተ ጽጌ አቋቋም",
                "parts": parts,
            })
    return orders


def build_missing(spine, report):
    """The ግጻዌ's orders the scanned book does not carry.

    Measured rather than assumed: an order counts as carried when at least a
    quarter of its parts are findable in the scanned book's text.
    """
    def norm(s):
        return re.sub(r"[\s።፡፣፤፥.]+", "", s or "")

    haystack = norm(" ".join(p["verse"] for o in spine for p in o["parts"]))
    kept = []
    for order in gitsawe_orders():
        hit = total = 0
        for p in order["detail"]:
            n = norm(p.get("verse", ""))
            if len(n) < 25:
                continue
            total += 1
            mid = len(n) // 2
            if any(q and q in haystack for q in (n[:22], n[mid - 11:mid + 11], n[-22:])):
                hit += 1
        share = hit / total if total else 1.0
        if share >= 0.25:
            continue
        title = order["title"]
        kind = VIGIL if "ዋዜማ" in title else MAHLET
        kept.append({
            "id": order_id("gitsawe", title),
            "kind": kind,
            "feast": re.sub(r"^ሥርዓተ\s+(ዋዜማ|ማኅሌት)\s+ዘ?", "", title).strip(),
            "month": None,
            "day": None,
            "source": "ግጻዌ",
            "parts": [{"key": p["key"], "verse": p["verse"]} for p in order["detail"]],
        })
        report["from_gitsawe"].append((title, round(share * 100)))
    return kept


def main():
    report = {"empty": 0, "unmapped": [], "from_gitsawe": []}
    vocab = part_vocabulary()

    spine = build_spine(vocab, report)
    tsige = build_tsige(vocab, report)
    missing = build_missing(spine, report)
    orders = spine + tsige + missing

    seen = {}
    for o in orders:
        if o["id"] in seen:
            sys.exit(f"id collision: {o['feast']!r} and {seen[o['id']]!r}")
        seen[o["id"]] = o["feast"]

    if report["unmapped"]:
        sys.exit("ጽጌ orders whose month could not be read: " + "; ".join(report["unmapped"]))

    OUT.mkdir(parents=True, exist_ok=True)
    for stale in OUT.glob("*.json"):
        stale.unlink()

    by_month = {}
    for o in orders:
        by_month.setdefault(o.get("month") or 0, []).append(o)

    index_months = []
    for month, items in sorted(by_month.items()):
        items.sort(key=lambda o: (o.get("day") or 99, o["kind"] != VIGIL))
        (OUT / f"m{month}.json").write_text(
            json.dumps({"month": month, "orders": items},
                       ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        index_months.append({
            "month": month,
            "name": MONTHS[month - 1] if month else "",
            # Only the fields an order actually has: a null in the index has to
            # be decoded into a non-null Kotlin default, and cannot be.
            "orders": [
                {k: o[k] for k in ("id", "kind", "feast", "day", "season", "source")
                 if o.get(k) is not None}
                | {"parts": len(o["parts"])}
                | ({"whenSunday": True} if o.get("whenSunday") else {})
                for o in items
            ],
        })

    (OUT / "index.json").write_text(
        json.dumps({"contentVersion": CONTENT_VERSION, "months": index_months},
                   ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    parts = sum(len(o["parts"]) for o in orders)
    chars = sum(len(p["verse"]) for o in orders for p in o["parts"])
    size = sum(f.stat().st_size for f in OUT.glob("*.json"))
    print("spine (scanned book): %d orders" % len(spine))
    print("ዘመነ ጽጌ (dated):       %d orders" % len(tsige))
    print("from the ግጻዌ:         %d orders" % len(missing))
    for t, s in report["from_gitsawe"]:
        print("    %-56s %d%% carried by the scan" % (t[:54], s))
    print("\n%d orders, %d parts, %d chars, %.2f MB in %d files"
          % (len(orders), parts, chars, size / 1e6, len(list(OUT.glob('*.json')))))
    if report["empty"]:
        print("  (%d headings had no parts under them and were dropped)" % report["empty"])


if __name__ == "__main__":
    main()
