# Changelog

All notable changes to Sinq (ስንቅ) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows a pre-release `0.MINOR.PATCH` scheme (PATCH for fixes,
MINOR for features; `versionCode` increments on every release). `1.0.0` is
reserved for the first public release.

## [0.9.9.7] — 2026-08-15

_versionCode 37 · the reminder writes itself_

### Changed
- **The opening spinner is gone; the reminder is written instead.** Where the
  splash used to mark its pause with a small spinning ring, *Memento Mori* is
  now inked onto the screen letter by letter under a soft gold nib, then signed
  off with a hand-drawn underline; **Remember Death** and **ሞትን አስብ** settle
  beneath it. The words are drawn in the reading serif, scaled to fit narrow
  screens, and a tap still moves on early. Nothing spins — the wait reads as a
  moment of attention rather than a loading state. With animations turned off
  system-wide the words simply appear already written.

## [0.9.9.6] — 2026-08-15

_versionCode 36 · a reminder for every intention_

### Added
- **ምጽዋት and ንስሐ can hold many reminders now.** Each intention used to be a
  single nudge; now it is a list. Keep as many as you like, each with its own
  name (which rides in the notification so they read apart in the tray), its own
  cadence (weekly, every other day, or monthly on an Ethiopian day) and its own
  time. Your existing reminder migrates in as the first entry, unchanged. They
  are still intentions, not habits — nothing is recorded or shown as done.

### Changed
- **ሰዓታት is tucked away for now.** The bundled digitization is still partial
  (መሐረነ አብ and other portions are missing), so the Library card and its search
  entries are hidden until the text is whole. The reader, route and data stay
  intact for when it returns.

### Fixed
- **Search no longer goes blank behind a stale filter.** A source filter (say
  መጻሕፍት) chosen for one query stayed on for the next; if the new query didn't
  match that source, the results vanished even though other sources had matches.
  The filter now falls back to "All" whenever it names a source the current
  query didn't hit.

## [0.9.9.5] — 2026-08-15

_versionCode 35 · the widget shows where it stands_

### Changed
- **The ግጻዌ widget got its polish.** Each card now carries a page indicator —
  gold for the shown day, muted for the other — baked into the card itself so
  the dots always agree with what's visible; with only one day available they
  disappear along with the swipe. Today is explicitly labelled ዛሬ (symmetric
  with ነገ), a gold "ግጻዌውን ክፈት →" action line names what a tap does, and every
  refresh snaps the stack back to Today so a browse to tomorrow never lingers
  overnight. Tomorrow's card only exists when tomorrow actually has readings;
  an empty today says so honestly instead of letting tomorrow pose as today.

## [0.9.9.4] — 2026-08-15

_versionCode 34 · the real መጽሐፈ ሰዓታት_

### Changed
- **ሰዓታት is now the real book.** The starter text is replaced by a full
  digitization of መጽሐፈ ሰዓታት ዘሌሊት ወዘነግህ (Tinsae Ze-Gubae edition, Addis
  Ababa, ፲፱፻፶፯ ዓ.ም.): the መቅድም on አባ ጊዮርጊስ ዘጋሥጫ, then all 42 sections in
  printed order — the fifteen ስብሐት parts of the night office alternating with
  the biblical canticles and the ምስለ intercessions — 199 Ge'ez–Amharic paired
  lines. It is one office, not four hours, so the reader now reads as one
  continuous scroll: the hour chips are gone, and a ይዘት (contents) sheet
  jumps to any section. Line pairing, the three language modes, tap-to-select
  sharing and the red typo-review flag all carry over; search now normalizes
  the traditional ፡ word separators, so queries typed with plain spaces match
  the printed text in either language.

## [0.9.9.3] — 2026-08-14

_versionCode 33 · ሰዓታት in the Library, and pages of their own_

### Added
- **ሰዓታት (Seatat) in the Library.** The prayers of the hours, Ge'ez-first with
  a line-by-line Amharic translation: each Amharic line sits directly under its
  Ge'ez line — smaller, italic, visually attached — so the page reads as one
  continuous prayer with an inline translation. Three persisted language modes
  (ግዕዝና አማርኛ · ግዕዝ · አማርኛ); hour chips (ጠዋት · ቀትር · ማታ · ሌሊት) that keep
  each hour's reading position; tap-to-select sharing and font sizing like the
  other readers; searchable in both languages from the unified search. A word
  marked `*` in the bundled text renders a small red asterisk — an in-app
  review flag for a spelling that still needs checking (stripped from shares
  and search).
