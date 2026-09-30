# Changelog

All notable changes to Sinq (ስንቅ) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows semantic-style releases.

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
