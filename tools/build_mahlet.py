#!/usr/bin/env python3
"""Build the ሥርዓተ ማኅሌት corpus from the merged editorial edition, the ዘመነ ጽጌ
አቋቋም, and the ግጻዌ.

Three sources, merged:

  sources/mahlet/months/*.json
      The spine: the book-first editorial merge of the scanned ሥርዓተ ማኅሌት
      ዘዓበይት በዓላት with the Telegram editions (t.me/EOTCmahlet). One file per
      Ethiopian month; feasts carry their orders; each order has the book's
      text where the book has it, and the Telegram editions as versions. A
      version is an edition to choose, never a continuation: they are shipped
      side by side and the reader picks one. A chant marked alternative_to is
      "or", not "and then" — carried as a flag and drawn as such. See
      sources/mahlet/README.md for the merge's own rules.

  sources/tsige/*አቋቋም*.json
      ዘመነ ጽጌ, one order per date its Sundays can fall on. The season floats, so
      the book could only give it as "fourth week"; these give ጥቅምት ፲፰, and the
      app's own calendar knows which applies this year.

  sources/gitsawe/mahlets.json
      Only the orders neither the book nor any edition carries — measured, not
      assumed. Each keeps its source.

Never edited by hand; corrections live below as asserted swaps.
"""
import difflib
import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MERGED = ROOT / "sources/mahlet/months"
BOOK_SNAPSHOT = ROOT / "sources/mahlet/sources/book.json"
TSIGE = ROOT / "sources/tsige"
GITSAWE = ROOT / "sources/gitsawe/mahlets.json"
OUT = ROOT / "app/src/main/assets/content/mahlet"

# 2: orders gained `versions` and parts gained `alternative`. Readers decode
# either version — the new fields default — so this is a record, not a gate.
CONTENT_VERSION = 2

MONTHS = ["መስከረም", "ጥቅምት", "ኅዳር", "ታኅሣሥ", "ጥር", "የካቲት", "መጋቢት",
          "ሚያዝያ", "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜን"]
MONTH_ALIASES = {"ጳጉሜን": ("ጳጒሜን", "ጳጉሜ"), "ታኅሣሥ": ("ታኅሳስ", "ታኅሣስ", "ታህሳስ")}

# The merge's service keys, and what the app calls them. Anything else in the
# corpus is reported rather than guessed at.
KINDS = {
    "wazema": "vigil",
    "mahlet": "mahlet",
    "angergari_order": "angergari",
    "prayer": "prayer",
    "procession": "procession",
    "unspecified": "unspecified",
}
KIND_RANK = {k: i for i, k in enumerate(
    ["vigil", "mahlet", "angergari", "procession", "prayer", "unspecified"])}
VIGIL, MAHLET = "vigil", "mahlet"

# What a version-only order says it came from. Amharic, like the other source
# labels the list shows beside an order ("ግጻዌ", "ማኅሌተ ጽጌ አቋቋም").
TELEGRAM = "ቴሌግራም"

# The feasts the book cannot date because they move with Fasika, or sit on a
# Sunday the calendar has to find — and the computus key the app appoints them
# by. See MahletComputus.on(). A feast the merge marks as not fixed must appear
# here or in UNRESOLVED_DATES, or the build stops: an undated order is one that
# no day can ever reach.
MOVABLE = {
    "ስብከት": "sibket",
    "ብርሃን": "birhan",
    "ኖላዊ": "nolawi",
    "ሆሣዕና": "hosanna",
    "ሰሙነ ሕማማት": "himamat",
    "ዓርብ ስቅለት": "siklet",
    "ቀዳም ስዑር": "kedamSiur",
    "ትንሣኤ": "fasika",
    "ዳግም ትንሣኤ": "dagmTinsae",
    "ዕርገት": "erget",
    "ጰራቅሊጦስ": "peraklitos",
}
# "ዘመነ ጽጌ — ፫ኛ ሳምንት": the book's own ordinal-week ጽጌ orders, keyed tsige1..6.
TSIGE_WEEK = re.compile(r"^ዘመነ ጽጌ\s*[—–-]\s*([፩-፱])ኛ")

