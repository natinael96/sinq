# UI / UX audit implementation progress

Started 16 September 2026; follow-up reviewed 17 September 2026. Work follows [the full audit](UI_UX_PRODUCT_AUDIT.md). Existing working-tree changes are preserved. This is a live checklist, not a claim that every finding is fixed.

## Verification

- The user explicitly chose code/build checks only. No Android device is connected; visual, TalkBack, lifecycle and delivery checks remain unverified.
- Final code/build verification: `testDebugUnitTest lintDebug assembleDebug` passed. All 423 JVM tests passed with zero failures/errors/skips; Android lint reported no issues (zero errors, warnings or hints); the debug APK built successfully. Content validation passed with zero warnings; local Markdown links and `git diff --check` pass.
- All product proposals are being reviewed against actual functionality; editorial content and live-user validation are not replaced with invented material.

## Priority findings

| Finding | Implementation status |
|---|---|
| F01 Journal administration bypass | Shared fail-closed gate around entire private surfaces; foreground relock. Implemented; policy/source checks pass, device checks remain unverified. |
| F02 Notes exposed through Marks/export | Notes gated; note export uses protected backup flow; ordinary exports scoped to selected tab. Implemented; policy/source checks pass, device checks remain unverified. |
| F03 False map chapter identity | Actual chapter sets and first unread calculation; scrollable chapter sheet. Regression tests added. |
| F04 Whole-day completion from chapter | Chapter-only completion reducer; partial/multiple-plan tests added. |
| F05 Explicit chapter 1/resume race | Chapter 0 explicitly means resume; waits for stored chapters. |
| F06 Psalm note anchor | Uses actual visible Psalm number and edition. |
| F07 Requested Mahlet service | Initializes selected service from requested order ID. |
| F08 Mahlet contents offsets | Counts editions note and editions selector separately. |
| F09 Automatic completion | Explicit reversible prayer/Synaxarium completion. |
| F10 Reminder summaries | Shared reactive DaySchedule including vows/penance; settings and quiet-hour warnings consume it. |
| F11 Keep screen awake | Shared preference-aware reader lifecycle helper across text readers. |
| F12 Restart complete plan | New cycle clears only selected plan progress; lifetime map preserved. Regression test added. |
| F13 Dark red contrast | Dark liturgical red now measures 6.08:1 against the background and 5.21:1 against the surface. |
| F14 Missing content states | Shared loading/retry states cover the main catalogs, reading plans, reading map, books, Mahlet, Gitsawe and Sunday cycle. Search, journal and online commentary have failure recovery. Remaining surfaces are listed below. |
| F15 Backup scope | Optional offering settings; format version 3; old versions remain readable. Implemented; policy/source checks pass, device checks remain unverified. |

## Page and flow coverage

“In progress” means at least one change is underway; remaining recommendations still require review. “Pending” does not imply every proposal should be implemented unchanged.

