#!/usr/bin/env python3
"""Validate the bundled content before a release. Exits non-zero on any failure.

This is a release gate, not a linter: it guards the invariants that would
silently corrupt a user's data or ship unreadable text, and deliberately does
not duplicate what the Kotlin tests already assert (deserialization, ግጻዌ
reference resolution, Ethiopian date keys, synaxarium day totals).

What it checks:
  * Section IDs are unique, and STABLE against the checked-in snapshot —
    bookmarks and highlights reference them, so a rename orphans user data.
  * Manifests are internally consistent and every referenced file exists.
  * contentVersion is present and a positive integer everywhere it's declared.
  * No content is empty where content is required.
  * Text is free of control characters, replacement characters, and stray
    markup that would render literally.

Run:  python3 tools/validate_content.py [--update-snapshot]
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
CONTENT = os.path.join(ROOT, "app", "src", "main", "assets", "content")
SNAPSHOT = os.path.join(ROOT, "tools", "section-ids.json")

errors: list[str] = []
warnings: list[str] = []


def fail(msg: str) -> None:
    errors.append(msg)


def warn(msg: str) -> None:
    warnings.append(msg)


def load(*parts: str):
    path = os.path.join(CONTENT, *parts)
    if not os.path.isfile(path):
        fail(f"missing file: {os.path.relpath(path, ROOT)}")
        return None
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except json.JSONDecodeError as e:
        fail(f"invalid JSON in {os.path.relpath(path, ROOT)}: {e}")
        return None


# --- text hygiene -----------------------------------------------------------

# C0 controls except tab/newline, plus U+FFFD, which means a decode already lost data.
BAD_CHARS = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f�]")
# Markup that would render literally in the reader.
STRAY_MARKUP = re.compile(r"</?\s*[a-zA-Z]{1,3}\s*>")


def check_text(where: str, text: str) -> None:
    if BAD_CHARS.search(text):
        cps = {hex(ord(c)) for c in BAD_CHARS.findall(text)}
        fail(f"{where}: control/replacement characters {sorted(cps)}")
    m = STRAY_MARKUP.search(text)
    if m:
        warn(f"{where}: stray markup {m.group(0)!r}")


def check_version(where: str, node: dict) -> None:
    if "contentVersion" not in node:
        return  # not every manifest declares one
    v = node["contentVersion"]
    if not isinstance(v, int) or isinstance(v, bool) or v < 1:
        fail(f"{where}: contentVersion must be a positive integer, got {v!r}")


# --- collectors -------------------------------------------------------------

def collect_section_ids() -> dict[str, list[str]]:
    """Every permanent section id, grouped by source file."""
    ids: dict[str, list[str]] = {}

    manifest = load("manifest.json")
    if manifest:
        check_version("manifest.json", manifest)
        for hour in manifest.get("hours", []):
            hid, fname = hour.get("id"), hour.get("file")
            if not hid or not fname:
                fail(f"manifest.json: hour entry missing id/file: {hour}")
                continue
            data = load(fname)
            if data is None:
                continue
            if data.get("id") != hid:
                fail(f"{fname}: id {data.get('id')!r} does not match manifest {hid!r}")
            sections = data.get("sections", [])
            if not sections:
                fail(f"{fname}: hour has no sections")
            ids[fname] = [s.get("id", "") for s in sections]
            for s in sections:
                sid = s.get("id")
                if not sid:
                    fail(f"{fname}: a section has no id")
                if not s.get("title"):
                    fail(f"{fname}/{sid}: section has no title")
                verses = s.get("verses", [])
                if not verses:
                    fail(f"{fname}/{sid}: section has no verses")
                for i, v in enumerate(verses):
                    check_text(f"{fname}/{sid} v{i}", v)

    # Runtime Psalm section ids remain the historical ps_N contract even though
    # their text now comes from the unified Amharic-1980 Bible edition.
    psalms = load("bible", "am-1980", "books", "19-psalms.json")
    if psalms:
        chapters = [c for c in psalms.get("chapters", []) if isinstance(c.get("n"), int) and 1 <= c["n"] <= 150]
        if len(chapters) != 150:
            fail(f"bible/am-1980 Psalms: expected 150 chapters, found {len(chapters)}")
        ids["bible/am-1980/psalms"] = [f"ps_{c['n']}" for c in chapters]
    return ids


def check_synaxarium() -> None:
    """Both editions of the ስንክሳር, and the structure the reader draws by.

    The scans carry the shape now, so these check the shape rather than the
    prose: every day has its heading and its commemorations, every hymn is a
    block of its own, the reading is one piece with its reference, and no two
    entries in a day share the id a bookmark points at.
    """
    manifest = load("sinksar", "manifest.json")
    if manifest:
        check_version("sinksar/manifest.json", manifest)
        codes = [e.get("code") for e in manifest.get("editions", [])]
        if codes != ["am", "ge"]:
            fail(f"sinksar/manifest.json: editions are {codes}, expected ['am', 'ge']")

    totals = {}
    for edition in ("am", "ge"):
        days_seen = entries = arke = readings = 0
        for m in range(1, 14):
            name = f"{edition}-{m}.json"
            data = load("sinksar", name)
            if data is None:
                continue
            if data.get("month") != m:
                fail(f"sinksar/{name}: declares month {data.get('month')}")
            if data.get("edition") != edition:
                fail(f"sinksar/{name}: declares edition {data.get('edition')}")

            days = data.get("days", [])
            nums = [d.get("day") for d in days]
            if len(nums) != len(set(nums)):
                dupes = {n for n in nums if nums.count(n) > 1}
                fail(f"sinksar/{name}: duplicate days {sorted(dupes)}")
            if sorted(nums) != list(range(1, len(nums) + 1)):
                fail(f"sinksar/{name}: days are not 1..{len(nums)}")

            for d in days:
                where = f"sinksar/{name} day {d.get('day')}"
                days_seen += 1
                if not d.get("header"):
                    fail(f"{where}: no heading")
                if not d.get("entries"):
                    fail(f"{where}: no entries")

                ids = [e.get("id") for e in d.get("entries", [])]
                if len(ids) != len(set(ids)):
                    # A bookmark points at one of these; a repeat would make a
                    # bookmark on either entry light up on both.
                    fail(f"{where}: repeated entry id {sorted(set(x for x in ids if ids.count(x) > 1))}")
                if not all(ids):
                    fail(f"{where}: an entry has no id")

                for e in d.get("entries", []):
                    entries += 1
                    paras = e.get("paragraphs", [])
                    if not paras:
                        fail(f"{where} entry {e.get('id')}: no paragraphs")
                    for para in paras:
                        check_text(f"{where} entry {e.get('id')}", para.get("text", ""))
                    hymn = e.get("arke")
                    if hymn is not None:
                        arke += 1
                        if not hymn.strip():
                            fail(f"{where} entry {e.get('id')}: empty አርኬ")
                        elif not hymn.lstrip().startswith("ሰላ"):
                            # The hymn is the salutation. Anything else here
                            # means the block typing slipped.
                            fail(f"{where} entry {e.get('id')}: አርኬ does not open with ሰላም")

                reading = d.get("reading")
                if reading is not None:
                    readings += 1
                    if not reading.get("text", "").strip():
                        fail(f"{where}: reading with no text")
                    check_text(f"{where} reading", reading.get("text", ""))

        totals[edition] = (days_seen, entries, arke, readings)
        if days_seen != 366:
            fail(f"sinksar/{edition}: {days_seen} days, expected 366")

    for edition, (days_seen, entries, arke, readings) in totals.items():
        print(f"sinksar {edition}: {days_seen} days, {entries} entries, "
              f"{arke} አርኬ, {readings} readings")

    # Only the Amharic edition carries the feast lists and the daily reading;
    # the Ge'ez scans have neither, and claiming otherwise would mean the
    # generator had started inventing them.
    if totals.get("ge", (0, 0, 0, 0))[3] != 0:
        fail("sinksar/ge: the Ge'ez edition should carry no readings")


def check_scripture() -> None:
    catalog = load("bible", "catalog.json")
    if not catalog:
        return
    # gez-1980 gained three verses when two merged ones were split back apart
    # on the ፯፧ / ፭፧ / ፮፧ markers the scan left inside them.
    expected = {"am-1980": (93, 44290), "gez-1980": (1, 2462)}
    for edition in catalog.get("editions", []):
        eid = edition.get("id")
        if eid not in expected:
            continue
        stats = edition.get("stats", {})
        if (stats.get("books"), stats.get("verses")) != expected[eid]:
            fail(f"bible/{eid}: unexpected catalog totals {stats}")
        if eid == "am-1980":
            meta = load("bible", eid, "meta.json")
            if meta and len(meta.get("books", [])) != expected[eid][0]:
                fail(f"bible/{eid}: metadata book count mismatch")

    # The catalogue is a claim about the files beside it; count them and check.
    # It said 2,459 for a Psalter that held 2,462 until this was added.
    for edition in catalog.get("editions", []):
        eid = edition.get("id")
        counted = 0
        books_dir = os.path.join(CONTENT, "bible", eid, "books")
        if not os.path.isdir(books_dir):
            continue
        for name in sorted(os.listdir(books_dir)):
            if not name.endswith(".json"):
                continue
            with open(os.path.join(books_dir, name), encoding="utf-8") as handle:
                book = json.load(handle)
            counted += sum(len(c.get("verses", [])) for c in book.get("chapters", []))
        if counted and counted != edition.get("stats", {}).get("verses"):
            fail(f"bible/{eid}: catalogue says {edition['stats']['verses']} verses, files hold {counted}")


def check_wudase() -> None:
    data = load("wudase", "wudase.json")
    if not data:
        return
    check_version("wudase/wudase.json", data)
    sections = data.get("sections", [])
    sids = [s.get("id") for s in sections]
    if len(sids) != len(set(sids)):
        fail("wudase: duplicate section ids")
    weekdays = [s.get("weekday") for s in sections if s.get("weekday", 0) in range(1, 8)]
    if sorted(weekdays) != list(range(1, 8)):
        fail(f"wudase: weekday portions are not exactly 1..7 (got {sorted(weekdays)})")
    for s in sections:
        if not s.get("am") or not s.get("ge"):
            fail(f"wudase/{s.get('id')}: missing a language")


# --- section-id stability ---------------------------------------------------

def check_id_stability(current: dict[str, list[str]], update: bool) -> None:
    """Section ids are permanent contracts — bookmarks and highlights point at
    them, so a regeneration that renames one silently orphans user data."""
    for fname, ids in current.items():
        dupes = {i for i in ids if ids.count(i) > 1}
        if dupes:
            fail(f"{fname}: duplicate section ids {sorted(dupes)}")

    if update:
        with open(SNAPSHOT, "w", encoding="utf-8") as f:
            json.dump(current, f, ensure_ascii=False, indent=1, sort_keys=True)
        print(f"snapshot updated: {os.path.relpath(SNAPSHOT, ROOT)}")
        return

    if not os.path.isfile(SNAPSHOT):
        fail(
            f"no section-id snapshot at {os.path.relpath(SNAPSHOT, ROOT)} — "
            "run: python3 tools/validate_content.py --update-snapshot"
        )
        return

    with open(SNAPSHOT, encoding="utf-8") as f:
        known = json.load(f)

    for fname, ids in known.items():
        if fname not in current:
            fail(f"{fname}: file in the snapshot is gone from the content")
            continue
        missing = set(ids) - set(current[fname])
        if missing:
            fail(
                f"{fname}: {len(missing)} section id(s) disappeared, e.g. {sorted(missing)[:5]} — "
                "existing bookmarks and highlights point at these"
            )
    for fname, ids in current.items():
        added = set(ids) - set(known.get(fname, []))
        if added:
            warn(f"{fname}: {len(added)} new section id(s), e.g. {sorted(added)[:5]}")


def check_books() -> None:
    """The ሌሎች መጻሕፍት shelf: the index and the book files must agree exactly.

    The ids are content addresses (a hash of the title), and a bookmark or a
    ማኅሌት join points at one, so a book file that the index does not list — or an
    index row with no file behind it — is a dead link rather than a cosmetic
    mismatch.
    """
    index = load("books", "index.json")
    if index is None:
        return
    if not isinstance(index.get("contentVersion"), int) or index["contentVersion"] < 1:
        fail("books/index.json: contentVersion must be a positive integer")

    listed: dict[str, dict] = {}
    for shelf in index.get("shelves", []):
        if not shelf.get("name"):
            fail(f"books/index.json: shelf {shelf.get('key')!r} has no name")
        for meta in shelf.get("books", []):
            bid = meta.get("id", "")
            if bid in listed:
                fail(f"books/index.json: duplicate book id {bid}")
            listed[bid] = meta
            if not meta.get("title"):
                fail(f"books/index.json: book {bid} has no title")
            if not meta.get("key"):
                fail(f"books/index.json: book {bid} has no fold key for the ማኅሌት join")

    on_disk = {
        os.path.splitext(f)[0]
        for f in os.listdir(os.path.join(CONTENT, "books"))
        if f.endswith(".json") and f != "index.json"
    }
    for orphan in sorted(on_disk - set(listed)):
        fail(f"books/{orphan}.json is not listed in the index")
    for missing in sorted(set(listed) - on_disk):
        fail(f"books/index.json lists {missing}, but there is no such file")

    # Every recension pointer has to resolve, or the reader offers a dead tap.
    for bid, meta in listed.items():
        for ref in list(meta.get("variants", [])) + [meta.get("variantOf")]:
            if ref and ref.get("id") not in listed:
                fail(f"books/{bid}: points at unknown recension {ref.get('id')}")

    chapters = blocks = chars = 0
    for bid, meta in listed.items():
        book = load("books", f"{bid}.json")
        if book is None:
            continue
        if book.get("title") != meta.get("title"):
            fail(f"books/{bid}: title {book.get('title')!r} != index {meta.get('title')!r}")
        n_blocks = sum(len(c.get("blocks", [])) for c in book.get("chapters", []))
        n_chars = sum(len(b.get("text", "")) for c in book.get("chapters", [])
                      for b in c.get("blocks", []))
        if len(book.get("chapters", [])) != meta.get("chapterCount"):
            fail(f"books/{bid}: {len(book.get('chapters', []))} chapters, index says "
                 f"{meta.get('chapterCount')}")
        if n_blocks != meta.get("blockCount") or n_chars != meta.get("charCount"):
            fail(f"books/{bid}: {n_blocks} blocks/{n_chars} chars, index says "
                 f"{meta.get('blockCount')}/{meta.get('charCount')}")
        for c in book.get("chapters", []):
            for b in c.get("blocks", []):
                if not b.get("text", "").strip():
                    fail(f"books/{bid} ch{c.get('number')}: empty block")
                    break
        chapters += len(book.get("chapters", []))
        blocks += n_blocks
        chars += n_chars

    # The printer's tier marks are turned into line breaks by the generator;
    # one left behind means a book was added without being run through it.
    for bid in sorted(listed):
        raw = os.path.join(CONTENT, "books", f"{bid}.json")
        with open(raw, encoding="utf-8") as f:
            if "፪ማ፡" in f.read():
                fail(f"books/{bid}.json still carries a ፪ማ፡ tier mark")

    print(f"books: {len(listed)} books, {chapters} chapters, {blocks} blocks, {chars} chars")


def check_mahlet() -> None:
    """The merged ሥርዓተ ማኅሌት: the index and the month files must agree exactly.

    An order id is what a route carries, so an id in the index with no order
    behind it is a dead tap. And a ጽጌ order without a date is unreachable: the
    season is chosen by which of its dates falls on a Sunday, so a dateless one
    can never be appointed.
    """
    index = load("mahlet", "index.json")
    if index is None:
        return
    check_version("mahlet/index.json", index)

    listed, dated, tsige, from_gitsawe = {}, 0, 0, 0
    for month in index.get("months", []):
        num = month.get("month")
        if not isinstance(num, int) or not 0 <= num <= 13:
            fail(f"mahlet/index.json: month {num!r} is out of range")
            continue
        for meta in month.get("orders", []):
            oid = meta.get("id", "")
            if oid in listed:
                fail(f"mahlet/index.json: duplicate order id {oid}")
            listed[oid] = num
            if not meta.get("feast"):
                fail(f"mahlet/index.json: order {oid} has no feast")
            if meta.get("kind") not in ("vigil", "mahlet"):
                fail(f"mahlet/index.json: order {oid} has kind {meta.get('kind')!r}")
            if meta.get("season") == "tsige":
                tsige += 1
                if meta.get("whenSunday") and meta.get("day") is None:
                    fail(f"mahlet/index.json: ጽጌ order {oid} is appointed by a date "
                         f"it does not carry")
            if meta.get("day") is not None:
                dated += 1
            if meta.get("source") == "ግጻዌ":
                from_gitsawe += 1

    parts = 0
    for month in index.get("months", []):
        num = month["month"]
        data = load("mahlet", f"m{num}.json")
        if data is None:
            continue
        orders = {o.get("id"): o for o in data.get("orders", [])}
        for meta in month.get("orders", []):
            order = orders.get(meta.get("id"))
            if order is None:
                fail(f"mahlet/index.json lists {meta.get('id')} in month {num}, "
                     f"but m{num}.json has no such order")
                continue
            if len(order.get("parts", [])) != meta.get("parts"):
                fail(f"mahlet/m{num}.json {meta['id']}: {len(order.get('parts', []))} "
                     f"parts, index says {meta.get('parts')}")
            for p in order.get("parts", []):
                parts += 1
                if not p.get("verse", "").strip():
                    fail(f"mahlet/m{num}.json {meta['id']}: a part has no verse")
                check_text(f"mahlet/m{num}.json {meta['id']}", p.get("verse", ""))
        for oid in orders:
            if oid not in listed:
                fail(f"mahlet/m{num}.json holds {oid}, which the index does not list")

    print(f"mahlet: {len(listed)} orders ({dated} dated, {tsige} ዘመነ ጽጌ, "
          f"{from_gitsawe} from the ግጻዌ), {parts} parts")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument(
        "--update-snapshot",
        action="store_true",
        help="rewrite the section-id snapshot after an intentional content change",
    )
    args = ap.parse_args()

    ids = collect_section_ids()
    check_synaxarium()
    check_scripture()
    check_wudase()
    check_books()
    check_mahlet()
    check_id_stability(ids, args.update_snapshot)

    total_sections = sum(len(v) for v in ids.values())
    print(f"content: {len(ids)} files, {total_sections} permanent section ids")

    for w in warnings:
        print(f"  warning: {w}")
    for e in errors:
        print(f"  ERROR: {e}", file=sys.stderr)

    if errors:
        print(f"\ncontent validation FAILED — {len(errors)} error(s)", file=sys.stderr)
        return 1
    print(f"content validation passed ({len(warnings)} warning(s))")
    return 0


if __name__ == "__main__":
    sys.exit(main())
