# Sinq project status

Reviewed **2026-09-18**, against the published **v2.5.1** release and the checked-in assets, build files, workflows, and Kotlin implementation. This is a repository audit plus recorded GitHub and website publication evidence; it does not establish the state of Google Play or a device installation.

## UI/UX remediation code pass

The [70-surface audit](UI_UX_PRODUCT_AUDIT.md) now has an [implementation tracker](UI_UX_FIX_PROGRESS.md). Current changes address journal access, exact reading destinations and completion, reminder summaries, loading/retry states, accessible controls, record correction and backup scope. These changes are included in v2.4.0. The tracker explicitly lists remaining engineering, product and device-verification work; the historical verification results below are not an all-clear for these new changes.

The 17 September follow-up also addresses historical reading assignment context, consistent plan-start confirmation, saved-state loading/retry, guarded mutation failures, Save/Cancel reminder drafts, reader appearance reset and source-credit navigation. Follow-up validation results are recorded below.

The final implementation pass adds the requested Prayer clock widget, penance Save/Cancel configuration, contextual tour actions, explanatory copy and additional reader note/share tools. Existing Home and Library structure is retained as an explicit product decision. Confession preparation is now a minimal private note editor with hidden list previews, and the dedicated communion-preparation route has been removed by user direction. Combined code/build verification results are recorded below.

### Release publication

