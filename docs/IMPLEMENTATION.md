# Sinq implementation reference

Current as of **2026-09-17**, version **2.4.0 / 74**, release source. For inventory, verification results and outstanding work, see [project status](PROJECT_STATUS.md). The previous deep dive is preserved as a [v0.2.6 archive](archive/IMPLEMENTATION_0.2.6.md).

## Build baseline

| Setting | Value |
|---|---|
| App module / identity | `app` / `com.sinq.app` |
| Kotlin namespace | `com.agpeya.app` |
| Android SDK | minimum 23, target/compile 36 |
| Gradle / AGP | 8.11.1 / 8.10.1 |
| Kotlin / Compose BOM | 2.1.21 / 2025.05.01 |
| JVM bytecode | 11; CI JDK 17 |
| Persistence | DataStore 1.1.6, Room 2.8.4 |
| Serialization / KSP | kotlinx.serialization 1.8.1 / 2.1.21-2.0.2 |
| Desugaring | desugar_jdk_libs 2.1.5 |

The version catalog and `app/build.gradle.kts` are authoritative. There is no runtime content generation and no backend service required for bundled reading.

## Architecture and source map

`MainActivity.kt` hosts the Compose navigation shell, repositories expose flows and suspend operations, and screens use Compose state and coroutine scopes. The code does not use a dedicated application ViewModel layer. Repository responsibilities now extend well beyond the original seven DataStores.

All paths below are relative to `app/src/main/java/com/agpeya/app/`.

| Path | Responsibility |
|---|---|
| `data/ContentRepository.kt` | Bundled hours and Psalter; content cache |
| `data/ScriptureRepository.kt` | Scripture catalog, books, chapters, passages |
| `data/GitsaweRepository.kt` | Fixed/movable/Sunday lectionary, reference collections |
| `data/BahreHasab.kt`, `SundayCycleCalendar.kt`, `FastingCalendar.kt` | Computus, Sunday windows and fasting |
| `data/SynaxariumRepository.kt`, `WudaseRepository.kt` | Bilingual devotional content |
| `data/BookRepository.kt`, `MahletRepository.kt`, `MahletComputus.kt` | Church books and Mahlet |
| `data/SettingsRepository.kt`, `ModesRepository.kt`, `HoursRepository.kt`, `LayoutRepository.kt` | User settings, schedules, custom hours and section overlays |
| `data/HabitsRepository.kt`, `PrayerJourney.kt`, `ReadingPlanRepository.kt` | Journey and reading progress |
| `data/PrayerListRepository.kt`, `OfferingRepository.kt`, `PenanceRepository.kt` | Prayer intentions, giving/vows and penance records |
| `data/JournalDatabase.kt`, `JournalRepository.kt`, `JournalLock.kt` | Room journal and optional access gate |
| `data/UserDataRepository.kt`, `HighlightRepository.kt`, `MarksRepository.kt` | Bookmarks, recents, scroll positions, highlights and combined marks |
| `data/BackupRepository.kt` | Selected export, preview and restore |
| `data/UpdateRepository.kt` | Build-gated GitHub release metadata checks |
| `model/` | Content and user-record contracts |
| `reminders/` | Alarm scheduling, receivers and notification identities |
| `search/` | Amharic homophone folding and search helpers |
| `widget/` | Gitsawe, memento mori and canonical prayer-clock widgets |
| `ui/` | Screens, reader/share helpers, strings, theme and components |

## Persistence and compatibility

Bundled JSON is read-only. Repositories load it from Android assets and cache decoded data. `CacheTrimmer` participates in cache management. Changes to the source corpus belong in the corresponding generator and source records.

Preferences DataStores include `settings`, `user_data`, `highlights`, `layouts`, `hours`, `prayer_modes`, `habits`, `prayer_list`, `offerings`, `penance`, `reading_plan`, `journal_lock` and `updates`. Their data models, defaults and migration behavior live with their repositories.

The journal uses the Room database `journal`, schema version 1. Queries browse by date and Ethiopian month, fetch passage-linked reflections and export entries. There is no journal full-text search. `JournalKind` is persisted by name rather than enum ordinal.

Section IDs, chapter routes, highlight identities and record IDs are compatibility contracts. `tools/section-ids.json` guards the 331 core section IDs. Do not update that snapshot merely to silence a regression after an accidental rename.

Backup format version 3 supports older data through defaulted fields. It covers selected habits/reading progress, marks, prayer list, setup and offerings; journal inclusion is opt-in. Import has a 2 MiB size limit, previews changes and applies repository-specific restoration. Confession drafts are excluded from export. Files written by `BackupRepository.writeTo` are plaintext JSON; the optional journal passphrase does not encrypt the database or export.