# The one feast whose printed date the merge could not settle (the book prints
# ፲ /፪/; the channel says ፲፪) and whose calendar it therefore left unresolved.
# Named so the build can say it is known, not silently shipped unreachable.
UNRESOLVED_DATES = {"ተክለ ሃይማኖት ወክርስቶሰ ሰምራ"}

# Two editions are the same text when their words, with the homophones folded
# and the punctuation gone, agree to this share. Below it a difference is a
# difference; above it, it is spelling. Not the part count: a stanza broken in
# two is the same stanza. What guards against a longer edition losing text is
# that the fuller of a pair stands, and that nothing fuller than the book folds
# into the book. The merge held such pairs for review; the maintainer decided.
SUPERFICIAL = 0.95

# What the channel appends to a post and what it points with, neither of which
# is chant. The join-and-share line, the feedback handle, and the bare English
# invitation go whole; the pointing hand the channel sets before a sung line
# — 292 of them — goes as a glyph and the line stays, because the line is the
# chant. Measured over every edition before being written: no part is only
# boilerplate, so no part disappears here.
BOILERPLATE = re.compile(
    r"#ይቀላቀሉ|አስተያየት ካለ|join and share|@[A-Za-z_][A-Za-z0-9_]+|t\.me/|https?://"
    # a phone number; a recording's caption; the channel's dedication line;
    # "for more, click the link and follow"; an editor's note and signature;
    # a reposter's credit
    r"|\+?[0-9]{9,}|ዩኒቨርሲቲ|የምርቃት ሥነ ሥርዐት|የቴሌግራም ቻናል|ለበለጠ ዕውቀት|ሊንኩን ተጭነው"
    r"|^\s*ይጠይቁ ወይም\s*$|^\s*ማስታወሻ\s*-|ያሬዳውያን ነን|\bvia\b|ማህሌታውያን|ማኅሌታውያን",
    re.I)
# The ቅኔ-lesson menu that follows the caption above: one word a line. Struck
# only from a part that carried the caption, since a lone ወርቅ is otherwise a
# word of the chant and not a price list.
AD_MENU = {"ፍቺ", "ርቃቄ", "ሠም", "ወርቅ", "ቅኔ", "ሙሉ ቤት ቅኔ"}
AD_CAPTION = re.compile(r"ዩኒቨርሲቲ|ጥያቄ ካለ|\+?[0-9]{9,}")
# A scanned page's footer — crosses set around its number — on a line of its
# own. Text, never; the ጽጌ scan carries them between stanzas.
SCAN_FOOTER = re.compile(r"^[†\s]*[0-9]+[†\s]*$")
GLYPHS = re.compile("[\U0001F300-\U0001FAFF\u2600-\u27BF\uFE0F]")


# The channel's typing, set as the book sets it. None of this changes a word.
#   ASCII colon between letters is the wordspace ፡ the keyboard lacked; a
#   question mark after a letter is ፧. A digit run is a Ge'ez numeral — in a
#   repeat mark (/2/, [፪], X3 → /፪/, /፪/, /፫/), a rubric (በ 1 ሀሌታ → በ ፩ ሀሌታ) or
#   a psalm reference (መዝ 21:1-2 → መዝ ፳፩፡፩-፪) alike, because that is the
#   numeral the app prints everywhere else. A hashtag is a rubric the channel
#   tagged (#ስቡዕ_ከተባለ_በኃላ → ስቡዕ ከተባለ በኃላ). ÷ stands in for ፣, ˮ for ”, -= for ፦.
#   A no-break space is a space; a zero-width space and Word's private-use
#   bullet are nothing; a stray single Latin letter glued to a word (ወረብe) is
#   the keyboard's.
GEEZ_ONES = "፩፪፫፬፭፮፯፰፱"
GEEZ_TENS = "፲፳፴፵፶፷፸፹፺"


