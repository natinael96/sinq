#!/usr/bin/env python3
"""Build the bundled ስንክሳር from the scanned editions in sources/sinksar/.

Two editions, thirteen months each, 366 days. The scans already carry the
structure the old pipeline had to guess at: the አርኬ is its own unnumbered
block, the narrative paragraphs are numbered, the day's feast lists are typed,
and the day's reading closes the file. So this writes that structure out and
the reader styles by field instead of by regex.

Shape per day:
    header    the ስንክሳር ዘወርኀ … / አመ … ለ… line
    entries   one per commemoration: numbered paragraphs, closed by its አርኬ
    feasts    the annual saints named for the day
    monthly   the ወርኀዊ በዓላት
    reading   the day's scripture, its citation folded in as one piece

Never edited by hand; corrections live below as asserted swaps.
"""
import hashlib, json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "sources/sinksar"
OUT = ROOT / "app/src/main/assets/content/sinksar"

CONTENT_VERSION = 2

# The Ethiopian year, in order, with the spellings the scans actually use: the
# files are named by month rather than numbered, and two of them differ from the
# app's own spelling — ጳጒሜን with the labiovelar ጒ, and ታኅሣሥ under the heading
# ዘወርኃ rather than ዘወርኀ. Matched on the month name alone for that reason.
MONTHS = ["መስከረም", "ጥቅምት", "ኅዳር", "ታኅሣሥ", "ጥር", "የካቲት", "መጋቢት",
          "ሚያዝያ", "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜን"]
MONTH_ALIASES = {"ጳጉሜን": ("ጳጒሜን", "ጳጉሜ", "ጳጒሜ")}
DAYS_IN = {13: 6}


def month_file(directory, name):
    """The scan for one month, matched on its name rather than the whole title."""
    wanted = {name, *MONTH_ALIASES.get(name, ())}
    for path in sorted(directory.glob("*.json")):
        if path.stem.split()[-1] in wanted:
            return path
    return None

EDITIONS = [("am", "አማርኛ"), ("ge", "ግእዝ")]

# "1 ፩ " — the scan writes each paragraph's number twice, once in Arabic and
# once in Ge'ez. Across both editions all 18,764 of them agree, so the Arabic is
# pure duplication and only the Ge'ez is kept. assert_numbers_agree() proves it
# on every run rather than trusting that measurement.
NUMBER = re.compile(r"^(\d+)\s+([፩-፼]+)\s+")

# Editorial dingbats and stray markup the scan carried over. Five blocks open
# with a bare variation selector, which is invisible but stopped the ሰላም from
# matching and turned four hymns into unnumbered prose; one opens with a stray
# angle bracket. Ethiopic (U+1200–U+137F) sits outside every range here.
JUNK = re.compile("[\u2190-\u21FF\u2600-\u27BF\u2B00-\u2BFF\uFE00-\uFE0F"
                  "\U0001F000-\U0001FAFF<>]")

# An አርኬ is the unnumbered block that closes a commemoration, and it always
# opens with the salutation. In the old pipeline this had to be inferred from
# prose formulas; here it is simply the shape of the block.
ARKE = re.compile(r"^ሰላ[ምመ]\s")

# The day's own heading. The Amharic writes it one way; the Ge'ez writes it four
# — አመ, ኣመ with the other አ, ወአመ, and the clipped አም — so it is matched on the
# shape rather than on one spelling, and only as a short opening block.
DAY_HEAD = re.compile(r"^(?:ስንክሳር\s+ዘወር[ኀኃ]\b|ወ?[አኣ][መም]\s)")
DAY_HEAD_MAX = 40
ANNUAL_HEAD = re.compile(r"የሚከበሩ.*በዓላት")
MONTHLY_HEAD = re.compile(r"^ወር[ኀኃ]ዊ\s+በዓላት")

# A citation closing the day's reading: "(፩ኛ ቆሮ. ፲፥፲፬-፲፰)".
CITATION = re.compile(r"^\((.+)\)[።፡\s]*$")

# Days whose paragraph numbering is not a clean ፩…N, and why. Left as they are
# rather than silently renumbered — the numbers are the book's, not ours — and
# listed here so a fourth one appearing is reported instead of absorbed.
KNOWN_NUMBERING_EXCEPTIONS = {
    ("am", 3, 26): "the first paragraph lost its number in the scan, so it opens at ፪",
    ("am", 5, 3): "the first paragraph lost its number in the scan, so it opens at ፪",
    ("am", 11, 21): "paragraph ፳፮ is a dingbat and no text — the scan lost it",
}

