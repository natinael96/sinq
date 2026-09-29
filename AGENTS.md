# AI Agent Guidelines for Sinq (ስንቅ)

Welcome to the **Sinq (ስንቅ)** repository. This document serves as the primary operational and architectural manual for all autonomous and pair-programming AI agents (including Claude Code, Cursor, Copilot, Antigravity, Windsurf, and custom CLI agents).

---

## 1. Project Mission & Identity

**Sinq (ስንቅ)** is a dedicated, distraction-free Android application for the **Ethiopian Orthodox Tewahedo Church** liturgical corpus and daily devotional prayer life:
- **Core Liturgical Corpus**: The Seven Canonical Hours of the Horologium (መጽሐፈ ሰዓታት), 32 canonical Melkea (መልክአ መልክዕ) hymns, 181 Mahlet feast orders (ሥርዓተ ማኅሌት), ውዳሴ ማርያም, and ዘወትር ጸሎት.
- **Canonical Scriptures**: Full 81-book Ethiopian Orthodox Bible, all 150 Psalms (መዝሙረ ዳዊት) in LXX order with Amharic and Ge'ez toggles.
- **Calendar & Lectionary**: Complete 366-day lectionary (ግጻዌ) and Synaxarium (ስንክሳር) across both Amharic and Ge'ez parallel editions, calculated via the computus (ባሕረ ሐሳብ).
- **Sacred Aesthetic**: Deep liturgical green (`#0E3B31`), gold accents (`#E8C46B`), ivory paper grounds, traditional red rubrication (ቀይ ጽሑፍ), and native Ge'ez numerals.
- **Strict Privacy**: 100% offline-first. No analytics SDKs, no tracking, no mandatory accounts.

---

## 2. Technology Stack & Platform Targets

- **Operating System / Target**: Android 6.0+ (API 23+) up to **Android 16 (compileSdk 36, targetSdk 36)**.
- **Language**: Kotlin 2.1+ (configured in `gradle/libs.versions.toml`).
- **UI Toolkit**: 100% **Jetpack Compose** with **Material 3**.
- **Architecture**: Single `ComponentActivity` (`MainActivity.kt`) with Compose Navigation (`androidx.navigation.compose`). **NO Android Fragments** (Compose throughout).
- **Serialization**: `kotlinx.serialization.json` (no reflection-based Gson/Jackson).
- **Persistence**:
  - `androidx.datastore.preferences`: Bounded state, settings, habits, and bookmarks.
  - `androidx.room`: SQLite database strictly for `JournalDatabase` (`JournalEntry` table).
- **Assets Pipeline**: Pre-processed, bundled JSON assets in `app/src/main/assets/content/`.

---

## 3. Essential Commands

### Testing & Verification (MANDATORY)
```bash
# Run all unit tests (always run before completing a task)
./gradlew testDebugUnitTest

# Run a specific test class
./gradlew testDebugUnitTest --tests "com.agpeya.app.data.SynaxariumDataTest"
./gradlew testDebugUnitTest --tests "com.agpeya.app.search.SynaxariumSearchTest"

# Run Android Lint
./gradlew lintDebug
```

### Building & Packaging
```bash
# Compile debug APK
./gradlew assembleDebug

# Compile release bundle (requires keystore.properties or SINQ_* environment variables)
./gradlew bundleRelease

# Clean build artifacts
./gradlew clean
```

### Content Validation & Generation
```bash
# Validate bundled assets against content schemas and constraints
python3 tools/validate_content.py

# Rebuild specific content corpora (outputs to app/src/main/assets/content/)
python3 tools/build_sinksar.py
python3 tools/build_mahlet.py
python3 tools/build_books.py
python3 tools/build_wudase.py
python3 tools/build_reading_plans.py
```

---

## 4. Repository Structure & Directory Map

```
├── app/
│   ├── build.gradle.kts                # App-level build config, SDK levels, versionCode/Name
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/content/         # Bundled offline JSON assets (Bible, Sinksar, Mahlet, etc.)
│       │   ├── java/com/agpeya/app/
│       │   │   ├── MainActivity.kt     # App entry point, root Scaffold, and top-level NavHost
│       │   │   ├── data/               # Repositories, DB, computus, search, and storage
│       │   │   ├── model/              # Kotlinx @Serializable data classes and domain entities
│       │   │   ├── reminders/          # AlarmManager receivers and notification workers
│       │   │   ├── ui/                 # Composable UI screens grouped by domain
│       │   │   │   ├── common/         # Reusable widgets, date pickers, rubrication, sharing
│       │   │   │   ├── gitsawe/        # Lectionary, Sinksar reader, and Sunday cycle screens
│       │   │   │   ├── habits/         # Prayer journey, streaks, habit tracking
│       │   │   │   ├── home/           # Main landing screen (hour suggestion, readings, fasts)
│       │   │   │   ├── journal/        # Journal entries, reflections, and passphrases
│       │   │   │   ├── library/        # Bookshelf, Scripture reader, Psalter, Wudase Maryam
│       │   │   │   ├── mahlet/         # Mahlet reader, feast order selectors
│       │   │   │   ├── marks/          # Bookmarks and highlight collections
│       │   │   │   ├── reading/        # Core reading layout, font scaling, pagination
│       │   │   │   ├── search/         # Homophone-tolerant Amharic global search
│       │   │   │   ├── settings/       # App preferences, notifications, typography settings
│       │   │   │   ├── strings/        # Bilingual string resources (Amharic & English interfaces)
│       │   │   │   └── theme/          # Liturgical palette, typography, spacing tokens
│       │   │   └── widget/             # AppWidget providers (Daily Gospel/Misbak)
│       └── test/                       # Comprehensive JVM unit tests (over 480 test cases)
├── gradle/
│   └── libs.versions.toml              # Version catalog for dependencies and plugins
├── sources/                            # Master transcriptions, OCR files, and raw sources
├── tools/                              # Python data generation, conversion, and validation scripts
├── docs/                               # Architecture, SRS, SDS, Design System, Liturgical Review
├── CHANGELOG.md                        # User-facing and developer release log
├── NOTICE / LICENSE                    # Legal, licensing, and copyright notices
└── AGENTS.md                           # This guide
```

