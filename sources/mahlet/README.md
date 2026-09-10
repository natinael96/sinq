# Merged Mahlet — book-first editorial edition

Start with `months/`: 13 readable monthly JSON files. The EOTC book is the default order; Telegram supplies separately identified versions and missing services/feasts. Nothing is appended to the middle of a book service merely because it mentions the same saint.

## Simple structure

Each month contains `feasts`. Each feast has `name`, `day`, `calendar`, and `orders`. Each service inside `orders` contains:

- `book`: the primary book order, when available, with ordered `chants`.
- `versions`: distinct Telegram candidate orders. Select a version; **do not concatenate versions or append them to the book order**. A version may be a source fragment, not a complete replacement. `part`, when supplied, is preserved.

Each chant has `form` and `text`, and sometimes a composition `title`. Unknown forms are `null`, not guessed. Book composition headings and their following text are one item. Source rubrics and translation paragraphs already inside the Mahlet book are retained; no separate Amharic/Tigrinya Melke books were imported.

Book alternatives have `alternative_to`, pointing to the preceding chant, and `scope: needs_review`. This means **OR**, not a subsequent compulsory chant. Their exact boundaries require editorial confirmation. Telegram’s existing nested `or` groups remain unchanged. Never flatten either kind into a compulsory sequence.

## Merge rules

- Match reviewed names within the same month; reject conflicting known dates. A unique same-name book feast can accept an undated Telegram record, with the original null date retained in `telegram_names`.
- The book remains primary. Explicit Telegram dates fill two undated book feasts and are marked `date_source: telegram`; ambiguous printed dates are not overwritten.
- Dated Telegram-only feasts are source-reported additions, not independently calendar-verified facts. Undated unmatched records and incomplete names are held out.
- Exact typography-equivalent Telegram editions with the same structure and part are consolidated, retaining all source links and titles. Letters, repetition counts, headings, ordering, and OR structure are not normalized away.
- Same-structure editions with at least 97% token similarity are held in review, not deleted or declared equivalent. Differences may be meaningful. The first retained source edition is a representative, not a preferred liturgical wording.
- Different versions remain separate. `comparison.shared` records form-and-text matches to book chants; `different_or_additional` means not exactly matched, **not proven new material**. The positions are zero-based depth-first leaf indexes through Telegram `or` groups.
- No fuzzy name matching, cross-month merges, automatic stanza insertion, invented dates, or silent sacred-text corrections.

## Calendar and provenance

`month_number` is the Ethiopian month number, not the book chapter number. The book omits Yekatit. Movable/seasonal material stays in its book chapter’s month file for navigation only, with `day: null`. This is not a calendar engine; Gitsawe integration has not been implemented.

Book source pointers use zero-based `chapter` and `[start, end)` block ranges. Each chant’s `source_blocks` points into `sources/book.json`; the title is the range’s first block. Each original block is accounted for once, including monthly headings. The raw snapshot preserves spelling, paragraph boundaries, and ambiguous headings exactly even where display structure has been extracted.

`sources/telegram/` contains the unchanged logical source JSON for all 495 editions. `sources/manifest.json` records original workspace paths and original-file SHA-256 hashes. Snapshots are JSON-reserialized, so compare parsed content rather than expecting identical whitespace/file hashes.

## Review and comparison

- `COMPARISON.md`: readable findings and monthly inventory.
- `comparison.json`: counts and every Telegram feast’s routing decision.
- `review/telegram.json`: held-out feasts with their complete source editions and reasons.
- `review/similar-versions.json`: near-repeated editions and the retained version they resemble.
- `review/book.json`: unresolved book dates and alternative boundaries.

This is a lossless, auditable editorial merge with conservative structured parsing, not a fully human-reviewed liturgical publication. In particular, source prose may remain grouped under the preceding form; use raw block references when reviewing chant/rubric boundaries.

Rebuild from the parent workspace with `python3 scripts/merge_mahlet.py`; test with `python3 -m unittest discover -s scripts -p 'test_merge_mahlet.py'`. Original sources and the existing website are untouched. This folder has not been published.