- **Dedicated pages for ምጽዋት and ንስሐ.** Each scheduled intention now opens its
  own page from Settings — toggle, cadence, time, and the next due day in the
  Ethiopian calendar — instead of expanding rows inline.
- **A focused page for every ግጻዌ section.** Tapping ምስባክ, ወንጌል or any reading
  opens only the cited verses in the reading face, with the lectionary role in
  the header and two doors out: open the book, or open the chapter that holds
  the passage.

### Changed
- **Open-ended ግጻዌ citations read to the chapter's end.** A reference with a
  start verse and no end is not a single verse: the passage page shows from
  the start through the chapter's last verse, across all Gitsawe types.

## [0.9.9.2] — 2026-08-13

_versionCode 32 · the streak becomes a journey_

### Changed
- **ጉዞ (Journey) replaces the streak.** The tab, formerly Streak, keeps its
  Amharic name and gains it in English, under a path icon instead of a flame.
  The hero is today's candle — lit when prayer is recorded, waiting when not,
  never sized by history — beside the period's count of distinct prayer days:
  "በዚህ ወር ፳፫ ቀን ጸልየዋል" / "23 days of prayer this month", or during a fast
  "የዐቢይ ጾም ፲፰ኛ ቀን — ፲፮ ቀን ጸልየዋል" / "Day 18 of ዐቢይ ጾም — prayed 16 days".
  Missing a day changes nothing but that day; coming back after a gap shows
  only "ተመልሰዋል — ዛሬ ይጀምሩ" / "You're back — begin today". Home's Today card
  speaks the same line from the same source of truth. Prayer records and
  backups are untouched — the new count derives from the same day records.
- **The year heatmap is the historical view.** Promoted under its own header,
  with a quiet liturgical-green wash on unprayed fasting days and the fast
  named in the tapped-day readout — prayer history read inside the Church's
  year. Per-habit rows now count distinct days this month; current/longest
  runs are gone.
- **The nightly nudge stops carrying a number.** "ሰርክ ደርሷል — ዕለቱን በጸሎት ዝጉ"
  every night; "ጾሙ በጸሎት ይታጀብ" on a fasting day; the feast named on a feast.
  The wording never depends on history — nothing at stake, nothing to lose.
- **The ringing alarm names its hour** — "ሰርክ ደርሷል" over the generic
  "ጊዜው ደርሷል", on the full-screen alarm and the notification title.

### Added
- **Passage sharing from every reader.** A tap anchors a verse run, the next
  tap moves its end; a slide-up bar offers copy, share as text, or share as an
  image card — now also in ስንክሳር, ውዳሴ ማርያም and the New Testament reader.
- **የመሃል ጸሎት — the in-between prayer.** Once a day, at a random moment
  between the last recorded prayer and the next scheduled one, a notification
  carrying one short prayer in full. Praying, not reading: nothing to open.
- **ምጽዋት and ንስሐ reminders.** Scheduled intentions, deliberately not habits —
  the app reminds and then looks away; weekly, every other day, or monthly on
  an Ethiopian month day.
- **The ግጻዌ widget carries tomorrow.** A two-card stack: today's readings,
  and tomorrow's for preparing.

### Removed
- **The streak, everywhere.** No current or longest streak, no flame icon, no
  streak share card (passage sharing carries the meaning now), no "streak
  lost" state — and no setting to bring any of it back.

## [0.9.9.1] — 2026-08-13

_versionCode 31 · the streak nudge fires every night_

### Fixed
- **The nightly streak reminder now behaves like the morning ግጻዌ one.** It fires
  every night while enabled — it used to skip any day that already had a log,
  which meant it stayed silent on exactly the nights you were paying attention
  and so read as broken. And where the ግጻዌ nudge carries the day's reading, this
  one now carries the streak at stake: "የ5 ቀን ጉዞህ እንዳይቋረጥ — የዛሬን መዝግብ" /
  "Keep your 5-day streak going — log today."

## [0.9.9] — 2026-08-13

_versionCode 30 · prayer list, sharing, and the day's psalm on Home_

### Added
- **የጸሎት ዝርዝር — a prayer list.** The people you remember in prayer, each with an
  optional intention, reached from the Home header. Plain rows, local like
  everything else, included in backups (older backup files still restore).