## Content generation

See [content structure](CONTENT_STRUCTURE.md) for the schemas and hour inventory and [sources](../sources/README.md) for inputs. Run generators from the repository root only when changing that corpus.

| Generator | Input / result |
|---|---|
| `extract_content.py` | `sources/hours/hour_mapping.json` and sibling `80-weahadu/data/am`; hours, Psalter and manifest |
| `extract_bible_editions.py` | Sibling `80-weahadu/data/am-1980` and `gez-1980`; full Amharic Bible and Ge'ez Psalms |
| `split_gitsawe_months.py`, `split_gitsawe_parts.py` | Preserved Gitsawe master → auditable source splits |
| `import_gitsawe_months.py`, `import_gitsawe_part2.py`, `import_gitsawe_part3.py`, `import_gitsawe_part5.py` | Fixed offices, movable weekdays, Sunday cycle and historical table |
| `build_sinksar.py` | `sources/sinksar/{am,ge}` → both Synaxarium editions |
| `build_wudase.py` | External Wudase source → devotional asset |
| `merge_seatat.py`, `merge_tsige.py`, `merge_tselot_nebiyat.py`, `build_books.py` | Scanned books and generated intermediates → curated library |
| `build_mahlet.py` | Merged Mahlet, Tsige and Gitsawe sources → orders and index |
| `build_reading_plans.py` | Bundled Scripture → reading plans |

`extract_content.py` still uses the legacy `data/am` input; it is distinct from the newer Scripture bundler. Do not assume the two generators consume the same directory or regenerate all corpora without their dependencies.

## Navigation and reading

The four tabs are **Home, Journey, Library, Settings**. Search and the combined marks screen are separate destinations. `MainActivity.kt` retains legacy routes needed for bookmarks and deep links while providing readers for Scripture, Gitsawe, Synaxarium, Wudase, books and Mahlet.

Reading preferences currently offer six sizes (**16, 18, 20, 22, 25, 28sp**), four reader faces (Abyssinica, Abay Light, Bela Bereka, Zemenay), line spacing, alignment and keep-screen-on. `ReadingTypography.kt` applies per-face optical adjustments. Waldba is additionally bundled for specialist text, not a fifth selectable reader face. Text and image sharing use `ui/common/` helpers.

Mahlet keeps service types, alternative chant branches and source editions distinct. Month 0 holds undated material; the Tsige season reader includes its relevant orders. The source-merge review files are editorial evidence, not automatic approval.

## Calendars and selection

`EthiopianDate` handles date conversion. `BahreHasab` calculates movable anchors and weekday/season windows; `SundayCycleCalendar` supplies fixed-anchored windows. `GitsaweRepository` combines these with fixed dates and feast records.

`selectSundayCycle` generally selects Sundays, with Holy Saturday explicitly supported. Great Lent seasonal rows have their own precedence; otherwise exact-date rows precede ranges, then seasonal rows. The broader `seasonalFor` path also combines weekday offices. These are implemented rules, not evidence that every rubric has been approved: [liturgical review](LITURGICAL_REVIEW.md) tracks that distinction.

## Reminders and widgets

`ModesRepository` supplies the built-in schedule: 06:00, 09:00, 12:00, 15:00, 18:00 and 21:00 enabled by default; Midnight and Veil start disabled. OS notification permission and alarm capabilities still determine delivery. The unreleased reading-reminder update uses automatic 06:30, 14:00 and 20:00 local-time slots, checking unfinished passages across all kept plans at each delivery. It removes the time picker and unanswered-notification backoff; launch also restores the schedule. Existing disabled reminders remain disabled. Additional schedulers handle Journey, Gitsawe, reading, breath prayer and special habits. System-event receivers rebuild relevant scheduling after boot/update/time changes.

`AlarmReceiver` delegates ringing to `AlarmRinger`, an insistent alarm-channel notification with a 60-second timeout, without a foreground service. `AlarmActionReceiver` handles endings and snooze. The old v0.2.6 alarm-service description is historical. Notification IDs are now centralized in `NotificationIds.kt`:

| Family | ID/range |
|---|---|
| Prayer / Journey / Gitsawe / reading / breath | 7001 / 7002 / 7003 / 7004 / 7005 |
| Done markers | 10000–10999 |
| Snooze | 11000–11999 |
| Special habits | 12000–12999 |