---

## 5. Non-Negotiable Rules for AI Agents

### Rule 1: Liturgical & Cultural Integrity
- **Red Rubrication (ቀይ ጽሑፍ)**: Holy names (God, Christ, Mary, saints, angels) and liturgical directives must be wrapped using the app's rubrication system (`Rubrication.kt`, `Scope.SINKSAR`, `Scope.MELKIE`, `Scope.GENERAL`). Never render raw strings where rubricated rendering is expected.
- **Ge'ez Numerals**: All liturgical and scripture numbering displayed in reading surfaces must use Ge'ez numerals (`geezNumeral(n)` from `GeezNumerals.kt`), not Arabic digits.
- **Calendar Calculations**: Date mathematics must go through `EthiopianDate.kt` and `BahreHasab.kt`. Do not attempt naive Gregorian offset math.
- **Parallel Text Independence**: In bilingual readers (e.g. Sinksar, Wudase Maryam), Amharic and Ge'ez are parallel *editions*, not parallel verses. Only switch editions at the document or day level; never force them side-by-side into a two-column verse grid where paragraph counts diverge.

### Rule 2: Absolute Offline Guarantee
- The core application (Bible, Psalter, Hours, Sinksar, Mahlet, Books, Calendar, Search, Journal) must **never require a network connection**.
- Only two intentional external network features exist:
  1. Optional Play In-App Updates (`PlayUpdateRepository.kt`).
  2. Optional external web link to Catena Bible commentary (`CatenaScreen.kt`).
- Do not introduce network calls, cloud sync, or remote APIs for reading or prayer features.

### Rule 3: Content Contract & Bookmark Permanence
- `sources/` contains raw inputs; `tools/*.py` transforms them into `app/src/main/assets/content/`. The Android app **only** reads from `assets/content/`.
- **Identity Stability**: Bookmarks and highlights use composite content-addressed keys:
  - Scripture: `scripture:<edition>:<bookKey>:<chapter>:<verse>`
  - Psalter: `am-1980:ps_<number>:<verse>` or `gez-1980:ps_<number>:<verse>`
  - Sinksar: `sinksar:<month>-<day>:<entryHash>`
  - Hours: `sinksar_verse` or `<hourId>:<sectionId>`
- **Never rename or re-hash section IDs or entry keys** without an automated migration in `HighlightRepository.kt` or `UserDataRepository.kt`. Breaking bookmarks breaks users' prayer journals.

### Rule 4: Versioning Policy
- Defined in `app/build.gradle.kts`:
  - `versionName`: Semantic versioning (`MAJOR.MINOR.PATCH`).
  - `versionCode`: **Must increment by 1 on every single update/release, without exception.**
  - When bumping releases, update `app/build.gradle.kts`, `ChangelogScreen.kt`, and `CHANGELOG.md`.

### Rule 5: Keep It Minimal & Clean (Ponytail Principles)
- **Do not over-engineer**: No speculative features, no single-implementation interfaces, no unnecessary delegating wrapper classes.
- **Reach for Kotlin stdlib and Android APIs**: Do not add external dependencies when stdlib or platform APIs already solve the problem.
- **Preserve Comments & Rationale**: The codebase features extensive domain commentary explaining liturgical and typographic reasoning. Always preserve existing docstrings and comments.

---

## 6. How to Implement Changes as an Agent

1. **Investigate First**:
   - Locate existing data models in `app/src/main/java/com/agpeya/app/model/`.
   - Inspect repository loaders in `app/src/main/java/com/agpeya/app/data/`.
   - Check UI components in `app/src/main/java/com/agpeya/app/ui/`.
2. **Execute Narrow, Exact Edits**:
   - Make precise changes using replacement tools. Avoid sweeping rewrites.
3. **Verify Locally**:
   - Always run `./gradlew testDebugUnitTest` to guarantee all tests pass.
   - If UI was touched, ensure preview and build assemble cleanly (`./gradlew compileDebugKotlin`).
4. **Audit for Dead Code**:
   - Remove unused imports, abandoned test scaffolding, and obsolete placeholder strings.