- **Sharing from the ግጻዌ and ስንክሳር readers.** Copy, share as text, or share as a
  rendered image card carrying the passage with its source and date.
- **The day's ቅዳሴ on the widget**, one quiet line under the readings.
- **Choose the streak reminder's time.** A ሰዓት row under the toggle in Settings
  opens the same clock the prayer-mode editor uses; saving re-arms the alarm
  immediately. Default unchanged at 21:30.
- **Two smaller reading sizes** — 13 and 15sp below the old floor of 17. Your
  saved size is unchanged.

### Changed
- **Home names the day's psalm.** የዕለቱ መዝሙረ ዳዊት sits beside Today as two equal
  cards, naming today's portion by the traditional weekday division; Sunday,
  which has no fixed portion, offers the whole Psalter. The library shortcut
  row is gone — መዝሙረ ዳዊት, ዘወትር ጸሎት and ውዳሴ ማርያም live on the Library tab. The
  Today card keeps the count, flame and heatmap; the per-habit dots now live
  only on the Streak screen it opens.

### Fixed
- **Prose no longer renders inside the አርኬ.** The mirror of 0.9.8's fix: after
  an explicit አርኬ line, everything to the end of the entry stayed hymn verse —
  22 lines across the year, including an entire second commemoration on ጥር ፲.
  Lines opening with formulas only prose uses (በዚችም ቀን…, ለእግዚአብሔርም ምስጋና…,
  the month colophon) now return to narrative, and a following ሰላም salutation
  reopens the hymn. All 366 days verified: no prose inside hymns, no hymn left
  in prose, and the 11 days without a hymn confirmed hymn-less in the source.

## [0.9.8] — 2026-08-13

_versionCode 29 · the አርኬ reads as a hymn again_

### Fixed
- **The ስንክሳር's አርኬ ran on as part of the saint's life.** The hymn was only set
  apart when the source wrote the word አርኬ on its own line, and much of the data
  never does — the entry goes straight from the closing benediction into the
  salutation. Those hymns were rendered as one more paragraph of narrative. It
  is worst in the last two months of the year: ነሐሴ labelled 12 of its hymns and
  left 44 unlabelled, and ጳጉሜን labelled none of its 13. A salutation opening
  with ሰላም and carrying the Ge'ez clause stops ፡፡ or ። now begins the hymn and is
  given the heading the source omitted. 815 → 880 entries across the year read
  correctly; ነሐሴ 12 → 53 and ጳጉሜን 0 → 12. Ordinary prose is untouched — the
  clause stops are what distinguish the sung salutation from the plain word ሰላም.

## [0.9.7] — 2026-08-13

_versionCode 28 · the nightly nudge actually arrives_

### Fixed
- **The nightly streak reminder never appeared.** The schedule was never the
  problem — it is armed on app open, on the toggle, and on boot, and the alarm
  fired on time. The notification was dropped by the system: `POST_NOTIFICATIONS`
  is a runtime grant on Android 13+, and the app asked for it in exactly one
  place, the prayer *mode* editor. Switching the reminder on from Settings armed
  an alarm that fired into nothing. This is why it looked selective — a prayer
  alarm rings and shows a full-screen intent without the grant, so only the
  notification-only reminders (the streak nudge and the ግጻዌ nudge) vanished.
  Switching either on now asks for the permission, and a banner above the toggles
  links to the system settings page when notifications are blocked outright —
  which the prompt alone cannot fix, since Android stops offering it after two
  denials.

## [0.9.6] — 2026-08-13

_versionCode 27 · a quieter Home_

### Removed
- **"እንደተነበበ ምልክት አድርግ" (Mark as read), and the prayer progress it fed.** Marking
  each section off by hand turned praying into a checklist. Gone with it: the
  progress bar in the reader, the "Resume where you left off" row on Home, the
  "3 of 12" counts on the hours list, and the ✓ on a finished hour. Streaks are
  unaffected — the "ጨርሰዋል?" prompt after an alarm still records the hour.
- **The attribution line under ውዳሴ ማርያም.** The page carries the prayer and
  nothing else. The digitization is still credited in NOTICE and README.

### Changed
- **The hours list on Home starts collapsed.** The prayer for now is already on
  screen as a card; the full list is something you go looking for.
- **The Today block is smaller** — smaller dots, a tighter heatmap over ten weeks
  instead of fourteen, less space around both.
