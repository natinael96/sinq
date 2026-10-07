# Contributing to Sinq (ስንቅ)

Thank you for your interest in contributing to **Sinq (ስንቅ)**! 

Sinq is a sacred, distraction-free Android application dedicated to the **Ethiopian Orthodox Tewahedo Church** liturgical corpus and daily devotional prayer life. Whether you are fixing a bug, refining Jetpack Compose UI, proposing liturgical corrections, or improving documentation, your contributions help preserve and present this heritage with dignity and care.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Core Invariants & Philosophy](#core-invariants--philosophy)
- [How Can You Contribute?](#how-can-you-contribute)
  - [Reporting Issues & Bugs](#reporting-issues--bugs)
  - [Liturgical & Textual Corrections](#liturgical--textual-corrections)
  - [Suggesting Features](#suggesting-features)
  - [Code Contributions](#code-contributions)
- [Development Setup](#development-setup)
  - [Prerequisites](#prerequisites)
  - [Building the Project](#building-the-project)
  - [Running on a Device or Emulator](#running-on-a-device-or-emulator)
- [Development & Contribution Workflow](#development--contribution-workflow)
  - [Branching Strategy](#branching-strategy)
  - [Commit Message Guidelines](#commit-message-guidelines)
  - [Pull Request Process](#pull-request-process)
  - [AI-Assisted Contributions](#ai-assisted-contributions)
- [Quality Gates & Testing](#quality-gates--testing)
- [Content Pipeline Architecture](#content-pipeline-architecture)
- [Licensing & Intellectual Property](#licensing--intellectual-property)

---

## Code of Conduct

All contributors and community participants are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). We are committed to providing a welcoming, respectful, and harassment-free environment for everyone.

---

## Core Invariants & Philosophy

Before diving into code or content changes, please familiarize yourself with the non-negotiable principles of the project:

### 1. Liturgical & Cultural Integrity
* **Traditional Red Rubrication (ቀይ ጽሑፍ):** Liturgical directives, holy names (God, Christ, St. Mary, saints, angels), and solemn versicles must use the app's rubrication system (`Rubrication.kt`, `Scope.SINKSAR`, `Scope.MELKIE`, `Scope.GENERAL`). Never render unrubricated raw strings where traditional rubrication is expected.
* **Ge'ez Numerals:** Liturgical, chapter, and psalm numbering displayed in reading surfaces must use native Ge'ez numerals via `geezNumeral(n)` (`GeezNumerals.kt`), not Arabic digits.
* **Computus & Calendar Math:** All date and seasonal calculations must pass through `EthiopianDate.kt` and `BahreHasab.kt`. Naive Gregorian offset math is strictly prohibited.
* **Parallel Editions Independence:** In bilingual views (e.g., Sinksar, Wudase Maryam), Amharic and Ge'ez are parallel *editions* rather than verse-by-verse alignments. Document and day-level switching are supported; do not force divergent texts into rigid two-column verse grids.

### 2. 100% Offline-First Guarantee & Privacy
* The core application (Scriptures, Psalter, Hours, Sinksar, Mahlet, Church Books, Calendar, Search, Journal) works **completely offline**.
* **Zero Telemetry:** No analytics SDKs, tracking libraries, crash reporters sending telemetry, or mandatory user accounts.
* External network calls are strictly restricted to:
  1. Optional Play In-App Updates (`PlayUpdateRepository.kt`) or release checks.
  2. Optional external links to Catena Bible patristic commentary (`CatenaScreen.kt`).

### 3. Bookmark Stability & Data Permanence
* Bookmarks, highlights, and history use stable content-addressed keys:
  * Scripture: `scripture:<edition>:<bookKey>:<chapter>:<verse>`
  * Psalter: `am-1980:ps_<number>:<verse>` or `gez-1980:ps_<number>:<verse>`
  * Sinksar: `sinksar:<month>-<day>:<entryHash>`
  * Hours: `sinksar_verse` or `<hourId>:<sectionId>`
* **Never rename or re-hash section IDs or entry keys** without an automated migration. Breaking keys corrupts user prayer journals and saved marks.

### 4. Minimal Dependencies ("Ponytail" Principle)
* Avoid introducing large third-party libraries for problems readily solved by the Kotlin standard library or native Android Jetpack APIs.
* Keep the footprint lightweight, fast, and maintainable.

---

## How Can You Contribute?

### Reporting Issues & Bugs

If you encounter a bug or unexpected behavior:
1. Search the [GitHub Issues](https://github.com/natinael96/sinq/issues) tracker to check if it has already been reported.
2. If not, open a new issue with a clear title and description. Include:
   * **Device model** and **Android version** (e.g., Pixel 7, Android 14).
   * **App version** (e.g., v2.5.1, found in Settings).
   * **Steps to reproduce** the issue.
   * **Expected vs. actual behavior**.
   * Screenshots or screen recordings when relevant.

### Liturgical & Textual Corrections

Textual fidelity is of paramount importance to this project. If you notice a typographical error, missing stanza, or incorrect commemoration:
1. Open an issue or discussion citing the specific book, date, hour, or chapter.
2. Provide the reference edition or venerable source print (e.g., EOTC Synod 1980 Amharic Bible, printed Gitsawe, official Synaxarium publications).
3. **Important:** Prayer and liturgical texts under `app/src/main/assets/content/` are generated artifacts. Please read [Content Pipeline Architecture](#content-pipeline-architecture) before submitting changes.

### Suggesting Features

We welcome ideas that align with our sacred, distraction-free aesthetic:
1. Open an issue to discuss your proposal before investing substantial time in an implementation.
2. Describe the feature, its liturgical or devotional basis, and why it benefits users.
3. Feature ideas must respect the offline-first and privacy invariants.

### Code Contributions

1. Look for existing issues labeled `good first issue` or `help wanted`.
2. For larger features or refactorings, comment on the issue first so efforts can be coordinated.
3. Create a fork, follow the development guidelines below, and open a Pull Request.

---

## Development Setup

### Prerequisites

| Tool | Version | Notes |
| :--- | :--- | :--- |
| **JDK** | 17 | Used in CI and Gradle builds |
| **Android SDK** | API 36 (`compileSdk 36`, `targetSdk 36`) | Min SDK is 23 (Android 6.0+) |
| **Android Studio** | Ladybug / Koala or newer | Compatible with Android Gradle Plugin 8.10+ |
| **Python** | 3.10+ | Required only if running content generators / validators |

### Building the Project

Clone your fork and assemble the debug build:

```bash
git clone https://github.com/<your-username>/sinq.git
cd sinq
./gradlew assembleDebug
```

The resulting debug APK is located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Running on a Device or Emulator

With an Android device connected via USB debugging or an active emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Development & Contribution Workflow

### Branching Strategy

1. Work on a descriptive feature or bugfix branch off `master`:
   ```bash
   git checkout -b fix/psalm-numeral-rendering
   # or
   git checkout -b feat/lectionary-audio-toggle
   ```
2. Keep your branch focused on a single topic. Avoid bundling unrelated fixes or reformatting into one pull request.

### Commit Message Guidelines

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short imperative summary>

[optional body explaining motivation and context]

[optional footer, e.g., Closes #42]
```

**Common types:**
* `feat`: A new feature or user-facing addition
* `fix`: A bug fix
* `docs`: Documentation updates
* `style`: Formatting or cosmetic UI adjustments with no logic change
* `refactor`: Code changes that neither fix a bug nor add a feature
* `test`: Adding or correcting tests
* `chore`: Build scripts, dependencies, or tool updates

**Examples:**
```
feat(search): optimize homophone folding index for Sinksar entries
fix(rubrication): wrap saint titles with Scope.SINKSAR in commemoration cards
docs: add CONTRIBUTING.md and CODE_OF_CONDUCT.md
```

### Pull Request Process

1. **Keep Diffs Minimal:** Write clean, focused code. Avoid opportunistic whitespace changes across untouched files.
2. **Run All Quality Gates:** Verify locally before opening the PR (see [Quality Gates & Testing](#quality-gates--testing)).
3. **PR Description:**
   * Describe what the PR accomplishes and why.
   * Reference corresponding issue numbers (`Closes #123` or `Fixes #456`).
   * Include before/after screenshots or GIFs for UI modifications.
4. **Code Review:** Address feedback respectfully. Push new commits directly to your PR branch to update the review.

### AI-Assisted Contributions

We welcome contributors who use AI coding tools (Claude, Copilot, ChatGPT, Antigravity, etc.), provided:
* **You understand every line:** Do not submit code you cannot explain or debug.
* **Domain invariants are respected:** AI generators frequently make naive assumptions about Gregorian dates, Arabic numerals, or cloud dependencies. Verify all liturgical and offline constraints.
* **No bulk low-quality spam:** PRs consisting of unsolicited automated refactorings without issue consensus will be closed.

---

## Quality Gates & Testing

Before submitting a pull request, ensure all gates pass cleanly:

```bash
# 1. Validate all bundled offline content assets
python3 tools/validate_content.py

# 2. Run the complete JVM unit test suite (480+ tests)
./gradlew testDebugUnitTest --no-daemon

# 3. Run Android Lint
./gradlew lintDebug --no-daemon

# 4. Confirm debug build assembles
./gradlew assembleDebug --no-daemon
```

To run a specific unit test during development:
```bash
./gradlew testDebugUnitTest --tests "com.agpeya.app.data.SynaxariumDataTest"
./gradlew testDebugUnitTest --tests "com.agpeya.app.search.SynaxariumSearchTest"
```

---

## Content Pipeline Architecture

> **Crucial Rule:** The JSON assets under `app/src/main/assets/content/` are generated from master sources. **Never hand-edit the generated JSON files.**

1. Master transcriptions, OCR inputs, and mappings live under `sources/`:
   * `sources/hours/hour_mapping.json`: Prayer hour composition and structure
   * `sources/gitsawe/`: Lectionary source transcriptions
   * `sources/wudase/`: Wudase Maryam texts
   * `sources/mahlet/`: Mahlet orders and feast sources (curated alongside [EOTC Mahlet](https://t.me/EOTCmahlet))
2. Extraction and build scripts live under `tools/`:
   * `python3 tools/extract_content.py`: Rebuilds canonical hours
   * `python3 tools/build_sinksar.py`: Rebuilds Synaxarium assets
   * `python3 tools/build_mahlet.py`: Rebuilds Mahlet orders
   * `python3 tools/build_books.py`: Rebuilds church books
   * `python3 tools/build_wudase.py`: Rebuilds Wudase Maryam
   * `python3 tools/validate_content.py`: Verifies integrity and constraints
3. To update content, edit the source or generator, run the appropriate script, validate with `tools/validate_content.py`, and test.

---

## Licensing & Intellectual Property

Contributions to this project are subject to the two-tier licensing model of Sinq:

* **Source Code:** Licensed under the [Apache License 2.0](LICENSE). By submitting a pull request, you agree that your code contributions will be licensed under Apache-2.0.
* **Liturgical & Devotional Content:** The bundled texts and transcriptions (Scripture, Hours, Gitsawe, Sinksar, Mahlet) are non-commercial devotional works provided under [Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 (CC BY-NC-ND 4.0)](NOTICE) or their respective public domain / ecclesiastical source terms. See [NOTICE](NOTICE) and [docs/CONTENT_RIGHTS.md](docs/CONTENT_RIGHTS.md) for detailed provenance.

---

Thank you for contributing your time and talents to **Sinq (ስንቅ)**!