def geez_numeral(n):
    if n <= 0:
        return str(n)
    if n >= 10000:
        return geez_numeral(n // 10000) + "፼" + (geez_numeral(n % 10000) if n % 10000 else "")
    if n >= 100:
        a, b = divmod(n, 100)
        return (geez_numeral(a) if a > 1 else "") + "፻" + (geez_numeral(b) if b else "")
    t, o = divmod(n, 10)
    return (GEEZ_TENS[t - 1] if t else "") + (GEEZ_ONES[o - 1] if o else "")


def normalise(line):
    line = line.replace("\u00a0", " ").replace("\u200b", "").replace("\uf0d8", "")
    # repeat marks first, so the digit inside is still a digit
    line = re.sub(r"\[\s*([፩-፼]+|[0-9]+)\s*\]",
                  lambda m: "/%s/" % (geez_numeral(int(m.group(1))) if m.group(1)[0] in "0123456789" else m.group(1)), line)
    line = re.sub(r"/\s*([0-9]+)\s*ጊዜ\s*/", lambda m: "/%s/" % geez_numeral(int(m.group(1))), line)
    line = re.sub(r"\b[Xx×]([0-9])\b", lambda m: "/%s/" % geez_numeral(int(m.group(1))), line)
    line = re.sub(r"[0-9]+", lambda m: geez_numeral(int(m.group(0))), line)
    line = re.sub(r"#([ሀ-፼][ሀ-፼_]*)", lambda m: m.group(1).replace("_", " "), line)
    line = re.sub(r"(?<=[ሀ-፼])_(?=[ሀ-፼])", " ", line)
    line = re.sub(r"(?<=[ሀ-፼])\s*:\s*(?=[ሀ-፼])", "፡", line)
    line = re.sub(r"(?<=[ሀ-፼])\s*\?", "፧", line)
    line = line.replace("÷", "፣").replace("ˮ", "”").replace(" -=", "፦").replace("-=", "፦")
    line = re.sub(r"(?<=[ሀ-፼])[A-Za-z](?![A-Za-z])", "", line)
    return line


def clean_chant(text):
    raw = (text or "").split("\n")
    advert = any(AD_CAPTION.search(l) for l in raw)
    lines = []
    for line in raw:
        if BOILERPLATE.search(line) or SCAN_FOOTER.match(line):
            continue
        if advert and line.strip() in AD_MENU:
            continue
        line = normalise(GLYPHS.sub("", line))
        line = re.sub(r"[ \t]+", " ", line).strip()
        if line:
            lines.append(line)
    return "\n".join(lines)


def clean_name(s):
    """A part name as the list shows it: no marks, no glyphs, nothing glued on."""
    s = GLYPHS.sub("", s or "")
    s = re.sub(r"[\u200b\uf0d8\u00a0]", " ", s)
    s = re.sub(r"@[A-Za-z_][A-Za-z0-9_]+", "", s)
    s = re.sub(r"[\s፦:\-\"'“”]+$", "", s).strip("\"'“” ")
    return re.sub(r"\s+", " ", s).strip()


# The one form whose specific hymn lives in the chant's title: the merge writes
# form "መልክእ" and title "መልክአ ሥላሴ". The title is the part's name, because the
# name is what opens the hymn on the shelf.
TITLED_FORM = "መልክእ"

# "አመ ፲ወ፩ ለጥቅምት (ጥቅምት ፲፩ እሑድ ሲውል)" — the ጽጌ orders are keyed by the date whose
# falling on a Sunday appoints them. The ወ is the joiner in a compound numeral
# (፲ወ፩ is eleven) and sits outside the ፩-፼ block, so it has to be admitted here
# or every date past ten is read as no date at all.
TSIGE_HEAD = re.compile(r"^አመ\s+([፩-፼ወ]+)\s*ሁ?\s+ለ(\S+)")
ORDER_HEAD = re.compile(r"^\s*\d*\s*ሥርዓተ\s+(ዋዜማ|ማኅሌት)\s+(.+)$")
MONTH_HEAD = re.compile(r"ዘወር[ኃኀ]\s")

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

# The ጽጌ source has not changed, so its split must not either. The vocabulary
# the split rests on now comes from the merge's snapshot of the same scan;
# if that ever yields a different count, the snapshot and the scan have
# diverged and this must be read again.
TSIGE_EXPECTED_PARTS = 626


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


# ── the spine: the merged edition ───────────────────────────────────────────

def words(parts):
    """The text of an order as folded words: homophones one letter, no marks."""
    out = []
    for p in parts:
        for c in p["verse"]:
            cp = ord(c)
            for start, target in FOLD_SERIES:
                if start <= cp <= start + 7:
                    c = chr(target + cp - start)
                    break
            out.append(c)
    return re.sub(r"[()፡።፣፤፥፦:.,;\-–—\"«»'“”]+", " ", "".join(out)).split()


def same_text(a, b):
    """Whether two runs of parts are one text, spelling and segmentation aside.

    Compared as words, not as parts: two posts of one order segment it
    differently as often as not — a stanza broken in two, a refrain given its
    own line — and a rule that counted parts would call those different. The
    words are what is sung.
    """
    wa, wb = words(a), words(b)
    if not wa or not wb:
        return False
    return difflib.SequenceMatcher(None, wa, wb, autojunk=False).ratio() >= SUPERFICIAL


# An edition may fold into the book only if it carries no more text than the
# book does, within this share of the words: the book is never replaced, so an
# edition that is the book plus a stanza would lose the stanza on folding.
BOOK_TOLERANCE = 0.02


def fuller(a, b):
    return len(words(a)) >= len(words(b))


def dedupe(base, versions, report, base_is_book):
    """Fold the editions that only differ in spelling into the one that stands.

    An edition the same as [base] — the book's text, or the edition standing
    for a bookless order — adds nothing and goes, its post kept on the order.
    Among the rest a like pair becomes one, and it is the fuller of the two
    that stands: two posts of one order can differ by a clause the shorter
    lost, and a rule that kept the first would keep the truncated one when
    it happened to come first. Every link survives either way.

    The book's own text is never replaced, even by a fuller edition: it is
    the book. An order standing on an edition takes the fuller edition as
    its text, since neither is the book and the fuller is the better copy.
    """
    kept, into_base = [], []
    base_parts = base
    for v in versions:
        if same_text(base_parts, v["parts"]) and not (
            base_is_book
            and len(words(v["parts"])) > len(words(base_parts)) * (1 + BOOK_TOLERANCE)
        ):
            if not base_is_book and not fuller(base_parts, v["parts"]):
                base_parts, v["parts"] = v["parts"], base_parts
                # The order's own post is now the absorbed one; the caller
                # records the swap through the returned parts and url.
                into_base.append(v["url"])
                v["url"], into_base[-1] = into_base[-1], v["url"]
            else:
                into_base.append(v["url"])
            report["folded_into_base"] += 1
            continue
        for k in kept:
            if same_text(k["parts"], v["parts"]):
                if not fuller(k["parts"], v["parts"]):
                    k["parts"], v["parts"] = v["parts"], k["parts"]
                    k["url"], v["url"] = v["url"], k["url"]
                    k["title"], v["title"] = v["title"], k["title"]
                    k["id"], v["id"] = v["id"], k["id"]
                k.setdefault("also", []).append(v["url"])
                report["folded"] += 1
                break
        else:
            kept.append(v)
    return kept, [u for u in into_base if u], base_parts

def chant_to_part(chant, alternative=False):
    """One chant of the merge as one part of an order, or None to drop it.

    The specific hymn of a መልክእ is in its title; every other form is its own
    name. A chant with no text is a bare heading the scan carried — 66 of the
    book's 1,645 — and is dropped, as the previous pipeline dropped them.
    """
    text = clean_chant(chant.get("text"))
    form = chant.get("form")
    title = clean_name(chant.get("title"))
    # One post glued a stanza onto the hymn's name. A name is a few words; if
    # it runs past that, the name is the words before the first ሰላም and the
    # rest is verse.
    if len(title) > PART_NAME_MAX:
        cut = title.find("ሰላም", 1)
        if cut > 0:
            title, spill = title[:cut].strip(), title[cut:].strip()
            text = (clean_chant(spill) + "\n" + text).strip()
    if not text:
        return None
    key = title if (form == TITLED_FORM and title) else (form or "")
    part = {"key": clean_name(key), "verse": text}
    if alternative or chant.get("alternative_to") is not None:
        part["alternative"] = True
    return part


def flatten(chants, report, alternative=False):
    """Chants to parts, depth-first through the Telegram `or` groups.

    An `or` group is a choice. Its first branch reads as the order does; every
    later branch is marked alternative, so the page can say "ወይም" between them
    instead of setting them one after another as though both were sung.
    """
    parts = []
    for chant in chants:
        if not isinstance(chant, dict):
            continue
        if "or" in chant:
            for i, branch in enumerate(chant["or"]):
                branch = branch if isinstance(branch, list) else [branch]
                parts += flatten(branch, report, alternative=(i > 0))
            continue
        part = chant_to_part(chant, alternative)
        if part is None:
            report["empty_chants"] += 1
            continue
        parts.append(part)
    return parts


def movable_key(feast):
    """The computus key a non-fixed feast is appointed by, or None."""
    name = feast["name"].strip()
    if name in MOVABLE:
        return MOVABLE[name]
    m = TSIGE_WEEK.match(name)
    if m:
        return "tsige%d" % geez_int(m.group(1))
    return None


def build_spine(report):
    orders = []
    for path in sorted(MERGED.glob("*.json")):
        month_file = load(path)
        month = month_file["month_number"]
        for feast in month_file["feasts"]:
            for service, order in feast["orders"].items():
                kind = KINDS.get(service)
                if kind is None:
                    report["unknown_kind"].append((feast["name"], service))
                    continue
                versions = []
                for v in order.get("versions") or []:
                    vparts = flatten(v.get("chants", []), report)
                    if not vparts:
                        continue
                    src = (v.get("sources") or [{}])[0]
                    versions.append({
                        "id": v["id"],
                        "title": clean_name(normalise(GLYPHS.sub("", src.get("title") or "")).replace("#", " ")) or None,
                        "url": src.get("url"),
                        "parts": vparts,
                    })
                book = order.get("book")
                url = None
                if book:
                    parts = flatten(book.get("chants", []), report)
                    source = None
                elif versions:
                    # No book text: the first edition stands for the order and
                    # the rest remain to be chosen. The merge is explicit that
                    # first means representative, not preferred.
                    first = versions.pop(0)
                    parts, source, url = first["parts"], TELEGRAM, first["url"]
                    report["version_only"] += 1
                else:
                    report["empty"] += 1
                    continue
                if not parts:
                    report["empty"] += 1
                    continue
                versions, also, parts = dedupe(parts, versions, report, base_is_book=book is not None)
                o = {
                    "id": order_id("merged", feast["id"], service),
                    "kind": kind,
                    "feast": feast["name"].strip(),
                    "month": month,
                    "day": feast.get("day"),
                    "source": source,
                    "parts": parts,
                }
                if url:
                    o["url"] = url
                if also:
                    o["also"] = also
                if versions:
                    o["versions"] = versions
                movable = movable_key(feast)
                if movable:
                    o["movable"] = movable
                elif feast.get("day") is None and feast["name"].strip() not in UNRESOLVED_DATES:
                    report["unappointable"].append((feast["name"], feast.get("calendar")))
                orders.append(o)
    return orders


# ── ዘመነ ጽጌ, dated ────────────────────────────────────────────────────────────

def part_vocabulary():
    """Every name a part of a ማኅሌት goes by, for splitting the ጽጌ books.

    The አቋቋም scans alternate a part's name with its verse, but not strictly
    enough to split on length. So the split is made on a vocabulary — the
    ግጻዌ's own part names, plus the names the merge's snapshot of the ማኅሌት scan
    uses often enough to be names rather than text. That snapshot is the same
    scan the vocabulary used to be read from.
    """
    vocab = {fold(p["key"]) for order in gitsawe_orders() for p in order["detail"]}
    counts = {}
    for chapter in load(BOOK_SNAPSHOT)["chapters"]:
        for b in chapter["blocks"]:
            t = (b.get("text") if isinstance(b, dict) else b) or ""
            t = t.strip()
            if not t or ORDER_HEAD.match(t) or MONTH_HEAD.search(t) or len(t) > 40:
                continue
            counts[fold(t)] = counts.get(fold(t), 0) + 1
    vocab |= {k for k, n in counts.items() if n >= 3}
    for name in EXTRA_PART_NAMES:
        if fold(name) not in vocab:
            sys.exit(f"part name no longer in the scan snapshot: {name!r}")
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


def cleaned(parts):
    """The scan's parts through the same cleaning as the channel's, after the
    split — so the split, and the count asserted on it, see the scan as it is."""
    out = []
    for p in parts:
        verse = clean_chant(p["verse"])
        if verse:
            out.append({**p, "verse": verse})
    return out


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
            current = {"key": "", "verse": text}
            parts.append(current)
    return [p for p in parts if p["verse"]]


def build_tsige(vocab, report):
    orders = []
    for path in sorted(TSIGE.glob("*አቋቋም*.json")):
        for chapter in load(path)["chapters"]:
            title = (chapter.get("title") or "").strip()
            m = TSIGE_HEAD.match(title)
            blocks = [(b.get("text") or "").strip()
                      for b in chapter.get("blocks", [])
                      if (b.get("text") or "").strip()
                      and (b.get("text") or "").strip() != title]
            if not m:
                # Each file opens with the season's general order, undated. It
                # is the one to fall back on when no date matches, so it is kept
                # rather than skipped along with the empty trailing chapter.
                if not title or not blocks:
                    continue
                parts = cleaned(split_parts(blocks, vocab))
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
            parts = cleaned(split_parts(blocks, vocab))
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


# ── the ግጻዌ's own, where nothing else has them ──────────────────────────────

def build_missing(spine, report):
    """The ግጻዌ's orders neither the book nor any edition carries.

    Measured rather than assumed: an order counts as carried when at least a
    quarter of its parts are findable in the merged text — book and versions.
    """
    def norm(s):
        return re.sub(r"[\s።፡፣፤፥.]+", "", s or "")

    verses = [p["verse"] for o in spine for p in o["parts"]]
    verses += [p["verse"] for o in spine for v in o.get("versions", []) for p in v["parts"]]
    haystack = norm(" ".join(verses))
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
            "parts": cleaned([{"key": p["key"], "verse": p["verse"]} for p in order["detail"]]),
        })
        report["from_gitsawe"].append((title, round(share * 100)))
    return kept


