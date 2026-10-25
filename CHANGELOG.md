# Changelog

All notable changes to Sinq (ስንቅ) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows semantic-style releases.

## [1.8.8] — 2026-10-25

_versionCode 130 · Sunday Canticles Liturgical Redesign, Typographic Corrections, and Adaptive Card Layouts_

### Added
- **Sunday Canticles (ጸሎተ ነቢያት) Liturgical Redesign.** Revamped Sunday Canticles with Ge'ez numeral emblem badges (፩–፳), parsed dual-line titles separating canticle names from scripture references, 0.5dp hairline dividers, and an elevated full Psalter action card.

### Changed
- **Adaptive ReadingCard Layout.** Removed rigid height and width constraints in favor of responsive `wrapContentHeight` and `heightIn(min = 140.dp)`, capped local fontScale at 1.3x, unified typography under `LocalReadingFont`, and introduced adaptive vertical stacking for narrow devices (<360dp) or high font scales.
- **Card Layout Responsiveness.** Hardened card wrapping with `FlowRow` in Mahlet today chant orders and Reading study reflections, updated manuscript share preview to adapt to system font scales, expanded library card subtitle allowance to 2 lines, and prevented scripture codex title/count collisions.
- **Textual & Typographic Corrections.** Corrected split words, typos, missing spaces, and standardized Ge'ez numerals across Amharic and Ge'ez editions of ጸሎት ነቢያት (`39a66aaa6b.json` and `d46f2d9514.json`).

## [1.8.7] — 2026-10-25

_versionCode 129 · Books Candle Reading Card, Floating Bottom Bar Cradle Polish, and Update Check Fix_

### Added
- **Liturgical Candle Reading Progress Card.** Redesigned the "continue reading" hero card at the top of the Books screen featuring an authentic animated `CandleProgress` indicator with a 12dp×20dp flickering flame, 1dp track with 2dp gold fill, 600ms intro animation, reduced-motion fallback, merged accessible semantics, and three distinct states ("Start reading", "Continue", "Read again").

### Changed
- **Refined Floating Bottom Bar Cradle.** Sunk the 5-layer candle flame base 2.5dp into a deeper 13dp × 52dp notch cradle with steeper eased shoulders, reduced flame-to-icon spacing, and removed opaque backgrounds to let content scroll behind the floating bar.
- **Immediate Update Check on Resume.** Fixed app update checking to evaluate on every app launch and resume with session-scoped dismissal, ensuring update notices are not indefinitely hidden by legacy persistent cache.

## [1.8.6] — 2026-10-25

_versionCode 128 · Notched Bottom Bar Redesign, Organic Candle Flame Indicator, and Scoped Tab Swiping_

### Added
- **Organic 5-Layer Candle Flame Active Indicator.** Designed a realistic animated candle flame rendered entirely with Compose Paths and Canvas: cubic ease-out radial glow, asymmetric gold-to-amber teardrop body, bright white-yellow inner core, cool blue base, and curved wick.
- **Dynamic Flame Physics.** Added multi-frequency sine wave flicker (2.1Hz, 3.7Hz, 5.3Hz), 14° directional travel lean during tab changes, damped spring upright settling, and arrival ignition pop. Full reduced-motion setting compliance with an instant, static flame.
- **Eased Cubic Notched Bar Surface.** Crafted a custom `NotchedBarShape` with smooth cubic bezier shoulders that slide continuously with decelerating easing behind the active tab, perfectly cradling the candle flame within the preserved 72dp bar height.
- **Scoped Horizontal Tab Swiping.** Re-enabled fluid horizontal gesture swiping across all primary tabs (`Home`, `Journey`, `Books`, `Settings`) while scoping touch interaction on the 13-month Ethiopian Year Heatmap to prevent accidental tab switching during heatmap exploration.

### Changed
- **Liturgical Tab Styling.** Replaced default tab selection indicators with a lit active icon state featuring subtle warm flame illumination, spring scale pop, and smooth icon crossfades.

## [1.8.5] — 2026-10-25

