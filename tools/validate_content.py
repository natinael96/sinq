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

    psalms = load("psalms.json")
    if psalms:
        items = psalms.get("psalms", [])
        if len(items) != 150:
            fail(f"psalms.json: expected 150 psalms, found {len(items)}")
        ids["psalms.json"] = [p.get("id", "") for p in items]
        for p in items:
            if not p.get("verses"):
                fail(f"psalms.json/{p.get('id')}: no verses")
            for i, v in enumerate(p.get("verses", [])):
                check_text(f"psalms.json/{p.get('id')} v{i}", v)
    return ids


def check_synaxarium() -> None:
    manifest = load("sinksar", "manifest.json")
    if manifest:
        check_version("sinksar/manifest.json", manifest)
    for m in range(1, 14):
        data = load("sinksar", f"{m}.json")
        if data is None:
            continue
        if data.get("month") != m:
            fail(f"sinksar/{m}.json: declares month {data.get('month')}")
        days = data.get("days", [])
        nums = [d.get("day") for d in days]
        if len(nums) != len(set(nums)):
            dupes = {n for n in nums if nums.count(n) > 1}
            fail(f"sinksar/{m}.json: duplicate days {sorted(dupes)}")
        for d in days:
            if not d.get("entries"):
                fail(f"sinksar/{m}.json day {d.get('day')}: no entries")
            for i, e in enumerate(d.get("entries", [])):
                check_text(f"sinksar/{m}.json d{d.get('day')} e{i}", e.get("text", ""))


def check_scripture() -> None:
    manifest = load("scripture", "nt-manifest.json")
    if not manifest:
        return
    check_version("scripture/nt-manifest.json", manifest)
    books = manifest.get("books", [])
    if len(books) != 27:
        fail(f"scripture: expected 27 books, found {len(books)}")
    keys = [b.get("key") for b in books]
    if len(keys) != len(set(keys)):
        fail("scripture: duplicate book keys in the manifest")
    for meta in books:
        book = load("scripture", f"{meta['key']}.json")
        if book is None:
            continue
        chapters = book.get("chapters", [])
        if len(chapters) != meta.get("chapters"):
            fail(
                f"scripture/{meta['key']}: manifest says {meta.get('chapters')} chapters, "
                f"file has {len(chapters)}"
            )
        expected = list(range(1, len(chapters) + 1))
        if [c.get("chapter") for c in chapters] != expected:
            fail(f"scripture/{meta['key']}: chapters are not contiguous 1..N")
        for c in chapters:
            verses = c.get("verses", [])
            if not verses:
                fail(f"scripture/{meta['key']} ch {c.get('chapter')}: no verses")
            ns = [v.get("n") for v in verses]
            if ns != sorted(ns) or len(ns) != len(set(ns)):
                fail(f"scripture/{meta['key']} ch {c.get('chapter')}: verse numbers unsorted/duplicated")


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
