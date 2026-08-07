# Changelog

All notable changes to Sinq (ስንቅ) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows a pre-release `0.MINOR.PATCH` scheme (PATCH for fixes,
MINOR for features; `versionCode` increments on every release). `1.0.0` is
reserved for the first public release.

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