| Audit ID | Surface | Status |
|---|---|---|
| 01 | App shell and bottom navigation | Persistent labels and stable tab targets implemented; gesture/device QA pending |
| 02 | Memento Mori launch screen | Explicit launches bypass overlays; reduced-motion hold fixed; canvas title has heading/content-description semantics; TalkBack QA pending |
| 03 | First-run introduction: welcome and offline explanation | Early language choice and scrollable content implemented |
| 04 | Onboarding name and Christian-name form | Saveable input, awaited persistence and error feedback implemented |
| 05 | Onboarding prayer-level choice | Radio semantics and explanation of three starting presets versus five Settings choices implemented; reading length is distinguished from spiritual rank |
| 06 | Tutorial and What's New tours | Explicit launch bypass and contextual actions implemented for the replayable tutorial and latest release tour; destinations retain onboarding/privacy gates |
| 07 | Home | Active-plan entry, loading/retry and hour-management action implemented; suggestion explicitly labeled as time-of-day based; existing hierarchy retained pending usability evidence |
| 08 | Home: all-hours sheet and update notice | Scrollable hours sheet and larger update targets implemented |
| 09 | Journey | Larger two-column hour controls and labeled totals implemented |
| 10 | Journey year heatmap and day detail | Date-picker alternative implemented; imported older history is included in year navigation (regression tests pass) |
| 11 | Manage habits, rename/create, and scheduling | Overflow menu and deletion confirmation implemented |
| 12 | Library | Search entry implemented; existing shelf grouping retained as a product decision (seasonal content remains within church books) |
| 13 | Scripture hub | Search and last-read chapter entry implemented |
| 14 | Old Testament and New Testament book lists | Loading/retry and weighted names implemented |
| 15 | Scripture reader, citations, and chapter picker | Explicit chapter/resume, chapter-only completion and chapter-picker scroll reset fixed; duplicate ordinary-next controls removed |
| 16 | Psalter: full book, daily portion, Ge'ez/Amharic, and Sunday canticles | Psalm/edition note anchor and screen-awake fixed; resume QA remains |
| 17 | Prayer-hour reader: vertical and paged modes | Explicit completion, loading/retry and stable section-ID/offset persistence implemented (regression tests pass) |
| 18 | Bible reading dashboard | Restart and chapter progress fixed; saved-state loading/retry and guarded mutation feedback implemented; lectionary failure no longer stays on Loading |
| 19 | Choose reading plan and start confirmation | Loading/retry and shared start confirmation implemented; shows estimated reading amount, calendar start/end and automatic reminder explanation |
| 20 | Reading map and book-chapter sheet | Actual chapter identity, next unread, scrolling and targets implemented |
| 21 | Reading plan day list | Every assigned chapter is reachable and independently markable; Scripture links retain explicit plan/day context and label the assignment (regression tests added) |
| 22 | Wudase Maryam reader | Screen-awake and tab selection semantics implemented; edition-position QA remains |
| 23 | Gitsawe day page and source selection | Loading/retry implemented; service-state QA remains |
| 24 | Gitsawe passage / Misbak excerpt | Existing Misbak and screen-awake changes retained; device QA pending |
| 25 | Sunday cycle: options and selected service | Loading/retry and final empty state implemented |
| 26 | Synaxarium reader and contents | Explicit completion and stable section-specific bookmarks implemented, including special readings |
| 27 | Church-book shelves and shelf detail | Loading/retry and invalid-shelf recovery implemented |
| 28 | Church-book reader and recensions | Atomic content/metadata load, chapter/block positioning, chapter resume and reader tools implemented |
| 29 | Mahlet month catalog and search | Loading/retry and current-date updates implemented; catalog QA pending |
| 30 | Mahlet Tsige seasonal catalog | Loading/retry and current-date updates implemented |
| 31 | Mahlet reader: service tabs, versions, contents, and references | Requested service, contents offsets and tab/edition resets fixed |
| 32 | Search and results | Stale-result clearing, retry and keyboard actions implemented |
| 33 | Marks: bookmarks, highlights, notes, and text export | Privacy gate, scoped export, search, color filters, Undo and export feedback implemented |
| 34 | Journal list and month navigation | Privacy gate and larger numbered month cells implemented; spoken-date QA remains |
| 35 | Journal entry editor | Draft restoration, relock continuity and save/load feedback implemented |
| 36 | Journal passphrase, unlock, and secure-session behavior | Fail-closed gate, foreground relock, secure ownership and verification busy state implemented; lifecycle/device QA pending |
| 37 | Confession preparation | Notes-only editor: latest draft resumes, autosave, private gate and export exclusion; no guided content |
| 38 | Penance list, configuration, quota, and progress recording | Private gate, summary cards and Save/Cancel configuration drafts implemented; loading/retry and guarded writes retain failed forms; deletion/progress removal confirmed |
| 39 | Communion preparation — removed | Dedicated screen and route removed at user request; ordinary journal notes remain available |
| 40 | Prayer list, add-name row, person editor, and removal | Add scrolls into view, waits for persistence and preserves failed input; editor scrolls |
| 41 | Fasting calendar | Textual fast status and upcoming summary implemented |
| 42 | Bahre Hasab reference | Responsive columns, bilingual cycle-value labels and explanations of the implemented calculations added; specialist editorial review remains separate |
| 43 | Catena commentary WebView | Main-frame failure, retry, network explanation and WebView cleanup implemented; browser/device QA remains |
| 44 | Settings overview | Reactive explicitly labeled today count and radio targets implemented; prayer presets localized |
| 45 | Reading settings and live preview | Real reader preview, shared keep-awake, atomic appearance reset and reader/paging scope explanation implemented |
| 46 | Reading-font page | Scrollable font selection and radio semantics implemented |
| 47 | Copy format and highlight names | Highlight samples, label reset and Marks filter retrieval implemented |
| 48 | Prayer settings and prayer-level sheet | Scrollable prayer-level sheet and localized preset names implemented |
| 49 | Manage prayer hours | All reminders for an hour toggle together; multiple times are disclosed; notification permission and removal confirmation implemented |
| 50 | Customize prayer-hour sections and add-Psalm picker | Section action menu, reset/removal confirmation and existing-Psalm indication implemented |
| 51 | Reminders settings, daily timeline, quiet hours, and sound sheet | Reactive schedule, conflict detection, scrollable sound choices and stoppable sound preview implemented |
| 52 | Prayer modes list | Separate edit action, notification-state refresh, loading/retry and guarded writes with failure feedback implemented; creation actions wrap |
| 53 | Mode editor and reminder-entry sheet | Scrolling, wrapped weekday chips and enabled-state preservation implemented |
| 54 | Alms reminders | Save/Cancel draft editor implemented with saved draft restoration, guarded writes and failure feedback; new reminders stay disabled until enabled; deletion confirmation added |
| 55 | Repentance reminders | Save/Cancel draft editor implemented with saved draft restoration, guarded writes and failure feedback; new reminders stay disabled until enabled; deletion confirmation added |
| 56 | Tithe reminder configuration | Save/Cancel draft editor implemented with saved draft restoration, guarded writes and failure feedback; new reminders stay disabled until enabled; deletion confirmation added |
| 57 | Tithe ledger, amount/date form, percentage, and currency | Exact amount parsing, Ethiopian date picker and ledger editing implemented |
| 58 | Vows, pledge, and fulfilment history | New reminders start disabled; fulfilment removal with confirmation implemented |
| 59 | Shared schedule editor, day controls, and time pickers | Scrollable controls, Pagume 6 selection and next-leap-day regression test implemented |
| 60 | Records and personal-data landing page | Protected journal export route and plain explanation of local storage, manual transfer, plaintext journal exports and penance exclusion implemented |
| 61 | Backup export, journal verification, restore preview, and results | Scoped offering preferences, v3 compatibility, broader preview and saved selection implemented |
| 62 | Battery/reminder troubleshooting | Battery guidance describes possible causes and avoids delivery guarantees; device/vendor checks remain |
| 63 | Changelog and replay tour | Existing version/tour updates retained; device QA pending |
| 64 | About | Localized About and corrected local-data/network copy implemented |
| 65 | Licenses and source credits | Contents navigation, localized headings, selectable notices/URLs, source/license links and browser-failure feedback implemented |
| 66 | Shared reader tools, verse selection, highlights, cross-references, and sharing | Share-operation lifetime and selection semantics improved; note actions added to Wudase/Synaxarium/Mahlet and current-service Mahlet sharing; verse-only tools remain scoped to indexed verse content |
| 67 | Shared Ethiopian date picker | Named year controls, selected semantics and adaptive 52dp day columns implemented; device QA pending |
| 68 | Notification and alarm surfaces | Bible unfinished-only behavior preserved; direct launches and reminder summaries fixed; delivery QA pending |
| 69 | Gitsawe home-screen widget | Dated destination opens without launch overlays; widget/device QA pending |
| 70 | Memento Mori home-screen widget | Widget opens without launch overlays; widget/device QA pending |

