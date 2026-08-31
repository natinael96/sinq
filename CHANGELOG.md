# Changelog

All notable changes to Sinq (ስንቅ) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows semantic-style releases (PATCH for fixes, MINOR for
features; `versionCode` increments on every release).

## [1.6.1] — 2026-08-31

_versionCode 57 · the journal keeps what you wrote_

### Fixed
- **Journal entries could be lost when the editor closed.** The final save was
  launched from the screen's own `rememberCoroutineScope`, which Compose
  cancels at exactly the moment `onDispose` runs — so the write raced its own
  cancellation and could drop what had just been typed. The last write now goes
  to a process-lived scope, so a save that starts always finishes.
- **Nothing was written until the editor closed.** Switching apps or the system
  reclaiming the process left no record at all, despite a comment claiming
  otherwise. Entries now save a second after typing stops, so leaving the
  screen cleanly is no longer what the text depends on.

### Changed
- **The journal says when it has saved.** A tick appears in the entry header
  once the text is on disk. The editor still has no Save button, but "is this
  saved?" was a question the screen previously gave no way to answer.

## [1.6.0] — 2026-08-31

_versionCode 56 · what is owed, what was promised, what was thought_

### Added
- **አስራት.** A ledger for the tithe: record what you receive and the tenth is
  worked out for you; record what you give and it shows what is still owed,
  over an Ethiopian month or year. The share is adjustable for those who keep
  a different fraction, and amounts are counted in a currency you name. Giving
  beyond the tithe is shown as a surplus rather than a negative debt.
- **ስዕለት.** Vows and pledges, each tied to the feast it was promised on, with
  what was promised set against what has been kept. A one-time vow stops
  reminding once it is fulfilled; a standing one keeps its rhythm.
- **Feast-anchored reminders.** Cadences now include a yearly date and a named
  feast, alongside the weekly, every-other-day and monthly ones. The ወርኀዊ
  በዓላት come from the bundled ስንክሳር itself, so choosing "ቀን ፲፱" and choosing
  "ቅዱስ ገብርኤል" are the same act; movable feasts such as ፋሲካ are computed each
  year from the ባሕረ ሓሳብ.
- **ማስታወሻ — a journal.** Write the day down, browsed a ግእዝ month at a time.
  Every entry keeps the Church's day it was written on — the feast, the fast,
  the day's ግጻዌ — so an old entry reads as more than a date. A "write about
  this" action in the Psalter and the ግጻዌ reader starts an entry linked back
  to the passage. Entries can be locked behind a passphrase, and the journal
  never appears in screenshots or the app switcher.
- **Preparing for ንስሐ.** A kind of entry built to be destroyed: it never
  leaves the device, never enters a backup, and "ንስሐ ገብቻለሁ" deletes it rather
  than filing it away.
- **Choose what a backup contains.** Export is now a checklist rather than
  all-or-nothing. The journal is off by default and asks for your passphrase
  before it can be included — the file itself is plain text, so what happens
  to it afterwards is yours to look after.

### Changed
- **Quiet hours now silence every reminder.** They previously applied only to
  the ringing prayer alarms and the ሕሊና prayer; the nightly, ግጻዌ, ምጽዋት and
  ንስሐ notifications ignored them entirely. A silenced reminder still keeps its
  schedule — only that one occurrence is dropped. Because the silence is now
  total, the setting warns when a reminder is timed inside the window and
  would never arrive.
- **The Reminders page is grouped** — daily, giving, and sound — rather than
  one flat run of rows, and the reminder descriptions now speak in one voice
  instead of three.

### Fixed
- **The text-size control was labelled "የንባብ ፊደል"**, the same as the font
  picker directly above it, leaving the size stepper with no label of its own.
- **"Keep screen on" never showed its explanation**, though one was written
  and translated.
- **The last-backup date** was printed as a raw Gregorian date in an app that
  is otherwise ግእዝ-first everywhere.
- **"1 reminders on."** The English count had no singular form.

## [1.5.2] — 2026-08-31

_versionCode 55 · the alarm asks again_

### Fixed
- **"ጨርሰዋል?" comes back after an alarm.** The 1.5.0 notification rework hung
  that follow-up on the notification's delete intent, which only fires when a
  user clears a notification — not on the app-side cancel behind Dismiss, the
  auto-cancel behind Open, or the 60-second timeout. Only swiping the alarm
  away reached it. Every ending now posts the prompt explicitly, with the
  timeout carried by its own alarm; Snooze stays the one ending that asks
  nothing.