# ── output ───────────────────────────────────────────────────────────────────

def main():
    report = {"empty": 0, "empty_chants": 0, "version_only": 0, "folded": 0, "folded_into_base": 0,
              "unknown_kind": [], "unmapped": [], "unappointable": [], "from_gitsawe": []}
    vocab = part_vocabulary()

    spine = build_spine(report)
    tsige = build_tsige(vocab, report)
    missing = build_missing(spine, report)
    orders = spine + tsige + missing

    if report["unknown_kind"]:
        sys.exit("orders of a kind this build does not know: "
                 + "; ".join(f"{f} ({k})" for f, k in report["unknown_kind"]))
    if report["unmapped"]:
        sys.exit("ጽጌ orders whose month could not be read: " + "; ".join(report["unmapped"]))
    if report["unappointable"]:
        sys.exit("feasts with no day and no computus key — add them to MOVABLE or UNRESOLVED_DATES: "
                 + "; ".join(f"{n} ({c})" for n, c in report["unappointable"]))
    tsige_parts = sum(len(o["parts"]) for o in tsige)
    if tsige_parts != TSIGE_EXPECTED_PARTS:
        sys.exit(f"ዘመነ ጽጌ split to {tsige_parts} parts, not {TSIGE_EXPECTED_PARTS}: "
                 f"the vocabulary has drifted from the scan it used to be read from")

    seen = {}
    for o in orders:
        if o["id"] in seen:
            sys.exit(f"id collision: {o['feast']!r} and {seen[o['id']]!r}")
        seen[o["id"]] = o["feast"]

    OUT.mkdir(parents=True, exist_ok=True)
    for stale in OUT.glob("*.json"):
        stale.unlink()

    by_month = {}
    for o in orders:
        by_month.setdefault(o.get("month") or 0, []).append(o)

    index_months = []
    for month, items in sorted(by_month.items()):
        items.sort(key=lambda o: (o.get("day") or 99, KIND_RANK.get(o["kind"], 9)))
        (OUT / f"m{month}.json").write_text(
            json.dumps({"month": month, "orders": items},
                       ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        index_months.append({
            "month": month,
            "name": MONTHS[month - 1] if month else "",
            # Only the fields an order actually has: a null in the index has to
            # be decoded into a non-null Kotlin default, and cannot be.
            "orders": [
                {k: o[k] for k in ("id", "kind", "feast", "day", "season", "source", "movable")
                 if o.get(k) is not None}
                | {"parts": len(o["parts"])}
                | ({"whenSunday": True} if o.get("whenSunday") else {})
                | ({"versions": len(o["versions"])} if o.get("versions") else {})
                for o in items
            ],
        })

    (OUT / "index.json").write_text(
        json.dumps({"contentVersion": CONTENT_VERSION, "months": index_months},
                   ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    parts = sum(len(o["parts"]) for o in orders)
    versions = sum(len(o.get("versions", [])) for o in orders)
    vparts = sum(len(v["parts"]) for o in orders for v in o.get("versions", []))
    alts = sum(1 for o in orders for p in o["parts"] if p.get("alternative"))
    chars = sum(len(p["verse"]) for o in orders for p in o["parts"])
    size = sum(f.stat().st_size for f in OUT.glob("*.json"))
    print("spine (merged edition): %d orders, %d of them Telegram-only" % (len(spine), report["version_only"]))
    print("ዘመነ ጽጌ (dated):         %d orders" % len(tsige))
    print("from the ግጻዌ:           %d orders" % len(missing))
    for t, s in report["from_gitsawe"]:
        print("    %-56s %d%% carried by the merge" % (t[:54], s))
    movable = sum(1 for o in orders if o.get("movable"))
    print("\n%d orders, %d parts (%d marked ወይም), %d editions holding %d more parts, %d appointed by the computus"
          % (len(orders), parts, alts, versions, vparts, movable))
    print("%d chars, %.2f MB in %d files" % (chars, size / 1e6, len(list(OUT.glob('*.json')))))
    print("  %d editions folded into a like edition, %d into the text they repeat — %d superficial in all"
          % (report["folded"], report["folded_into_base"], report["folded"] + report["folded_into_base"]))
    if report["empty_chants"]:
        print("  (%d chants had no text and were dropped)" % report["empty_chants"])
    if report["empty"]:
        print("  (%d orders had no parts and were dropped)" % report["empty"])


if __name__ == "__main__":
    main()
