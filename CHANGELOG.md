# Changelog

All notable changes to Sinq (ስንቅ) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows semantic-style releases.

## [1.5.5] — 2026-10-02

_versionCode 105 · Bold settings feedback card, journey day and prayer hours horizontal scrolling, and clock picker alignment_

### Added
- **Prominent Feedback Card on Settings.** Replaced nested text list item with a dedicated, illuminated liturgical hero card in Settings featuring deep emerald gradients, gold accents, and a high-visibility *"Share your thoughts"* action pill.
- **Horizontal Journey Day Strip.** Added an interactive, horizontally scrollable day strip on the Journey screen displaying recent days with Ethiopian Ge'ez numerals, weekday labels, prayer completion dots, and a quick-return button to today.
- **Horizontal Canonical Hours Pills.** Replaced the 4-column x 2-row grid of prayer hours with a smooth horizontally swiping row of tactile capsule pills with golden completion checks.
- **Interactive Heatmap Cells.** Made Ethiopian year heatmap density cells directly clickable to select and inspect dates, integrating a calendar picker icon.

### Fixed
- **Clock Time Picker Unalignment.** Fixed horizontal alignment and right-edge dial clipping when setting prayer alarms and reminder schedules across all dialogs.
- **Content Provenance & Typography.** Updated Synaxarium and Wudase Maryam digitization attributions and added Abba Garima to web typography credits.

## [1.5.4] — 2026-10-02

_versionCode 104 · Distraction-free prayer mode, universal reading progress bars, and Mahlet screen redesign_

### Added
- **Distraction-Free Prayer Mode.** Integrated system bar immersion and soft auto-hide of headers for Hourly Canonical Prayers, Psalm of the Day (Psalter), and Wudase Maryam. Hides the status bar (clock, battery, notifications) and controls after 3 seconds of stillness or upon scroll, with seamless tap-to-reveal interaction.
- **Universal Reading Progress Bars.** Added subtle 2dp golden reading progress indicator across all scripture and devotional readers: Psalter, Sinksar, Scripture, Wudase Maryam, Melkea/Mahlet, Shelf Books, and Gitsawe lectionary passages.
- **Mahlet List Screen Redesign.** Streamlined Mahlet feast index with header action search icon, refined Ethiopian month pills with Ge'ez count badges, and accessible 48dp liturgical service buttons for vigil (ዋዜማ) and dawn (ማኅሌት).

## [1.5.3] — 2026-10-02

_versionCode 103 · Settings hub reorganization, daily quote bookmarks, and UI card refinements_

### Added
- **Settings Category Hubs.** Reorganized settings into 4 structured category hubs (Preferences, Notifications, Records, Appearance) with live status subtitles.
- **SinqCard Containers & Semantic Leading Icons.** Enclosed settings and spiritual records rows into styled `SinqCard` containers with leading icons (`Paid`, `VolunteerActivism`, `FavoriteBorder`, `Bookmarks`, `FormatListBulleted`, `CalendarMonth`).
- **Desert Fathers Daily Quote Bookmarking.** Added bookmarking support to daily quotes with direct bookmark navigation and journaling from bookmarks.
- **Daily Quotes Source Attribution.** Added attribution citing `catenabible.com` in the contemplation sheet and share intents.

## [1.5.2] — 2026-10-02

_versionCode 102 · Compact lockscreen keyguard widget support and dimensions optimization_

### Added
- **Compact Lockscreen Widget Support.** Configured `targetCellWidth="2"`, `targetCellHeight="1"`, and reduced minimum height in widget provider metadata, allowing the Desert Fathers saying widget to qualify for compact lockscreen widget trays (e.g. Samsung One UI 6.1+ under-clock widget editor and Android keyguard pickers).

## [1.5.1] — 2026-10-02

_versionCode 101 · Guaranteed lockscreen quote notifications, on-by-default quote reminders, and streamlined home layout_

### Added
- **Guaranteed Lockscreen Visibility for Daily Quotes.** Upgraded daily quote notification channel to `IMPORTANCE_DEFAULT` with silent sound and non-intrusive priority, guaranteeing display on Android lockscreens across vendor skins (Samsung One UI, Pixel, Xiaomi) where low-importance silent notifications are suppressed.
- **On-by-Default Daily Quote & App-Start Sync.** Switched daily quote lockscreen reminder to enabled by default and added automatic sync on application startup.