### Changed
- **The ጉዞ page is more compact.** About 80dp less chrome — a tighter heading,
  hero, and section rhythm — so more of the day's habits sit on screen. The
  year heatmap and the habit rows' touch targets are untouched.

## [1.5.1] — 2026-08-31

_versionCode 54 · the home page, restored_

### Fixed
- **The daily-psalms and ጸሎት ዘዘወትር cards are back on the home page.** 1.5.0
  removed the upper bound on the cards' heights to stop large text clipping,
  but each card holds an internal `weight()`; unbounded, those weights grew
  into the whole page and pushed the bottom two cards off a dashboard that
  does not scroll at normal text size. Heights are bounded again and now scale
  with the text instead of being outgrown by it.
- **The streak grid grows with its card.** The heatmap and candle are drawn in
  dp, so a card stretched by a large text size left them adrift in a
  half-empty box; they now scale with the card (capped so the grid cannot
  crowd out the reading beside it).

### Changed
- **The ስንክሳር button says which day it opens.** It is now a filled button
  rather than a list row, and outside today it names the day in view —
  "የሐምල ፰ ስንክሳር" — so moving between days relabels it.
- **The ባሕረ ሐሳብ year card matches the rest of the app.** It used the Material
  primary colour, which resolves to a pale mint in dark theme; it now uses
  Sinq's own deep green and muted ivory like every other hero surface.

## [1.5.0] — 2026-08-31

_versionCode 53 · Play-Store readiness: everything from the pre-flight audit
except the Bible-rights question (deliberately held open)._

### Changed
- **Alarms ring without a foreground service.** The prayer alarm is now an
  insistent alarm-channel notification (alarm-stream sound + vibration looping
  up to 60s) — the `systemExempted` service and both foreground-service
  permissions are gone, and `SCHEDULE_EXACT_ALARM` (≤ API 32) fixes silently
  inexact alarms on Android 12/12L. Snooze, dismiss, and the "done?" follow-up
  are unchanged.
- **The ግጻዌ widget shows today, always, at any size.** No more 19:00 flip to
  tomorrow; the card renders directly (no collection service), adapts its
  rows/kidase/footer to the granted size, can shrink to a slim strip, rolls
  over just past midnight, and stops its refresh alarm when removed. The
  widget picker now has English text on English-system devices.
- **All Amharic UI text addresses the user in the polite plural.** Notification,
  backup, reminder, and prayer-list strings no longer slip into familiar
  masculine singular.
- **What's New is bilingual.** Every release entry now has an Amharic
  rendering; the page follows the app language.
- **A Licenses & sources screen.** Settings now carries the full attribution
  record: scripture (corrected scope), ግጻዌ, both Synaxarium sources (with the
  MIT notice), Wudase Maryam, all five fonts with the full OFL 1.1 text, and
  the app's own Apache-2.0.

### Fixed
- **A corrupted backup can no longer plant later crashes.** Restored reminder
  times are clamped and duplicate added-psalms deduplicated.
- **Search no longer permanently retains ~25–35 MB.** Indexing stops pinning
  parsed books, the book cache became an 8-entry LRU, and all content caches
  release under memory pressure.
