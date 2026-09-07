# Gitsawe Parts 2–5

These files split the non-monthly collections from `../gitsawe-master.json`.
They preserve the source structure and are deliberately separate from the 13
fixed-calendar month files.

## Part 2 — movable feasts and fasts

`02-movable-feasts-and-fasts.json` contains 49 sections from scans 357–378.
These are selected by the computus rather than an Ethiopian month/day. They
include the weekdays of Nineveh, the Fast of Heraclius, Great Lent themes, the
end of the forty-day fast, Rikbe Kahnat, Ascension, the Apostles' Fast, and a
supplication day. Every section has a title, service data, and scan provenance.

This collection maps naturally to the existing `SeasonalEntry` and
`GitsaweService` models, but weekday identity must be retained when it is later
normalized; several adjacent days have distinct readings.

## Part 3 — Sunday Gitsawe with mezmur

`03-sunday-cycle-and-mezmur.json` contains 91 ordered sections from scans
379–414. It combines two selection systems:

- fixed Ethiopian date ranges or fixed feasts that apply when Sunday falls
  within/on them;
- movable Sunday sequences around Great Lent, Hosanna, Resurrection,
  Ascension, Pentecost, the rainy season, Filseta, and Pagumen.

The book does not print the 11 chapter headings described by its contents page.
The master therefore stores derived chapter boundaries separately from the 91
canonical flat sections. Period, hymn, `gize`, heading, and reading fields are
optional because some rows are continuations, rubrics, or partial services.
They must not be filled with guessed content.

Every row now carries the selector its heading prints, attached by
`tools/import_gitsawe_part3.py`: a fixed Ethiopian date span (plus `spans` for
a rubric naming two dates, such as ዕንባቆም), or a season key with a week. The
computus seasons (`abiyTsom`, `tnsae`, `holy_saturday`) come from
`BahreHasab`; the fixed-anchored ones (`tsige`, `astemhro`, `sibket`, `birhan`,
`nolawi`, `lidet`, `kremt`) from `SundayCycleCalendar`, whose header documents
how each week number is counted. `kremt` weeks are the book's printed hymn
ordinals ፩ኛ–፲፭ኛ, not Sunday ordinals.

One transcription is corrected on import: section 27 reads ፳፫ in the master,
but scan 390 prints "ለታኅሣሥ ፳፰ ቀን መርዓዊ" (አማኑኤል's monthly day).

## Part 5 — Bahre Hasab table

`05-bahre-hasab-tables.json` preserves scans 425–427. The three printed pages
are column fragments of one table—not three independent tables. The canonical
`unified_table` joins each corresponding row and contains 15 Ethiopian years
(2001–2015). The three raw `extracted_sections` remain for provenance.

This is reference and computus-validation data, not a lectionary service. It
must not be decoded as `GitsaweService`, and its limited year range must not
replace the app's general Bahre Hasab algorithm.

Run `python3 tools/split_gitsawe_parts.py` to regenerate all four files.