- **Section headings are legible as headings** — 15sp bold rather than a 13sp
  label, and collapsible headings now match plain ones instead of reading as a
  lesser rank with a chevron.

### Fixed
- **Three light-theme contrast failures.** The unchecked habit dot (2.89:1) and
  the unset bookmark icon (2.29:1) sat under the 3:1 an icon control needs, and
  the current-hour badge's gold-on-gold reached only 4.20:1 against a 4.5:1 bar.
  Now 3.19, 3.33 and 4.57. Dark theme already passed throughout.
- **The ስንክሳር data test.** It asserted 365 days and 1817 entries against data
  that 0.9.1 re-extracted to 366 and 2308, so master and the v0.9.1 tag both
  shipped a failing build. The totals were stale, not the data: 366 is the whole
  fixed-calendar book — twelve months of thirty days plus all six of ጳጉሜን. The
  test now also checks the manifest agrees with each month file, and that a month
  covers its days with no gap and no repeat.

### Internal
- CI no longer runs on every push and pull request; it is manual-only from the
  Actions tab. Releases are unaffected and still gate themselves on content
  validation, the unit tests and lint before building anything.

## [0.9.1] — 2026-08-12

_versionCode 26 · a ስንክሳር for the whole year_

### Added
- **ስንክሳር for the seven months that had none.** ጥር, የካቲት, መጋቢት, ሚያዝያ, ግንቦት, ሰኔ
  and ሐምሌ carried only a day heading and a feast list — over half the year opened
  to nothing to read. They now hold the full daily commemorations — some 650 new
  entries, re-extracted from the `Nexuss0781/synaxarium` dataset by the new
  `tools/extract_sinksar_hf.py`.
- **The closing ጸሎት reads in Amharic.** Tapping the salutation that ends each
  day's ስንክሳር switches between the Ge'ez verses and their Amharic rendering. The
  last two stanzas have no Amharic counterpart and stay in Ge'ez. The choice is a
  reading aid for the moment, not a saved setting.

### Changed
- መስከረም, ጥቅምት, ታኅሣሥ, ነሐሴ and ጳጉሜን now come from the same source as the rest of
  the year, for one consistent voice throughout. Their readings are more
  condensed than in 0.9.0: several saints who had a full entry of their own are
  now commemorated within the day's narrative instead.

### Known issues
- Four months repeat a day from the source data: ጥር 21 repeats ጥር 20, መጋቢት 29
  repeats መጋቢት 28, ሚያዝያ 4 and 6 repeat 3 and 5, and ሐምሌ 21 repeats ሐምሌ 20.
- ሕዳር is untouched: it keeps the original extraction rather than the new source,
  because that source is not the daily synaxarium for this month but a single
  ድርሳነ መስቀል homily repeated across all 30 days. (Corrected in 0.9.6: this entry
  previously implied the homily had shipped. It never did — the bundled ሕዳር is
  and always was the daily synaxarium.)

## [0.9.0] — 2026-08-12

_versionCode 25 · prayer progress, search, quiet hours, and a design system_

### Added
- **Your place in an hour is kept.** Each section in the prayer reader has a
  read toggle, a thin bar under the app bar shows how far through the hour you
  are, and Home offers to resume an hour you started but didn't finish. Marks
  are held against the section itself, so reordering or hiding sections never
  loses them, and the day resets at midnight — an hour is prayed anew each day.
- **Home marks the current hour** with a "now" badge and shows how much of each
  hour you have read.
- **Search understands references.** Typing "መዝሙር 23", "ሉቃስ 10", "Luke 4" or
  just "23" jumps straight to the page. Results are grouped by where they came
  from, in a fixed order, with a filter row showing each source's match count —
  so one prayer match is no longer buried under scripture hits.
- **Restoring a backup shows you what will happen first.** Choosing a file now
  reports when it was made, what it holds, and how much of that is actually new
  on this device, before anything is written. A file from a newer version of the
  app is refused rather than half-applied.
- **Quiet hours** silence reminders overnight without switching them off. The
  alarm still re-arms while silent, so tomorrow's reminder is never lost.
- **Today's ግጻዌ on Home** now names the day's reading and feast, instead of
  being a label with an arrow on it.

### Changed
- **A shared design system.** One set of spacing, corner radii, icon sizes,
  cards, rows, headers and app bars across every screen, so the prayers,
  Psalter, scripture, ግጻዌ, ስንክሳር and ውዳሴ ማርያም read as one application rather
  than as neighbours. Recorded in `docs/DESIGN_SYSTEM.md`.
