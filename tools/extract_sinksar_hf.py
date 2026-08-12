"""Normalize the Hugging Face ስንክሳር (synaxarium) dataset into the app's format.

Source: the `Nexuss0781/synaxarium` dataset on the Hugging Face Hub — 366 rows
(ወር, ቀን, መጽሃፍ), the day's reading as Markdown. Rows are pulled with the stdlib
over the datasets-server API, so neither `datasets` nor `pyarrow` is needed:

    python tools/extract_sinksar_hf.py --months 1-13

Writes one file per Ethiopian month to app/src/main/assets/content/sinksar/,
each: { "month": N, "days": [ { "day": D, "entries": [ {title, text} ] } ] } —
the same shape as tools/extract_sinksar.py, which this superseded as the source
of the bundled ስንክሳር.

Each day becomes: the day heading, one entry per commemoration (with its አርኬ
hymn kept under it), the 📌 feast lists, and the closing 📖 scripture reading.
The fixed closing salutation ("ዘአቅረብኩ ማኅሌተ…") is dropped — the app renders its
own from SYNAXARIUM_CLOSING_STANZAS, in Ge'ez and Amharic, so bundling it here
would print it twice.

Nothing is written unless --months names the months, because the source is
uneven: ሕዳር (month 3) is not the daily synaxarium at all but a single ድርሳነ መስቀል
homily repeated across all 30 days, and a handful of other days duplicate their
neighbour. Pass --check to report those defects without writing anything.
"""
import argparse
import json
import re
import sys
import urllib.parse
import urllib.request
from collections import defaultdict
from hashlib import sha256
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app" / "src" / "main" / "assets" / "content" / "sinksar"

DATASET = "Nexuss0781/synaxarium"
ROWS_API = "https://datasets-server.huggingface.co/rows"

# Dataset's Amharic month name -> (Ethiopian month number, name used by the app).
MONTHS = {
    "መስከረም": (1, "Meskerem"), "ጥቅምት": (2, "Tikimt"), "ሕዳር": (3, "Hidar"),
    "ታኅሣሥ": (4, "Tahsas"), "ጥር": (5, "Tir"), "የካቲት": (6, "Yekatit"),
    "መጋቢት": (7, "Megabit"), "ሚያዝያ": (8, "Miazia"), "ግንቦት": (9, "Ginbot"),
    "ሰኔ": (10, "Sene"), "ሐምሌ": (11, "Hamle"), "ነሐሴ": (12, "Nehasse"),
    "ጳጉሜ": (13, "Pagume"),
}
NAMES = {num: name for num, name in MONTHS.values()}

SEP = re.compile(r"^[፠*\s]+$")                       # ፠፠፠… block separator
CHAPTER = re.compile(r"^\*{0,2}Chapter\s+\d+\*{0,2}$")   # scraper artefact
# "ስንክሳር ዘወርኀ ግንቦት ፫" / "ዘወርኃ ህዳር" — the day's own heading line.
DAY_HEADING = re.compile(r"^(ስንክሳር\s+)?ዘወር[ኀኅሀኃ]")
# Every commemoration opens with "በዚች(ም) ቀን/ዕለት".
MARKER = re.compile(r"በዚች(ም)?\s*(ቀን|ዕለት|ዕለተ)")
ARKE = "አርኬ"                  # the hymn label the renderer sets apart
ENDERS = "።፡፤፥"                # Ge'ez sentence enders

# The source repeats the fixed closing salutation at the end of every day. The
# app renders its own (SYNAXARIUM_CLOSING_STANZAS), in both Ge'ez and Amharic,
# so the whole salutation is dropped here rather than bundled twice. Matched by
# the opening words of each of its stanzas, Ge'ez and Amharic alike, because the
# day does not always reach them in the same order.
CLOSING = "ዘአቅረብኩ"
CLOSING_OPENERS = (
    CLOSING,                        # stanza 1, Ge'ez
    "ነቢያት ቅዱሳን ወሐዋርያት",              # stanza 2, Ge'ez
    "ለዘጸሐፎ በክርታስ",                   # stanza 3, Ge'ez
    "ጌታ ሆይ",                        # stanza 1, Amharic
    "እናንተም ቅዱሳን ነቢያት",               # stanza 2, Amharic
    "ይህን መጽሐፍ ወረቀት",                 # stanza 3, Amharic
    "ክርስቶስ ተንሥአ",                    # the resurrection coda that follows
)

# A feast-list item: "፩.ቅዱስ ያሶን ሐዋርያ" / "1) …". Anything else ends the list.
LIST_ITEM = re.compile(r"^[፩-፼0-9]+\s*[.)]")
# A scripture quote closes with its reference — "(ሉቃስ ፲ ፥ ፲፯)".
SCRIPTURE_REF = re.compile(r"\(([^()]{2,40})\)\s*$")

