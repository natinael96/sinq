"""Bundle the Amharic Bible and Ge'ez Psalms as minified JSON assets.

Reads : ../80-weahadu/data/{am-1980,gez-1980}/
Writes: app/src/main/assets/content/bible/

The upstream schema is preserved verbatim at the JSON-value level, including
headings, poetry lines, cross references, footnotes, alternate verse numbers,
and non-integer verse identifiers. Only insignificant JSON whitespace changes.
"""

from __future__ import annotations

import json
import re
import shutil
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
SOURCE_ROOT = ROOT.parent / "80-weahadu"
SOURCE_DATA = SOURCE_ROOT / "data"
OUT = ROOT / "app" / "src" / "main" / "assets" / "content" / "bible"
FULL_EDITION = "am-1980"
PSALM_EDITION = "gez-1980"


def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def write_minified(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )


def source_commit() -> str:
    try:
        return subprocess.check_output(
            ["git", "-C", str(SOURCE_ROOT), "rev-parse", "HEAD"], text=True
        ).strip()
    except (OSError, subprocess.CalledProcessError):
        return "unknown"


def validate_book(book: dict, edition: str, expected: dict) -> tuple[int, int]:
    if book.get("edition") != edition:
        raise ValueError(f"{edition}/{expected['file']}: edition mismatch")
    if book.get("book") != expected["id"]:
        raise ValueError(f"{edition}/{expected['file']}: book id mismatch")
    chapters = book.get("chapters", [])
    verses = sum(len(chapter.get("verses", [])) for chapter in chapters)
    if len(chapters) != expected["chapters"] or verses != expected["verses"]:
        raise ValueError(
            f"{edition}/{expected['file']}: expected "
            f"{expected['chapters']} chapters/{expected['verses']} verses, "
            f"found {len(chapters)}/{verses}"
        )
    return len(chapters), verses


# Corrections applied to the upstream text on the way through.
#
# The 80-weahadu extraction is very clean — a word-boundary check against the
# other 65 books found nothing wrong in the Amharic Psalter — but a few verses
# carry marks from the scan, and the Ge'ez Psalter lost some verse boundaries.
# Each entry is asserted to apply, so a rebuild against a source that has since
# been corrected fails loudly instead of silently dropping the fix.
#
# Keyed by (edition, book id, chapter, verse).
TEXT_FIXES = {
    # The acrostic letter for the ቤት section, plus debris from the scan, left
    # inside the verse. All 22 letters are already carried as headings, and this
    # one duplicates the heading that stands above verse 9.
    ("am-1980", "PSA", 118, 7): (
        "አቤቱ፥ የጽድቅህን ፍርድ ስማር በቅን ልብ አመሰግንሃለሁ። ቤት ll*",
        "አቤቱ፥ የጽድቅህን ፍርድ ስማር በቅን ልብ አመሰግንሃለሁ።",
    ),
}

# The Ethiopic comma and full stop follow the word they close; the scan put a
# space in front of them in six verses of the Amharic Psalter.
SPACE_BEFORE_PUNCTUATION = re.compile(r"\s+([።፤፥፣])")

# Ge'ez verses that swallowed the verse after them, with the verse marker left
# in the text saying exactly where the boundary was. Splitting on the marker is
# mechanical; the twelve other merged verses in this edition are not, because
# the boundary has to be inferred, and those are left alone.
SPLIT_ON_MARKER = {
    ("gez-1980", "PSA", 5, 6),
    ("gez-1980", "PSA", 140, 4),
}
VERSE_MARKER = re.compile(r"\s*(\d{1,3})፧\s*")


def mend(edition: str, book_id: str, book: dict) -> None:
    """Apply the corrections above to one book, in place."""
    for chapter in book.get("chapters", []):
        n = chapter.get("n")
        rebuilt = []
        for verse in chapter.get("verses", []):
            key = (edition, book_id, n, verse.get("n"))
            text = verse.get("t", "")

            fix = TEXT_FIXES.get(key)
            if fix:
                if text != fix[0]:
                    raise SystemExit(f"{key}: text has changed upstream; correction stale")
                text = fix[1]

            if edition == "am-1980":
                text = SPACE_BEFORE_PUNCTUATION.sub(r"\1", text)

            if key in SPLIT_ON_MARKER and VERSE_MARKER.search(text):
                pieces = VERSE_MARKER.split(text)
                verse["t"] = pieces[0].strip()
                rebuilt.append(verse)
                # split() alternates text, number, text, number, …
                for i in range(1, len(pieces) - 1, 2):
                    rebuilt.append({"n": int(pieces[i]), "t": pieces[i + 1].strip()})
                continue

            verse["t"] = text
            rebuilt.append(verse)
        chapter["verses"] = rebuilt


def main() -> None:
    if not SOURCE_DATA.is_dir():
        raise FileNotFoundError(f"80-weahadu data not found at {SOURCE_DATA}")

    staging = OUT.with_name(OUT.name + ".tmp")
    if staging.exists():
        shutil.rmtree(staging)
    staging.mkdir(parents=True)

    catalog = {
        "schemaVersion": 1,
        "source": "https://github.com/EOTCOpenSource/80-weahadu",
        "sourceCommit": source_commit(),
        "license": "CC BY-NC-ND 4.0",
        "editions": [],
    }

    write_minified(staging / "canon.json", load(SOURCE_DATA / "canon.json"))
    write_minified(staging / "names-am.json", load(SOURCE_DATA / "names" / "am.json"))

    for edition in (FULL_EDITION, PSALM_EDITION):
        source_dir = SOURCE_DATA / edition
        meta = load(source_dir / "meta.json")
        if meta.get("id") != edition:
            raise ValueError(f"{edition}: metadata id mismatch")

        chapters = verses = 0
        selected_books = meta["books"] if edition == FULL_EDITION else [
            book for book in meta["books"] if book["id"] == "PSA"
        ]
        for expected in selected_books:
            source_file = source_dir / expected["file"]
            book = load(source_file)
            mend(edition, expected["id"], book)
            book_chapters, book_verses = validate_book(book, edition, expected)
            chapters += book_chapters
            verses += book_verses
            write_minified(staging / edition / expected["file"], book)

        actual = {"books": len(selected_books), "chapters": chapters, "verses": verses}
        if edition == FULL_EDITION and actual != meta["stats"]:
            raise ValueError(f"{edition}: expected totals {meta['stats']}, found {actual}")

        if edition == FULL_EDITION:
            write_minified(staging / edition / "meta.json", meta)
        catalog["editions"].append(
            {
                "id": edition,
                "title": meta["title"],
                "titleEn": meta["title_en"],
                "language": meta["language"],
                "languageName": meta["language_name"],
                "year": meta["year"],
                "era": meta["era"],
                "stats": actual,
                "scope": "full" if edition == FULL_EDITION else "psalms",
                **({"meta": f"{edition}/meta.json"} if edition == FULL_EDITION else {}),
            }
        )
        print(f"{edition}: {actual['books']} books, {chapters} chapters, {verses} verses")

    write_minified(staging / "catalog.json", catalog)
    (staging / "LICENSE").write_text(
        (SOURCE_ROOT / "LICENSE").read_text(encoding="utf-8"), encoding="utf-8"
    )

    if OUT.exists():
        shutil.rmtree(OUT)
    staging.rename(OUT)
    print(f"Extracted Amharic Bible and Ge'ez Psalms to {OUT}")


if __name__ == "__main__":
    main()
