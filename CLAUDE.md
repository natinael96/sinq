# Claude / AI Assistant Guide for Sinq (ስንቅ)

> **Primary Reference**: See [AGENTS.md](AGENTS.md) for the complete operational manual, architectural rules, and directory map.

## Quick Commands

```bash
# Run all unit tests (MANDATORY before finishing any task)
./gradlew testDebugUnitTest

# Run specific unit test
./gradlew testDebugUnitTest --tests "com.agpeya.app.data.SynaxariumDataTest"

# Compile Kotlin & assemble APK
./gradlew assembleDebug

# Run linter
./gradlew lintDebug

# Validate bundled content assets
python3 tools/validate_content.py
```

## Architecture at a Glance

- **Platform**: Android 6.0+ (API 23+) targeting Android 16 (compileSdk 36, targetSdk 36).
- **UI Toolkit**: 100% Jetpack Compose with Material 3. Single `MainActivity: ComponentActivity`. Navigation Compose. No Fragments.
- **State & Data**:
  - `DataStore Preferences`: App state, settings, habits, and bookmarks.
  - `Room SQLite`: Strictly for `JournalDatabase` (`JournalEntry`).
  - `assets/content/`: Pre-processed bundled JSON corpora (Bible, Psalter, Sinksar, Mahlet, Seatat, Books).
- **Core Principles**:
  - **100% Offline-First**: No network required for any reading, praying, or liturgical calendar features.
  - **Sacred Aesthetics & Liturgy**: Traditional red rubrication (`Rubrication.kt`), native Ge'ez numerals (`geezNumeral()`), computus/calendar math (`BahreHasab.kt`, `EthiopianDate.kt`).
  - **Bookmark Permanence**: Never alter existing section IDs or content hash schemes.
  - **Versioning**: `versionCode` must increment by 1 on every single update in `app/build.gradle.kts`.