_versionCode 127 · Ge'ez Scripture Reader, Cleaned Monastic Sayings, Mahlet & Zema Polish, and Dense Book Cards_

### Added
- **Full Ge'ez Scripture & Parallel Mode (ግእዝና አማርኛ-ግእዝ ትይዩ ንባብ).** Enabled Ge'ez and side-by-side Amharic-Ge'ez parallel reading across all 66 canonical books in the Scripture reader.
- **Unified Heatmap Legend Reference.** Updated prayer journey heatmap legends with localized labels ("ትንሽ" / "Less", "፫+ ጸሎት" / "3+ prayers"), fasting indicators ("● ጾም" / "● Fasting"), high-contrast today beacons ("ዛሬ" / "Today"), and full prayer markers ("✦ ሙሉ" / "✦ Full").
- **Misbak Language Toggling in Lectionary.** Added direct language toggling (`ግእዝ` / `አማርኛ`) in the top bar when viewing or opening Misbak passages in Gitsawe and Sunday lectionaries, with fallback to the Amharic Psalter text.

### Changed
- **Subtle Scripture Translation Dropdown.** Replaced the cycling translation button in the Scripture reader with an impeccable, subtle dropdown menu displaying all available translations (`አማርኛ`, `ግእዝ`, `English`, `አማርኛ + ግእዝ`, `አማርኛ + English`) with active selection checkmarks.
- **Comprehensive 5-Line Stanza Matching in Mahlet.** Expanded hymn movement resolution across all 5 poetic lines of each stanza with sister-hymn cross-linking (`ማኅሌተ ጽጌ` and `ሰቆቃወ ድንግል`), resolving 218 out of 226 movements directly to their verses.
- **Pristine Mahlete Tsige & Segokwaw Dengel.** Restored authentic Ge'ez liturgical formatting, eliminated duplicate numbering in hymn stanzas, and unified hemistich punctuation (`።`).
- **Sanitized Desert Fathers Corpus.** Removed scanned running headers, spliced footnote fragments, and OCR artifacts across the Sayings of the Desert Fathers and daily quotes.
- **Hoisted Mahlet Today Card.** Positioned today's appointed Mahlet feast card directly at the top of the Mahlet screen, eliminating the need to scroll.
- **Compressed Church Books Cards.** Streamlined book cards in the Books tab into a compact, refined profile while preserving full sacred styling and prominence for the daily reading card.

## [1.8.4] — 2026-10-25

_versionCode 126 · English Synaxarium, Direct Gitsawe Rollover, Desert Fathers Hub, and App Size Optimization_

### Added
- **Full English Synaxarium (መጽሐፈ ስንክሳር).** Added English translations across all 13 months of the Ethiopian year, enabling bilingual and trilingual reading alongside Amharic and Ge'ez text with Arke hymns.
- **Dedicated Desert Fathers Hub & Glossary.** Expanded Sayings of the Desert Fathers with per-father browsing, topic exploration, and a dedicated monastic glossary.
- **Zema and Mahlet Literature.** Added Segokwaw Dengel (ሰቆቃወ ድንግል) and Mahlete Tsige (ማኅሌተ ጽጌ) to the spiritual library with Ge'ez liturgical typography.
- **High-Contrast Today Beacons in Heatmaps.** Highlighted today's date in both annual and compact prayer journey heatmaps with vivid emerald green in light mode and bright crisp white in dark mode.

### Changed
- **Direct Gitsawe Rollover Navigation.** Tapping the home screen Gitsawe card when displaying tomorrow's evening lectionary now routes directly to tomorrow's date.
- **Gitsawe Home Card Lectionary Title Prominence.** Decoupled feast banners from lectionary titles so that daily, seasonal, and evening lectionary reading titles are prominently displayed with eve designations cleanly positioned in the upper gold accent kicker.
- **App Size Reduction by ~30%.** Shrank APK size from 18.00 MB to 12.71 MB by pruning redundant font files, deduplicating Ge'ez scripture, replacing the bundled 16MB pre-built database with on-the-fly runtime indexing (`CorpusIndexer`), and enabling R8 full mode.

## [1.8.3] — 2026-10-23

