# Sinq: detailed UI, UX, and product audit

Audit date: 16 September 2026. Baseline: local Android working tree, version 2.3.3 / code 73, including the uncommitted automatic Bible reminder changes. This is an audit, not an implementation or release approval.

**Navigation:** [Product assessment](#product-assessment) · [Priority findings](#highest-priority-findings) · [70 page and flow reviews](#page-by-page-review) · [Shared standards](#cross-product-design-standards-to-apply) · [Implementation sequence](#recommended-implementation-sequence) · [Device checks](#device-and-usability-validation-still-required)

## How to read this report

This review covers the four main tabs, every destination registered in `MainActivity`, reader variants, forms, dialogs, onboarding, reminders, and both widgets. Findings come from navigation, Compose layouts, state handling, repositories, bundled content, strings, and theme definitions. Each page review includes what to preserve, what needs changing, and how to verify the change.

**Important limitation:** no Android device was connected and no emulator was installed. No screenshots, TalkBack sessions, physical touch measurements, or live user interviews were available. “Confirmed” means the behavior or discrepancy is established by the source; it does not mean a device reproduction was performed. Layout failures and lifecycle races are identified as **risks** until reproduced. Product recommendations are **proposals**, not measured user preferences. Previously passing unit tests do not establish UI quality; no application build or tests were rerun for this documentation-only audit.

Priorities:

| Priority | Meaning |
|---|---|
| P0 | Trust/privacy blocker: address before the next public release. |
| P1 | Incorrect destination, progress, or important task behavior. Fix before broad visual redesign. |
| P2 | Significant usability, accessibility, consistency, or recovery improvement. |
| P3 | Useful polish or discovery improvement after core tasks work reliably. |

Source links are relative to this report; line anchors refer to the inspected working tree and will move as code changes. Page numbers below are audit IDs, not a claim that every sheet is a separate navigation destination.

## Product assessment

Sinq has a coherent identity and substantial useful content. Its strongest proposition is a dependable, offline companion for Ethiopian Orthodox prayer and reading: know what belongs to today, open it quickly, return to your place, and keep personal records privately. The green/ivory/gold palette, Ethiopic typography, calendar awareness, bundled corpus, restrained devotional language, and lack of account setup all support that proposition.

The principal weakness is consistency across the product. Different screens interpret “read,” “done,” “saved,” and “private” differently. Several features are well designed in isolation but do not share the same interaction contract. This is more consequential than changing card radii or adding animation.

The next iteration should make three promises dependable:

1. **Take me to the right place.** Explicit citations, service selections, notes, and contents links must preserve their exact destination.
2. **Remember what actually happened.** Opening a page, finishing a passage, and completing today's plan must remain distinct actions.
3. **Respect my privacy and attention.** A journal lock must protect every route to its contents; reminder summaries must accurately explain what will happen.

Preserve the four main destinations initially. Test better labels and grouping before introducing another tab or a large navigation redesign. Make Home about today and continuing; Journey about practice and reflection; Library about finding texts; Settings about configuration and data management. A small contextual “Continue reading” action is a stronger first experiment than a new dashboard full of statistics.

## Highest-priority findings

| ID | Priority / evidence | Problem and consequence | Required outcome |
|---|---|---|---|
| F01 | P0 / confirmed | Journal top-bar actions remain accessible while locked; removal/reset does not verify the existing passphrase. | Authenticate before changing or removing the lock; locked state must fail closed. |
| F02 | P0 / confirmed | Marks displays passage-linked journal notes and exports their full bodies without the journal gate. | Apply the same privacy boundary to note previews, entry points, and exports. |
| F03 | P1 / confirmed | Reading map marks chapters `1..readCount`, regardless of the actual chapter IDs read. | Render the real completed chapter set; choose a genuinely unread chapter. |
| F04 | P1 / confirmed | Scripture's plan footer can mark an entire multi-chapter day complete from its first chapter. | Clearly separate chapter/passage completion from explicit whole-day completion. |
| F05 | P1 / confirmed code discrepancy; timing needs reproduction | Scripture treats chapter 1 as unspecified; restoration also commits before the saved-chapter flow necessarily loads. | Explicit chapter 1 wins; ordinary resume waits for stored state. |
| F06 | P1 / confirmed | Psalter toolbar notes use the daily subset index as the full Psalter index. | A note on Tuesday's first Psalm links to Psalm 31, not Psalm 1. |
| F07 | P1 / confirmed | Mahlet groups a requested service with its siblings but initializes the selected tab to zero. | Open the service ID the user selected. |
| F08 | P1 / confirmed | Mahlet contents indexing omits the separate editions-note item when versions exist. | Every contents item lands on its matching part. |
| F09 | P1 / confirmed | Synaxarium marks today's habit complete as soon as entries load. Prayer paging marks completion on entry to the last section. | Adopt an explicit and consistent completion policy, with an easy correction. |
| F10 | P1 / confirmed | Reminder overview omits reminder families; quiet-hour conflicts omit Bible reading and penance; day timeline loads only once. | One reactive source of truth for enabled, due, suppressed, and completed reminders. |
| F11 | P1 / confirmed | “Keep screen on while reading” is only implemented in the prayer-hour reader. | Honor the preference throughout supported readers or accurately narrow the setting. |
| F12 | P1 / confirmed | Restarting a finished plan calls `start`, which preserves its completed ledger. | Define “read again” separately from “resume”; create an explicit new cycle if that is the promise. |
| F13 | P2 / calculated | Dark liturgical red is approximately 3.72:1 on the dark background and 3.19:1 on the dark surface. | Adjust normal-size red text to a suitable contrast or constrain its use to qualifying large text. |
| F14 | P2 / confirmed pattern; runtime impact varies | Multiple content pages use empty/null as both loading and final state, with no recoverable error state. | Consistent loading, empty, unavailable, and retry states. |
| F15 | P1 / confirmed | Backup always exports tithe percentage/currency and restore applies them, even when offerings were not selected. | Export/restore scope must honor selected categories, including their settings. |

The detailed page reviews below are the actionable inventory. P0 does not imply remote exploitation: F01/F02 concern access within the app by someone who can use the unlocked device, precisely the situation the journal passphrase is meant to address.

## Page-by-page review

### 01. App shell and bottom navigation

Source: [MainActivity](../app/src/main/java/com/agpeya/app/MainActivity.kt#L417), [AgpeyaBottomBar](../app/src/main/java/com/agpeya/app/ui/common/AgpeyaBottomBar.kt#L98).

**Preserve:** four stable tabs, selected-tab semantics, visible selected state, and shared tab state in a pager.

**UI — P2, risk:** only the selected tab has a visible label. The selected slot expands while the others shrink. This weakens recognition for new users and can produce narrow inactive slots on small screens; enlarged text is measured with unlimited width and then clipped. Give each destination a stable, adequate target and test persistent labels. The brand can remain without changing the width of every target after each tap.

**UX — P2, risk:** horizontal gestures change the main tab while child surfaces also use horizontal scrolling. Test the Journey heatmap and any rails near tab boundaries. Back from a non-Home tab returns to Home; verify that this is understandable and that tab scroll positions survive opening a detail and returning.

**Product:** the overall structure is sound. Improve labels, discoverability, and destination continuity before expanding navigation.

**Acceptance:** at 320dp width and 200% font size, all tabs remain operable; TalkBack identifies each label and selected state; a heatmap drag does not unexpectedly change tabs; returning from a detail preserves the originating tab and scroll position.

### 02. Memento Mori launch screen

Source: [MementoMoriScreen](../app/src/main/java/com/agpeya/app/ui/intro/MementoMoriScreen.kt#L90), [launch overlay](../app/src/main/java/com/agpeya/app/MainActivity.kt#L102).

**Preserve:** a distinctive devotional identity and tap-to-dismiss behavior.

**UI — P2, risk:** custom-drawn lettering requires explicit semantic treatment; its fit-to-width behavior needs large-text verification. The animation accounts for reduced motion in several durations, but the final hold remains fixed.

**UX — P2, confirmed placement:** the overlay appears once per activity launch, including launches initiated by a reminder or widget. An update tour can follow it. This delays a user who already chose a specific prayer or passage. Bypass introductory overlays for explicit notification/widget actions, or make the requested destination immediately reachable.

**Product proposal:** keep the reflective moment for intentional app launches; evaluate whether frequent users want a shorter repeat experience.

**Acceptance:** a reminder tap reaches its target without a compulsory tour; reduced-motion mode has no drawing animation; screen-reader users can identify and dismiss the screen.

### 03. First-run introduction: welcome and offline explanation

Source: [IntroScreen](../app/src/main/java/com/agpeya/app/ui/intro/IntroScreen.kt#L91).

**Preserve:** a short explanation of purpose, optional onboarding, and no account requirement.

**UI — P2, risk:** centered pages and fixed footer space need small-height, landscape, and large-text testing. Pagination dots should have an accessible page-position equivalent.

**UX — P2:** provide a discoverable language choice before users encounter several pages of instructions. Explain offline reading accurately while distinguishing optional online commentary, updates, and feedback.

**Product proposal:** first value should be opening a prayer or today's reading. Keep introductory explanation brief and offer the longer tutorial afterward.

**Acceptance:** both interface languages can be selected without finishing onboarding; the main task is reachable through Skip; every welcome page is readable at 200% text size.

### 04. Onboarding name and Christian-name form

Source: [IntroScreen state and saves](../app/src/main/java/com/agpeya/app/ui/intro/IntroScreen.kt#L97).

**Preserve:** names are optional and can personalize the experience without account creation.

**UX — P1, lifecycle risk:** the form uses `remember`, and saving launches a composition-owned coroutine before navigation continues. Rotation can discard unsaved form state; navigating immediately after Skip/Finish can race preference writes. Await persistence before completing onboarding, and represent saving or a retry if necessary.

**UI — P2, risk:** the centered form has no dedicated scroll/IME treatment. Keep the active field and Continue visible with the keyboard open.

**Acceptance:** enter both names, rotate, finish immediately, and relaunch; values and completion state agree. A failed save preserves the input and offers recovery.

### 05. Onboarding prayer-level choice

Source: [IntroScreen level choices](../app/src/main/java/com/agpeya/app/ui/intro/IntroScreen.kt#L390).

**Preserve:** a manageable initial commitment rather than requiring every prayer hour.

**UI/accessibility — P2, confirmed:** selected cards are custom clickable surfaces without the same radio-selection semantics used by standard choice controls. Expose one selected option and the full label.

**UX — P2:** onboarding offers three levels while Settings exposes five. Explain that these are starting presets and that they can be changed later; show concrete differences in included prayer content, without implying a spiritual rank.

**Acceptance:** TalkBack announces the selected preset; switching it updates an understandable summary; changing it later does not erase prayer history.

### 06. Tutorial and What's New tours

Sources: [WhatsNewTour](../app/src/main/java/com/agpeya/app/ui/intro/WhatsNewTour.kt#L36), [IntroScreen/TourScaffold](../app/src/main/java/com/agpeya/app/ui/intro/IntroScreen.kt), [tour content](../app/src/main/assets/content/tour/tour.json).

**Preserve:** replayable help, bilingual tour content, Skip, and scrollable release-tour bodies.

**UX — P2:** tours explain features but do not directly take the reader to the explained action. Add a contextual “Try it” action where useful and retain a clear exit. Avoid stacking a long tutorial and release tour before the first useful screen.

**Product — P2:** release tours should cover a few meaningful behavior changes, while changelog details remain available separately. Content and actual destinations must be checked together; a release note claiming a fixed service link is insufficient if the reader still initializes the wrong tab.

**Acceptance:** skipping persists; replay works from Settings; the correct release is shown once; page position and large-text scrolling work after rotation.

### 07. Home

Source: [HomeScreen](../app/src/main/java/com/agpeya/app/ui/home/HomeScreen.kt#L100).

**Preserve:** current Ethiopian date, today's prayer entry, daily lectionary, restrained width on large screens, and the existing narrow-screen shortcut stacking.

**UI — P2, proposal:** the prayer and Gitsawe cards both use strong hero treatment. Establish one primary “now” action and a quieter secondary reading action; test the resulting hierarchy rather than assuming both cards are equally important to every user. Avoid making updates visually compete with the devotional task.

**UX/product — P2, confirmed distinction:** Home and `PrayerSchedule.currentHourId` both use the built-in time-of-day recommendation rather than the active mode's edited reminder times. Decide whether “now” means the traditional hour or the user's personal schedule, label it accurately, and test a mode with substantially changed times. Do not imply that the fallback currently reads the personalized schedule.

**UX — P2:** there is no prominent continuation for an active Bible plan. Introduce a small contextual continuation only when relevant. Keep all-hours access discoverable and comfortably tappable; its current text affordance is compact.

**State — P2:** visible hours begin as an empty list; unavailable content and an intentionally empty setup need distinct treatment and a direct management action.

**Acceptance:** with a custom schedule, all hours hidden, a completed day, no reading plan, and two active plans, Home presents an accurate next action without misleading counts.

### 08. Home: all-hours sheet and update notice

Sources: [AllHoursSheet](../app/src/main/java/com/agpeya/app/ui/home/HomeScreen.kt#L773), [UpdateLine](../app/src/main/java/com/agpeya/app/ui/common/UpdateLine.kt#L52).

**UI — P2, risk:** the all-hours sheet is a non-scrollable Column. Built-in hours plus custom hours and enlarged text can exceed the available height. Use a bounded scrolling list and preserve full prayer names.

**UI/accessibility — P2, risk:** the update line is fixed at 28dp with nested open/dismiss controls. Compose can expand touch bounds, but this does not resolve neighboring target overlap. Allocate clear targets and test the dismiss action independently.

**Product:** show the destination/version before sending users out of the app; a failed external open should produce a recoverable message.

**Acceptance:** every hour remains reachable with 20 custom hours; the update notice can be dismissed without opening the download page; a missing browser does not fail silently.

### 09. Journey

Source: [JourneyScreen](../app/src/main/java/com/agpeya/app/ui/habits/JourneyScreen.kt#L124).

**Preserve:** welcoming return language, no punitive streak reset, current-date updates, and a journal action that remains available when scrolled.

**UI — P2, risk:** four prayer-hour labels share a compact row with a 28dp minimum height. Long names are ellipsized; adjacent expanded touch bounds may interfere. Prefer a less dense arrangement at narrow widths and large font sizes. The two unlabeled fractions in the Today header need clear meaning: prayer hours and habits are different denominators.

**UX — P1:** Journey aggregates manually checked habits with automatically marked reader activity. Fix F09 so the record has a consistent meaning; let users correct accidental completions.

**Product proposal:** emphasize returning and reflection over accumulating a count. Make habit management discoverable near the habit list, even if its canonical home remains Settings.

**Acceptance:** a user can explain both totals, identify each prayer hour, change one completion without touching another, and understand why a reading was marked complete.

### 10. Journey year heatmap and day detail

Source: [EthiopianYearHeatmap](../app/src/main/java/com/agpeya/app/ui/habits/EthiopianYearHeatmap.kt#L112).

**Preserve:** Ethiopian year navigation, textual day descriptions, selected semantics, and previous/next-day controls after selection.

**UI/accessibility — P2, risk:** 13dp cells are too densely arranged to rely on touch expansion alone. Keep the visual overview but provide an equivalent larger list/calendar route or date picker for day selection. Distinguish fasting background from activity intensity beyond color.

**UX — P2:** explain what one level measures and the earliest available year. The hardcoded app epoch limits selectable history; imported older history needs an explicit product rule rather than apparently missing days.

**Acceptance:** a screen-reader user can find a specific day without traversing hundreds of tiny cells; the selected day exposes its date, activity, and fasting context; older imported records have a defined representation.

### 11. Manage habits, rename/create, and scheduling

Source: [ManageHabitsScreen](../app/src/main/java/com/agpeya/app/ui/habits/ManageHabitsScreen.kt#L61).

**Preserve:** show/hide, schedule customization, and accessible move-up/down alternatives to dragging.

**UI — P2, risk:** a custom-habit row can contain visibility, rename, up, down, and delete buttons in addition to its text. At phone widths those controls consume most of the row. Keep one primary row action and move infrequent actions to a menu or edit sheet.

**UX — P2, confirmed:** custom-habit deletion executes immediately. Provide Undo or confirmation describing what happens to existing records. Reordering should announce the new position. Schedule and rename are different actions; make the row's primary action explicit.

**Acceptance:** a long custom name is identifiable at 200% text size; accidental deletion is recoverable; hidden habits and previous history remain understandable.

### 12. Library

Source: [LibraryScreen](../app/src/main/java/com/agpeya/app/ui/library/LibraryScreen.kt).

**Preserve:** corpus-first browsing and the active reading-plan card.

**UI — P2, proposal:** group Scripture/reading plan, daily devotional texts, and church books into a small number of clearly labeled sections. Maintain a consistent relationship between title, description, and progress; avoid using the same strong card style for every destination.

**UX — P2:** readers who remember a text's name should see Search at the point of browsing. Make recently read content and saved items easy to return to. Mahlet currently requires going through church books; evaluate a direct seasonal/contextual entry without duplicating the entire catalog.

**Product:** communicate corpus language and edition before opening a reader. An English interface does not imply English scripture content.

**Acceptance:** a new user can find today's Synaxarium, a Bible book, Mahlet, and saved verses without needing the tutorial.

### 13. Scripture hub

Source: [ScriptureHubScreen](../app/src/main/java/com/agpeya/app/ui/library/ScriptureHubScreen.kt).

**Preserve:** the uncomplicated Old Testament/New Testament/Psalter split.

**UX/product — P2, proposal:** this is a useful taxonomy but a weak return destination. Add a compact last-read reference and contextual search; do not put a second competing reading-plan dashboard here. Explain the available language/edition once.

**UI:** maintain consistent row descriptions and full labels at large text.

**Acceptance:** “browse a testament” and “continue my last chapter” are both obvious, distinct actions.

### 14. Old Testament and New Testament book lists

Source: [ScriptureListScreen](../app/src/main/java/com/agpeya/app/ui/library/ScriptureListScreen.kt#L104).

**Preserve:** traditional grouping and chapter counts.

**State — P2:** an initial empty collection has no distinct loading/error presentation. Missing content should not look like an empty testament.

**UI — P2, risk:** book names and chapter counts share an unweighted name row. Long Ethiopic names can displace trailing content at narrow widths. Reserve the count's space and allow names to wrap sensibly.

**UX — P2:** add lightweight in-list search or a clear global search entry; indicate the last-read chapter separately from the book's total chapters.

**Acceptance:** all long titles remain identifiable at 320dp/200%; an unavailable index has Retry; searching a book opens the intended book rather than a content-text result list.

### 15. Scripture reader, citations, and chapter picker

Source: [ScriptureReaderScreen](../app/src/main/java/com/agpeya/app/ui/library/ScriptureReaderScreen.kt#L181).

**Preserve:** explicit content loading/retry states, citation highlighting, verse tools, and chapter selection.

**UX — P1, F05:** distinguish an unspecified chapter from explicitly requested chapter 1. Do not mark restoration complete while `lastChapters` still contains its initial empty value. The intended rule should be explicit citation > current restored screen state > saved reading position > chapter 1.

**UX — P1, risk:** changing chapters updates `chapter` but does not explicitly reset or restore the list position. Test from the bottom of a long chapter; the next chapter must begin at its intended position, not an inherited offset.

**Completion — P1, F04:** the footer displays a position within today's reading but invokes whole-day `markDay`/`unmarkDay` from any included chapter. A user finishing chapter one can accidentally suppress all later reminders. Provide “Mark this passage read” and a clearly labeled, separate whole-day action if desired. Do not infer that all assigned chapters have been read.

**UI — P2:** plan-specific navigation and generic next-chapter navigation can appear together. Consolidate them around the active reading context, and replace raw next-book slugs with display names.

**Acceptance:** chapter-1 citations ignore unrelated saved position; a three-chapter assignment remains unfinished after its first chapter; two plans count independently; next chapter and Back preserve a predictable location.

### 16. Psalter: full book, daily portion, Ge'ez/Amharic, and Sunday canticles

Source: [PsalterScreen](../app/src/main/java/com/agpeya/app/ui/psalter/PsalterScreen.kt#L219).

**Preserve:** daily portions, edition switching, verse tools, and Sunday content.

**UX — P1, F06:** the toolbar's note action uses the visible subset index to address the full Psalm list. Resolve the displayed Psalm itself, then build both label and route from its actual number. Verse-specific actions already use the actual section and provide a model to follow.

**State — P2:** distinguish loading/unavailable Psalter and Sunday content from an empty result. Preserve daily/full mode through configuration changes.

**UI — P2:** make daily/full and edition state explicit; the user should know whether they are seeing one day's portion or the complete Psalter. Number-search input should accept the numeral forms promised by the UI, or clearly guide the user to the supported form.

**Acceptance:** notes on the first/middle/last Psalms of every daily range reopen the same Psalm; Sunday canticles have usable unavailable states; selection remains tied to the correct edition.

### 17. Prayer-hour reader: vertical and paged modes

Source: [ReadingScreen](../app/src/main/java/com/agpeya/app/ui/reading/ReadingScreen.kt#L224).

**Preserve:** prayer-level filtering, full-hour override, keep-screen-on behavior, contents navigation, and the option of vertical/paged reading.

**State — P2:** an absent hour, failed content load, and all-hidden sections can lead to no reading content without an explanatory state. Show a useful distinction and a route to restore sections.

**Completion — P1, F09:** paged mode marks the hour prayed when the final section becomes current, even if its text is long and unread. Vertical mode relies on the footer becoming visible. Make the completion policy explicit and reversible; an explicit finish action is the clearest default for reliable records.

**Persistence — P2, lifecycle risk:** position saving launches from `onDispose` using a composition scope that may be cancelled. Save stable progress while reading or use a persistence scope with an appropriate lifetime. Persist a meaningful anchor, not only an array position that can change after customization.

**Acceptance:** entering a long final section does not claim completion prematurely; deleting/hiding sections does not restore to an unrelated section; leaving immediately preserves the chosen place.

### 18. Bible reading dashboard

Source: [ReadingPlanScreen](../app/src/main/java/com/agpeya/app/ui/reading/ReadingPlanScreen.kt#L109), [ReadingPlanRepository](../app/src/main/java/com/agpeya/app/data/ReadingPlanRepository.kt#L85).

**Preserve:** multiple compatible plans, catch-up support, clear daily passages, and the new automatic morning/afternoon/night reminders.

**State — P2:** initial empty content/state can briefly resemble “choose a plan.” Use explicit readiness before presenting onboarding or empty states. The remembered date must refresh across midnight.

**Accessibility — P2:** the custom passage completion control supplies a Checkbox role but not its checked state. Use a proper toggle semantic and adequate layout space.

**UX — P1, F12:** the finished-plan restart calls `start`, which re-dates while preserving read history. “Restart/read again” must either start a separate cycle or explicitly mean resume; do not present a fresh start while everything remains complete.

**Product:** when a plan begins, state that reminders arrive at 06:30, 14:00, and 20:00 local time only while today's assignment remains unfinished. Preserve automatic scheduling; do not add a required time-picker setup.

**Acceptance:** no-plan, one-plan, two-plan, partial day, complete day, missed day, and completed-plan states all have clear next actions; midnight refreshes the assignment; a new cycle has defined history behavior.

### 19. Choose reading plan and start confirmation

Source: [ReadingChooseScreen](../app/src/main/java/com/agpeya/app/ui/reading/ReadingChooseScreen.kt#L77).

**Preserve:** a small set of curated plans and compatibility restrictions.

**UX — P2, confirmed inconsistency:** choosing a card starts a plan immediately here, whereas the dashboard has a start dialog. Use one predictable pattern. Show duration, expected reading amount, compatibility, and automatic reminder behavior before commitment.

**State — P2:** disable repeated starts while saving and preserve the selection on failure. A rejected incompatible plan should explain how to resolve the conflict without losing existing progress.

**Acceptance:** a card tap has the same meaning wherever plans are presented; users can compare before starting; double taps do not produce confusing navigation or repeated writes.

### 20. Reading map and book-chapter sheet

Source: [ReadingMapScreen model](../app/src/main/java/com/agpeya/app/ui/reading/ReadingMapScreen.kt#L52), [BookSheet](../app/src/main/java/com/agpeya/app/ui/reading/ReadingMapScreen.kt#L316).

**Preserve:** a visual overview of progress across the corpus and direct chapter access.

**Correctness — P1, F03:** a count cannot identify which chapters were read. With chapter 10 completed, `n <= book.read` colors chapter 1 and “next” opens chapter 2. Pass chapter IDs to the sheet and compute the chosen unread/resume chapter from actual state.

**UI/accessibility — P2:** the sheet uses a non-scrollable FlowRow of 34dp chapter cells. Large books and large text need scrolling and a larger alternative to the dense grid. Expose chapter number, completed/unread state, and selection semantics. Add textual progress to visual book fills.

**Acceptance:** noncontiguous progress renders correctly; a long book's final chapter is reachable; all-read books do not misleadingly offer their last chapter as “next unread.”

### 21. Reading plan day list

Source: [ReadingPlanDaysScreen](../app/src/main/java/com/agpeya/app/ui/reading/ReadingPlanDaysScreen.kt#L46).

**Preserve:** access to past and future assignments.

**UX — P1/P2:** each day opens only its first passage although its summary can contain multiple passages. Provide a selected-day detail or individually actionable passages, with day-specific completion context. A historical assignment should not silently acquire today's completion controls.

**State — P2:** initial null plan is shown as no plan before loading resolves; refresh the date across midnight and distinguish missing/retired plans from loading.

**Acceptance:** for a day spanning several books, every assigned passage is reachable and the user can return to that day; completing it changes the intended day's record.

### 22. Wudase Maryam reader

Source: [WudaseMaryamScreen](../app/src/main/java/com/agpeya/app/ui/library/WudaseMaryamScreen.kt#L94).

**Preserve:** good loading/retry states, daily prayer followed by weekday content, edition choice, and companion-book links.

**Accessibility — P2, confirmed:** the section strip uses clickable text with visual selection rather than the common selectable-pill semantics. Announce the current section and support reliable targets.

**UX — P2, risk:** the strip scrolls away with each page, and native text selection overlaps with tap-based stanza selection. Explain selection once in context and keep an easy path back to section navigation. Verify that the overlaid selection bar does not hide the final stanza or companion links.

**Product:** preserve the intentional daily-prayer-first sequence; consistency does not require forcing every devotional reader into a Bible-style chapter model.

**Acceptance:** section switching works with TalkBack; selected text and edition agree; the last content remains reachable while selection tools are open.

### 23. Gitsawe day page and source selection

Source: [GitsaweScreen](../app/src/main/java/com/agpeya/app/ui/gitsawe/GitsaweScreen.kt#L132).

**Preserve:** previous/next/today navigation, Ethiopian date picker, source variants, passage previews, and links to Synaxarium and Mahlet.

**State — P2:** the nullable loading model lacks an explicit caught failure and retry. Retain the good no-readings state, but distinguish it from failed content.

**UI — P2, risk:** the day line combines arrows, a dual-calendar date, and Today. At large fonts the date becomes ellipsized. Put the date on a readable line and allow secondary controls to wrap. Repeated seasonal/monthly source labels need a distinguishing subtitle when several sources share the same label.

**UX/product — P2:** explain what Daily/Seasonal/Monthly means and why more than one source is offered. For unsupported citations, offer copy/reference details and a route to search rather than a dead row. Keep Ge'ez/Amharic Misbak choice close to the text.

**Acceptance:** swiping and arrow controls choose the same date; multiple source variants are distinguishable; load failure has Retry; every unavailable passage has a useful next action.

### 24. Gitsawe passage / Misbak excerpt

Source: [GitsawePassageScreen](../app/src/main/java/com/agpeya/app/ui/gitsawe/GitsawePassageScreen.kt#L121).

**Preserve:** focused excerpts, full-chapter/book exits, commentary, and the current unique list keys for printed chant lines. The recent duplicate-key fix should not be reported as an outstanding defect.

**UX — P2:** note routes serialize the citation but omit the printed chant payload. Verify that reopening a note restores the same excerpt/edition rather than a substitute Psalm text. When Amharic is unavailable and the code falls back to Ge'ez, identify that fallback instead of silently implying translation.

**State — P2:** distinguish unavailable citation from an exception, and include a useful escape such as opening the full book. Share output should preserve the printed chant's provenance and reference.

**Acceptance:** every Misbak note/share reopens or identifies the same text; unavailable translation is clearly labeled; a missing citation does not strand the user.

### 25. Sunday cycle: options and selected service

Source: [SundayCycleScreen](../app/src/main/java/com/agpeya/app/ui/gitsawe/SundayCycleScreen.kt#L47).

**Preserve:** selected-service Back behavior and links to full hymns where found.

**State — P2, confirmed:** an empty loaded list produces an empty options list. Explain that no Sunday-cycle entry is available for this date and provide a return/change-date action.

**UX — P2:** preserve the selected date in the title; distinguish service roles and full scripture references. A failed hymn lookup currently removes the “full hymn” doorway; make this limitation understandable when users expect continuation.

**Acceptance:** empty dates, multiple options, missing hymn matches, and unsupported citations remain understandable; Back first exits the selected option, then returns to the originating date.

### 26. Synaxarium reader and contents

Source: [SynaxariumScreen](../app/src/main/java/com/agpeya/app/ui/gitsawe/SynaxariumScreen.kt#L189).

**Preserve:** bilingual editions, retries, day navigation, contents, selectable paragraphs, and closing prayer.

**Completion — P1, F09:** today's habit is marked done immediately after entries load. Opening accidentally, or only reading a heading, records completion. Replace this with the agreed reader completion rule and an explicit correction path.

**UX — P2, confirmed:** bookmarks store a day route without the entry anchor even though the screen accepts `initialEntry`. Save the entry/paragraph location so opening a mark returns to the saved text. Reset/restore scroll intentionally when changing day or edition; the existing list state is shared.

**UI — P2, risk:** previous, next, calendar, contents, and tools compete with the title. Move date navigation to a dedicated row on narrow screens. Verify that paragraph bookmark icons do not cover text and selection tools do not hide the closing prayer.

**Acceptance:** merely opening does not complete the habit; a saved entry reopens at that entry; date and edition changes never leave an unrelated mid-page offset.

### 27. Church-book shelves and shelf detail

Source: [BookShelfScreen](../app/src/main/java/com/agpeya/app/ui/books/BookShelfScreen.kt).

**Preserve:** traditional shelves, descriptions, and collection counts.

**State — P2:** initial empty index and zero counts can look like final data; invalid/empty shelf IDs can produce a blank list. Add loaded-empty and unavailable states with recovery.

**UX/product — P2:** expose text language, book/recension relationships, and search at catalog level. Count labels should describe what is counted—books, orders, or chapters—consistently. Provide a way to reveal full long titles truncated in rows.

**Acceptance:** a missing shelf is explained; a new user can distinguish church books from Bible books and Mahlet orders; long titles remain identifiable.

### 28. Church-book reader and recensions

Source: [BookScreen](../app/src/main/java/com/agpeya/app/ui/books/BookScreen.kt#L102).

**Preserve:** bilingual gloss styling, rubrication, chapter titles, and links between recensions.

**State — P2, confirmed:** nullable book/meta and empty chapters feed directly into reading content without explicit loading or error UI. Provide an unavailable state and catalog return.

**UX — P1/P2, risk:** chapter changes do not explicitly reposition the list; variant metadata also changes leading item counts. Restore by stable chapter/block anchor and reset to chapter start for explicit next/previous actions. Expose font tools consistently—the file reads the font setting but does not offer the same tools menu as neighboring readers.

**Accessibility — P2:** block selection changes the background without a corresponding selected semantic. Make selection state and range discoverable.

**Acceptance:** next chapter starts predictably; a linked hymn block lands accurately even when variant metadata loads later; unavailable books do not leave blank pages.

### 29. Mahlet month catalog and search

Source: [MahletListScreen](../app/src/main/java/com/agpeya/app/ui/mahlet/MahletListScreen.kt#L97).

**Preserve:** month paging, feast-name search with Amharic folding, grouped service options, and the Tsige entry.

**State — P2:** an empty initial index immediately renders “none”; today's orders are loaded once with `LocalDate.now`. Distinguish loading and refresh date-sensitive content.

**UX — P1, related to F07:** service chips send distinct IDs, but the destination must honor those IDs. Today's grouped card opens only its first order even though it lists several; make each advertised service independently actionable or label the card's destination clearly.

**UI — P2, risk:** month-strip scrolling uses a pixel estimate rather than measured item position. Test differing densities and long month names; use actual item positioning. Nested service targets must have adequate separation.

**Acceptance:** every displayed service opens that service; the selected month stays visible across font/density settings; failed loading does not look like a month with no content.

### 30. Mahlet Tsige seasonal catalog

Source: [MahletSeasonScreen](../app/src/main/java/com/agpeya/app/ui/mahlet/MahletSeasonScreen.kt#L65).

**Preserve:** separation between this year's appointed Sundays, other-year texts, and undated orders.

**UX — P2:** explain these distinctions before the lists, with a clear current-year label. Provide a useful empty/loading/unavailable state; today/year is remembered once and should refresh when appropriate.

**UI — P2:** ensure date numerals and long feast names remain readable without reducing the entire row to ellipsis.

**Acceptance:** current-year and reference-only orders are clearly distinguished; the page remains useful out of season and with missing content.

### 31. Mahlet reader: service tabs, versions, contents, and references

Source: [MahletScreen](../app/src/main/java/com/agpeya/app/ui/mahlet/MahletScreen.kt#L100).

**Preserve:** multiple service kinds, replacement editions/translations, part navigation, rubric styling, and full-hymn links.

**Correctness — P1, F07:** after loading the requested order and siblings, `tab` remains zero. Initialize it from `orders.indexOfFirst { it.id == orderId }` once loading completes, respecting subsequently restored user state.

**Correctness — P1, F08:** contents offset calculation counts one editions item, while both `editionsNote` and `editions` are inserted. Use stable part keys or a derived item model rather than manually counting leading rows.

**UI/UX — P2:** service/edition controls scroll away, and changes share one list position. Keep current service/edition visible in a compact summary and intentionally reset/restore location on change. Long part titles, ordinal counts, and “full hymn” links compete on one line. Reflow those metadata/actions at large fonts.

**State/product:** add unavailable/retry states and clearly mark external Telegram references. Missing external handlers should be recoverable.

**Acceptance:** opening each sibling service selects the requested one; every contents jump is correct with and without versions/references; changing edition does not leave the reader at an unrelated offset.

### 32. Search and results

Source: [SearchScreen](../app/src/main/java/com/agpeya/app/ui/search/SearchScreen.kt#L85).

**Preserve:** debouncing, reference shortcuts, source filters, recent searches, and highlighted snippets.

**UX — P2, confirmed:** previous results remain visible while a new query searches; loading is shown only if results/reference are empty. Users can tap stale results under a new query. Clearly indicate updating, bind results to the resolved query, or temporarily prevent stale navigation.

**State — P2:** search calls have no local error state and can leave `searching` unresolved on failure. Add Retry and preserve the query. Explain the two-character minimum and provide examples when there is no history.

**UI/accessibility — P2:** result-title/source-tag rows need narrow-screen testing; announce updated result counts without excessive interruption. The IME Search action needs an intentional keyboard-dismiss/focus behavior.

**Product:** disclose search scope. Current source groups do not represent every app feature, including personal records and a dedicated Mahlet source. Do not imply a universal search if the product only searches text collections.

**Acceptance:** changing a query cannot open an old result as if it matched the new query; no-result, unavailable, single-character, citation, and mixed-source states are clear.

### 33. Marks: bookmarks, highlights, notes, and text export

Source: [MarksScreen](../app/src/main/java/com/agpeya/app/ui/marks/MarksScreen.kt#L78), [journal query](../app/src/main/java/com/agpeya/app/data/JournalDatabase.kt#L114).

**Preserve:** a unified return point for saved reading and named highlight colors.

**Privacy — P0, F02:** passage-linked regular journal notes are loaded and displayed without `JournalLock` or `SecureScreen`. Export serializes all notes regardless of the currently selected tab, without the backup flow's journal opt-in/authentication. Confession drafts are excluded by the database query; this finding concerns the remaining private notes. Reuse one authorized export path and gate sensitive previews.

**UX — P2, confirmed:** tapping a note opens its source passage, not its note editor. Offer clear “Open note” and “Open passage” actions. Give each empty tab specific guidance. Removal should have Undo; exports need explicit success/failure and scope.

**Product:** add search/filtering when the collection grows; named highlight colors should be useful for retrieval, not only display.

**Acceptance:** a locked journal's note text cannot be read or exported here; exporting Bookmarks does not silently export notes; note and source navigation are unambiguous.

### 34. Journal list and month navigation

Source: [JournalScreen](../app/src/main/java/com/agpeya/app/ui/journal/JournalScreen.kt#L118).

**Preserve:** local records, date grouping, optional passphrase, secure-window intent, and distinct reflection/confession kinds.

**Privacy — P0, F01:** the top bar is built outside the locked-content branch. Its menu enables change/remove, and removal directly calls `clearPassphrase`. Require current authentication for both actions; a confirmation dialog alone is not authentication. Use unknown/loading/locked/unlocked states rather than initially assuming unlocked.

**UX/accessibility — P2:** the compact month strip has tiny adjacent targets and generic action labels. Expose dates and entry counts, provide a larger date selection alternative, and label previous/next month rather than relying on arrow glyphs. Refresh the remembered date across midnight.

**State — P2:** separate loading from a genuinely empty journal. Clearing confession drafts should wait for success before presenting the follow-up penance prompt.

**Acceptance:** from a cold locked journal, no toolbar route can remove/reset the passphrase without verification; month navigation announces dates; an empty month is distinguishable from an empty journal or load failure.

### 35. Journal entry editor

Source: [JournalEntryScreen](../app/src/main/java/com/agpeya/app/ui/journal/JournalEntryScreen.kt#L79).

**Preserve:** autosave, exit-time detached saving, linked source references, and delete handling that avoids resurrecting an erased entry.

**Privacy — P1, architecture risk:** the editor uses `SecureScreen` but does not enforce the journal gate itself. Protect every entry route with shared authorization rather than relying solely on the list. Test note creation, existing-entry navigation, restored navigation, and notification routes; this report does not claim arbitrary public deep links are exported.

**UX — P1/P2, race risk:** asynchronous draft loading can replace the current body while the text field is already available. Keep the editor in a loading state until initialized, then show Saving/Saved/Couldn't save. Current delayed-save errors have no visible recovery.

**UI — P2:** a 400dp text field and no dedicated IME handling need reflow. Keep writing area and current save state visible with the keyboard. Explain the consequence of switching an existing reflection to a confession draft.

**Acceptance:** type immediately after opening a slow-loaded entry; no text is overwritten. Leave before the debounce, rotate, and relaunch; text is retained or an explicit failure is shown. All private entry routes authenticate consistently.

### 36. Journal passphrase, unlock, and secure-session behavior

Sources: [JournalLockGate](../app/src/main/java/com/agpeya/app/ui/journal/JournalLockGate.kt), [SecureScreen](../app/src/main/java/com/agpeya/app/ui/journal/SecureScreen.kt).

**Preserve:** confirmation of a new phrase, validation, no-recovery explanation, and screenshot protection on sensitive screens.

**UX — P2:** support keyboard Done, visible verification progress, error focus/announcement, and scrollable forms with the keyboard. Use appropriate password input configuration; offer a reveal control if it can be done without weakening privacy.

**Privacy — P1, lifecycle risks:** current unlocked state is per composition, not a clearly defined foreground session. Test backgrounding, recents, rotation, and transitions between secure screens. Independent add/clear operations on the same activity's secure flag can conflict during navigation; use a shared owner/session policy.

**Product:** describe the lock honestly as an app access gate, not a guarantee that exported files are encrypted. Keep recovery expectations clear before setup.

**Acceptance:** background/return follows a documented relock policy; all secure-screen transitions protect recents/screenshots; failed verification preserves the field and announces the error.

### 37. Confession preparation

Sources: [ConfessionPrepScreen](../app/src/main/java/com/agpeya/app/ui/nisiha/ConfessionPrepScreen.kt), [bundled examination](../app/src/main/assets/content/nisiha/examination.json).

**Current product status — confirmed:** the bundled examination has an empty intro and no sections. The implemented wizard is not a usable shipped preparation flow; it displays Coming Soon. Existing Coming Soon labels are therefore accurate and should not be removed merely because wizard code exists.

**UX/product — P2:** offer a useful existing action from the placeholder, such as writing a private confession draft, or remove the entry until content is ready. Explain availability without implying a completed feature.

**Before activation:** review the content, persistence, privacy, step navigation, and error recovery. Wizard notes live in saved UI state until the final explicit save; leaving midway needs a clear draft policy. Saving needs `try/finally` and a recoverable error so a failed write does not leave the button permanently busy.

**Acceptance:** current users never mistake a placeholder for available guidance; future content activation has tested draft recovery, locked access, keyboard handling, and successful final save.

### 38. Penance list, configuration, quota, and progress recording

Source: [PenanceScreen](../app/src/main/java/com/agpeya/app/ui/nisiha/PenanceScreen.kt#L82).

**Preserve:** private-screen treatment, neutral reminder titles, explicit deletion confirmation, and support for several types of practice.

**UI — P2, risk:** each card is a full form, with label/switch/delete on one row and five kind chips on another unwrapped row. Separate summary from editing and allow chips/actions to wrap. Inline form editing across many records becomes difficult to scan.

**UX — P2:** Add immediately creates and enables a reminder before a meaningful label/configuration is entered. Let the user finish a draft before enabling it. Progress records have no visible edit/undo action; accidental quantities need correction. A blank quantity saves zero, so explain when note-only progress is intended.

**Product:** distinguish reminder enabled, quota completed, and obligation settled. Avoid making a number stand in for pastoral judgment.

**Acceptance:** a mistaken progress entry is correctable; adding and cancelling does not leave an unnamed enabled reminder; five kinds remain accessible at large text; the journal's privacy policy also protects this page.

### 39. Communion preparation — latent route, not exposed in Library

Sources: `CommunionPrepScreen` (removed during remediation at the user’s request), [route comment](../app/src/main/java/com/agpeya/app/MainActivity.kt#L964), [content](../app/src/main/assets/content/kurban/kurban.json).

**Status:** the route exists but is intentionally not exposed as a normal Library feature. Treat it as a pre-release surface, not a missing live menu item.

**Before activation — P2:** distinguish content loading from Coming Soon; refresh daily checklist state at midnight; make checklist rows expose checked state; give expanded prayers an expanded/collapsed semantic. Status summaries involving confession drafts or penance need the same privacy policy as those records.

**Product:** a checklist should help preparation without presenting the app as deciding readiness. Review content and language before making it discoverable.

**Acceptance:** no accidental public entry before content readiness; activated checklist states are accessible, date-correct, private where needed, and clearly described.

### 40. Prayer list, add-name row, person editor, and removal

Source: [PrayerListScreen](../app/src/main/java/com/agpeya/app/ui/prayerlist/PrayerListScreen.kt#L121).

**Preserve:** living/departed groups, fast repeated name entry, an explicit edit dialog, and Undo after removal. This is a good interaction pattern to reuse elsewhere.

**UX — P2:** the top-bar Add action expands an editor at the bottom of a potentially long list. Explicitly scroll/focus it; focus alone should not be relied upon to locate a newly offscreen lazy item. Do not clear the typed name until persistence succeeds.

**UI — P2, risk:** person name and note are flattened into one ellipsized line; use a secondary line when needed. The edit dialog needs keyboard/large-text scrolling. Consider search for long lists before adding more grouping controls.

**Acceptance:** Add from the top of a 100-person list reveals the input; failed saving keeps the name; swipe removal and dialog removal both offer a working Undo.

### 41. Fasting calendar

Source: [FastingScreen](../app/src/main/java/com/agpeya/app/ui/fasting/FastingScreen.kt#L57).

**Preserve:** a clear today state, annual fast list, weekly note, and reactive current date.

**UI — P2:** current/past/future dots need a textual equivalent so color is not the only status indicator. Long fast names, date spans, and duration must reflow at large text.

**UX/product — P2, proposal:** add a clear upcoming-fast summary and calendar convention. If browsing other years is a real use case, provide it consistently with Bahre Hasab rather than making users infer the relationship. An empty calculation result should not silently look like a year without fasts.

**Acceptance:** no-fast, weekly-fast, and annual-fast states are distinguishable; a user can identify the next relevant date without interpreting dot colors.

### 42. Bahre Hasab reference

Source: [BahreHasabReferenceScreen](../app/src/main/java/com/agpeya/app/ui/library/BahreHasabReferenceScreen.kt#L69).

**Preserve:** current/future year selection, cycle values, and dual-calendar observance dates.

**UI — P2, risk:** a long year rail, another horizontal values rail, and a fixed two-column observance grid require testing at 320dp/200%. Switch to one column where names and dates become hard to read.

**UX — P2:** offer a clear return to the current year. Explain technical terms with short optional definitions. Several labels are hardcoded Amharic even with English UI; preserve traditional terms but add localized explanations where useful.

**Product:** keep this a reference tool; link dates to the relevant daily reading or fasting context only when that destination is accurate.

**Acceptance:** the chosen year remains clear after scrolling; all cycle values have understandable names; dates do not truncate at enlarged text.

### 43. Catena commentary WebView

Source: [CatenaScreen](../app/src/main/java/com/agpeya/app/ui/catena/CatenaScreen.kt#L64).

**Preserve:** source identity, reference context, early-fathers preference, browser escape, and blocked file/content access.

**State — P2, confirmed:** there is progress UI but no app-owned network/error/retry state. Offline commentary should explain that the bundled scripture remains available and offer Retry or return to the passage.

**UX — P2:** distinguish online third-party commentary before entry. Toolbar Back exits the screen while system Back may navigate WebView history; make that distinction predictable. Preserve the original scripture reference while navigating within commentary.

**Product:** qualify the offline promise accurately; do not let a failed external site appear to be missing Bible content.

**Acceptance:** airplane mode, loading failure, external navigation, browser absence, and repeated Back presses all have understandable outcomes.

### 44. Settings overview

Source: [SettingsOverviewScreen](../app/src/main/java/com/agpeya/app/ui/settings/SettingsOverviewScreen.kt#L72).

**Preserve:** category-based navigation, theme/language controls, and summaries of current preferences.

**Correctness — P1, F10:** the reminder summary count omits Bible reminders and prayer-mode entries. “Off” can be shown while reminders are active. Derive summaries from the same complete model as the reminder page.

**UI/accessibility — P2:** the large-font radio fallback is useful but needs one logical selectable row rather than separate row/radio focus targets. Display understandable text-size labels rather than exposing `sp` as the only explanation.

**UX — P2:** prayer-level names are hardcoded Amharic in the shared helper even when the surrounding UI is English. Preserve terminology but provide understandable localized meaning.

**Acceptance:** all reminder combinations produce accurate summaries; theme/language changes persist; each choice is announced once with its selected state.

### 45. Reading settings and live preview

Source: [ReadingSettingsScreen](../app/src/main/java/com/agpeya/app/ui/settings/SettingsScreen.kt#L627).

**Preserve:** preview, size, spacing, alignment, font choice, and copy-format access.

**Correctness — P1, F11:** keep-screen-on is advertised for reading generally but only consumed by the hour reader. Apply it consistently or accurately label its scope. Horizontal reading mode is implemented in the prayer-hour reader and Psalter, rather than every text reader; make this scope clear.

**UI — P2, confirmed:** preview typography is constructed manually and bypasses the optical scaling and line-box rules of `readingBodyStyle`. Reuse the actual reader style so the preview predicts the result. Offer readable size labels and sensible reset controls.

**Acceptance:** all supported readers match the preview for every font/size/spacing combination; scope-specific settings state their scope; keep-screen-on works exactly where promised.

### 46. Reading-font page

Source: [ReadingFontScreen](../app/src/main/java/com/agpeya/app/ui/settings/SettingsScreen.kt#L1199), [ReadingFontPicker](../app/src/main/java/com/agpeya/app/ui/settings/SettingsScreen.kt#L186).

**Preserve:** real Ethiopic samples rather than font names alone.

**UI/accessibility — P2:** the page is a non-scrollable Column of four samples; large text/landscape can push choices offscreen. Selected state is shown by border/check without explicit single-choice semantics. Use a scrollable selectable group.

**UX:** show a longer sample with numerals and mixed punctuation, using the same reader rendering. Preserve the current selection when returning.

**Acceptance:** all four fonts can be reached and identified at 200% text size and in landscape; TalkBack announces the selected font.

### 47. Copy format and highlight names

Source: [CopyFormatScreen](../app/src/main/java/com/agpeya/app/ui/settings/SettingsScreen.kt#L788).

**Preserve:** live output preview and reader-defined names for highlight colors.

**UX — P2:** copying format and organizing highlights are separate jobs. Group them distinctly and add clear paths from the relevant reader actions/Marks filters. Show an actual color sample alongside its name; allow reverting to the default label.

**Product:** make unavoidable attribution/signature behavior clear before sharing. Keep the preview accurate for one verse, a range, and text without verse numbering.

**Acceptance:** copied output matches the preview; custom color names are used consistently in selection tools and saved-item retrieval; long labels do not obscure controls.

### 48. Prayer settings and prayer-level sheet

Source: [PrayerSettingsScreen](../app/src/main/java/com/agpeya/app/ui/settings/SettingsScreen.kt#L883).

**Preserve:** a small landing page linking level, hours, and habits.

**UI — P2, risk:** five detailed levels occupy a non-scrollable sheet. Make the body scrollable and use a single selectable radio target per option.

**UX/product — P2:** explain what changing the level actually changes, distinguish it from reminders and visible hours, and retain historical records. Labels should be understandable in both UI languages.

**Acceptance:** every level remains reachable with large text; users can predict affected prayer content; the setting does not silently change unrelated reminder preferences.

### 49. Manage prayer hours

Source: [ManageHoursScreen](../app/src/main/java/com/agpeya/app/ui/hours/ManageHoursScreen.kt#L118).

**Preserve:** a unified place to rename, show/hide, reorder, customize, and change hour reminders.

**UX — P1/P2:** only the first matching reminder entry is represented for an hour. A custom mode can contain multiple entries for that hour; clarify whether the switch controls one or all, and disclose additional times. Otherwise a user may think an hour is off while another reminder remains enabled.

**UX — P2:** enabling here lacks the notification-permission request path present in Mode Editor. Align permission handling and show an accurate enabled-but-blocked state. Custom-hour removal is immediate; add recovery and explain effects on schedules/history.

**UI:** time, name, switch, and menu are distinct targets; make the primary row action and secondary time editing obvious at large text.

**Acceptance:** multiple reminders for one hour cannot be hidden behind a misleading single switch; enabling on Android 13+ handles denied permission; removal is recoverable.

### 50. Customize prayer-hour sections and add-Psalm picker

Source: [CustomizeScreens](../app/src/main/java/com/agpeya/app/ui/customize/CustomizeScreens.kt#L63).

**Preserve:** reversible hiding, non-drag reordering, and adding Psalms.

**UI — P2, risk:** multiple controls beside each section squeeze titles. Use a simpler row with a deliberate edit/actions surface. Distinguish hidden from removed content in text, not opacity alone.

**UX — P2:** Reset applies immediately, and removing an added Psalm has no Undo. Describe what reset restores and provide recovery. The picker should indicate already-added Psalms and explain supported number input; loading and no-match states need explicit messages.

**Acceptance:** a heavily customized hour can be reset intentionally and recovered where promised; added/hidden/default sections are distinguishable; empty customization leads to a useful reader state.

### 51. Reminders settings, daily timeline, quiet hours, and sound sheet

Source: [RemindersSettingsScreen](../app/src/main/java/com/agpeya/app/ui/settings/SettingsScreen.kt#L928), [QuietHoursRow](../app/src/main/java/com/agpeya/app/ui/settings/SettingsScreen.kt#L519), [DayTimeline](../app/src/main/java/com/agpeya/app/ui/settings/SettingsScreen.kt#L1224).

**Preserve:** permission guidance, no-active-hour warning, quiet-hour conflict explanation, and the consolidated day timeline.

**Correctness — P1, F10:** quiet-hour conflict counting omits reading and penance; timeline entries are loaded once without setting dependencies. Toggle a reminder or change quiet hours and the displayed schedule can remain stale. Replace separate ad hoc counts with a reactive shared schedule/eligibility model.

**UX — P2:** distinguish “enabled,” “scheduled today,” “already completed,” “inside quiet hours,” and “blocked by Android.” Battery-optimization exemption is not proof that reminders will or will not arrive; phrase it as guidance, not a definitive failure diagnosis.

**UI — P2, risk:** the sound sheet contains several groups and snooze choices in a non-scrollable Column. Allow scrolling and a sound preview with a clear stop action. Explain 24-hour device time consistently, particularly alongside Ethiopian date/time conventions.

**Bible behavior to preserve:** automatic 06:30 / 14:00 / 20:00 local reminders, only while today's required reading is unfinished, respecting quiet hours and the master toggle. No required scheduling questionnaire.

**Acceptance:** edits immediately update summaries and timeline; quiet-hour warnings include every affected family; complete reading suppresses later slots; sound choices remain accessible at 200% text size.

### 52. Prayer modes list

Source: [ModesScreen](../app/src/main/java/com/agpeya/app/ui/modes/ModesScreen.kt#L54).

**Preserve:** built-in mode, duplication, custom empty mode, active choice, and deletion confirmation.

**UX — P2:** row tap edits while the radio activates. These are different actions that need clear affordances and spoken labels. Explain “mode” with an example such as different daily schedules, and show the currently active one prominently.

**State — P2:** empty initial state should not look like no modes. Recheck notification availability on resume, as the main reminders page already does. Show feedback if activating/rescheduling fails.

**Acceptance:** selecting versus editing is unambiguous; returning from Android notification settings refreshes status; deleting the active custom mode explains its replacement.

### 53. Mode editor and reminder-entry sheet

Source: [ModeEditorScreen](../app/src/main/java/com/agpeya/app/ui/modes/ModeEditorScreen.kt#L65).

**Preserve:** sorted reminder entries, explicit no-days validation, notification-denial handling, and editing built-in times.

**UI — P2, risk:** a non-scrollable entry sheet combines hour chips, a clock, seven day chips, and Save/Delete. This is a high-probability overflow surface on small screens or large text. Use a scrollable form, wrapping days, and a compact time-input alternative.

**UX — P1/P2, confirmed:** saving an edited entry forces `enabled = true`, even if it was previously disabled. Preserve the previous state or explicitly label that Save enables the reminder. Reset built-in times and delete-entry actions need a clear consequence and recovery. A missing mode currently produces a blank body.

**Acceptance:** Save is always reachable; editing a disabled reminder does not unexpectedly enable it; missing modes show recovery; resetting times is deliberate.

### 54. Alms reminders

Source: [SpecialHabitScreen](../app/src/main/java/com/agpeya/app/ui/settings/SpecialHabitScreen.kt#L80), `intention/{habit}` with ALMS.

**Preserve:** multiple named intentions and flexible cadence.

**UX — P2:** Add immediately persists an unnamed enabled reminder and requests permission. Use a draft editor and activate only after the user saves the intended reminder. Deletion is immediate and needs Undo. The next-due label should distinguish disabled reminders from scheduled ones.

**UI:** collapse existing reminders into readable summaries; move detailed editing into one consistent form with adequate field width.

**Acceptance:** Add/Cancel leaves no enabled artifact; a disabled reminder does not misleadingly imply it will fire; save failures retain the draft.

### 55. Repentance reminders

Source: [SpecialHabitScreen](../app/src/main/java/com/agpeya/app/ui/settings/SpecialHabitScreen.kt#L221), REPENTANCE variant.

**Preserve:** access to penance records and a clearly labeled Coming Soon preparation entry.

**UX/product — P2:** apply the alms form improvements, but review privacy separately. User-entered repentance labels currently become notification titles; explain that visibility and consider a neutral private default. Keep preparation availability honest because bundled examination content is empty.

**Acceptance:** notification text follows the user's privacy expectations; the available reminder task is distinguishable from the unfinished preparation feature; removing a reminder is recoverable.

### 56. Tithe reminder configuration

Source: [SpecialHabitScreen](../app/src/main/java/com/agpeya/app/ui/settings/SpecialHabitScreen.kt#L95), TITHE variant.

**Preserve:** separate ledger and reminder configuration, with navigation from the ledger.

**UX — P2:** make the relationship explicit: changing a reminder does not record a payment. Apply draft/save, deletion recovery, and accurate next-due behavior. Show a route back to the ledger's relevant period.

**Product:** avoid implying the scheduler knows an amount is paid unless it is deliberately integrated with the ledger's rules.

**Acceptance:** recording money and editing reminders cannot be confused; disabled reminders are clearly inactive; Back returns to the expected ledger context.

### 57. Tithe ledger, amount/date form, percentage, and currency

Sources: [TitheScreen](../app/src/main/java/com/agpeya/app/ui/settings/TitheScreen.kt#L80), [OfferingCommon](../app/src/main/java/com/agpeya/app/ui/settings/OfferingCommon.kt#L54).

**Preserve:** separate income/given entries, period totals, deletion confirmation, and optional notes.

**UX — P2:** there is no visible entry-edit action; correcting an amount requires deleting and recreating it. Add editing or reliable Undo. Changing percentage recalculates displayed periods using the current percentage; explicitly define whether that is retrospective. Currency is a shared display label, not a conversion—say so when changing it.

**Calendar — P2, confirmed inconsistency:** dates are displayed in Ethiopian format but the amount dialog opens a Gregorian Material picker. Label the calendar or use the shared Ethiopian picker with a clear equivalent date.

**Data-entry — P2:** `parseAmount` truncates extra decimal digits and arithmetic can overflow for extreme input; validate a reasonable supported range and explain invalid input rather than silently changing it. Test locale separators and zero amounts.

**UI:** stack totals/actions when large figures or text do not fit two columns; the period navigator needs a readable center label.

**Acceptance:** edit/correct entries without losing notes; exact minor-unit totals survive entry; dates are unambiguous; percentage/currency changes explain their effect on history.

### 58. Vows, pledge, and fulfilment history

Source: [VowScreen](../app/src/main/java/com/agpeya/app/ui/settings/VowScreen.kt#L81).

**Preserve:** monetary and nonmonetary fulfilment, one-time versus recurring behavior, and deletion confirmation.

**UX — P2:** cards expose a full form even when simply reviewing progress. Separate view and edit. Adding a vow immediately creates an enabled reminder; use a draft flow. Fulfilments lack visible edit/undo, making accidental settlement difficult to correct.

**Product:** “Record payment” is misleading for prayer or fasting vows with optional amounts. Use “Record fulfilment” and show amount only when relevant. Clearly distinguish pledge amount, remaining amount, reminder enabled, and settled status. Amounts in notifications need an explicit privacy choice or a neutral default.

**Acceptance:** a nonmonetary vow can be understood and completed without money language; a mistaken fulfilment can be corrected and reminders resume according to policy.

### 59. Shared schedule editor, day controls, and time pickers

Source: [ScheduleEditor](../app/src/main/java/com/agpeya/app/ui/settings/ScheduleEditor.kt#L123).

**Preserve:** weekly, alternate-day, monthly, yearly, and feast-based schedules with contextual feast descriptions.

**Correctness/UX — P2, confirmed discrepancy:** yearly Pagume displays a maximum of five days but Save clamps only to 1..30; switching from another month can retain an invalid displayed day. Leap-day behavior is also not represented. Validate month/day as a pair and explain the rule for a leap-only anniversary.

**Accessibility — P2:** steppers labeled only “−” and “+” need context such as previous month/increase day. Feast rows combine a toggleable parent and clickable RadioButton; make one logical selectable target.

**UI — P2, risk:** dialog content needs scrolling at large text. For alternate-day schedules, display the anchor/next due date instead of leaving an unexplained empty body.

**Acceptance:** changing month never saves an invalid combination; leap-year dates have a defined rule; TalkBack identifies what each stepper changes; Save remains reachable with keyboard/landscape.

### 60. Records and personal-data landing page

Source: [RecordsScreen](../app/src/main/java/com/agpeya/app/ui/settings/RecordsScreen.kt).

**Preserve:** an accessible home for personal data, backup, and existing records.

**IA — P2, proposal:** offerings, penance, Marks, prayer list, fasting, identity, and backups are different jobs. Group them into “My records,” “Personal details,” and “Backup & restore”; reconsider placing a calendar/reference tool under personal records. Journal discovery should be consistent with Journey, without duplicating private previews here.

**UI/UX:** summaries should state available records or settings, not technical storage details. Use direct task labels, especially for export versus restore.

**Acceptance:** users can find a payment record, change their name, and create a backup without opening unrelated categories.

### 61. Backup export, journal verification, restore preview, and results

Source: [BackupRows](../app/src/main/java/com/agpeya/app/ui/settings/SettingsScreen.kt#L251).

**Preserve:** explicit category selection, journal excluded by default, passphrase verification for locked-journal export, plaintext warning, restore preview, and success/failure dialogs. Reuse these protections for Marks export.

**UX — P1/P2, confirmed:** restore preview can say “nothing new” based only on new days/bookmarks, even though other categories exist. Show all relevant category counts and what will merge versus replace. Do not imply that every personal setting or record is restored identically.

**Scope — P1, F15:** [BackupRepository](../app/src/main/java/com/agpeya/app/data/BackupRepository.kt#L101) exports tithe percentage and currency unconditionally; restore applies the percentage and any nonblank currency. A bookmarks-only backup can therefore change unrelated offering settings. Encode selected categories explicitly, or make omitted category settings nullable and preserve local values on restore.

**State — P2:** file operations need visible busy state and protection from repeated submissions. Selection and authentication state must survive the system file picker safely. Test cancellation and process recreation rather than assuming a successful handoff.

**Product:** make the backup's scope and plaintext nature clear before sharing it. Explain what is deliberately excluded, including confession-related data according to repository policy, and what happens to reminders after restore.

**Acceptance:** exports contain only chosen categories; locked journal inclusion requires verification; restore containing only highlights/other records does not falsely say nothing new; corrupt and unsupported files preserve existing data and show a useful error.

### 62. Battery/reminder troubleshooting

Source: [BatteryHelpScreen](../app/src/main/java/com/agpeya/app/ui/settings/BatteryHelpScreen.kt#L36).

**Preserve:** concise OEM/background guidance and a direct Android settings link.

**UX/product — P2:** start with actual app status—notification permission, enabled schedule, quiet hours, and next expected reminder—before generic battery advice. A settings link alone does not tell a user whether the problem was fixed. Offer a clearly labeled test notification and refresh status on return.

**Acceptance:** a user can distinguish an intentional quiet-hour suppression from permission denial; an unavailable settings intent is explained; the test result leads to a specific next step.

### 63. Changelog and replay tour

Source: [ChangelogScreen](../app/src/main/java/com/agpeya/app/ui/settings/ChangelogScreen.kt#L587).

**Preserve:** bilingual release notes and replay access.

**UI/UX — P3:** a long history of expanded cards is hard to scan. Show installed/latest context, a short current-release section, and expandable older entries. Keep user-visible behavior first and put implementation details behind a secondary technical link where useful.

**Product:** validate release claims against current behavior. The Mahlet service-chip claim needs the destination fix described in F07; accurate release communication is part of trust.

**Acceptance:** users can identify their installed release and replay its tour; old release notes remain accessible without overwhelming the page.

### 64. About

Source: [AboutScreen](../app/src/main/java/com/agpeya/app/ui/settings/AboutScreen.kt#L41).

**Preserve:** source attribution, version, privacy explanation, maintainer contact, and license access.

**Localization — P2, confirmed:** the screen explicitly uses EnglishStrings even when the app is Amharic. Localize product explanations and navigation; retained proper names and original source titles are appropriate.

**UX/product:** privacy text should match actual app-gate behavior, plaintext export, optional online commentary, and update checks. Make the support destination and need for an external app/network clear; show a fallback if it cannot open.

**Acceptance:** both UI languages have understandable About/privacy content; displayed version matches the build; support links either work or show a recoverable message.

### 65. Licenses and source credits

Source: [LicensesScreen](../app/src/main/java/com/agpeya/app/ui/settings/LicensesScreen.kt#L36).

**Preserve:** detailed corpus-specific attribution and distinction between app code and content licenses.

**UI/UX — P2/P3:** long English text and plain URL strings are difficult to navigate/copy. Add a contents list, selectable text, usable source/license links, and localized explanatory headings while retaining authoritative legal text. Bundle-access instructions should be understandable to an ordinary user, not just an asset path.

**Product:** source accuracy and permissions are a separate editorial/legal verification task; this UI audit does not certify rights to every corpus.

**Acceptance:** users can find a particular corpus's attribution quickly, copy its source, and open a license without searching externally.

### 66. Shared reader tools, verse selection, highlights, cross-references, and sharing

Sources: [SectionUi](../app/src/main/java/com/agpeya/app/ui/reading/SectionUi.kt#L337), [ShareMenu](../app/src/main/java/com/agpeya/app/ui/common/ShareMenu.kt), [ReaderTitle](../app/src/main/java/com/agpeya/app/ui/common/ReaderTitle.kt#L121), [PassageShare](../app/src/main/java/com/agpeya/app/ui/common/PassageShare.kt).

**Preserve:** one reusable selection action system, named highlight colors, range references, copy formatting, and image choices.

**Discoverability — P2:** tap-to-select and tap-again-to-extend is not self-evident, particularly beside native SelectionContainer behavior. Offer a brief contextual hint and accessible equivalent. Clearly distinguish a temporary selection, a saved highlight, and the original citation tint.

**Accessibility — P2:** chapter sheets highlight the current row by background but should announce selection. Named swatches also need selected state, not only color labels. Long cross-reference lists and sharing options must scroll.

**UX — P2, lifecycle risk:** image generation launches from inside the animated selection surface after dismissal. Verify that closing selection does not cancel a slow render/share operation when its composition disappears. Keep a stable operation owner and communicate busy/success/failure. Do not close a user's context on a failed action.

**Product:** standardize available core tools across readers, while allowing corpus-specific omissions. A reader should explain unavailable commentary or translations without pretending every corpus has the same capabilities.

**Acceptance:** select a range with TalkBack, copy/share it, open a cross-reference, return, and recover the original place; very long image output is not silently truncated; cancellation/failure preserves context.

### 67. Shared Ethiopian date picker

Source: [EthiopianDatePicker](../app/src/main/java/com/agpeya/app/ui/common/EthiopianDatePicker.kt#L61).

**Preserve:** Ethiopian months, saved selection state, leap-year month length, and bounded day-grid scrolling.

**Accessibility — P2, confirmed:** year arrows are described as previous/next day. Correct the labels. Day cells use visual highlighting without selected semantics; announce the full selected date and calendar.

**UI — P2, risk:** six columns inside a dialog can produce tight targets. Add adequate target space or a larger alternative, and test the month rail with enlarged text. For distant dates, consider direct year entry rather than many taps.

**Acceptance:** year arrows announce year changes; every selected day is spoken with month/year; Pagume 5/6 transitions are correct; users can reach distant dates without losing the selection.

### 68. Notification and alarm surfaces

Sources: [AlarmRinger](../app/src/main/java/com/agpeya/app/reminders/AlarmRinger.kt#L105), [ReadingReminderReceiver](../app/src/main/java/com/agpeya/app/reminders/ReadingReminderReceiver.kt), [SpecialHabitReminderReceiver](../app/src/main/java/com/agpeya/app/reminders/SpecialHabitReminderReceiver.kt#L68).

**Preserve:** explicit Open/Snooze/Dismiss for prayer alarms, distinct notification IDs, generic penance titles, and reading reminders based on unfinished assignments.

**UX — P1/P2:** notification taps are intentional task entries and should bypass launch/tour interruption. The reading reminder opens the dashboard; consider opening the next unread assignment with a clear route back to the plan. Show only relevant unfinished references when partial completion exists.

**Privacy — P2:** repentance labels and vow remaining amounts can appear in notifications. Test actual lock-screen behavior with platform settings; do not assume a private in-app record means private notification content. Prefer neutral defaults for sensitive details.

**Product:** define how nightly practice reminders coexist with Bible reminders at night to avoid contradictory prompts. Preserve automatic three-slot reading scheduling and suppress later reminders after all today's assignments are complete. Completing one of two plans must not suppress the other.

**Acceptance:** verify morning partial completion, afternoon completion, quiet hours, two plans, midnight, timezone change, reboot, notification denial, snooze, and stale-notification taps on a real device. App tests alone cannot certify OEM delivery behavior.

### 69. Gitsawe home-screen widget

Source: [GitsaweWidgetProvider](../app/src/main/java/com/agpeya/app/widget/GitsaweWidgetProvider.kt#L104).

**Preserve:** responsive row count, dated content, whole-widget opening, and daily/timezone refresh handling.

**UX/product — P2:** the widget chooses the daily source and one service; the app also offers seasonal/monthly alternatives. Identify that source/service so the difference is intentional and understandable. On load failure the widget mostly falls back to its title; add a concise open-to-retry affordance.

**UI — P2, device risk:** verify small/large launcher sizes, long feast names, night appearance, accessibility labels, and date/reference truncation. This RemoteViews surface does not automatically inherit Compose layout behavior.

**Acceptance:** resize across supported dimensions; displayed source/date matches the opened page; after midnight/timezone change a stale widget does not silently send the user to an unintended day.

### 70. Memento Mori home-screen widget

Source: [MementoWidgetProvider](../app/src/main/java/com/agpeya/app/widget/MementoWidgetProvider.kt#L62), [widget layout](../app/src/main/res/layout/widget_memento.xml).

**Preserve:** simple devotional purpose and a resource content description for the custom lettering image.

**UI/accessibility — P2, confirmed implementation difference:** lettering is rendered into a bitmap using density rather than scaled text units, then shrunk to fit. It does not respond like ordinary large text. Verify readability at supported sizes and consider a scalable text alternative while retaining the custom face where appropriate.

**UX — P2:** the displayed gloss follows the app language, while the image description comes from Android resources. Keep spoken and displayed language aligned after changing app language.

**Acceptance:** the phrase remains readable and correctly announced at minimum widget size and enlarged text; changing language refreshes both visual and spoken content.

## Cross-product design standards to apply

### Visual system: improve application, preserve identity

Retain the warm light background, dark green surfaces, bronze/gold emphasis, sage completion color, and liturgical red. The theme already defines Material roles deliberately. Avoid introducing another palette, decorative card family, or animation system for each feature.

Use a small set of repeatable patterns: a clear page heading; one primary task; ordinary navigation rows for secondary destinations; summaries that open editors; a shared reader toolbar; a shared recoverable state panel; and bounded scrolling sheets. Lists should remain lists—full forms should appear when editing rather than occupying every record card.

Do not mark a layout as “broken” solely because it uses a compact visual icon: Compose can enlarge hit targets outside visible bounds. The concern is deliberately allocated space, proximity, and collisions. Aim for at least 48dp independently usable controls, particularly in grids and adjacent controls, and verify the actual bounds. [Android's default accessibility guidance](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).

Expose checked/selected/expanded state, progress, meaningful dates, and contextual action labels through the semantics appropriate to each control. Decorative icons can remain silent when their parent has a complete label. Custom canvases/grids need a usable semantic equivalent. [Compose semantics documentation](https://developer.android.com/develop/ui/compose/accessibility/semantics).

### Contrast: measured tokens, not a screenshot certification

The following are calculated sRGB relative-luminance ratios for opaque source colors. They do not account for gradients, transparency, highlights, device rendering, or all possible surfaces.

| Foreground / background | Approximate ratio | Interpretation |
|---|---:|---|
| Light gold `#7E5F1E` / ivory `#EFEDE2` | 5.05:1 | Good base pairing for normal text. |
| Light muted `#56655A` / ivory | 5.25:1 | Good base pairing. |
| Dark gold `#E0BC65` / dark ground `#0A2E27` | 8.06:1 | Strong base pairing. |
| Dark muted `#A6C2B4` / dark ground | 7.67:1 | Strong base pairing. |
| Dark liturgical red `#EF3B2E` / dark ground | 3.72:1 | Below the 4.5:1 normal-text benchmark. |
| Dark liturgical red / dark surface `#103A31` | 3.19:1 | Below the normal-text benchmark. |
| Light liturgical red `#A5140A` / ivory | 6.61:1 | Good base pairing. |

Use 4.5:1 for normal text and 3:1 for qualifying large text as design benchmarks; assess actual rendered size and weight before judging a particular red-text occurrence. This is not a declaration of WCAG certification for the native application. [WCAG 2.2 contrast criterion](https://www.w3.org/TR/WCAG22/#contrast-minimum).

### Reader contract

Every reader should consistently answer:

| Question | Expected behavior |
|---|---|
| What am I reading? | Full title, chapter/section/date, language/edition, and source where relevant. |
| Why did I land here? | A citation, bookmark, plan, contents item, or saved position is honored precisely. |
| How do I change place? | Predictable previous/next and an accessible contents/chapter selector. |
| How do I personalize text? | The same font/size/spacing controls and an accurate preview. |
| What is selected? | Visible and spoken selection state, with a clear dismissal. |
| Did I finish? | An explicit, reversible completion rule appropriate to the corpus. |
| Can I return? | Stable anchors and deliberate restoration after navigation/rotation. |
| What if content fails? | Loading, unavailable, Retry, and a useful way back. |

Keep devotional structure where it matters: Wudase's daily opening, Misbak's printed chant, Synaxarium entries, and Mahlet's service/edition distinctions should not be flattened into a generic chapter model.

### Calendar, time, and localization contract

Use a shared current-date source for daily screens. Decide deliberately when browsing a selected historical date should remain fixed versus when “Today” should advance. Test Ethiopian New Year, Pagume, leap years, midnight, and timezone changes.

Label the time convention: the existing reminder schedule uses device-local 24-hour time. Do not mix that silently with traditional Ethiopian clock expressions in explanations. Show the calendar clearly whenever a picker and its displayed result differ.

Separate interface localization from source text. Traditional titles and scripture may remain Amharic/Ge'ez while navigation, errors, status, and task instructions should follow the chosen UI language. Test strings with realistic long content rather than only short English placeholders.

### State and editing contract

Represent loading, loaded-empty, loaded-content, saving, and failure explicitly. An empty list is not sufficient evidence that a user has no records or a corpus has no content. Delay an empty-state CTA until the underlying load has resolved.

For personal forms, preserve drafts through normal navigation/configuration changes, validate before committing, and communicate saving failure. Make delete/reset consequences explicit and offer Undo where feasible. Use the prayer list's removal pattern as a starting point; use the protected backup picker as the model for sensitive export.

## Proposed information architecture refinements

These are hypotheses to validate with users, not a mandate to move every feature at once.

| Area | Main job | Suggested emphasis |
|---|---|---|
| Home | What should I open now? | Today's prayer, daily reading, contextual continue-reading, quiet update notice. |
| Journey | How has my practice been? | Today's honest completion record, history, reflection/journal. |
| Library | Find and read a text. | Scripture, daily devotional texts, church books, Search, saved reading. |
| Settings | Change how the app behaves. | Reading, prayer setup, reminders, personal records/data, help/about. |

Keep one canonical destination per task and add contextual shortcuts rather than duplicate feature implementations. Examples: Home can continue the same plan hosted in Library; Journey can open the same journal protected everywhere; a payment record can link to its own reminder editor.

Test terminology with native Amharic readers and bilingual users. “Mode,” “Records,” “Marks,” and prayer-level names deserve particular attention. The app should describe tasks in familiar language without exposing its repository structure or technical units.

## End-to-end journey audit and acceptance scenarios

| Journey | Current friction | Scenario that should pass |
|---|---|---|
| First prayer | Launch overlay, onboarding, optional tutorial can stack. | New user selects language, skips optional setup, opens a useful prayer, and can personalize later. |
| Return from reminder | Tour/splash can cover the destination. | Tapping an hour/reading notification opens the exact relevant task promptly. |
| Read today's Bible assignment | Whole-day completion is offered from individual chapters. | Finish one passage: remaining passages stay unfinished and later reminders remain eligible. |
| Complete two plans | Multiple independent records and reminders. | Complete plan A: B remains; finish B: later reading slots stay silent. |
| Follow a citation | Chapter-1 restore and reader-position differences. | A link to chapter 1 verse N lands there regardless of previous chapter; Back restores source. |
| Save and revisit a Psalm note | Daily subset index maps to the wrong Psalm. | Create a note in every daily range; all routes/labels match the actual Psalm. |
| Choose a Mahlet service | Requested sibling ID becomes tab zero. | Every service chip, search result, and day link opens its requested service and edition context. |
| Private reflection | Lock administration and Marks bypass the gate. | Locked users cannot read notes or remove/reset/export them through any app route. |
| Record an offering | No edit path; calendar/currency ambiguity. | Correct amount/date/note without destructive recreation and with accurate totals. |
| Diagnose missing reminder | Summary, quiet-hour count, and timeline differ. | One consistent status explains due/completed/quiet/blocked and updates immediately after changes. |
| Restore on another installation | Preview undercounts categories. | All supported categories have accurate previews, defined merge rules, and explicit outcome. |
| Read with large text | Several sheets/forms do not scroll. | All destinations remain operable at 200% text, with the keyboard and screen reader. |

## Recommended implementation sequence

### Phase 1: protect trust and correct results

Address F01/F02 first. Centralize journal authorization across pages and exports; keep unknown lock state closed. Then fix exact reading destinations and completion semantics: F03–F09 and F12. Add focused regression tests around chapter identity, requested Mahlet service, contents offsets, per-plan completion, and authentication boundaries.

**Exit condition:** private notes are not exposed through secondary routes; maps and saved links show the actual passage; completing one passage cannot silently complete unrelated reading. Existing reminder work retains its automatic three-slot behavior.

### Phase 2: make states and forms dependable

Fix F10/F11/F15, loading/error models, date refresh, editor initialization, autosave feedback, and restore preview. Rework non-scrollable sheets/forms, oversized inline record editors, and irreversible small actions. Standardize controls and semantics before changing visual styling.

**Exit condition:** main tasks have clear busy/error/recovery states; settings summaries agree with behavior; every form can be completed at 320dp/200% text with keyboard input.

### Phase 3: improve discovery and visual hierarchy

Refine Home's emphasis, contextual continuation, Library grouping/search entry, tab labels, catalog summaries, and personal-record grouping. Apply the shared reader contract and adjust dark red contrast. Keep the existing visual identity.

**Exit condition:** representative users can find and complete core tasks without a tutorial; they can explain what is selected, saved, private, and scheduled.

### Phase 4: validate and polish

Run the device matrix below, review copy in both languages, exercise real notification delivery, and refine motion/spacing based on observed failures. Update release notes only after the described behavior is verified.

Do not estimate these phases purely by counting screens: shared privacy, reader, and schedule changes have cross-cutting dependencies. The scope should be broken into small reviewable changes after the completion/privacy contracts are agreed.

## Device and usability validation still required

| Dimension | Minimum cases |
|---|---|
| Phone layout | 320dp, 360dp, 412dp widths; portrait and short landscape. |
| Larger windows | Tablet/foldable width, split-screen, resize while reading. |
| Text | Default, 150%, 200%; every offered reading font; smallest/largest in-app reading size. |
| Appearance | Light/dark/system; highlighted text; liturgical red; selected/disabled/error states. |
| Accessibility | TalkBack traversal, checked/selected state, touch bounds, keyboard focus, reduced motion. |
| Language | Amharic and English UI; long Ethiopic names; mixed-script notes; numeral entry. |
| Data volume | Empty/new install, long books, 100 prayer names, many custom hours/habits, long journal/ledger history. |
| Lifecycle | Rotation, background/return, process recreation, leaving during save, returning from system picker/settings. |
| Calendar | Midnight, Ethiopian New Year, Pagume 5/6, leap-year schedules, changed timezone. |
| Connectivity | Airplane mode for all bundled readers; failed Catena/update/support links. |
| Permissions | Notifications granted/denied; exact-alarm capability; battery/OEM restrictions; relevant supported Android versions. |
| Privacy | Locked/unlocked routes, exports, recents/screenshots, notification previews, failed verification. |

Conduct task-based sessions with representative readers: a first-time user, a regular prayer-hour user, someone reading Scripture daily, a Mahlet/Ge'ez reader, and someone relying on large text or TalkBack. Ask them to perform tasks rather than rate a mockup. Record task success, wrong turns, misunderstood controls, and recovery. Avoid collecting prayer/journal content or treating religious activity as an engagement target.

## Coverage and boundaries

The navigation review includes `intro`, the four-tab `home` host, `prayerlist`, `fasting`, `search`, `bookmarks`, Catena, Bahre Hasab, Psalter, journal/list/editor, habits, Wudase, Scripture hub/testament lists/reader, Gitsawe/day/passage/Sunday cycle, Synaxarium, reading settings/fonts/copy, prayer settings, reminders, church-book shelves/shelf/book, Mahlet list/season/reader, records, intention variants, tithe, vows, penance, confession preparation, reading dashboard/choose/map/day list, latent communion preparation, prayer-hour reader, modes/editor, battery help, customize hours/sections, tours/tutorial, changelog, About, and licenses. Shared interactive surfaces and both widgets are reviewed separately above.

Repository HTML files are **supporting artifacts, not additional shipped Android pages**: `prototypes/home-dashboard.html`, `logos/preview.html`, `docs/mahlet-ui.html`, `docs/mahlet-audit.html`, and `docs/reminders-audit.html`. Their existence was inventoried; they were not treated as evidence of current runtime appearance. The external feedback/download website is not implemented as a website in this checkout and was not audited as a separate deployed product. External Catena content was evaluated as an integration surface, not reviewed page by page.

This report does not certify theological/editorial accuracy of all bundled text, content licensing, storage encryption, Android security, or performance across devices. Those require separate validation. It does provide a complete source-based review of the app's registered pages and associated user-facing flows, with the remaining runtime checks explicitly identified.