- **The readers are quieter.** Section titles are properly centred, bookmarking
  sits at the head of a section and marking-as-read at its foot, so nothing
  stands between the title and the first verse. Verse spacing now grows with the
  text instead of collapsing at the largest size, and lines stop widening past a
  comfortable measure on a tablet.
- **The dark theme was designed on its own terms** rather than inverted: a
  warmer, lower-glare ground for night prayer, surfaces that step up gently, and
  highlight colours retuned so they stay distinguishable over green.
- **Motion throughout** — expanding sections, selection, sheets, page
  transitions — kept short and small, and switched off entirely when the system
  animation setting is off.
- **Settings is grouped** into Reading, Prayer and reminders, Your data, and
  More, instead of one long list.
- **Backup and restore failures explain themselves**, including whether your own
  data was affected. They no longer report a bare "failed".
- The gold accent is deeper on the light theme. It carries nearly every small
  label in the app and only reached 3.1:1 on the ivory ground; it now clears
  WCAG AA.

### Fixed
- **Amharic labels across Home, ግጻዌ and search were rendering in the device's
  font**, not the bundled one, and with Latin letter spacing that pulled Ethiopic
  syllables apart. Four undefined text roles were falling through to Material's
  defaults.
- **Dark-theme controls were painted from Material's stock palette** — switches,
  chips, bottom sheets, dropdown menus and the date picker all drew from colours
  the app had never defined, and switches were dark-on-dark.
- The heatmap's year arrows were unlabelled; a screen reader announced two
  anonymous buttons.
- The bookmark remove button was labelled in Amharic even in English.

## [0.8.3] — 2026-08-10

_versionCode 24_

### Changed
- **The opening is quicker** — memento mori now holds for about 1.3 seconds
  instead of 2.2, with a faster fade, and a small spinner sits beneath the
  words. A tap still skips straight through.

## [0.8.2] — 2026-08-10

_versionCode 23 · backup & restore_

### Added
- **Backup and restore** in Settings. Your streak history, bookmarks and
  highlights save to a file through the system file picker and come back on
  restore. Restoring **merges** rather than replaces — a day marked done in
  either the file or on the device stays done — so bringing back an old backup
  can never erase newer progress. Nothing is uploaded anywhere; the app still
  has no internet permission.

## [0.8.1] — 2026-08-10

_versionCode 22 · አጽዋማት_

### Added
- **The fasting calendar**, from a button beside search on Home. It shows what's
  in effect today — a named fast with which day of it you're on, or the ረቡዕ/ዓርብ
  rule — then every fast of the Ethiopian year with its dates and length:
  ጾመ ነነዌ, ዐቢይ ጾም, ጾመ ሐዋርያት, ጾመ ፍልሰታ, ጾመ ነቢያት and ጾመ ገሃድ.

## [0.8.0] — 2026-08-10

_versionCode 21 · search everywhere & reading fixes_

### Added
- **Search now reaches the whole app** — the New Testament, all 1,817 ስንክሳር
  commemorations, and ውዳሴ ማርያም join the prayer hours and Psalter.
- **Copy and share** — a tapped verse can be copied or shared, a ግጻዌ day shares
  its ነግህ and ቅዳሴ readings, and a scripture chapter shares in full. Shared text
  is signed "— ስንቅ".

### Changed
- **A ግጻዌ citation now highlights as one block** instead of a separate box per
  verse, in both the prayer/Psalter reader and the scripture reader.
- **The ስንክሳር closing ጸሎት reads as a coda** — smaller and tightly set, rather
  than claiming a screenful at the end of every day.
- **The About page is tighter**, and the Menbere font is removed.

### Fixed
- Bookmarks open the right section again after you reorder or hide sections in
  an hour; they now follow the section itself rather than its position.
- Finishing the first-run intro no longer re-anchors the navigation graph.

## [0.7.1] — 2026-08-09

_versionCode 20 · memento mori & a roomier Home_

### Added
- **The app opens on _memento mori_** (ሞትን አስብ) — the monastic reminder, held
  for a moment, tap to move on.

### Changed
- **The hours list is collapsible** from its header on Home, with the count
  shown beside it. Expanded by default.
