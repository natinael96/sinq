# Sinq project status

Reviewed **2026-09-16**, against local commit **69ef0a7** and the checked-in assets, build files, workflows, and Kotlin implementation. This is a repository audit; it does not establish the state of Google Play, remote workflow runs, deployed websites, or a device installation.

## Current baseline

| Item | Current repository state |
|---|---|
| Application | Sinq (ስንቅ), native Android, Amharic-first with English interface support |
| Version | **2.3.2**, `versionCode 72`; latest changelog entry dated 2026-09-15 |
| Android identity | `applicationId com.sinq.app`; Kotlin namespace `com.agpeya.app` |
| Android floor / target | API **23** (Android 6.0) / API **36**; compile SDK 36 |
| Architecture | Single app module, Compose UI, repository-backed state, bundled JSON content |
| Persistence | Preferences DataStore for settings and bounded user records; Room/SQLite for journal entries |
| Navigation | Home, Journey, Library, Settings; search, marks, readers, and management screens are pushed destinations |
| Product stage | Implemented application with a versioned release history and automated release workflow; original Phase 1/V1 plans are historical |
| Distribution evidence | Local workflow builds signed APK and Play-upload AAB on `v*` tags; actual publication is not verified here |

## Implemented features

| Area | Implemented behavior | Main evidence |
|---|---|---|
| Prayer hours | Eight built-in hours; scroll and paged readers; section navigation; custom hours and section layouts | `ContentRepository`, `HoursRepository`, `LayoutRepository`, `ReadingScreen` |
| Scripture | Amharic 1980 Bible, Psalter, Ge'ez Psalms switch, citation links and cross-references | `ScriptureRepository`, `ScriptureReaderScreen`, `PsalterScreen` |
| Daily offices | Fixed-date Gitsawe, movable weekday offices, Sunday cycle and Bahre Hasab reference | `GitsaweRepository`, `BahreHasab`, `SundayCycleCalendar` |
| Calendar | Ethiopian/Gregorian conversion, holidays and annual fasting calendar | `EthiopianDate`, `HolidayCalendar`, `FastingCalendar` |
| Synaxarium | Amharic and Ge'ez editions, all 366 dates in each edition | `SynaxariumRepository`, `SynaxariumScreen` |
| Wudase | Daily prayers, weekday portions, Amharic/Ge'ez switching and day swiping | `WudaseRepository`, `WudaseMaryamScreen` |
| Church library | Curated shelf of 37 books and a dedicated Mahlet reader | `BookRepository`, `MahletRepository`, `ui/books`, `ui/mahlet` |
| Mahlet | Month pager, feast-name search, distinct service targets, versions and alternatives, seasonal Tsige access, reader toolbar and contents | `MahletListScreen`, `MahletScreen`, `MahletSeasonScreen` |
| Reading plans | Bundled plans, day readings, progress, book map, completion flow and reminder | `ReadingPlanRepository`, `ui/reading/ReadingPlan*` |
| Personal records | Journey/habits, prayer list, tithe ledger, vows, penance, confession and communion preparation | Corresponding repositories and `ui/habits`, `ui/prayerlist`, `ui/nisiha`, `ui/settings` |
| Journal | Day/month browsing, passage-linked reflections, confession drafts, optional passphrase gate | `JournalDatabase`, `JournalRepository`, `JournalLock`, `ui/journal` |
| Marks and search | Bookmarks, verse highlights, passage-linked journal entries; homophone-folded text search | `MarksRepository`, `MarksScreen`, `search/AmharicSearch.kt` |
| Sharing | Text and image-card export, with gallery saving on supported Android versions | `ui/common/Sharing.kt`, `PassageShare.kt` |
| Backup | Selectable local-file export/import with restore preview; optional journal inclusion | `BackupRepository`, `SettingsScreen` |
| Reminders | Prayer alarms and snooze, nightly Journey, daily Gitsawe, reading, breath-prayer and special-habit nudges | `reminders/` |
| Widgets | Daily Gitsawe and memento mori home-screen widgets | `widget/`, manifest receiver declarations |

