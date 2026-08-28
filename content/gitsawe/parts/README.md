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

Fixed Sunday ranges can later normalize to `MonthlyEntry`; movable Sunday rows
fit `SeasonalEntry`. Both already share `GitsaweService` in the app model.

## Part 4 — Athanasius funeral lectionary

`04-athanasius-funeral-lectionary.json` contains 25 sections from scans 415–424.
It is a funeral and memorial lectionary, not a date-driven calendar collection.
Its sequence covers:

- readings according to who died (bishops, priests, deacons, children,
  monastics, adults, and women);
- seven rite chapters;
- prayers at church and burial;
- readings for the 3rd, 7th, 12th, 30th, 40th, 80th/100th, half-year, annual,
  and other memorial observances.

Many sections include `መስተበቍዕ`, a supplication distinct from the scripture
slots. The scripture portions still use the existing Gitsawe reading/service
shape, but this collection needs explicit user selection rather than automatic
calendar matching.

## Part 5 — Bahre Hasab table

`05-bahre-hasab-tables.json` preserves scans 425–427. The three printed pages
are column fragments of one table—not three independent tables. The canonical
`unified_table` joins each corresponding row and contains 15 Ethiopian years
(2001–2015). The three raw `extracted_sections` remain for provenance.

This is reference and computus-validation data, not a lectionary service. It
must not be decoded as `GitsaweService`, and its limited year range must not
replace the app's general Bahre Hasab algorithm.

Run `python3 tools/split_gitsawe_parts.py` to regenerate all four files.
