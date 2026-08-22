"""Bundle selected 80-weahadu editions as lossless, minified JSON assets.

Reads : ../80-weahadu/data/{am-2000,am-1980,gez-1980}/
Writes: app/src/main/assets/content/bible/

The upstream schema is preserved verbatim at the JSON-value level, including
headings, poetry lines, cross references, footnotes, alternate verse numbers,
and non-integer verse identifiers. Only insignificant JSON whitespace changes.
"""

from __future__ import annotations

import json
import shutil
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
SOURCE_ROOT = ROOT.parent / "80-weahadu"
SOURCE_DATA = SOURCE_ROOT / "data"
OUT = ROOT / "app" / "src" / "main" / "assets" / "content" / "bible"
EDITIONS = ("am-2000", "am-1980", "gez-1980")


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

    for edition in EDITIONS:
        source_dir = SOURCE_DATA / edition
        meta = load(source_dir / "meta.json")
        expected_stats = meta["stats"]
        if meta.get("id") != edition:
            raise ValueError(f"{edition}: metadata id mismatch")

        chapters = verses = 0
        for expected in meta["books"]:
            source_file = source_dir / expected["file"]
            book = load(source_file)
            book_chapters, book_verses = validate_book(book, edition, expected)
            chapters += book_chapters
            verses += book_verses
            write_minified(staging / edition / expected["file"], book)

        actual = {"books": len(meta["books"]), "chapters": chapters, "verses": verses}
        if actual != expected_stats:
            raise ValueError(f"{edition}: expected totals {expected_stats}, found {actual}")

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
                "meta": f"{edition}/meta.json",
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
    print(f"Extracted {len(EDITIONS)} editions to {OUT}")


if __name__ == "__main__":
    main()
