# Content source and rights record

Reconciled **2026-09-16** against the repository's [NOTICE](../NOTICE), bundled licenses, generators and source records. This document records the project's existing attributions and unresolved provenance; it does not newly grant rights or independently establish ownership of upstream material.

## Code and content are separate

The source code uses [Apache-2.0](../LICENSE). Bundled content under `app/src/main/assets/content/` does not inherit that grant. The authoritative repository attribution record is [NOTICE](../NOTICE), with an in-app counterpart in `ui/settings/LicensesScreen.kt`.

| Material | Recorded source and terms | Current qualification |
|---|---|---|
| Prayer-hour Scripture | EOTCOpenSource/80-weahadu; CC BY-NC-ND 4.0 | `extract_content.py` uses legacy `data/am`; mapping is in `sources/hours/hour_mapping.json` |
| Library Scripture | Same project; full Amharic 1980 Bible and Ge'ez 1980 Psalms | Catalog records source commit `5bd3bf31092b22fc3e26dc4b330778448773ebce`; digitizer's license alone does not settle underlying translation provenance |
| Gitsawe | Maintainer's scan/transcription; NOTICE records CC BY-NC-ND 4.0 | Replaces the older “separate private permission required” description; source preserved under `sources/gitsawe/` |
| Synaxarium | Maintainer's Amharic/Ge'ez scanned editions, 366 days each; NOTICE records CC BY-NC-ND 4.0 | Earlier bot/Hugging Face corpora are no longer bundled; their MIT notice is not the license for the current Synaxarium |
| Mahlet | Maintainer's book scan, EOTC Mahlet Telegram editions, Tsige sources and Gitsawe fallback orders; NOTICE records CC BY-NC-ND 4.0 | Preserve edition-specific source links, alternatives and outstanding editorial/source review |
| Church-book shelf | Maintainer's scanned traditional texts; NOTICE records CC BY-NC-ND 4.0 | 37 app books; preserve source and transformation records |
| Wudase/daily prayers | `tecleet/wudase-mariam`, no source-repository license stated in the existing record | Traditional prayer provenance does not by itself close transcription-specific rights questions |

The maintainer's 2026-07-28 decision was to keep Sinq noncommercial while these texts ship: no ads, in-app purchases or subscriptions. The repository records the Bible license transition from MIT (`c419216`) to CC BY-NC-ND (`f1b2786`). This audit did not recheck those upstream commits online.

## Text transformations and provenance

`extract_content.py` selects and arranges psalms/gospels and strips surrounding whitespace. `psalm118_clean()` separates acrostic letters from stanza-final verse text into headings. The existing attribution discloses this change; upstream correction of that encoding remains an unresolved follow-up, not an approved action recorded here.

`extract_bible_editions.py` preserves the source JSON values while minifying whitespace. Psalm labels follow Ge'ez/LXX numbering. Gitsawe imports preserve source pages, alternate readings and malformed citations rather than inventing links. The current import pipeline has four import scripts (months, Part 2, Part 3, Part 5); there is no Part 4 importer in this checkout.

The current Synaxarium generator removes duplicate Arabic paragraph numerals after checking agreement with Ge'ez numerals, removes specified scan artifacts and adds date headings to two Ge'ez days without source headings. These transformations are disclosed in NOTICE.

Mahlet keeps the book spine, independent Telegram editions and explicit alternatives separate. Review files under `sources/mahlet/review/` are not proof of publication approval. Corrections must retain auditable source references.

## Fonts

| Font | Repository record |
|---|---|
| Abyssinica SIL | SIL OFL 1.1; [license](AbyssinicaSIL-OFL.txt) |
| Noto Sans Ethiopic | SIL OFL 1.1; recorded in NOTICE and in-app licenses |
| Ethiopic Abay Light | [Font notice](fonts/Ethiopic_Abay_Light-license.txt) |
| Bela Bereka | [Font notice](fonts/Bela_Bereka-license.txt) |
| Zemenay | [Font notice](fonts/Zemenay-license.txt); existing record notes a discrepancy between embedded ETHL metadata and distributor OFL terms |
| Waldba | [Font notice](fonts/Waldba-license.txt); bundled specialist face |

The four selectable reader fonts are Abyssinica, Abay Light, Bela Bereka and Zemenay. Noto provides interface typography and Waldba is separately bundled. Do not infer a fifth selectable reader font from its asset presence. See [font subsetting](FONT_SUBSETTING.md) for technical handling.

## Reminder sounds

The current alarm implementation uses Android ringtone/notification/alarm sound choices through `RingtoneManager` and notification channels. There is no `app/src/main/res/raw/` sound directory in this checkout. The old “bundled sound 1/2” blanks were planning placeholders, not missing attributions for shipped audio files.

## Outstanding approvals

- [ ] Record the underlying Bible translation's provenance/rights evidence, separately from the digitizer's license and edition label.
- [ ] Record the fluent reviewer, role, approved source edition and hour-mapping/text review scope.
- [ ] Record the outcome of any upstream Psalm 118 encoding correction.
- [ ] Resolve transcription-specific source questions, including the unlicensed Wudase source and the mixed Mahlet provenance where necessary.
- [ ] Resolve the Zemenay embedded-license/distributor discrepancy rather than treating this document as new clearance.

No reviewer name, source permission, legal sign-off or publication status was invented in this refresh. See [liturgical review](LITURGICAL_REVIEW.md) for text/calendar approval and [project status](PROJECT_STATUS.md) for technical evidence.