[v2.5.1](https://github.com/natinael96/sinq/releases/tag/v2.5.1) is published from app commit `559511a` (versionCode 77). The [release workflow](https://github.com/natinael96/sinq/actions/runs/35332935818) passed content validation, 441 JVM tests and release-vital lint, then built and attached the signed installable APK and Play-upload AAB. The in-app changelog and bilingual What's New tour include this version. The [release-triggered website workflow](https://github.com/natinael96/sinq/actions/runs/35332936045) and GitHub Pages deployment completed successfully, and the live homepage and changelog report 2.5.1.

The [website](https://sinq.natinael96.tech/) is deployed with the original hero, a minimal lower homepage, a separate [About page](https://sinq.natinael96.tech/about.html), consistent Install/About/Changes header links and “powered by 2ቡና” in every footer. Browser checks passed at 320, 390 and 1440 pixels in light/dark themes; all eight live pages were checked for the shared navigation and attribution. These website checks do not replace Android device testing.

### Current remediation verification

On 2026-09-17, `testDebugUnitTest lintDebug assembleDebug` completed successfully: **423 JVM tests passed**, with zero failures, errors or skips. Android lint reported **no issues (0 errors, warnings or hints)**, and the debug APK built successfully. Content validation passed with zero warnings; changed Markdown links and whitespace checks passed.

The user selected **code/build checks only**. Visual, TalkBack, foreground/background lifecycle and notification-delivery checks remain unverified. These results do not close the remaining recommendations in the implementation tracker or establish release readiness.

## Refinements in 2.4.1

The working tree adds Library overflow search, Journey’s compact Today hairlines, a weekday-aligned Ethiopian date-picker popup, and distraction-free Psalter/daily/hourly prayer text. Hourly prayers, Bible chapters and today’s Synaxarium automatically record completion after a reading gesture reaches at least half the content, replacing reader completion buttons. Bible tracking remains chapter-specific and preserves independent plans; opening or restoring alone does not count. The prayer clock includes ሌሊት ፱ ሰዓት at 03:00 independently of reminder settings.

Scripture and scanned church books now start at chapter 1 unless an explicit chapter or citation is requested; only prayer-hour reading retains its saved position. Reader choices are named Scroll mode and Swipe mode and the active choice appears in the reader menu. See the [2.4.1 implementation notes](UI_UX_FIX_PROGRESS.md#user-directed-refinements-in-241) for verification.

Release verification passed locally: bundled-content validation reported zero warnings; **436 JVM tests passed** with no failures, errors or skips; release-vital lint and debug APK assembly completed successfully. Device checks remain unverified.

## Reminder setup and reliability in 2.5.0

First-time users receive a compact Home checklist for notification permission and battery restrictions; existing installs are not interrupted. The daily Gitsawe reminder time is editable, shown in the daily schedule, applied immediately and included in backup/restore.

Reminder delivery and cancellation now share a serialized final-state gate. The once-daily breath prayer uses an atomic DataStore claim, prayer-mode mutations are atomic, prayer schedule rebuilds are mutually exclusive and ringing alarms carry a per-ring session so competing terminal actions have one winner. Receivers re-read current state rather than delivering from stale configuration.

Local release gates passed on 2026-09-18: bundled-content validation reported zero warnings; the website parser selected 2.5.0 as the latest of 68 releases; **439 JVM tests passed**; release-vital lint passed; and the debug APK assembled successfully. Remote release and website publication are verified above; device delivery checks remain unverified.

## Adaptive navigation and accessibility in 2.5.1

Compact windows retain the bottom navigation bar. At 600dp and wider the app uses a Material navigation rail, while root content is centered within an 840dp maximum width. Shared and screen-specific controls across readers, Library, Journey, Mahlet and Settings now meet consistent touch-target sizing.

The Ethiopian-year heatmap scales with the system font size and uses a border for selection; completed prayer hours include a checkmark instead of relying on color alone. The reader scroll grip has a larger acquisition target and cancels stale drag work before beginning a new gesture.

Local release verification passed on 2026-09-18: **441 JVM tests passed** with zero failures, errors or skips; debug and release-vital lint passed; the debug APK assembled; and `git diff --check` was clean. The tag workflow repeated the release gates and published both signed artifacts; remote website publication is verified above. No Android device or emulator was attached, so TalkBack, large-screen rendering and performance remain unverified on hardware.

## Current baseline

| Item | Current repository state |
|---|---|
| Application | Sinq (ስንቅ), native Android, Amharic-first with English interface support |
| Version | **2.5.1**, `versionCode 77`; latest changelog entry dated 2026-09-18 |
| Android identity | `applicationId com.sinq.app`; Kotlin namespace `com.agpeya.app` |
| Android floor / target | API **23** (Android 6.0) / API **36**; compile SDK 36 |
| Architecture | Single app module, Compose UI, repository-backed state, bundled JSON content |
| Persistence | Preferences DataStore for settings and bounded user records; Room/SQLite for journal entries |
| Home-screen widgets | Daily Gitsawe, Memento Mori and the new website-style Prayer clock; clock uses live digital time and a periodically drawn canonical-hour dial |
| Navigation | Home, Journey, Library, Settings; search, marks, readers, and management screens are pushed destinations |
| Product stage | Implemented application with a versioned release history and automated release workflow; original Phase 1/V1 plans are historical |
| Distribution evidence | GitHub v2.5.1 release published with a signed installable APK and Play-upload AAB; live website reports 2.5.1 |

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
| Personal records | Journey/habits, prayer list, tithe ledger, vows, penance, private confession notes | Corresponding repositories and `ui/habits`, `ui/prayerlist`, `ui/nisiha`, `ui/settings` |
| Journal | Day/month browsing, passage-linked reflections, confession drafts, optional passphrase gate | `JournalDatabase`, `JournalRepository`, `JournalLock`, `ui/journal` |
| Marks and search | Bookmarks, verse highlights, passage-linked journal entries; homophone-folded text search | `MarksRepository`, `MarksScreen`, `search/AmharicSearch.kt` |
| Sharing | Text and image-card export, with gallery saving on supported Android versions | `ui/common/Sharing.kt`, `PassageShare.kt` |
| Backup | Selectable local-file export/import with restore preview; optional journal inclusion | `BackupRepository`, `SettingsScreen` |
| Reminders | Prayer alarms and snooze, nightly Journey, configurable-time Gitsawe, reading, breath-prayer and special-habit nudges; first-run permission/battery setup on Home | `reminders/`, `HomeScreen` |
| Widgets | Daily Gitsawe, memento mori and canonical prayer-clock home-screen widgets | `widget/`, manifest receiver declarations |

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

## Reading-reminder update in 2.4.0

The working tree now schedules Bible reading reminders automatically at **06:30, 14:00 and 20:00 local time**, once a plan is started. Follow-ups depend on unfinished passages across all kept plans, rather than `lastReadOn`. Quiet hours and the existing off switch remain; the time picker and automatic pause after unanswered nudges are removed. App launch, reboot and clock/timezone changes rebuild the schedule. Existing disabled reminders remain disabled. This is separate from the released baseline audited below. Verification: 394 JVM tests passed, including 11 reading-reminder regression tests covering partial/multiple-plan completion, quiet hours, slot rollover, late delivery and daylight-saving transitions. Debug lint and APK assembly also passed on a retry with one worker and a 1 GiB Gradle heap after the first daemon exited unexpectedly. No Android device/emulator was attached for delivery or visual checks.

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

The journal passphrase is an interface gate, **not database encryption**. Journal entries are stored in SQLite. Backup export writes **plaintext JSON**, with journal inclusion off by default and confession drafts excluded. `JournalLock.kt`, `BackupRepository.writeTo` and the export UI now consistently describe the plaintext behavior.

## Build and automation

- Gradle wrapper 8.11.1, Android Gradle Plugin 8.10.1, Kotlin 2.1.21, Compose BOM 2025.05.01.
- JVM source/target 11; CI uses JDK 17. Core-library desugaring supports older Android APIs.
- Room 2.8.4 with KSP 2.1.21-2.0.2; journal database schema version 1 is exported under `app/schemas/`.
- `ci.yml` is **manual-only** (`workflow_dispatch`): content validation, unit tests, debug lint and debug assembly. Push/PR CI is currently paused.
- `release.yml` runs content validation, unit tests and release-vital lint before separate APK/AAB builds. The APK enables update notices; the AAB does not. Signing uses repository secrets in CI or local signing configuration.
- `site.yml` regenerates the separate `gh-pages` checkout on release/manual runs and handles pushes to that branch; Vercel deployment depends on configured credentials.

## Earlier documentation-baseline verification on 2026-09-16

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
6. **Comment drift:** update-check comments still mention a daily throttle/settings opt-out; actual current code gates by build flag and checks on launch. The misleading journal-export encryption comment was corrected during the subsequent UI/UX remediation.
7. **Publication:** GitHub release signing and the live site are verified for 2.5.1; Play enrollment, testing and store publication remain unverified.

## Documentation map

- [README](../README.md): product overview, build and contribution entry point.
- [Implementation](IMPLEMENTATION.md): code organization and operational details.
- [SRS](SRS.md) and [SDS](SDS.md): current requirements and design.
- [Content structure](CONTENT_STRUCTURE.md), [rights](CONTENT_RIGHTS.md), [source directory](../sources/README.md): data contracts and provenance.
- [Design system](DESIGN_SYSTEM.md), [screen flows](WIREFRAMES.md), [font subsetting](FONT_SUBSETTING.md): UI implementation guidance.
- [Phase 1 checklist](PHASE1_CHECKLIST.md): reconciled foundation checklist and remaining human decisions.
- [Historical plan](../PLAN.md), [v0.2.6 implementation archive](archive/IMPLEMENTATION_0.2.6.md): historical decisions, not current feature contracts.