# Trailing per-day boilerplate: the closing coda the app renders itself
# (SYNAXARIUM_CLOSING_CODA) and the scribe's colophon repeated on every day.
BOILER = ("ክርስቶስ ተንሥአ", "ይህን መጽሐፍ ወረቀት", "ጌታ ሆይ ከብልጽግናቸው", "እናንተም ቅዱሳን ነቢያት")


def fetch_rows() -> list[dict]:
    """Every row of the dataset, via the datasets-server rows API."""
    rows: list[dict] = []
    offset = 0
    while True:
        q = urllib.parse.urlencode({
            "dataset": DATASET, "config": "default", "split": "train",
            "offset": offset, "length": 100,
        })
        with urllib.request.urlopen(f"{ROWS_API}?{q}", timeout=120) as r:
            payload = json.load(r)
        batch = payload.get("rows", [])
        if not batch:
            break
        for item in batch:
            if item.get("truncated_cells"):
                raise SystemExit(f"row {item['row_idx']} came back truncated")
            rows.append(item["row"])
        offset += len(batch)
        print(f"  fetched {len(rows)}/{payload.get('num_rows_total')}", flush=True)
        if offset >= payload.get("num_rows_total", 0):
            break
    return rows


def strip_markup(s: str) -> str:
    """Drop Markdown emphasis and its debris; leave the Ge'ez untouched."""
    s = s.replace(" ", " ").replace("​", "")
    # Zero-width and bidi formatting marks: invisible, but they trail the closing
    # bracket of a scripture reference and would defeat an end-of-text anchor.
    s = re.sub("[\u200b-\u200f\u202a-\u202e\u2066-\u2069\ufeff]", "", s)
    s = re.sub(r"\*+", "", s)                       # **bold** and stray *
    s = re.sub(r"<\s*/?\s*b\s*[<>]", " ", s)        # <b> markup debris
    return re.sub(r"[ \t]+", " ", s).strip()


def paragraphs(md: str) -> list[str]:
    """Markdown -> cleaned paragraphs, preamble and rule lines dropped."""
    out = []
    for raw in re.split(r"\n\s*\n", md):
        lines = []
        for line in raw.split("\n"):
            line = strip_markup(line)
            if not line or line == "---" or CHAPTER.match(line):
                continue
            lines.append(line)
        if lines:
            out.append("\n".join(lines))
    return out


def opens_commemoration(p: str) -> bool:
    """True when paragraph [p] starts a new commemoration.

    One opens with "በዚች(ም) ቀን/ዕለት" — either outright, or after the opening
    invocation ("አንድ አምላክ በሆነ…") or a "ዳግመኛም". Any other text before the
    marker means this is mid-narrative, not a new commemoration.
    """
    m = MARKER.search(p[:90])
    if not m:
        return False
    head = p[:m.start()].strip(" ፡።፤፥")
    return not head or head.startswith(("ዳግመኛም", "ደግሞ", "አንድ አምላክ"))


def split_title(block: list[str]) -> tuple[str, list[str]]:
    """The block's first sentence titles the commemoration; the rest is body."""
    first = block[0]
    idx = next((i for i, ch in enumerate(first) if ch in ENDERS), -1)
    if idx == -1 or idx > 200:
        # The source forgot its sentence punctuation. Take a short opening
        # phrase as the title but keep the paragraph whole, so no prose is lost.
        head = first[:120]
        cut = head.rfind(" ")
        return (head[:cut] if cut > 40 else head).strip(), list(block)
    rest = first[idx + 1:].strip()
    return first[:idx + 1].strip(), ([rest] if rest else []) + list(block[1:])


