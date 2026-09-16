# Liturgical correctness review

Updated **2026-09-16** for **2.3.2 / 72**. This record separates current implementation evidence from independent source approval. Code and automated tests can establish behavior and structural consistency; they cannot establish church approval.

## Current review register

| Topic | Current code/content evidence | Remaining review |
|---|---|---|
| Prayer-hour mapping | Eight generated Agpeya hours; mapping under `sources/hours/hour_mapping.json` | Name the reviewer and approved edition; confirm mapping, translation and Psalm 118 presentation |
| Great Lent versus Resurrection | `movableSeasonOn` still returns whole-week Lent windows and Sunday Resurrection windows; Part 2 weekdays and Part 3 Sunday selection are separate paths | Confirm each collection's intended scope; the different scopes alone do not prove a defect |
| Pentecost | Current `tnsae` Sunday window is offsets 69–146 from Nineveh; offset 118 is included | Confirm source row/week association; the old “outside 69–117” code defect no longer describes this implementation |
| Fixed-anchored Sunday seasons | `SundayCycleCalendar` is implemented and combined by `GitsaweRepository` | Audit each printed selector and precedence against a named edition/page; old 24-of-43 unreachable counts are not a current audit |
| Sunday-marked fixed/monthly entries | Fixed, monthly and Sunday-cycle collections have distinct selectors | Verify weekday gating for source rubrics such as Meskerem 25 marked ሰንበት |
| Nativity fast length | Endpoint/count interpretation was previously left open | Record the authoritative inclusive/exclusive convention rather than changing dates to match a headline count |
| Weekly/fixed fasts | Implemented in `FastingCalendar` with tests | Source-backed review of exceptions, endpoints and feast collisions |
| Date conversion/computus | Dedicated conversion, Bahre Hasab and fasting tests; prior cross-checks recorded below | Preserve anchor evidence and document any new exceptional rules |
| Mahlet dates and alternatives | App computus and source edition/alternative handling implemented | Review ambiguous dates, chant boundaries, OR scopes and retained near-duplicates in `sources/mahlet/review/` |
| Synaxarium completeness | Validator finds all 366 dates in both Amharic and Ge'ez editions | Proofread the actual entries against scans; day presence is not textual approval |

No reviewer identity or full liturgical sign-off was added during this technical documentation audit.

## Superseded content gaps

The earlier Amharic Yekatit 3 gap and absence of a Ge'ez Synaxarium described the previous source corpus. The current scan-based corpus includes **366 days in each language**, built by `tools/build_sinksar.py`. Do not use the old missing-day list to patch the new corpus. Any discrepancy now needs a citation to the current source scan and generated entry.

Part 3 Sunday selection and Mahlet calendar integration are implemented. Their existence closes the old “not implemented” statements; it does not automatically close editorial approval.

## Review process

1. Identify the authoritative printed edition/page or named liturgical authority.
2. Write the rule in the church's terms before checking the implementation.
3. Compare each affected collection and its precedence/selector logic against that rule.
4. Correct generators or code only with documented evidence; do not fabricate missing content.
5. Add a regression test citing that evidence.
6. Check leap years, Pagumen, year boundaries and feast/Sunday collisions.
7. Walk a complete Ethiopian year and record reviewer, date, evidence and result.

## Previously recorded technical cross-checks

The earlier review recorded Fasika 2018 EC as **2026-04-12**, Nineveh as **2026-02-02**, and the Ethiopian Millennium anchor **2007-09-12 = Meskerem 1, 2000 EC**. It also recorded a 1900–2100 conversion round-trip check and Pagumen/leap-rule coverage. These are retained technical findings, not a new independent liturgical ruling in this audit. Relevant suites include `BahreHasabTest`, `FastingCalendarTest` and `EthiopianDateTest`.

For the current run and corpus totals see [project status](PROJECT_STATUS.md). Record future approvals with reviewer name/role, edition/page, exact rule, test reference, date and remaining exceptions.