- **Small hardening and polish.** Distinct alarm-notification request codes
  (taps can't misroute), negative-numeral and empty-book guards, home cards
  grow instead of clipping at large font scales, Synaxarium red meets light
  contrast, 48dp touch targets for the week-day toggle, memento-mori text
  follows the app language, and "coming soon" wording states facts instead.
- **Dead weight removed:** the Se'atat settings remnants, an unreferenced
  legacy settings screen, and an unused asset file.
- **The version is visible.** The What's New row shows the installed version
  (read from the package, never hardcoded again) and the Settings page closes
  with a quiet version footer. A hosted privacy policy now backs the Play
  listing, and the licenses screen records the Gitsawe transcription's
  open-content release and the Font.et provenance of all three local fonts.

## [1.4.0] — 2026-08-31

_versionCode 52 · the Psalter and the ግጻዌ agree_

### Changed
- **The Psalter now uses the Ge'ez (LXX) psalm numbering everywhere.** Both
  bundled editions were renumbered (`tools/renumber_psalms_geez.py`) so መዝሙር ፶
  is the Miserere and the numbers match every ግጻዌ citation — misbak links no
  longer land one psalm off (the reported 150/151 mismatch). Existing psalm
  bookmarks/highlights keyed by number will shift accordingly.
- **The ግጻዌ page turns with a swipe.** Sliding the page horizontally moves to
  the neighbouring day, and a permanent "ዛሬ" pill button returns to (and marks)
  today's ግጻዌ.
- **Manage-hours no longer offers manual reordering.** The up/down arrows are
  gone; hours keep their canonical order.

### Fixed
- **Prayer-list Marian conclusion spelling.** ጠጣሳት/ጠጣስ corrected to ጳጳሳት/ጳጳስ.
- **Toggling "Full Psalms" in an hour no longer crashes.** The paged reader
  guarded against the section list changing size mid-frame.
- **Release page only offers the installable APK.** The `.aab` (not installable
  on phones) moved off the public release assets into a workflow artifact.

## [1.3.2] — 2026-08-28

_versionCode 51 · Bahre Hasab renders correctly_

### Fixed
- **The Bahre Hasab year card shows real values.** The hero card no longer
  displays raw template text in place of the year, evangelist, and Fasika date.
- **Ge'ez numerals are correct beyond 199.** Years and ዓመተ ዓለም now render in
  proper positional notation (e.g. ፳፻፲፰ for 2018) instead of a run of repeated
  ፻ marks, across the year rail and cycle-value chips.

## [1.3.1] — 2026-08-28

_versionCode 50 · a focused, more accessible reading experience_

### Changed
- **The Gitsawe widget follows the day automatically.** It shows today's
  readings during the day and tomorrow's from 19:00, with a single uncluttered
  page and the restored dawn-and-cross Sinq mark.
- **Long-form readers are easier to use.** Scripture, Synaxarium, and Wudase
  Maryam now share the reading-alignment setting, readable tablet-width limits,
  explicit retry states, and clearer accessibility selection state.
- **Bahre Hasab is now a live year explorer.** A highlighted current year and
  horizontal year rail expose computus values and movable observances for the
  current Ethiopian year plus the next 25 years.
- **Incomplete specialist readers have been removed from the interface.** The
  dedicated Se'atat reader and Athanasius funeral collection no longer appear;
  the app's canonical prayer hours remain unchanged.

### Fixed
- **Small UI controls and widget text are more accessible.** The prayer-list
  remove action now has a full touch target, and compact widget labels meet the
  minimum supported text size.

## [1.3.0] — 2026-08-28

_versionCode 49 · the complete source-backed Gitsawe_

### Added
- **Movable weekday Gitsawe.** Readings for Nineveh, Heraclius, Great Lent,
  Rikbe Kahnat, Ascension, and the Apostles' Fast now follow the Ethiopian
  computus instead of being limited to the fixed calendar.
- **Sunday Gitsawe and mezmur.** Sundays with an unambiguous printed date or
  movable-season rule expose their additional readings and hymns from the daily
  Gitsawe, with valid citations opening directly in Scripture.
- **Athanasius funeral and memorial lectionary.** The Library now includes the
  source's funeral, burial, supplication, and memorial collections.
- **Bahre Hasab reference.** The printed 2001–2015 EC table is available in the
  Library as a historical reference, while live dates continue to use the
  app's computus.
- **Text alignment controls.** Reading settings now offer justified, left,
  right, and centred text across the app's reading surfaces.

### Changed
- **A new Sinq launcher mark.** Seven pieces form the Provision cross, tying
  the app icon to the seven canonical prayer hours and Sinq's name.
- **The complete licensed Gitsawe source is reproducible.** Parts 1–5 retain
  scan provenance, source-preserving splits, importers, and validation tests.
- **Content rights are explicit.** Repository and in-app asset notices now
  distinguish the separately licensed Gitsawe transcription from Apache-2.0
  code and CC-licensed Scripture.

## [1.2.0] — 2026-08-28

_versionCode 48 · the complete fixed-cycle Gitsawe_

### Added
- **Every Ethiopian calendar day now has a fixed-cycle Gitsawe entry.** The 65
  previously missing dates are bundled, bringing coverage to all 366 possible
  month-days, including leap-year Pagumen 6.
- **The evening office is now visible.** Source-backed ሠርክ readings appear after
  ነግህ and ቅዳሴ and are included when sharing the day's Gitsawe. Hidar 28
  remains without ሠርክ because the transcribed source explicitly omits it.

### Changed
- **Printed but malformed citations remain readable without becoming broken
  links.** Sinq preserves their source text while only making validated chapter
  references tappable.
- **The fixed calendar import is reproducible.** A checked-in importer merges
  newly transcribed days and evening offices without replacing existing
  translations or synaxarium notes.

## [1.0.5] — 2026-08-24

_versionCode 45 · quieter alarms, daytime breath prayer_

### Changed
- **Prayer alarms stay in the notification shade.** Ringing reminders no longer
  launch or wake a full-screen activity; Open, Snooze, and Dismiss remain
  available directly on the ongoing notification.
- **Breath prayer follows the waking day.** Its single daily time is now chosen
  randomly from the active mode's Morning prayer time through 21:00, without
  being re-rolled by completed prayers or constrained by later prayer alarms.

## [1.0.4] — 2026-08-24

_versionCode 44 · a clearer Library, an accessible Journey_

### Changed
- **The year's journey can be explored without tiny touch targets.** Every
  available heatmap day now has a spoken date, habit count, fasting context,
  and selected state, while full-size previous and next controls provide a
  comfortable way to move through the calendar.
- **Journey controls explain their state.** The prayer-hours disclosure now
  announces whether it is expanded or collapsed, and the heatmap legend and
  monthly summaries adapt more safely to narrow screens and larger text.
- **Library descriptions follow the interface language.** Wudase Maryam and
  Zewotr supporting copy now appears in Amharic or English as selected.

### Fixed
- **Journey stays on the right day.** Its date advances at local midnight and
  refreshes when the app resumes, preventing an overnight session from writing
  prayer records to yesterday.
- **Year and day selection remain consistent.** Switching heatmap years now
  selects a valid day in the displayed year, and dates before Sinq's data epoch
  are no longer interactive.

## [1.0.3] — 2026-08-24

_versionCode 43 · today's path, given room to be seen_

### Changed
- **Today's progress has a row of its own.** The Home dashboard now gives the
  completion count, candle, Journey status, and ten-week heatmap the full screen
  width instead of sharing a half-width card.
- **Daily readings sit together.** Today's Psalms and ዘወትር ጸሎት / ውዳሴ
  ማርያም appear as compact companion cards, with the latter opening directly to
  the existing daily Wudase reading.
- **The new reading row remains adaptive.** Its cards sit side by side on normal
  phones and stack on narrow screens or with larger accessibility text.

## [1.0.2] — 2026-08-24

_versionCode 42 · the day at a glance, settings with room to breathe_

### Added
- **Focused Settings pages.** Reading, prayer, reminders, and local data now
  have dedicated screens, while the Settings landing page keeps only the eight
  choices people need to scan.
- **Reading comfort controls.** A real prayer-text preview now accompanies a
  safely migrated 16–28sp size scale, four Ethiopic font previews, and compact,
  normal, or relaxed line spacing shared by the app's readers.
- **Editable quiet hours and reminder health warnings.** Start and end times can
  be changed directly, and notification or background restrictions are shown
  while relevant and rechecked when the app resumes.
- **Backup recency.** Successful local backups record their time so Settings can
  show whether the last backup was today, yesterday, or earlier.

### Changed
- **Home is a glanceable dashboard.** Current and next prayer shortcuts replace
  the expanding hour list, all hours remain available in a sheet, and the full
  Gitsawe feast and reading card stays visible alongside today's heatmap and
  Psalter portion.
- **Reminder controls are coherent.** Prayer level uses an explanatory radio
  sheet, alert behavior and sound share one sheet, and the randomized reminder
  is now named የሕሊና ጸሎት (Prayer of the heart).
- **Settings remain green and gold.** The visual identity, selected checks, and
  gold accents are preserved while spacing and navigation become calmer.

### Fixed
- **Home stays current across time boundaries.** The date, suggested prayer,
  daily Gitsawe, progress, and Psalter portion refresh while Home is visible.
- **Settings adapt safely.** Narrow screens, large accessibility text, and wide
  displays reflow instead of truncating localized choices or stretching cards.

## [1.0.1] — 2026-08-23

_versionCode 41 · prayer that meets people where they are_

### Added
- **Five selectable Agpeya prayer levels.** Settings now offers መዝሙር ፶,
  መጀመሪያ, እድገት, ጽናት, and ሙሉ, with a curated, progressive Psalm
  priority for every hour and the larger 7/14/24/full progression at Midnight.
- **Continue with the complete hour.** A reader using a shorter level can reveal
  all Psalms for the current hour without changing the saved default.

### Changed
- **The Gospel remains foundational at every level.** Shorter levels affect
  only the Psalms; every Gospel reading remains present and concludes its hour
  or Midnight watch. The complete level preserves all bundled prayer content.
- **Prayer-level allocation is offline and data-driven.** Psalm priorities and
  the Psalm 50-only choice live in bundled configuration rather than UI code.

## [1.0] — 2026-08-23

_versionCode 40 · the first stable Sinq release_

### Added
- **Persistent Bible highlighting and dependable verse sharing.** Bible verses
  can now keep one of four highlight colours, selected ranges update in one
  atomic operation, and copied/shared passages retain their book or reference.
  Existing Psalm highlights migrate safely to the Amharic 1980 edition.
- **A complete daily checklist.** Sinkisar joins church and prostrations as a
  trackable practice, and the nightly reminder names what remains without being
  suppressed when an hour of prayer has already been marked.
- **Creator attribution.** About now links “Built by Natinael M.” directly to
  `@natinael96` on Telegram.

### Changed
- **The offline Bible now uses Amharic 1980 throughout.** The unused Amharic
  2000 bundle and non-Psalm Ge'ez books have been removed; Ge'ez 1980 remains
  available specifically for Psalms. This cuts the optimized APK substantially
  while keeping the requested reading library offline.
- **Home makes the day's paths visible.** Hours is an early, compact expandable
  card, and Today's Gitsawe uses the same devotional green-and-gold hero
  treatment as Prayer for now.
- **Prayer List is denser and easier to scan.** Prayers use an adaptive two-column
  card grid, with the Marian prayer presented as a distinct concluding element.
- **Reader and Settings headers adapt to narrow screens.** Long titles ellipsize
  cleanly, secondary reader actions live in a compact tools menu, and Settings
  rows no longer crowd or clip their current value.
- **Selection actions are responsive and accessible.** Highlight colours and
  copy/share actions use an inset-safe two-row panel, announce individual colour
  names to screen readers, and dismiss after completing an action.

### Fixed
- **Fasting boundaries are exact.** ጾመ ሐዋርያት ends on ሐምሌ 4 and ጾመ
  ፍልሠታ ends on ነሐሴ 15 without an added day; ጾመ ነቢያት and ዐቢይ ጾም
  retain their existing calculations.
- **Sinkisar search no longer crashes on valid entries.** Same-day results have
  stable unique identities, and Ge'ez text, Ethiopic punctuation, empty queries,
  no-result queries, and result navigation are covered by regression tests.
- **Psalm highlights cannot cross translations incorrectly.** Amharic and Ge'ez
  use separate identities, and switching language clears any pending selection
  before copy, share, or highlight can target different versification.
- **Reminder chains recover reliably.** Breath-prayer and daily reminder alarms
  re-arm independently after firing and after reboot, update, time, or timezone
  changes.
- **Image shares cannot overwrite one another.** Each generated passage card has
  a unique cache file and sharing failures are handled without crashing.

## [0.10] — 2026-08-22

_versionCode 39 · one Scripture, one day's Gitsawe_

### Added
- **The complete Ethiopian Orthodox Bible is now in the Library.** Old and New
  Testaments read from the Amharic 2000 edition, fully offline. Scripture has a
  single home with Bible and Psalms as clear categories instead of two competing
  Library entries.
- **Psalms now carry their proper editions.** Amharic 1980 is the default and
  Ge'ez 1980 is available from the Psalm reader. Misbak remembers its own
  language choice without changing the rest of Scripture.

### Changed
- **የዕለቱ ግጻዌ now reads as a day, not a directory.** The selected Ethiopian
  and Gregorian date leads the page, previous and next days sit within reach,
  and a historical day returns to Today in one tap. Misbak and Scripture are
  distinct devotional preview cards that open into the focused cited passage.
- **Gitsawe continuity is explicit.** Misbak opens in Ge'ez by default and
  carries that choice into its Psalm; Scripture remains Amharic 2000 and leads
  naturally from passage to chapter to book. Existing bookmark and Gitsawe
  routes remain compatible.

### Removed
- The duplicated legacy `psalms.json` and New-Testament-only `content/scripture`
  bundle. All canonical reading content now comes from the unified, reproducible
  80-weahadu edition bundle.

## [0.9.9.8] — 2026-08-15

_versionCode 38 · the whole week fits_

### Fixed
- **The "how often" dialog no longer cuts off half the week.** Saturday and
  Sunday were being clipped off the right edge of the day row, so they could not
  be picked at all, and "Monthly" was squeezed into a column one letter tall.
  The cadence chips now wrap to a second line instead of being crushed, and each
  day takes an equal share of the row — all seven are visible and tappable on
  any screen width. Affects both the ምጽዋት and ንስሐ reminders, which share the
  dialog.

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