_versionCode 125 · Large-Screen Adaptive Layouts, Design System Polish, and Widget Localization_

### Added
- **Large-Screen & Foldable Adaptations.** Adapted secondary and modal reading routes (Scripture list, Gitsawe lectionary, Mahlet orders, Fasting details, Global search, Changelog, and About) with centered maximum readable content width (840dp for grids/lists, 680dp for prose) to eliminate awkward stretched layouts on tablets and foldables.
- **Widget Localization & Previews.** Added full bilingual string resources and preview bindings for home screen widgets (Prayer Clock & Daily Monastic Quote).

### Changed
- **Liturgical Aesthetic & Contrast Polish.** Enhanced Desert Fathers Sayings Hub, Reading title chapter ornaments, Ethiopic calendar heatmap contrast tokens, and Home screen hero card typography.
- **Quality & Lint Audit.** Resolved all Android lint issues and updated Settings time formatting to use fixed locale conventions.

## [1.8.2] — 2026-10-22

_versionCode 124 · Sayings of the Desert Fathers in Books Tab, Unboxed Manuscript Hub, and Android 6.0 (API 23+) Restoration_

### Added
- **Dedicated Desert Fathers Section in Books.** Added a permanent "የአበው ምክር (Sayings of the Desert Fathers)" entry to the Church Books tab, providing immediate access to the 844 Patristic sayings corpus.
- **Unboxed Manuscript Hub.** Redesigned the Desert Fathers Sayings Hub to follow the app's authentic liturgical manuscript styling, replacing card boxes with delicate hairline dividers, live search, and filter chips by prominent monastic fathers and saved bookmarks.

### Changed
- **Restored Android 6.0 (API 23+) Compatibility.** Lowered `minSdkVersion` back to 23, restoring full device compatibility for 1,158 legacy device models on Google Play.
- **Streamlined Home Row.** Removed the redundant "View all" button on the Desert Fathers card on the Home screen.

## [1.8.1] — 2026-10-22

_versionCode 123 · Custom Reading Reminders, Quiet Hours Split Card, Dynamic What's New, and Reading Plan Fixes_

### Added
- **Customizable Reading Reminders.** Configure reading reminder frequency (1x, 2x, or 3x daily) with customizable time slots via a bottom sheet picker.
- **Dynamic What's New Dialog.** Re-engineered launch update announcement dialog to pull release highlights dynamically from release notes, ensuring update details always match the installed version.

### Changed
- **Extended Notification Retention Default.** Raised default notification auto-dismissal timeout from 2 hours to 4 hours (240 minutes) across prayer hours and reading nudges.
- **Quiet Hours Split Card.** Unified Quiet Hours with the modern split card pattern (`22:00 – 06:00` badge) and single dual-tab start/end time picker dialog.
- **Reading Plan Catch-Up Fix.** Fixed yearly reading plan progress tracking so readers falling behind are properly handed their due day with clear catch-up options.
- **Home & Liturgical UI Polish.** Formatted Desert Fathers quote preview to single-line italic with internal "View all" navigation, bound widget evening rollover directly to liturgical Eve (ዋዜማ), removed duplicate hymn titles in Wudase Maryam, and streamlined Journey screen header.

## [1.8.0] — 2026-10-22

_versionCode 122 · Complete 1962 Amharic Bible, Settings UI Redesign, and Streamlined Support_

### Added
- **Digitized 1962 Amharic Bible Edition.** Integrated the full 1962 publication text digitized from publication PDFs, meticulously formatted and proofread with authentic Ge'ez chapter headings and verse numbering.
- **Dedicated Split Reminder Cards.** Replaced expanding accordion rows with a modern split card pattern (`[ Info + Time Badge ] | [ Switch ]`), allowing direct time changes via time picker while toggling switches independently.
- **Streamlined Help & Feedback Hub.** De-cluttered the bottom of Settings into two elegant rows: "ስለ ስንቅ (About Sinq)" and "እርዳታና ድጋፍ (Help & Feedback)" with a dedicated support sheet.