### Changed
- **Streamlined Home Dashboard.** Removed the Annual Reading plan progress card from the Home screen, restoring spacious vertical hierarchy and focus to the canonical prayer hours (`NowCard`), daily scripture lectionary (`GitsaweCard`), and the hairline Desert Fathers contemplation row.

## [1.5.0] — 2026-10-02

_versionCode 100 · Desert Fathers Daily Quotes, Lockscreen widgets, privacy-first lockscreen notifications, and multi-year rotation_

### Added
- **Desert Fathers Daily Sayings (የአበው ምክር).** Bundled 844 canonical sayings from the Desert Fathers (*Apophthegmata Patrum* / ዜና አበው) covering 124 Desert Fathers and Mothers with full Amharic and Ge'ez titles.
- **Continuous Multi-Year Rotation.** Implemented an advancing multi-year progression ensuring consecutive years never display the same sayings on the same day (~2.31 years of unique content).
- **Lockscreen & Home Widget.** Created an offline Android AppWidget with `widgetCategory="home_screen|keyguard"`, with ongoing public lockscreen notification fallback for devices without lockscreen widget support.
- **Home Contemplation Row & Sheet.** Added an ultra-compact hairline row on Home (`☩  የአበው ምክር  ·  [Author]  →`) opening a contemplation sheet with sharing and direct spiritual journaling.
- **Privacy-First Lockscreen Notification Policy.** Explicitly configured notification visibility across the app: only Daily Quote and Daily Gitsawe are visible on the secure lockscreen (`VISIBILITY_PUBLIC`); prayer alarms, habit streaks, breath prayers, and reading reminders are strictly quarantined (`VISIBILITY_PRIVATE` and `VISIBILITY_SECRET`).
- **Notification Schedule & Settings.** Added on/off toggle and custom time picker in Settings allowing users to customize when the daily quote refreshes.
- **Reading Plan Visuals.** Refined reading plan overview with visual section badges, part headers, and bookmark markers.

## [1.4.2] — 2026-09-30

_versionCode 99 · Sinksar punctuation setting, streamlined prayer list divider, and content fixes_

### Added
- **Sinksar Word-Colon Setting.** Added setting toggle in Appearance & Reading allowing readers to toggle traditional wordspaces (`፡`) while preserving clauses and sentence stops (`፦`, `፤`, `።`).

### Changed
- **Prayer List Divider.** Replaced separate Living and Departed section headers with a quiet thin dividing line between categories.

### Fixed
- **Meskerem 21 Sinksar.** Corrected commemoration paragraph alignment and readings for Meskerem 21.

## [1.4.1] — 2026-09-30

_versionCode 98 · Streamlined 3-section settings hierarchy and first-run alert choice on home_

### Changed
- **Unified 3-Section Settings.** Reorganized Settings into Appearance & Reading, Prayer & Reminders, and Data & About with inline font picker, text size stepper, prayer level selector, and direct alert styling.
- **Home Screen Alert Choice.** Presented the 3-option reminder style choice (Alarm, Vibrate, Notification) directly on the Home screen for first-run installs and updates.

## [1.4.0] — 2026-09-30

_versionCode 97 · Preserved Ethiopic punctuation, top bar language selector, streamlined onboarding, and reminder setup_

### Added
- **Ethiopic Punctuation Preservation.** Preserved traditional wordspaces (`፡`) and punctuation (`፦`, `፤`, `።`) across the entire Synaxarium and Melkea corpora, with full normalization in search and rubrication.
- **Top Bar Edition Selector.** Relocated Amharic and Ge'ez language switches to the top app bar in both Sinksar and Wudase Maryam, keeping bottom control bars minimal and focused.
- **Reminder & Battery Setup.** Introduced a permission verification sheet and a Home notice bar ensuring reliable background prayer notifications.
- **Three-Choice Alert Styling.** Added onboarding selection between full audible ringing, discreet vibration only, and silent banner notifications.