Each hashed family uses `floorMod(hashCode, 1000)`. The disjoint ranges prevent cross-family overwrites; they do not promise collision-free arbitrary keys within a family.

Widget receivers expose today's Gitsawe and the memento mori widget. Calendar-driven widget updates and background notification delivery require device testing in addition to JVM tests.

## Connectivity

The manifest declares `INTERNET`. `UpdateRepository.check` requests GitHub release metadata at launch only when `BuildConfig.UPDATE_NOTICE` is true; failures are nonblocking. The repository's old “once a day” comment does not describe the current implementation.

`CatenaScreen` is a network-backed WebView for third-party commentary and sets an Early Fathers preference cookie. Bundled prayer and Scripture readers remain usable without connectivity. Do not advertise the whole app as having no network permission or as including offline commentary.

## Verification and release

```bash
python3 tools/validate_content.py
./gradlew testDebugUnitTest --no-daemon
./gradlew lintDebug --no-daemon
./gradlew assembleDebug --no-daemon
```

Use JDK 17 and installed SDK 36. Existing bundled assets suffice for a normal build. Local signing reads untracked `keystore.properties`; CI uses `SINQ_*` environment variables populated from repository secrets. Without signing configuration release output is unsigned.

The release workflow validates content, runs unit tests and release-vital lint, then builds **separately**:

```bash
./gradlew assembleRelease -PupdateNotice --no-daemon
./gradlew bundleRelease --no-daemon
```

The first is the hand-installed APK; the second is the Play-upload AAB without update notices. CI's ordinary checks are manual-only. The site workflow operates on the separate `gh-pages` branch. See [project status](PROJECT_STATUS.md) for what was actually verified during this refresh.

## UI/UX remediation architecture

- `JournalAccess` gates complete private surfaces, with foreground-only shared authentication; `SecureScreen` tracks overlapping owners. Entry drafts remain restorable while private content is removed on lock.
- `ContentLoad` distinguishes bundled-content loading, failure and retry; `rememberFlowLoad` does the same for saved-state flows without presenting initial empty values as real records. `ReaderSession` applies keep-awake consistently and provides explicit reversible completion.
- `UserAction` serializes writes in the current screen scope, surfaces recoverable failures and propagates coroutine cancellation. Reading-plan, mode, special-reminder and reader-presentation actions use it.
- Scripture routes may carry `plan` and `day` query parameters. `readerPlanDays` gives explicit assignment context precedence and does not substitute today's assignment for a stale link. Plan-day lists expose independent chapter completion, including Psalms.
- Alms, repentance and tithe reminders use saveable local drafts; Save writes a complete entry and rebuilds the schedule. Cancel makes no change. New entries remain disabled until explicitly enabled.
- `DaySchedule.observe` combines reminder/settings/progress flows for live summaries.
- Backup format 3 makes offering preferences optional; prior versions remain readable. Selected categories survive the document picker and previews include additional records.
- See [implementation progress](UI_UX_FIX_PROGRESS.md) for coverage and remaining work. No device QA or release approval is implied.

### Prayer clock widget

`PrayerClock` models the website's seven canonical times separately from personal reminder modes and the Home suggestion windows. `PrayerClockWidgetProvider` draws the Ge'ez-numbered dial into a bounded bitmap and uses `TextClock` for live digital time. Explicit broadcasts cover time/date/timezone changes, reboot, app replacement and widget resizing. A single non-wakeup alarm refreshes placed clocks; removing the last widget cancels it. Android may defer redraws while asleep or when exact-alarm access is unavailable. The clock opens a dedicated intent flag; MainActivity resolves the current canonical hour at tap time without treating that tap as an answered reminder.

Penance configuration now separates summary/progress from editable drafts and waits for persistence before dismissing a save. Tour pages can carry optional bilingual actions; the latest release links to Gitsawe and the replayable tutorial links to its features. Verse-specific reader tools remain scoped to indexed verse content; whole-day/service notes and sharing cover other readers.

## Notes-only confession preparation

`ConfessionPrepScreen` reuses the journal editor in a minimal mode: title, editable note and save/error feedback. `JournalDao.latestConfessionDraft()` resumes the most recently edited confession draft; a new blank draft is only persisted after writing. The existing journal access gate, secure-screen handling and export exclusion apply. Older notes remain accessible through the journal. The dedicated communion-preparation screen and navigation route were removed by user direction; ordinary journal notes and existing records remain available.

Confession note privacy also applies to journal cards: `JournalEntry.preview` returns an empty string for confession drafts, and the journal renders a fixed localized label. The body is only rendered in the editor; masking does not alter stored text.