### Changed
- **LXX Psalter & Canonical Prayer Hours Preserved.** Standalone Psalter reader and all canonical Horologium prayer hours strictly maintain the traditional Ethiopian Orthodox LXX translation, while Old Testament Bible reader connects to the 1962 Psalms.
- **Reading Plans Aligned.** Rebuilt 360-day annual, 180-day half-year, and 150-day psalter reading plans synchronized with the digitized text.

## [1.7.9] — 2026-10-22

_versionCode 121 · Google Play Automatic App Protection Compliance (API 24+)_

### Changed
- **Minimum SDK Bumped to API 24.** Raised `minSdkVersion` to 24 (Android 7.0 Nougat) to fully satisfy Google Play Console's automatic integrity and app protection requirements.

## [1.7.8] — 2026-10-21

_versionCode 120 · Liturgical Eve on Gitsawe Widget, Database Optimization, and JSON Minification_

### Added
- **Liturgical Eve (ዋዜማ) on Gitsawe Widget.** The home screen Gitsawe widget now prominently displays the Eve (ዋዜማ) designation in its header and status line when rolled over in the evening, highlighting feast attribution and upcoming commemorations.

### Changed
- **Optimized Corpus Database.** Rebuilt `sinq_corpus.db` with SQLite page size tuning (1024-byte pages) and FTS index defragmentation, reducing uncompressed database size by ~1.9 MB without altering any search schemas or query contracts.
- **Minified Lectionary & Patristic JSONs.** Stripped unnecessary whitespace and structural formatting overhead across `daily-gitsawe.json`, `sunday-cycle-gitsawe.json`, `daily_quotes.json`, and seasonal lectionary assets, saving ~500 KB.

## [1.7.7] — 2026-10-21

_versionCode 119 · Liturgical Purity, Live Card Sharing, Notification Retention, Reader Double-Tap, and Journal Integration_

### Added
- **Live Manuscript Card Sharing & WYSIWYG Preview.** Interactive bottom sheet for passage sharing with live preview, aspect ratio selector for «Square (1:1)» and «Story (9:16)», centered typography, authentic Ge'ez ornamental framing, and zero emojis.
- **Configurable Notification Retention Timeout.** System setting allowing users to customize notification auto-dismissal durations (30m, 1h, 2h, 4h, 6h, Never) using Android native `setTimeoutAfter`.
- **Deliberate Reader Immersion via Double-Tap.** Full-screen distraction-free immersion is now engaged strictly via deliberate double-tap rather than automatic timing, fixing the rapid bottom sheet dismissal glitch.
- **Enhanced Spiritual Journal Access.** Added an elegant top-header pill button and an inline feed reflection card on the Journey screen, removing bottom FAB screen occlusion.

### Changed
- **Pure Canonical Hours Restored.** Removed all seasonal and saint prayer interpolations from the seven canonical hours, strictly preserving the traditional pure Psalms and Gospel canon.
- **Liturgical Book Titles in English View.** Preserved traditional Amharic liturgical designations («ስንክሳር», «ውዳሴ ማርያም», «ሥርዓተ ማኅሌት», «ባሕረ ሐሳብ») across English interface views.
- **Desert Fathers Notification Frequency.** Enforced single-daily notification delivery at user-selected times, eliminating duplicate notification bursts on app resume.
- **De-boxed Devotional Heatmap.** Removed outer border and shadow cards on the Journey page heatmap for an open, unconstrained liturgical manuscript layout.

## [1.7.6] — 2026-10-19

_versionCode 118 · Kenona spiritual rules in Journal, Desert Fathers sayings hub, devotional heatmap layout, and prayer list polish_

### Added
- **ቀኖና (Kenona) in Journal.** Dedicated spiritual rule and ascetic discipline section within the prayer journal, featuring filter chips, entry kind sheet option, and 6 authentic Orthodox devotional presets (+ ፵፩ ስግደት, + ውዳሴ ማርያም, + ፯ቱ መሐረኒ ዳዊት, + መጽሐፈ ሰዓታት, + ፻ ጊዜ ኪርያላይሶን, + የዕለቱ ስንክሳር ንባብ).
- **Desert Fathers Sayings Hub.** Dedicated screen (`desert_fathers`) to browse, search, bookmark, copy, share, and reflect upon all 844 sayings from the early Desert Fathers in the personal prayer journal.
- **Randomized Desert Fathers Reminders.** Opt-in preference in Settings and Desert Fathers Hub allowing the daily reminder notification to randomly pick from the complete 844 patristic sayings collection.