- **The "prayer for now" card is about a third shorter**, and the "Continue"
  strip is gone — so the hours start much higher up the screen.

## [0.7.0] — 2026-08-09

_versionCode 19 · የዕለቱ ግጻዌ widget & typography polish_

### Added
- **የዕለቱ ግጻዌ home-screen widget.** Today's ምስባክ and ወንጌል with the Ethiopian
  date, at a glance; tapping it opens the ግጻዌ screen. It rolls over to the new
  day's readings at midnight, and adds no weight to the app.

### Changed
- **The reading fonts now sit together properly.** The bundled faces are not the
  same size at a given setting — x-heights differ by up to 19% — so pages looked
  bigger, smaller, or unevenly spaced depending on which font was chosen. Every
  face now carries a size correction, and line spacing is applied consistently
  rather than being left to each font's own metrics.
- **Tighter line spacing** on every reading page — prayers, Psalter, scripture,
  ስንክሳር, ውዳሴ ማርያም — so more text fits on screen without crowding it. The አርኬ
  hymn keeps its wider spacing, being verse rather than prose.
- **The font picker is collapsed by default** in Settings, with the current face
  named in the header.

## [0.6.0] — 2026-08-09

_versionCode 18 · choose your reading font_

### Added
- **Reading font setting.** Settings now offers four bundled Ethiopic faces
  beside the Abyssinica SIL default — **Ethiopic Abay Light**, **Bela Bereka**,
  **Zemenay**, and **Menbere**, all from [Font.et](https://www.font.et/) under
  the SIL Open Font License. Each row in the picker is rendered in its own face,
  and the choice applies to every prayer, Psalter, scripture, ስንክሳር, and
  ውዳሴ ማርያም page. Designers are credited in the README and `docs/fonts/`.

### Notes
- The APK grows from 4.3 MB to 6.2 MB for the bundled faces.

## [0.5.2] — 2026-08-08

_versionCode 17 · audit fixes & ዘወትር ጸሎት_

### Added
- **ዘወትር ጸሎት.** The daily opening prayer (ጸሎት ዘዘወትር) — no longer "coming
  soon". It opens from Home and the library as the first section of the
  ውዳሴ ማርያም reader, Amharic with the same Ge'ez toggle, from the same
  credited source.

### Fixed
Five high-severity bugs found by a full feature audit:
- The Bookmarks screen crashed when one group carried two names (hour
  rename, or bookmarks created in both app languages).
- Eight ስንክሳር entries whose አርኬ marker was glued to its verse rendered
  the hymn as numbered prose instead of the red centered verse style.
- Tapping the nightly streak notification could land on Home instead of
  the Streak screen after any prayer alarm re-armed.
- ግጻዌ citations naming chapters that don't exist (e.g. Mark 17) showed
  chapter 1 under the cited title and highlighted the wrong verses.
- Yekatit 3 showed Hamle 29's commemorations — the source carried a
  mislabeled duplicate day; it now honestly shows no entry until a
  trusted text for the day is found.

And a batch of smaller ones, including: downgrade no longer wipes
bookmarks; a psalm bookmarked in the Psalter and inside an hour toggle
independently; the ስንክሳር keeps the source's own list numbering, drops
leftover `<b>` markup, and keys its bookmarks to the Ethiopian date;
scripture citations landing past a chapter's end or on merged verses now
highlight the nearest real verse; the ውዳሴ ማርያም reader scrolls to the
top when switching days; and a failed content load is retried instead of
sticking as an empty screen until restart.

## [0.5.1] — 2026-08-07

_versionCode 16_

### Fixed
- **Synaxarium paragraphs are centered again.** The Ge'ez paragraph numeral sat
  in a left-only gutter that pushed the prose off-center; it now renders inline
  at the head of the paragraph, so the text spans the full width.
- **ውዳሴ ማርያም opens from the Home screen.** The Home library row still showed
  the disabled "coming soon" stub even though the reader shipped in 0.5.0.

## [0.5.0] — 2026-08-07

_versionCode 15 · ውዳሴ ማርያም, synaxarium reader reformat & bookmarkable scripture_

### Added
- **ውዳሴ ማርያም (Wudase Maryam).** The daily Praise of Mary opens from the library
  — no longer "coming soon". One portion per weekday (defaulting to today) plus
  ይወድስዋ መላእክት and አንቀጸ ብርሃን, in Amharic by default with a Ge'ez toggle on top.
  Text from the community dataset credited in-app and in the README.
- **Bookmark scripture.** The scripture-quote entries within the ስንክሳር and the
  library's Bible reader now carry a bookmark toggle; saved passages land in the
  existing Bookmarks screen and reopen where they came from.
- **Closing ጸሎት.** The fixed synaxarium closing prayer is now appended once at
  the end of every day, set apart in its own card, with the holy names in red.

### Changed
- **Synaxarium reader reformatted.** Each commemoration's paragraphs are numbered
  with Ge'ez numerals and given real spacing; the **አርኬ** hymn is set apart as
  centered red italic verse; the editorial emojis that prefixed lines in the
  source (❖ ✍️ 📌 📖 …) are stripped at render time, leaving the Ge'ez untouched.

## [0.4.0] — 2026-08-07

_versionCode 14 · ስንክሳር & daily reminder_

### Added
- **ስንክሳር (Synaxarium).** The full Amharic synaxarium — the daily commemorations
  of saints and events (367 days, 1,822 entries) — opens from a card on the ግጻዌ
  screen and follows the day picker, so any day's synaxarium is a tap away.
- **Daily ግጻዌ reminder.** An optional morning notification with today's ግጻዌ
  reading heading; tapping it opens the day's readings. Toggle it in Settings
  (on by default), scheduled with an exact, doze-exempt alarm.

## [0.3.1] — 2026-08-07

_versionCode 13_

### Added
- Browse the ግጻዌ readings for **any day** — a date picker on the Gitsawe screen
  reloads that day's daily, seasonal, and monthly readings.

### Changed
- The A− / A+ **font-size controls now appear on every reader** (the scripture
  reader included) and stay legible in dark mode — previously they used a deep
  green that vanished on the dark background.

### Fixed
- Opening a psalm from a ግጻዌ reading now **highlights the cited verses** in the
  Psalter, with a stronger, more visible tint on both readers.

## [0.3.0] — 2026-08-07

_versionCode 12 · ግጻዌ, Scriptures library & Bahre Hasab_

### Added
- **ግጻዌ (Gitsawe) — the daily lectionary.** A "የዕለቱ ግጻዌ" card on the home screen
  opens the day's readings — the synaxarium plus the ነግህ and ቅዳሴ offices — with
  every scripture reference tappable through to the text.
- **Daily, seasonal, and monthly offices.** When a date carries more than one
  office, a switcher lets you move between the daily reading, the movable-season
  reading, and the monthly reading.
- **ባሕረ ሓሳብ (Bahre Hasab).** The Ethiopian computus now locates the movable feasts
  and fasts (ጾመ ነነዌ, ዐቢይ ጾም, ትንሣኤ, ዕርገት …), so the correct seasonal readings
  appear during Lent and the Resurrection season.
- **ቤተ መጻሕፍት — a new Library tab** in the bottom navigation, holding the Psalter
  and the full **New Testament** (27 books) with a chapter-by-chapter reader in
  Ge'ez numerals. Gitsawe references open the exact passage and highlight the
  cited verses.

### Fixed
- **Prayer alarms could go silent for days at a time.** The schedule was rebuilt
  only on boot, update, clock change, or a mode edit; an aggressive battery
  manager force-stopping the app left the reminder chain broken until the next
  reboot. Opening the app now re-arms the full schedule, self-healing on launch.
- **The nightly 9:30 PM streak reminder often failed to fire.** It was an inexact,
  doze-deferrable alarm that App-Standby throttling suppressed once the app had
  gone unopened for a day or two. It now uses an exact, doze-exempt alarm.

## [0.2.6] — 2026-07-28

_versionCode 11_

### Added
- Guided tutorial for new users covering reminders, streaks, and the Psalter.
- Settings option to replay the tutorial.

### Changed
- About screen shown in English; recorded CC BY-NC-ND terms for the prayer text.

## [0.2.5] — 2026-07-24

### Added
- Unified search across the prayer hours and the Psalter.
- Nightly streak reminder.
- In-app guidance for granting the notification permission.

## [0.2.3] — 2026-07-24

### Fixed
- Four reminder and content bugs, including alarm deep-linking, crash safety, and
  main-thread I/O.
- Release crash caused by R8 stripping DataStore protobuf fields.

## [0.2.0] — 2026-07-16

### Added
- Full Psalter (መዝሙረ ዳዊት) screen and navigation.
- Share your streak as an image card.