GEEZ_VALUE = {"፩": 1, "፪": 2, "፫": 3, "፬": 4, "፭": 5, "፮": 6, "፯": 7, "፰": 8,
              "፱": 9, "፲": 10, "፳": 20, "፴": 30, "፵": 40, "፶": 50, "፷": 60,
              "፸": 70, "፹": 80, "፺": 90, "፻": 100, "፼": 10000}


def geez_numeral(n):
    """1–30, which is all a day number ever is."""
    if n <= 0:
        return ""
    tens, ones = divmod(n, 10)
    return ("፲፳፴"[tens - 1] if tens else "") + ("፩፪፫፬፭፮፯፰፱"[ones - 1] if ones else "")


def geez_int(s):
    total = run = 0
    for c in s:
        v = GEEZ_VALUE.get(c, 0)
        if v == 100:
            run = (run or 1) * 100
            total += run
            run = 0
        else:
            run += v
    return total + run


def entry_id(text):
    """Content-addressed, so re-splitting the day does not move a bookmark.

    The old ids were the entry's index within the day, which meant any change to
    how entries are cut renamed every entry after the first. This hashes the
    opening of the entry instead: the same commemoration keeps the same id
    across re-scans, and a bookmark on it survives.
    """
    return hashlib.sha1(re.sub(r"\s+", "", text)[:80].encode("utf-8")).hexdigest()[:8]


def split_number(text):
    m = NUMBER.match(text)
    if not m:
        return None, text
    return m.group(2), text[m.end():]


def parse_day(blocks, edition, month, day, agree, junk, synthesised):
    header, entries, feasts, monthly, reading = "", [], [], [], None
    current, arke = [], None
    # Whether any narrative has arrived since the hymn now in hand was opened.
    # Emptiness of `current` cannot answer that: it is cleared by close_entry(),
    # so a second stanza looked like a commemoration with no body.
    narrative_since_arke = False
    section = "body"          # body -> annual -> monthly -> reading
    reading_parts, citation = [], None

    def close_entry():
        nonlocal current, arke, narrative_since_arke
        if current or arke:
            first = current[0]["text"] if current else (arke or "")
            entries.append({
                "id": entry_id(first),
                "paragraphs": current,
                **({"arke": arke} if arke else {}),
            })
        current, arke = [], None
        narrative_since_arke = False

    for b in blocks:
        text, dropped = JUNK.subn("", b.get("text") or "")
        text = text.strip()
        junk[0] += dropped
        if not text:
            continue
        kind = b.get("type", "paragraph")

        if not header and not entries and not current and \
                len(text) <= DAY_HEAD_MAX and DAY_HEAD.match(text):
            header = text
            continue

        if kind == "numbered_paragraph":
            (monthly if section == "monthly" else feasts).append(text)
            continue

        if ANNUAL_HEAD.search(text) and section == "body":
            close_entry()
            section = "annual"
            continue
        if MONTHLY_HEAD.match(text):
            section = "monthly"
            continue

        if section in ("annual", "monthly"):
            # Anything that is not a list item after the lists is the reading.
            section = "reading"

        if section == "reading":
            m = CITATION.match(text)
            if m:
                citation = m.group(1).strip()
            else:
                reading_parts.append(text)
            continue

        num, body = split_number(text)
        if num:
            # The hymn closes its commemoration, so narrative after one belongs
            # to the next. Without this the whole day collapsed into one entry
            # with every hymn but the first hanging off nothing.
            if arke:
                close_entry()
            agree.append((int(NUMBER.match(text).group(1)), geez_int(num)))
            current.append({"n": geez_int(num), "text": body.strip()})
            narrative_since_arke = True
        elif ARKE.match(text):
            # A second ሰላም with no narrative between them is another stanza of
            # the same hymn — an አርኬ runs to several — not a commemoration with
            # no body, which the printed book does not have.
            if arke and not narrative_since_arke:
                arke = f"{arke}\n{text}"
            else:
                if arke:
                    close_entry()
                arke = text
                narrative_since_arke = False
        else:
            if arke:
                close_entry()
            current.append({"n": 0, "text": text})
            narrative_since_arke = True

    close_entry()

    # A handful of days open straight into their first paragraph with no heading
    # block. The heading is the date, which is never in doubt, so it is written
    # rather than left blank — an empty heading is a hole on the page.
    if not header:
        header = (f"ስንክሳር ዘወርኀ {MONTHS[month - 1]} {geez_numeral(day)}"
                  if edition == "am" else f"አመ {geez_numeral(day)} ለ{MONTHS[month - 1]}")
        synthesised[0] += 1

    out = {"day": day, "header": header, "entries": entries}
    if feasts:
        out["feasts"] = feasts
    if monthly:
        out["monthly"] = monthly
    if reading_parts or citation:
        # One piece, not two: the passage and the reference it came from are the
        # same thing to a reader, and splitting them made the citation a stray
        # paragraph that could be selected and shared on its own.
        out["reading"] = {"text": "\n".join(reading_parts)}
        if citation:
            out["reading"]["cite"] = citation
    return out


