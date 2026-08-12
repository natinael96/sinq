# Liturgical Correctness Review

Software tests prove the code does what it was told. They cannot prove it was
told the right thing. This document is the process for establishing that Sinq's
liturgical rules are *actually correct* — and the running record of which rules
have been verified, by whom, against what.

**Nothing in this document may be resolved by reading the existing code.** The
code is what is being checked.

## Status

| # | Rule | Status | Verified against | Date |
|---|---|---|---|---|
| 1 | Great Lent — whole-week vs Sunday-only reading | ⛔ **Unverified** | — | — |
| 2 | Resurrection season — Sunday-only matching | ⛔ **Unverified** | — | — |
| 3 | Pentecost (ጰራቅሊጦስ) reading | ⛔ **Unreachable** | — | — |
| 4 | Fixed-season readings (24 of 43 entries) | ⛔ **Unreachable** | — | — |
| 5 | ሰንበት-marked monthly readings — weekday gating | ⛔ **Unverified** | — | — |
| 6 | ጾመ ነቢያት length (43 vs 44 days) | ⛔ **Unverified** | — | — |
| 7 | Fasika / Nineveh / movable feast anchors | ✅ Verified | Computus cross-check; Fasika 2018 EC = 2026-04-12 | 2026-08-10 |
| 8 | Ethiopian ↔ Gregorian conversion | ✅ Verified | Round-trip 1900–2100, Millennium anchor | 2026-08-08 |
| 9 | Weekly ረቡዕ/ዓርብ fast, lapsing through the ኃምሳ | ⚠️ Implemented, unverified | — | — |
| 10 | Fixed fast dates (ፍልሰታ, ነቢያት, ገሃድ) | ⚠️ Implemented, unverified | — | — |

Legend: ✅ verified · ⚠️ implemented but not checked against a source ·
⛔ known defect or unverified rule

## Process

For each rule, in order. Do not skip step 1.

1. **Identify the authoritative source.** A printed መጽሐፈ ግጻዌ, the ባሕረ ሐሳብ
   literature, or a ruling from a diocesan liturgical authority. Record the
   edition and page. A website is acceptable only if it reproduces one of these
   and is named.
2. **Document the rule** in this file, in the church's own terms, before looking
   at the code.
3. **Compare** the documented rule against the implementation.
4. **Correct the implementation** if it diverges. The rule wins, not the code.
5. **Add a regression test** that encodes the *verified* rule, citing the source
   in a comment so a future reader knows what the assertion rests on.
6. **Document the edge cases** — leap years, the ጳጉሜን boundary, a fast that
   straddles the new year, a feast that collides with a Sunday.
7. **Validate the full annual cycle** — walk every day of at least one Ethiopian
   year and confirm each day resolves to the expected season, fast, and readings.

## Open questions

### 1–2. Lent vs Eastertide week matching
`BahreHasab.movableSeasonOn` matches **every day** of a Great Lent week to that
week's seasonal reading, but matches the Resurrection season on **Sundays only**.
One of these is wrong; they cannot both be right.

*History:* the Lent branch previously carried dead sub-expressions implying
Sundays-only while always evaluating true. The dead code was removed in v0.5.2
**without changing behaviour**, precisely so this question could be settled from
a source rather than by inference.

**Question:** are the seasonal ግጻዌ readings for ዘወረደ … ሆሣዕና read on the Sunday
only, or through the following week?

### 3. Pentecost falls outside the window
Pentecost is offset 118 from the Nineveh fast; the `tnsae` branch matches
`69..117`, so it never fires. `seasonal-gitsawe.json` contains a `tnsae` week 11
entry that may be the Pentecost reading, or may be an extraction artifact.

**Question:** does ጰራቅሊጦስ carry its own seasonal reading, and is that the week-11
entry?

### 4. Unreachable fixed seasons
24 of 43 seasonal entries use keys `movableSeasonOn` never emits — `astemhro`,
`filseta`, `genaTsom`, `kremt`, `lidet`, `pagumen`, `zere_demena`. These are
fixed-calendar seasons; matching for them was deferred, never implemented.

**Question:** what are the exact calendar spans for each, and do their readings
take precedence over the daily reading?

### 5. ሰንበት-marked monthly readings
`monthly-gitsawe.json` has an entry raw-keyed `ዘመስከረም፡ ፳፭ ሰንበት`, offered on
Meskerem 25 regardless of weekday, though the raw text says ሰንበት.

**Question:** should such entries only appear when the day is a Sunday?

### 6. ጾመ ነቢያት length
Implemented as ኅዳር ፲፭ → ታኅሣሥ ፳፰, which is **44 inclusive days**; the fast is
commonly quoted as **43**. The endpoints are well attested and are what the code
asserts; the headline number is not.

**Question:** which endpoint is exclusive, or is one of the dates different?

## Content gaps

These are *missing content*, not wrong rules. Do not fabricate replacements.

- **Yekatit 3 (ስንክሳር)** — the source carried a mislabeled duplicate day whose
  entries were Hamle 29's; both were removed in v0.5.2 rather than shipped
  wrong. The day now renders the empty state. A trusted text for Yekatit 3 is
  needed, ideally through `tools/extract_sinksar.py` rather than by hand.
- **Ge'ez ስንክሳር** — the bundled data has Amharic only; there is no Ge'ez field.
  Adding it needs a source and a schema change (`textGeez` per entry), then a
  regeneration.

## Rules already verified

**Fasika and the movable anchors** (rule 7) — `orthodoxEaster` uses Meeus's
Julian computus plus the Julian→Gregorian offset. Cross-checked independently:
Fasika 2018 EC = 2026-04-12, ጾመ ነነዌ = 2026-02-02 (a Monday, as required),
ዐቢይ ጾም = 55 days ending the eve of Fasika. Covered by `BahreHasabTest` and
`FastingCalendarTest`.

**Ethiopian ↔ Gregorian conversion** (rule 8) — verified by round-tripping every
day from 1900 to 2100 (73,000 days, zero failures), plus the Millennium anchor
(2007-09-12 = Meskerem 1, 2000 EC), the ጳጉሜን 5/6 boundary, and the leap rule
(EC year % 4 == 3). Covered by `EthiopianDateTest`.