### Changed
- **Home Desert Fathers Row.** Polished quote card with 1-line quote preview, ellipsis overflow, and a dedicated «ሁሉንም እይ ➔» pill button leading directly to the Hub.
- **Prayer List (`የጸሎት መዝገብ`).** Updated screen and header title to «የጸሎት መዝገብ» while preserving the clean, single-column diptych layout.
- **Devotional Year Heatmap & Reader Layout.** Restored the horizontal 366-day grid layout on the Journey screen and classic reading view configuration.

## [1.7.5] — 2026-10-19

_versionCode 117 · Major Architectural, Accessibility, Liturgical & Devotional System Upgrade_

### Added
- **Enhanced Search Engine & Exact Ranking.** Expanded homophone folding table to encompass all Ethiopic interchangeable families (ሀ/ሐ/ኀ, ሰ/ሠ, አ/ዐ, ጸ/ፀ, labialized velars, and Ge'ez numerals ፩-፼ to Arabic). Exact orthography matches now rank with top priority over folded variants.
- **Accessible Journey List View.** Introduced a dedicated week-by-week scrollable List view as an accessible TalkBack alternative to the 53x7 year heatmap, selectable in Settings.
- **Follow System Font Scale.** Added a Settings toggle allowing UI and reading text to dynamically follow system accessibility font scaling.
- **Vigil Black OLED Theme & WCAG Contrast.** Full WCAG 2.1 AA/AAA compliance across all Light, Dark, and Vigil Black tokens with subtle gold card framing.
- **Unified Backup Envelope (Schema v4).** SHA-256 integrity-verified export/import combining Room journal entries and DataStore preferences with atomic Merge/Replace options and 3 automatic safety backups. Private confession notes are permanently air-gapped from exports.
- **Authentic Manuscript Card Generator.** Share scriptures and liturgical passages as illuminated vellum cards featuring golden fillets, corner rosettes `፨`, rubricated headings, and optional attribution watermarks.
- **Sacred Manuscript Onboarding Flow.** A 3-step introductory setup for language, typography with live manuscript preview, and opt-in reminders.
- **Adaptive Tablet Navigation & Two-Pane Reading.** Dynamic navigation rail for medium/expanded screens (≥ 600dp) and synchronized side-by-side parallel reading for expanded displays (≥ 840dp).
- **Paged Leaf Reader Mode.** Scripture reading with horizontal swipe paging, 20% edge taps for page turning, middle 60% chrome toggle, and persistent verse-keyed anchoring across font size changes.
- **Multi-OEM Battery Optimization Guide.** Step-by-step background battery guidance with direct system deep links for Tecno, Infinix, Xiaomi, Samsung, and Stock Android devices.

### Changed
- **Home Adaptive FillColumn.** Automatically falls back to proportional vertical scrolling on compact displays (< 640dp) or enlarged font scales (≥ 1.3x) without card clipping.
- **Journey Screen Visual Density.** Floating Action Button now gracefully animates away while inspecting calendar days, with 120dp bottom padding preventing card overlap.
- **Reminders Policy.** Exact alarms transitioned to Google Play compliant `SCHEDULE_EXACT_ALARM` with runtime verification and boot/timezone rescheduling.

## [1.7.0] — 2026-10-18

_versionCode 116 · Tri-mode language configuration (Amharic, English, Bilingual), horizontal prayer scroll with shadow affordances, floating journal button, and comprehensive Amharic orthographic review_

### Added
- **Tri-Mode Language Selector.** Expanded language preferences to support three explicit options: Bilingual (`አማ + Eng`), Strict English (`English`), and Strict Amharic (`አማርኛ`). Applied dynamically across the entire app with dedicated bilingual display components.
- **Scroll Affordances & Elevation for Prayer Hours.** Embedded dynamic edge shadow gradient overlays with directional chevrons on the horizontal prayer hours carousel, accompanied by tactile card drop shadows and tonal elevation.
- **Persistent Floating Action Button for Journal.** Replaced the static in-list journal button with a persistent, elevated Floating Action Button (FAB) on the Journey page for fast access across scroll depths.

### Changed
- **Horizontal Prayer Carousel.** Refactored canonical prayer hour chips into a smooth horizontal scrolling row on the Journey page, freeing vertical screen real estate for habit tracking.
- **High-Density Devotion Heatmap.** Streamlined the devotional manuscript plate by removing redundant percentage and fast metrics, shifting the calendar upward for improved information density.
- **Amharic Liturgical & Orthographic Overhaul.** Corrected classical liturgical spellings (e.g., `ግእዝ`, `ራእይ`, `ወርኀዊ`, `በዓለ ሃምሳ`), standardized Ge'ez numerals in liturgical quotas, and enriched translations across reading and habit screens.

## [1.6.0] — 2026-10-16

_versionCode 115 · Unified manuscript heatmap plate, zero-gap Journey header, annual devotion progress, and liturgical micro-marks_

### Added
- **Annual Devotion Progress Bar.** Integrated a gold hairline devotion progress bar on the year heatmap plate tracking completed prayer days against total calendar days.
- **Liturgical Micro-Marks & Today Beacon.** Added sacred illumination micro-crosses (`✦`) for full devotion days (level 3+) and radiant beacon rings for today on both compact and full-year heatmaps.

### Changed
- **Zero-Gap Journey Screen Header.** Consolidated the year switcher (`< 2018 ዓ.ም. >`), date picker icon, and prayer counter pill directly into the manuscript plate header, eliminating redundant outer headers and dead vertical space.
- **Compact Heatmap Horizon.** Expanded Home compact heatmap to 12 weeks with enriched cell touch targets and liturgical indicators.

## [1.5.14] — 2026-10-15

_versionCode 114 · Comprehensive UI, design, and accessibility polish across home hero, habit heatmaps, reading, and search_

### Changed
- **Home Screen Visual Hierarchy.** Refactored the Gitsawe card from a hero card to an accented Sinq card, allowing the prayer Now card to reign as the singular visual hero on the page.
- **Screen Margin Consistency.** Standardized horizontal page padding across all settings, habit management, hour management, and customization screens to the canonical `Spacing.screen` (24dp) design token.
- **Heatmap Optical Polish.** Guarded center dots in the compact heatmap to prevent visual moiré on small cells while keeping the full 365-day layout compact and legible.
- **Liturgical Iconography.** Replaced the generic heart icon on ውዳሴ ማርያም with a liturgical Church emblem, and updated the LXX Psalter with a sacred codex symbol.
- **Reader & Search Smoothness.** Eliminated search result flicker during debounce typing, merged TalkBack announcements for Ge'ez numeral gutters and verses, and dynamically scaled section divider air at larger font sizes.

### Fixed
- **Heatmap Accessibility.** Added TalkBack summary descriptions to the compact heatmap and rich 365-day liturgical/prayer/fasting announcements to the year heatmap cells.
- **Candle Aura Boundary.** Added a canvas edge guard to the beeswax candle's golden aura bloom to eliminate edge clipping on narrow displays.

## [1.5.13] — 2026-10-15

_versionCode 113 · Compact Journey page prayer strips and habit rows, button-like Today manage control, and animated dawn horizon logo on Memento Mori_

### Added
- **Animated Dawn Horizon & Cross on Memento Mori.** Reimagined the opening Memento Mori breath screen with the animated dawn horizon line, radiant rising sun dome, central cross, and 6 unfurling sun rays matching the Sinq icon.

### Changed
- **Compact Journey Prayer & Habit Strips.** Reduced prayer hour strips to a sleek 34dp height and daily habit rows to 36dp (with 16dp rings) to minimize empty vertical gaps and increase visible screen density.
- **Button-Like Today Manage Action.** Redesigned the Today section "Manage" action into a subtle, noticeable button pill with liturgical gold accent and bold typography.

## [1.5.12] — 2026-10-14

_versionCode 112 · Ethiopian Beeswax candle in gentle stillness, Sacred Manuscript Folio heatmap plate with fasting dot and perforations, and compact interactive day inspection_

### Added
- **Ethiopian Church Beeswax Candle (ቱዋፍ).** Reimagined the daily prayer candle with natural shaded beeswax depth, crater pool, glowing wick ember, and gentle stillness harmonic sway and convection breathing.
- **Sacred Manuscript Folio (የብራና ሰሌዳ) Heatmap Plate.** Enclosed both Home and Journey prayer heatmaps in a traditional Ethiopian manuscript folio with gold fillet framing, corner rosettes (፨), center sacred dot (●), and perforated/dashed fasting edges.
- **Interactive Heatmap Day Inspection.** Tapping any day on the Journey year heatmap immediately opens the day inspection card showing canonical hours prayed, habits completed, active fast names (including weekly Wednesday/Friday fasts), and written journal reflections with previous/next day stepping.

### Changed
- **Compact Journey Heatmap Layout.** Streamlined cell footprint from 16dp to 11dp (13dp column pitch), lowering grid height by ~40% and surfacing 5–6 months on screen simultaneously without crowding.

## [1.5.11] — 2026-10-05

_versionCode 111 · Synaxarium Arke correction, Wudase Amlak prayer book, and English NKJV Bible integration_

### Added
- **Wudase Amlak (ውዳሴ አምላክ በአማርኛ) Prayer Book.** Added the complete 8 canonical daily sections (የዘወትር, ሰኞ, ማክሰኞ, ረቡዕ, ሐሙስ, ዓርብ, ቅዳሜ, እሑድ) under the Church Books (የጸሎት መጻሕፍት) shelf.
- **English Bible (NKJV) Translation.** Fully wired English New King James Version (NKJV) Bible with parallel Amharic/English reader toggles, verse selection, and global search.
- **What's New Update Dialog.** Added a launch update dialog presenting headline liturgical additions and fixes upon updating to a new version.

### Fixed
- **Synaxarium Arke Strophes.** Audited, verified, and corrected all 1,017 Synaxarium Arke strophes across the entire 366-day liturgical year against authentic Ethiopian Orthodox Tewahedo Church manuscripts.

## [1.5.10] — 2026-10-04

_versionCode 110 · Future date checklist reminders & deep linking, Scripture Psalms reader with highlights, and web guide styling_

### Added
- **Future Date Journal Checklist Restriction.** Browsing to future dates in the Journal now permits checklist-only entries with an informative liturgical notice banner, reserving prayer reflections and freeform text for the present day while supporting advance task and discipline planning.
- **Checklist Reminder Deep Linking.** Tapping a checklist reminder notification directly opens the target entry and date in the Journal, bypassing onboarding screens.
- **Scripture Psalms in Library Codex.** Accessing Psalms (መዝሙረ ዳዊት) from the Books & Scripture hub now opens the full 150-psalm Scripture Reader with verse selection, 4-color highlighting, bookmarks, personal notes, and verse copying, while keeping the dedicated prayer psalter for devotional hours and Home screen reading.

### Changed
- **Web User Guide & Liturgical Manual Polish.** Polished and unified the standalone web guide (`guide.html`) with responsive sidebar navigation, real-time search, bilingual Amharic/English toggles, and seamless site integration across all pages.

## [1.5.9] — 2026-10-04

_versionCode 109 · Home Psalter button polish, Wudase Maryam stability, and web manual redesign_

### Changed
- **Home Psalter Button Label.** Polished the Home screen Psalter button label to always display `መዝሙረ ዳዊት` (Psalter) accompanied by the daily psalm range caption (`የዕለቱ መዝሙር` / `መዝ ፩–፴`) instead of the generic range prefix.
- **Web User Guide Redesign.** Completely redesigned and styled the web User Guide & Liturgical Manual (`guide.html`) with a responsive 2-column documentation layout, sticky sidebar, real-time search filter, bilingual Amharic/English toggle, Expand/Collapse controls, Ge'ez numeral table of contents, scrollspy, and unified website navigation.

### Fixed
- **Wudase Maryam Home Navigation.** Fixed an app crash when opening Wudase Maryam from the Home screen by hardening horizontal pager key and page bounds, clamping initial page indices, and ensuring explicit daily section parameter routing.

## [1.5.8] — 2026-10-04

_versionCode 108 · Light mode default, Library codex structure, Church books typography polish, and Journal refinement_

### Changed
- **Default Light Mode Theme.** Configured light mode (Sacred Ivory / Parchment) as the app-wide initial default theme upon first launch across settings and theme resolution.
- **Library Codex Architecture.** Reverted Library screen to its clean canonical single `ቅዱሳት መጻሕፍት` entry pointing to the full 81-book canonical structure, keeping the signature `ReadingHeroCard`.
- **Books Page Typography Harmonization.** Standardized typography across Church book shelves (`ShelfRow` and `BookRow` in `BookShelfScreen.kt`) to `titleMedium` and `bodySmall`, eliminating bold reading serif weights on navigation buttons.
- **Journal Day Refinement.** Removed daily feast names from Journal entry cards and headers to keep prayer reflections clean and focused on personal devotion.
- **Web User Guide Integration.** Synchronized liturgical user manual (`guide.html`) with website navigation and site generator.

## [1.5.7] — 2026-10-04

_versionCode 107 · Interactive heatmap day inspection card, direct Horologium reader jump, and habit management flow_

### Added
- **Interactive Day Inspection Card.** Selecting any day on the Ethiopian year heatmap displays an inspection card detailing canonical hours and habits kept on that date, active fasts, day navigation chevrons, and a direct button to view the day's journal entry.
- **Direct Horologium Reader Jump.** Long-pressing any canonical hour strip item (or tapping an hour chip in the day inspection card) opens the Horologium reader directly to that hour's prayers (`hour/{hourId}`).
- **Discreet Habits Management Button.** Added an አስተካክል (Manage) text button in the Habits section header for direct access to habit configuration.
- **Daily Discipline Guidance & Checklist Integration.** Added guidance in habit creation advising daily disciplines for habits and Journal checklists for periodic tasks, backed by optional scheduled checklist reminders.

## [1.5.6] — 2026-10-03

_versionCode 106 · Unified home hairline dividers, HeroCard feedback in settings, horizontal journal day strip, and liturgical UI audit_

### Added
- **Horizontal Journal Day Strip.** Replaced the 30-day square grid on the Journal screen with an interactive, horizontally scrollable day strip showing Ethiopian weekday names, Ge'ez numerals, fasting status, written entry indicators, and smooth auto-scrolling to today.
- **Unified Today & Desert Father Hairline.** Merged the Today (ዛሬ) journey row and Desert Father saying (የአበው ምክር) on the Home dashboard into a single hairline block sharing a middle divider line to eliminate duplicate parallel rules.
- **HeroCard Settings Feedback.** Adopted the canonical Gitsawe `HeroCard` design for the Settings feedback card with liturgical contrast across both light and dark themes.

### Fixed
- **Liturgical UI Component Audit.** Closed accessibility and touch target gaps across Manage Hours, Marks, Penance, Search, About, and Special Habits.
- **Journey Screen Integrity.** Preserved the canonical 4-column canonical hours grid and responsive heatmap on the Journey screen.

## [1.5.5] — 2026-10-02

_versionCode 105 · Settings feedback card, journey day and hours horizontal scrolling, clock picker alignment_

### Added
- **Prominent Feedback Card on Settings.** Replaced nested text list item with a dedicated, illuminated liturgical hero card in Settings featuring deep emerald gradients, gold accents, and a high-visibility *"Share your thoughts"* action pill.
- **Horizontal Journal Day Strip.** Replaced the 30-day square grid on the Journal screen with an interactive, horizontally scrollable day strip showing Ethiopian weekday names, Ge'ez numerals, fasting status, written entry indicators, and smooth auto-scrolling to today.
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