def build_edition(code):
    src = SRC / code
    months, agree, junk, synthesised = [], [], [0], [0]
    for num, name in enumerate(MONTHS, 1):
        path = month_file(src, name)
        if path is None:
            sys.exit(f"no scan for {code}/{name} in {src.relative_to(ROOT)}")
        data = json.loads(path.read_text(encoding="utf-8"))
        days = [parse_day(c.get("blocks", []), code, num, c["number"], agree, junk, synthesised)
                for c in data["chapters"]]
        expected = DAYS_IN.get(num, 30)
        if len(days) != expected:
            sys.exit(f"{code}/{name}: {len(days)} days, expected {expected}")
        months.append({"month": num, "name": name, "edition": code,
                       "contentVersion": CONTENT_VERSION, "days": days})
    # Every day's paragraphs should run ፩…N. Two are known not to; a third would
    # mean the scan changed and the numbering can no longer be trusted as an index.
    gaps = set()
    for m in months:
        for d in m["days"]:
            nums = [p["n"] for e in d["entries"] for p in e["paragraphs"] if p["n"]]
            if nums and nums != list(range(nums[0], nums[0] + len(nums))):
                gaps.add((code, m["month"], d["day"]))
            elif nums and nums[0] != 1:
                gaps.add((code, m["month"], d["day"]))
    unexpected = gaps - set(KNOWN_NUMBERING_EXCEPTIONS)
    if unexpected:
        sys.exit(f"{code}: paragraph numbering broken on {sorted(unexpected)}")

    print("  %s: stripped %d dingbat/markup characters; wrote %d missing day headings"
          % (code, junk[0], synthesised[0]))

    bad = [p for p in agree if p[0] != p[1]]
    if bad:
        sys.exit(f"{code}: {len(bad)} paragraphs where the Arabic and Ge'ez "
                 f"numbers disagree, e.g. {bad[:3]} — the Arabic is not safe to drop")
    return months


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for stale in OUT.glob("*.json"):
        stale.unlink()

    manifest = {"contentVersion": CONTENT_VERSION, "editions": [], "months": []}
    for code, label in EDITIONS:
        months = build_edition(code)
        entries = arke = paras = feasts = readings = chars = 0
        for m in months:
            (OUT / f"{code}-{m['month']}.json").write_text(
                json.dumps(m, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
            for d in m["days"]:
                entries += len(d["entries"])
                arke += sum(1 for e in d["entries"] if e.get("arke"))
                paras += sum(len(e["paragraphs"]) for e in d["entries"])
                feasts += len(d.get("feasts", [])) + len(d.get("monthly", []))
                readings += 1 if d.get("reading") else 0
                chars += sum(len(p["text"]) for e in d["entries"] for p in e["paragraphs"])
                chars += sum(len(e.get("arke", "")) for e in d["entries"])
        manifest["editions"].append({"code": code, "name": label})
        if code == "am":
            manifest["months"] = [{"month": m["month"], "name": m["name"],
                                   "days": len(m["days"])} for m in months]
        print("%s: %d entries, %d paragraphs, %d አርኬ, %d feast names, %d readings, %d chars"
              % (label, entries, paras, arke, feasts, readings, chars))

    (OUT / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    size = sum(f.stat().st_size for f in OUT.glob("*.json"))
    print("\nwrote %d files, %.1f MB" % (len(list(OUT.glob('*.json'))), size / 1e6))


if __name__ == "__main__":
    main()