## Work still open

The remaining named implementation recommendations have been addressed or explicitly dispositioned above; this is **not a device-tested all-clear**. Home's existing hierarchy and Library's shelf grouping are retained rather than introducing an unvalidated navigation redesign. Cross-references and verse highlights remain limited to content that supplies stable verse metadata. Confession preparation is now notes-only by user direction; specialist review of liturgical terminology remains outside the code closeout. TalkBack, 320dp/200% text, rotation/backgrounding, widget refresh, notification delivery and backup-provider flows still need a device or emulator. The user selected code/build checks only.

Published as [v2.4.0](https://github.com/natinael96/sinq/releases/tag/v2.4.0), versionCode 74, from app commit `e090405`. The release workflow passed and attached the signed APK and Play-upload AAB. Device checks remain open.

## Follow-up on 17 September 2026

- Rechecked the open items against source; fixed plan chooser consistency and historical assignment context, including independent chapter completion in the day list (also for Psalms).
- Added shared flow loading/retry and a guarded mutation helper used by reading-plan, prayer-mode, reader-setting and special-reminder actions. Duplicate writes are prevented while an operation is running; cancellation is not shown as a failed save.
- Replaced live-edit alms/repentance/tithe cards with summary cards and explicit Save/Cancel drafts. Drafts survive rotation; failed saves retain them. Notification permission remains tied to enabling a reminder.
- Added reader appearance reset/scope copy, Memento title semantics, Catena network-recovery copy, and source-credit navigation/selectable text/links.
- Follow-up verification passed: all 419 JVM tests passed (zero failures, errors or skips), including seven new assignment-context and guarded-action tests. Content validation passed with zero warnings; Markdown link and whitespace checks passed. Android lint reported 0 errors, 42 warnings and 2 hints; debug APK assembly succeeded. The additional lint warning is a `toUri` style suggestion in the new source-link action.

## Regression evidence

The suite now includes journal access policy, partial/multiple-plan completion, restarting without losing lifetime progress, real chapter identity, backup scope and legacy decoding, leap-day scheduling, reordered/removed section resume, exact amount formatting and percentage overflow. These tests establish logic contracts; they do not substitute for Compose/device interaction checks.

Dark red contrast was calculated from sRGB relative luminance. The change does not assert that every color pairing throughout the app has been measured.

## Final implementation pass and requested clock widget

- Penance configuration now uses local Save/Cancel drafts. Failed configuration/progress writes keep the dialog open; new penances do not exist until saved and start with reminders disabled.
- Tutorial and latest What's New pages can open their corresponding features; the tour remains replayable. Onboarding and journal protection still apply to destinations.
- Added explanations for prayer presets, time-of-day suggestions, backup scope and Bahre Hasab calculation values. Added note/share tools where the underlying content supports them.
- Added **Prayer clock**, matching the local GitHub Pages dial source: seven canonical hours, Ge'ez numerals, gold elapsed-day arc and highlighted current hour. It is resizable; a native TextClock supplies live local time. Tapping resolves the current hour at tap time and opens it directly.
- The dial uses non-wakeup minute refreshes when exact-alarm access is available, with an inexact fallback and periodic launcher refresh. Android sleep/power policy may delay the drawn dial; it does not run a foreground service or wake the phone. Device behavior remains unverified.
- Final combined verification passed: 422 tests, zero failures/errors/skips; lint 0 errors, 41 warnings and 2 hints; debug APK assembled successfully. The clock’s missing English widget-resource translations and oversized preview resource flagged on the first lint run were corrected. Content validation, local documentation links and whitespace checks passed. No device/launcher test was performed.

### Notes-only preparation and lint cleanup — 2026-09-17

- Confession preparation opens the latest private confession note or a blank editor. No examination, guidance, date context or category picker appears there. Existing journal locking, autosave, retry and backup exclusion apply; an untouched blank page creates no record. Existing older drafts remain in the journal.
- Removed the dedicated communion-preparation screen and route at the user's request. Ordinary journal notes remain available; stored records were not migrated or deleted.
- Split API 31 widget metadata from older-device resources, made backup exclusion explicit, used Android KTX helpers and derived scroll state, and added missing external-link failure feedback and the Memento widget's spoken caption.
- Verified the completed notes change with `testDebugUnitTest lintDebug assembleDebug`: 422 tests passed, no failures/errors/skips, lint reported no issues, and the debug APK built. Content validation, local Markdown links and whitespace checks passed. Device testing remains unperformed.

### Confession preview privacy follow-up — 2026-09-17

- Confession list cards now show a fixed localized label and lock icon. The model returns no confession preview text, so the body is neither rendered nor included in list accessibility text. The original note remains intact for the editor.
- Added a regression test covering blank, multiline and long confession notes, preservation of the stored body, and continued previews for ordinary passage notes.
- Rechecked the remaining privacy/reminder/widget/backup items in source: the foreground session locks on stop, confession entries are excluded from passage-note lists and backup export/import, reminders check unfinished readings at delivery, and the clock resolves the prayer hour on tap. These source checks do not validate device behavior.
- Final verification: 423 JVM tests passed with no failures/errors/skips; lint reported no issues; the debug APK built successfully. Content validation, local documentation links and whitespace checks passed. Device checks remain unverified.

### User-directed refinements in 2.4.1

- Library search moves into the top-right overflow menu. Journey’s Today section returns to the four-column hairline strips and compact tally; spoken tally labels remain explicit.
- Only the date-picker popup is restyled: true Monday-first weeks, localized weekday labels, separate today/selected treatments, month/year menus and Today navigation. Regression checks cover weekday placement, Pagume and month rollover.
- Prayer readers (Psalter, daily Wudase and hourly prayers) omit saved highlight rendering and tap-selection bars. Existing stored highlights are preserved, and Scripture study tools remain available. Long-press copying and reader-menu sharing remain available.
- Completion buttons are removed from hourly prayers, Bible chapters and Synaxarium. A reading gesture followed by reaching at least half the text records completion automatically; opening/restoring alone does not count. Hourly prayers support vertical scrolling and horizontal paging. Bible completion records only that chapter in matching plans, preserves unrelated plans, and is idempotent. Only today’s Synaxarium updates today’s habit. Journey retains manual controls.
- Progress uses measured visible-item geometry and text lengths to weight longer sections. The clock widget now includes ሌሊት ፱ ሰዓት at 03:00, even when its reminder is disabled; its tap destination is the Veil reader.
- Reader choices are named Scroll mode and Swipe mode and the reader menu reflects the active choice. Scripture and scanned church books no longer expose recent-book shortcuts or restore saved chapters; ordinary entry starts at chapter 1 while explicit chapter/citation links still win. Prayer-hour reading retains position restoration.
- Release verification: content validation passed with zero warnings; 436 JVM tests passed with no failures, errors or skips; release-vital lint and debug APK assembly completed successfully. [v2.4.1](https://github.com/natinael96/sinq/releases/tag/v2.4.1) was published from commit `f2e00a2`; its remote workflow passed and attached the signed APK and Play-upload AAB. The release-triggered website workflow passed. Device checks remain unverified.

### Reminder setup and concurrency hardening in 2.5.0

- Added a first-run Home reminder-setup line and bottom sheet with live notification and battery status, explicit actions, automatic completion and a persistent Not now choice. Existing installs are not retroactively interrupted by the prompt.
- Made the daily Gitsawe reminder time editable, persisted and backup-safe; the timeline and scheduler consume the same setting and saving replaces the pending alarm immediately.
- Serialized final reminder delivery/cancellation, added final current-state checks, atomically claimed the once-daily breath prayer and canceled visible notifications when their source reminder is disabled.
- Made prayer-mode mutations atomic and schedule rebuilds mutually exclusive. Chain re-arming resolves current mode/hour state rather than trusting a stale receiver entry.
- Added per-ring session arbitration so competing snooze, dismiss, open, removal and timeout actions have exactly one winner, including a multithreaded regression test.
- Release verification: content validation passed with zero warnings; the website changelog parser selected 2.5.0; 439 JVM tests passed; release-vital lint and debug APK assembly completed successfully. [v2.5.0](https://github.com/natinael96/sinq/releases/tag/v2.5.0) was published from commit `57bf69d`; its remote workflow passed and attached the signed APK and Play-upload AAB. The release-triggered website workflow passed, and the live homepage and changelog report 2.5.0. Device checks remain unverified.

### Adaptive navigation and accessibility in 2.5.1

- Added a 600dp adaptive-navigation breakpoint: compact windows retain the bottom bar; wider windows use a Material navigation rail. Root destinations are centered within an 840dp maximum width.
- Normalized accessible touch targets across shared reader controls, Library, Journey, Mahlet and Settings while preserving the existing visual density.
- Made the Ethiopian-year heatmap respect system font scaling, removed undersized day targets and added a non-color selected border. Completed prayer hours now carry a checkmark cue as well as color.
- Enlarged the reader scroll grip and its minimum thumb size, and cancel stale drag jobs when a new gesture begins.
- Added boundary tests for the adaptive navigation rule. Local verification passed: 441 JVM tests, debug lint with no issues, release-vital lint, debug APK assembly and whitespace checks. Device, TalkBack, tablet and performance checks remain unverified.
