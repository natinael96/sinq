# Content Source & Rights Record
Fill this in before any content entry. No content ships without §3 completed.

## 1. Source edition
- Tradition followed: ☑ Agpeya structure (psalms + gospel per hour only — scope change 2026-06-12)
- Text source: **80-weahadu** open-source Amharic Bible — https://github.com/EOTCOpenSource/80-weahadu
  (JSON; LXX/Geez psalm numbering verified to match Agpeya references)
- Hour→psalm/gospel mapping source: agpeya.org per-hour pages (fetched 2026-06-12) → `content/hour_mapping.json` — reviewer must confirm
- Underlying Amharic translation/edition: ______ (👤 confirm — orthography suggests an older standard translation)

## 2. Rights status
- ☑ 80-weahadu now carries a LICENSE (added upstream 2026-07-28): **CC BY-NC-ND 4.0**
  — commit `c419216` added MIT, immediately superseded by `f1b2786` "Update license from MIT to Creative Commons".
  The operative terms are Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International.
  Those two commits touched `LICENSE` only; the Bible data is byte-identical.
- ☑ Sinq accepts the NC condition — the app is open source and will **not** be commercialized
  (confirmed by maintainer 2026-07-28). No ads, no IAP, no subscriptions, no in-app donations
  for as long as this text ships. There is currently no billing/ads/analytics dependency in the project.
- ☐ Underlying Bible translation rights checked (the repo's text has its own provenance): ______
  ⚠ **Still open.** EOTCOpenSource applying a license does not establish that they hold rights to the
  underlying Amharic translation. Orthography suggests an older standard translation (possibly public
  domain), but this is unverified. Ask upstream which edition the text was digitized from.
- Fallback if unresolved: another public-domain Amharic Bible text, or church-provided text

### 2a. ND compliance — how the pipeline stays inside the license
CC BY-NC-ND permits reproducing the material "in whole or in part" and including it unmodified in a
Collection; it forbids *sharing Adapted Material*. Selecting psalms and gospels per hour and bundling
them in the app is a Collection, and `tools/extract_content.py` copies verse text verbatim
(`v["text"].strip()`) — so the normal path is fine.

One exception to keep an eye on: `psalm118_clean()` rewrites stanza-final verses to peel the acrostic
letter off the end of the verse text, because the source glues it there. This is the only place the
pipeline alters licensed text. It is defensible as a technical correction of a data-encoding artifact
(CC 4.0 §2(a)(4) allows technical modifications), and the change is disclosed in the app's attribution,
but the clean fix is to correct the encoding upstream in 80-weahadu and drop the local transform.
- ☐ 👤 PR filed upstream to fix Psalm 118 acrostic encoding — date: ______ outcome: ______

## 3. Sign-off
- Rights cleared in writing: ☑ yes, for the compilation — upstream LICENSE file at
  https://github.com/EOTCOpenSource/80-weahadu/blob/main/LICENSE (CC BY-NC-ND 4.0).
  ☐ Provenance of the underlying translation — still outstanding, see §2.
- Attribution line for the app About page (shipped in `Strings.kt` → `EnglishStrings.aboutSourceBody`;
  the About page renders in English only, regardless of the app's language setting, so the licence
  notice always reads exactly as worded here):

  > Psalms and gospels are drawn from the 80-weahadu open-source Amharic Bible by EOTCOpenSource
  > (github.com/EOTCOpenSource/80-weahadu), used under the Creative Commons
  > Attribution-NonCommercial-NoDerivatives 4.0 International licence
  > (creativecommons.org/licenses/by-nc-nd/4.0). Passages are selected and arranged into the hours of
  > prayer; the verse text is reproduced unchanged, except that the acrostic letters of Psalm 118 are
  > shown as stanza headings. Provided as-is, without warranties.

  This satisfies CC 4.0 §3(a)(1): creator, licence notice + URI, link to the material, statement of
  changes, and warranty disclaimer.
- ☑ No Amharic translation of the attribution is needed — the About page is English-only by design
  (decided 2026-07-28), which also avoids a translated licence notice drifting from the original wording.

## 4. Reviewer (proofreading)
- Name/role (priest, deacon, fluent reader): ______________
- Agreed scope: reads every section on a phone screen before "approved" status
- Credit line for About page (with their consent): ______________

## 5. Reminder sounds licensing
- Bundled sound 1: ______ source/license: ______
- Bundled sound 2: ______ source/license: ______

## 6. Fonts
- Noto Sans Ethiopic (UI) — SIL Open Font License 1.1
- Abyssinica SIL 2.300 (prayer text) — SIL Open Font License 1.1; license copy saved at docs/AbyssinicaSIL-OFL.txt
- ☑ Both license texts must appear in the app's licenses page — satisfied 2026-08-31:
  an in-app "Licenses & sources" screen was added (Settings → Licenses & sources,
  `ui/settings/LicensesScreen.kt`, route "licenses"). It lists every bundled font
  (Abyssinica SIL, Noto Sans Ethiopic, Ethiopic Abay Light, Bela Bereka, Zemenay)
  with its copyright holder, carries the full OFL 1.1 text verbatim, and also holds
  the scripture/Gitsawe/Synaxarium/Wudase attributions plus the MIT notice for the
  Hugging Face synaxarium dataset. English-only, like the About page, so the legal
  wording never drifts.

## 7. ግጻዌ
- Source: the maintainer's licensed transcription of the printed ግጻዌ. The
  preserved master and structural reference live under `content/gitsawe/`.
- Scope: all 366 fixed Ethiopian calendar days, including Pagumen 6, plus the
  separately preserved movable, Sunday/mezmur, Athanasius funeral, and Bahre
  Hasab collections in Parts 2–5. Morning, liturgy, and evening offices are
  retained wherever present in the source.
- Provenance: `sourcePages` in `daily-gitsawe.json` records the supplied scan-page
  references. The first 40 records have no page metadata because none was
  supplied; no page numbers are inferred.
- Transformation: `tools/import_gitsawe_months.py` maps the transcription into
  Sinq's data model without rewriting the Ge'ez text, keeps alternate readings,
  and leaves malformed printed citations visible but unlinked.
  `tools/split_gitsawe_parts.py` makes source-preserving splits of Parts 2–5;
  `tools/import_gitsawe_part2.py` through `tools/import_gitsawe_part5.py` then
  normalize those splits into the app's movable weekday, Sunday-cycle,
  Athanasius, and historical Bahre Hasab assets. Only unambiguous printed
  calendar selectors are activated automatically.
- Rights: separately licensed to the Sinq maintainer. This content is not
  granted under Apache-2.0 or the Bible's CC BY-NC-ND licence; forks and
  redistributors must obtain their own permission.