### Changed
- **Streamlined First-Run Onboarding.** Merged overview presentation directly into profile setup and retired the redundant post-install What's New tour overlay.

## [1.3.2] — 2026-09-30

_versionCode 96 · Refactored stepper components and trimmed redundant interactive modifiers_

### Changed
- **Inline Chapter Stepper.** Inlined liturgical hour stepper directly into vertical reader, eliminating redundant single-caller wrapper layer.
- **Pruned Redundant Modifiers.** Cleaned duplicate `minimumInteractiveComponentSize` calls on `IconButton` and edition toggle capsules, relying on native Material 3 sizing contracts.

## [1.3.1] — 2026-09-30

_versionCode 95 · UI/UX accessibility, touch targets, and standardized button design system_

### Added
- **Standardized Button System.** Introduced unified `SinqPrimaryButton`, `SinqOutlinedButton`, `SinqDestructiveButton`, and `SinqStepperButton` design tokens with Material 3 styling and color-scheme awareness.

### Changed
- **Touch Target Compliance (48dp Minimum).** Enforced minimum 48dp interactive boundaries across edition toggles, date steppers, and navigation controls to eliminate tap misses.
- **Button Prominence & Hierarchy.** Elevated primary commit and confirmation actions in dialogs and forms across habits, vows, penance, tithe, and reading plans while preserving subdued text styling for dismissals.
- **Vector Icons.** Replaced raw text glyphs with accessible Material vector symbols across reader steppers, settings, and journal navigation.
- **Semantic Destructive Actions.** Applied semantic error coloration to irreversible deletion and reset confirmations.

## [1.3.0] — 2026-09-29

_versionCode 94 · Unified reader navigation, embedded Melkea hymns, and liturgical punctuation_

### Added
- **Embedded Melkea Hymns.** Integrated መልክአ ማርያም and መልክአ ኢየሱስ directly into Wudase Maryam as navigable pages.
- **Unified Reader Control Bar.** Single-line docked bar in Wudase Maryam and Sinksar with a scrollable portion track and hairline divider.

### Changed
- **Minimalist Edition Toggle.** Replaced bulky swipe/button toggles with a clean, single-action liturgical pill displaying the target language without decorative emojis.
- **Liturgical Punctuation & Strophes.** Restored traditional Ge'ez punctuation (`፦`, `፤`, `።`) and stanza numbering across Melkea hymns.

## [1.2.1] — 2026-09-29

_versionCode 93 · Codebase refinement and performance optimizations_

### Changed
- **Removed Unused Scaffolding.** Cleaned out dead and unreferenced models, repositories, and preferences.
- **Regex Optimization.** Hoisted and precompiled regular expressions in offering amounts entry.

## [1.2.0] — 2026-09-29

_versionCode 92 · Complete bilingual Sinksar across all 366 days in Amharic and Ge'ez_

### Added
- **Full 366-Day Sinksar.** Complete bilingual parallel editions for every day and month of the Ethiopian year.
- **Reader UI.** Numbered narrative paragraphs, Arke salutations, and liturgical red rubrication.
- **System Integration.** Deep integration with lectionary, search, and bookmarks.

## [1.0.0] — 2026-09-26

_versionCode 90 · Official Google Play release with structured liturgical corpus, 81-book Bible, and modern reader_

### Added
- **Structured Liturgical Corpus.** 32 canonical Melkea strophic hymns, unified 25-office Seatat (Horologium) with speaker roles (Priest, Deacon, Congregation), and 181 feast orders of Mahlet.
- **Complete Canonical Scriptures.** Full 81-book Ethiopian Orthodox Bible, complete Psalter (መዝሙረ ዳዊት), and liturgical calendar lectionary (ግጻዌ).
- **Refined Reading & Liturgical UI.** Automatic rubrication in traditional red ink, speaker indicators, anatomical focus kickers, custom Ge'ez typography, and offline prayers.
- **Target SDK 36.** Optimized for Android 16 (API 36) ensuring the app meets the latest Google Play standards for security, battery life, and performance.
- **Google Play Integration.** Direct in-app privacy policy access, Google Play rating and feedback, and responsive notification scheduling.