“Implemented” means present in the repository, not independently approved liturgical content or verified behavior on every supported device.

## Content inventory

The counts below come from the bundled assets and `python3 tools/validate_content.py`, not estimates from the old plan.

| Corpus | Current inventory |
|---|---|
| Prayer hours | 8 hours, 181 sections; 150 Psalter sections make 331 permanent section IDs across the validator's 9 core files |
| Amharic Scripture catalog | 93 source book records, 1,610 chapters, 44,290 verses; the catalog calls the edition the 81-book canon, so source-file count is not a canonical book count |
| Ge'ez Scripture catalog | Psalms only: 1 source book, 151 source chapters, 2,462 verses; the standard Psalter reader presents 150 Psalms |
| Amharic Synaxarium | 366 days, 1,066 entries, 895 arke hymns, 362 readings |
| Ge'ez Synaxarium | 366 days, 1,094 entries, 953 arke hymns; no separate readings in the validator's reading field |
| Church books | 37 books, 99 chapters, 4,830 blocks, 435,353 characters |
| Mahlet | 190 orders, 3,128 parts, 196 editions; 179 dated orders, 6 selected by computus, 41 Tsige orders and 2 Gitsawe-derived orders (categories overlap) |
| Scripture editions | Two catalog entries: full `am-1980` and Psalms-only `gez-1980` |

Source Mahlet merge statistics are different from final app statistics; see [the comparison record](../sources/mahlet/COMPARISON.md). Inputs are under `sources/`; runtime content is under `app/src/main/assets/content/`. The Bible and some generators require external sibling source repositories. A normal Android build uses the committed assets and does not require regenerating them.

## Recent changes

- **2.3.2 / code 72:** centralizes notification IDs and separates fixed notifications from hashed hour/habit families. This fixes the reading-plan nudge overwriting the morning Gitsawe notification and related snooze/done collisions.
- **2.3.1 / code 71:** improves Mahlet navigation with a month pager, search, service-specific targets, typography controls and contents; makes previously inaccessible month-0 Tsige orders reachable.
- **2.3.0 / code 70:** reading-plan navigation/map/completion improvements and prayer/reminder settings integration; see [the changelog](../CHANGELOG.md) for the full release record.

This documentation refresh does not change application code or increment the application version.

## Privacy and connectivity

Core prayer, Scripture, calendar, personal records and bundled-library features work offline. The manifest **does declare `INTERNET`**. Two implemented uses must be distinguished from bundled reading:

- Builds made with `-PupdateNotice` query GitHub release metadata when the app launches, using an ETag for unchanged responses. Default builds, including the workflow's Play AAB, disable this check through `BuildConfig.UPDATE_NOTICE`. There is no in-app APK installation.
- Opening Catena commentary loads a third-party website in a WebView and sets its Early Fathers preference cookie. This feature requires connectivity and is not bundled commentary.

No account, cloud synchronization, advertising SDK or analytics SDK is configured in the inspected app. Android automatic backup is disabled. Users can deliberately export their own files.

The journal passphrase is an interface gate, **not database encryption**. Journal entries are stored in SQLite. Backup export writes **plaintext JSON**, with journal inclusion off by default and confession drafts excluded. A stale comment in `JournalLock.kt` says exports are encrypted; `BackupRepository.writeTo` and the export UI establish the actual plaintext behavior. Correct that comment or implement a separately reviewed encryption change; do not advertise encryption from the comment alone.

## Build and automation