def convert_day(md: str) -> list[dict]:
    """One day's Markdown -> the app's list of {title, text} commemorations."""
    entries: list[dict] = []
    body: list[list[str]] = []          # a list of paragraphs per commemoration
    feasts: list[tuple[str, list[str]]] = []
    scripture: list[dict] = []
    pending: list[str] = []         # post-feast lines awaiting their reference
    current_feast: tuple[str, list[str]] | None = None
    day_heading = ""
    closing = False

    for p in paragraphs(md):
        level = len(m.group(1)) if (m := re.match(r"^(#+)\s*", p)) else 0
        text = re.sub(r"^#+\s*", "", p).strip() if level else p

        if level >= 2:                  # "## … ቀን የሚከበሩ ዓመታዊ የቅዱሳን በዓላት"
            closing = False             # a feast heading resumes real content
            current_feast = (text, [])
            feasts.append(current_feast)
            continue
        # The salutation runs to the end of the day; it also ends any feast list.
        if text.startswith(CLOSING_OPENERS):
            closing = True
            current_feast = None
            continue
        if closing or SEP.match(text) or text.startswith(BOILER):
            continue
        if current_feast is not None:
            # The list runs until the first paragraph that is not an item; that
            # paragraph (scripture, usually) is content and falls through.
            if LIST_ITEM.match(text.split("\n")[0]):
                current_feast[1].append(text)
                continue
            current_feast = None
        if feasts:
            # Past the feast lists, what remains is the day's scripture reading:
            # a lead-in, the quote, and a closing line carrying the reference.
            if m := SCRIPTURE_REF.search(text):
                quote = pending + [text[:m.start()].strip()]
                scripture.append({"title": "📖 " + m.group(1).strip(),
                                  "text": "\n".join(x for x in quote if x)})
                pending = []
            else:
                pending.append(text)
            continue
        if DAY_HEADING.match(text) and len(text) < 80:
            day_heading = day_heading or text
            continue
        if text.startswith(ARKE):
            # The አርኬ hymn closes the commemoration before it. Keep the label on
            # its own line so the renderer sets the verse apart.
            rest = text[len(ARKE):].strip(" ፡።፤፥\n")
            chunk = ARKE + ("\n" + rest if rest else "")
            if body:
                body[-1].append(chunk)
            else:
                body.append([chunk])
            continue
        if opens_commemoration(text) or not body:
            body.append([text])
        else:
            body[-1].append(text)

    for block in body:
        title, rest = split_title(block)
        entries.append({"title": title, "text": "\n".join(rest).strip()})

    # The day heading titles the opening entry, whose own first sentence is the
    # invocation and belongs in the body — matching the existing bundled months.
    if day_heading:
        if entries:
            head = [entries[0]["title"], entries[0]["text"]]
            entries[0] = {"title": day_heading,
                          "text": "\n".join(x for x in head if x).strip()}
        else:
            entries.append({"title": day_heading, "text": ""})

    for name, lines in feasts:
        lines = [x for x in lines if x]
        if lines:
            entries.append({"title": "📌 " + name, "text": "\n".join(lines).strip()})
    entries.extend(scripture)

    return [e for e in entries if e["title"] or e["text"]]


def report_duplicates(rows: list[dict]) -> None:
    """Flag days whose source text is byte-identical to another day's."""
    seen = defaultdict(list)
    for r in rows:
        seen[sha256(r["መጽሃፍ"].encode()).hexdigest()].append(
            (MONTHS[r["ወር"]][0], r["ወር"], int(r["ቀን"])))
    groups = sorted((v for v in seen.values() if len(v) > 1), key=len, reverse=True)
    dupes = sum(len(g) for g in groups)
    print(f"\n{len(rows) - dupes + len(groups)} distinct of {len(rows)} days; "
          f"{len(groups)} duplicate groups")
    for g in groups:
        days = ", ".join(str(d) for _, _, d in g)
        print(f"  month {g[0][0]:>2} {g[0][1]:<7} {len(g):>2} days share one text: {days}")


def parse_months(spec: str) -> list[int]:
    out: set[int] = set()
    for part in spec.split(","):
        part = part.strip()
        if not part:
            continue
        if "-" in part:
            a, b = part.split("-", 1)
            out.update(range(int(a), int(b) + 1))
        else:
            out.add(int(part))
    bad = [m for m in out if not 1 <= m <= 13]
    if bad:
        raise SystemExit(f"months out of range 1-13: {bad}")
    return sorted(out)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--months", default="",
                    help="Ethiopian months to write, e.g. '5-11' or '5,9,11'")
    ap.add_argument("--check", action="store_true",
                    help="report source defects and per-month totals, write nothing")
    ap.add_argument("--rows", type=Path,
                    help="use a saved rows.json instead of fetching from the Hub")
    ap.add_argument("--out", type=Path, default=OUT, help=f"output dir (default {OUT})")
    args = ap.parse_args()

    if not args.months and not args.check:
        ap.error("pass --months (nothing is written by default) or --check")

    if args.rows:
        rows = json.loads(args.rows.read_text(encoding="utf-8"))
    else:
        print(f"fetching {DATASET} …")
        rows = fetch_rows()

    by_month: dict[int, list[dict]] = defaultdict(list)
    for r in rows:
        num, _ = MONTHS[r["ወር"]]
        by_month[num].append({"day": int(r["ቀን"]),
                              "entries": convert_day(r["መጽሃፍ"])})

    wanted = parse_months(args.months) if args.months else sorted(by_month)
    args.out.mkdir(parents=True, exist_ok=True)

    print(f"\n{'':>2} {'month':<10} {'days':>4} {'entries':>8} {'chars':>9}  ")
    written = 0
    for num in sorted(by_month):
        days = sorted(by_month[num], key=lambda d: d["day"])
        entries = sum(len(d["entries"]) for d in days)
        chars = sum(len(e["text"]) for d in days for e in d["entries"])
        mark = ""
        if num in wanted and not args.check:
            (args.out / f"{num}.json").write_text(
                json.dumps({"month": num, "days": days}, ensure_ascii=False,
                           separators=(",", ":")), encoding="utf-8")
            written += 1
            mark = "-> written"
        print(f"{num:>2} {NAMES[num]:<10} {len(days):>4} {entries:>8} "
              f"{chars:>9,}  {mark}")

    report_duplicates(rows)
    print(f"\n{written} month file(s) written to {args.out}"
          if written else "\nnothing written")
    return 0


if __name__ == "__main__":
    sys.exit(main())