- Gradle wrapper 8.11.1, Android Gradle Plugin 8.10.1, Kotlin 2.1.21, Compose BOM 2025.05.01.
- JVM source/target 11; CI uses JDK 17. Core-library desugaring supports older Android APIs.
- Room 2.8.4 with KSP 2.1.21-2.0.2; journal database schema version 1 is exported under `app/schemas/`.
- `ci.yml` is **manual-only** (`workflow_dispatch`): content validation, unit tests, debug lint and debug assembly. Push/PR CI is currently paused.
- `release.yml` runs content validation, unit tests and release-vital lint before separate APK/AAB builds. The APK enables update notices; the AAB does not. Signing uses repository secrets in CI or local signing configuration.
- `site.yml` regenerates the separate `gh-pages` checkout on release/manual runs and handles pushes to that branch; Vercel deployment depends on configured credentials.

## Verification on 2026-09-16

- **Content validator: PASS**, zero warnings. It checks stable IDs, manifests, nonempty content and text hygiene, with corpus-specific checks.
- **Gradle unit-test task: PASS** (`./gradlew testDebugUnitTest --offline --no-daemon`). Gradle reused up-to-date test results: 58 suites, **389 tests**, zero failures/errors/skips. This was not a forced fresh execution of every test. The 58 Kotlin test source files cover content, calendars, reading plans, journal, backup format, search, reminders, rendering helpers and related logic.
- **Documentation checks: PASS.** All 18 tracked Markdown files updated; current-document relative links/heading anchors resolve; Gitsawe source hashes match; `git diff --check` is clean. The site generator parses the same published release entries after adding the Unreleased documentation note.
- **Font-subsetting dry run:** could not run because `fontTools` is not installed; no fonts were changed.
- No emulator/device QA, screenshot review, signed release build, remote CI inspection or store/deployment verification was performed for this documentation update.

## Outstanding work and evidence gaps

1. **Liturgical/editorial approval:** reviewer identity and source-backed approvals remain incomplete. Automated calendar/content tests do not replace review by a qualified reader. See [liturgical review](LITURGICAL_REVIEW.md).
2. **Source rights/provenance:** retain the recorded terms in [NOTICE](../NOTICE); underlying translation provenance, transcription-specific questions and font metadata inconsistencies remain review items in [the rights record](CONTENT_RIGHTS.md).
3. **Mahlet editorial review:** ambiguous dates, OR boundaries, undated held-out records and near-duplicate editions remain under `sources/mahlet/review/`. Do not merge alternatives into a compulsory chant sequence.
4. **Source regeneration:** the editorial `scripts/merge_mahlet.py` command belongs to its original external workspace and is not included here. This repo provides `tools/build_mahlet.py` to build from the checked-in merge.
5. **Device verification:** notification delivery across reboot, time changes, permission denial and vendor battery restrictions; Android 6 compatibility; large-font/TalkBack behavior; widget refresh; and restore behavior still need a recorded device matrix for a release-readiness claim.
6. **Comment drift:** update-check comments still mention a daily throttle/settings opt-out; actual current code gates by build flag and checks on launch. The journal export encryption comment also conflicts with execution. These are reported here; application source was not changed in this Markdown-only task.
7. **Publication:** Play enrollment/testing/publication, release-signing secret readiness and live site state cannot be inferred from workflow configuration.

## Documentation map

- [README](../README.md): product overview, build and contribution entry point.
- [Implementation](IMPLEMENTATION.md): code organization and operational details.
- [SRS](SRS.md) and [SDS](SDS.md): current requirements and design.
- [Content structure](CONTENT_STRUCTURE.md), [rights](CONTENT_RIGHTS.md), [source directory](../sources/README.md): data contracts and provenance.
- [Design system](DESIGN_SYSTEM.md), [screen flows](WIREFRAMES.md), [font subsetting](FONT_SUBSETTING.md): UI implementation guidance.
- [Phase 1 checklist](PHASE1_CHECKLIST.md): reconciled foundation checklist and remaining human decisions.
- [Historical plan](../PLAN.md), [v0.2.6 implementation archive](archive/IMPLEMENTATION_0.2.6.md): historical decisions, not current feature contracts.
