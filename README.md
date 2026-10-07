# Sinq (ስንቅ)

> 📦 **Latest Release:** [![Release](https://img.shields.io/github/v/release/natinael96/sinq?color=0E3B31&label=release&sort=semver)](https://github.com/natinael96/sinq/releases/latest) · See **[CHANGELOG.md](CHANGELOG.md)** for recent additions and **[PROJECT_STATUS.md](docs/PROJECT_STATUS.md)** for audit verification.

**The Ethiopian Orthodox Tewahedo Book of Hours (ሰዓታት) for Android — Amharic-first, with prayer and Scripture available offline.**

[![Latest Release](https://img.shields.io/github/v/release/natinael96/sinq?color=0E3B31&label=version&sort=semver)](https://github.com/natinael96/sinq/releases/latest)
[![Total Downloads](https://img.shields.io/github/downloads/natinael96/sinq/total?color=E4BC5A&label=downloads)](https://github.com/natinael96/sinq/releases)
[![Build Status](https://img.shields.io/github/actions/workflow/status/natinael96/sinq/release.yml?label=build&color=0E3B31)](https://github.com/natinael96/sinq/actions)
[![Platform](https://img.shields.io/badge/platform-Android%206.0%2B%20(API%2023%2B)-E4BC5A)](https://developer.android.com/about/versions/marshmallow)
[![Target SDK](https://img.shields.io/badge/targetSdk-36%20(Android%2016)-0E3B31)](https://developer.android.com)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-0E3B31)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/license-Apache--2.0-0E3B31.svg)](LICENSE)
[![Last Commit](https://img.shields.io/github/last-commit/natinael96/sinq?color=E4BC5A)](https://github.com/natinael96/sinq/commits/master)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-0E3B31.svg)](CONTRIBUTING.md)
[![Code of Conduct](https://img.shields.io/badge/Contributor%20Covenant-2.1-E4BC5A.svg)](CODE_OF_CONDUCT.md)

*Sinq (ስንቅ) — "provisions for the journey."*

Sinq brings the Agpeya's seven canonical prayer hours and the complete Psalter (መዝሙረ ዳዊት) to your phone in a focused, distraction-free reading experience — deep liturgical green, gold accents, and Ge'ez verse numerals. No account or analytics SDK: the prayer and Scripture texts ship in the APK, and personal records are stored locally. GitHub APK builds check for updates online; optional Catena commentary opens a third-party website. User-selected backups and shares export files ([privacy policy](https://natinael96.github.io/sinq/privacy-policy.html)).

The [UI/UX audit](docs/UI_UX_PRODUCT_AUDIT.md) and [fix tracker](docs/UI_UX_FIX_PROGRESS.md) document the ongoing remediation work and device checks.

## Features

### Prayer
- **The prayer hours** — ጸሎተ ነግህ (Morning), ሠለስት (Terce), ቀትር (Sext), ተሰዓት (None), ሰርክ (Vespers), ንዋም (Compline), መንፈቀ ሌሊት (Midnight, with its three watches), and the Veil prayer — with a time-of-day suggestion on the home screen.
- **Unified Scripture library** — the Old and New Testaments use the Amharic 1980 edition alongside the parallel English NKJV translation; all 150 Psalms use Amharic 1980 by default with a reader-local Ge'ez 1980 switch.
- **ውዳሴ ማርያም, ዘወትር ጸሎት, and ውዳሴ አምላክ** — a portion for each weekday plus ይወድስዋ መላእክት and አንቀጸ ብርሃን, in Amharic with a Ge'ez toggle, plus the complete 8 canonical daily sections of ውዳሴ አምላክ.

### Calendar and lectionary
- **ግጻዌ** — the complete source-backed lectionary: all 366 fixed dates,
  movable weekday seasons, and the Sunday/mezmur cycle, resolved through the
  Bahre Hasab. Each valid citation opens in the unified Scripture system.
- **ስንክሳር** — Amharic and Ge'ez editions, each covering 366 dates, with commemorations, 1,017 verified አርኬ hymns and closing prayer.
- **አጽዋማት** — the fasting calendar: what is in effect today, and every fast of the Ethiopian year.
- **Bahre Hasab reference** — the printed 2001–2015 EC annual table, available from the Library.

### Reading
- **Bible** — bundled Amharic 1980 Ethiopian Orthodox canon and English NKJV translation, organized into Old and New Testaments without a network connection.
- Two reading modes: vertical scroll or page-by-page swiping, remembered per preference.
- Six font-size steps (16–28sp), selectable Ethiopic faces, three line-spacing
  choices, and four text alignments, optically matched across reading surfaces.
- Keep-screen-on across supported text readers; prayer positions retain their section identity and offset.
- Light (Sacred Ivory default) and dark themes; Amharic and English interface languages.

### Personal
- **Bookmarks** — prayer sections, psalms, scripture chapters and ስንክሳር passages, in one list.
- **Highlights** — tap any verse to colour it (four colours), shared across every screen where the verse appears.
- **Search** — homophone-tolerant Amharic search (ሀ/ሐ/ኀ, ሰ/ሠ, ጸ/ፀ … treated as equal) across the prayers, Psalter, Scripture, ስንክሳር, ውዳሴ ማርያም and church books.
- **Copy, share and save** — export a verse, focused reading, selected ግጻዌ office or scripture chapter as text or paginated image cards; save images to the gallery on Android 10+.
- **Backup and restore** — selected Journey/reading history, marks, prayer lists, setup and offering records to a local JSON file. Journal export is opt-in, plaintext and excludes confession drafts.
- **Journey & habits** — track daily prayer and personal practices without punitive streaks or broken-run language; interactive Ethiopian-year heatmap with day inspection.
- **Reminders** — prayer-time notifications with per-mode configuration, plus a nightly streak nudge, morning ግጻዌ reading, and future date checklist reminders with deep linking.
- **Home-screen widgets** — today's ምስባክ and ወንጌል, memento mori widget, and 24-hour canonical prayer clock dial.
- **Reading plans** — day-by-day readings, progress, a book map and completion flow. Automatic reminders at 06:30, 14:00 and 20:00 local time follow up while today’s passages remain unfinished; quiet hours and the off switch still apply.
- **Journal and personal records** — reflections, prayer intentions, tithe/vow records, penance and private confession notes. The optional journal passphrase gates access; it does not encrypt the database.

### Expanded library
- **Church books** — 37 curated books with chapter navigation.
- **ሥርዓተ ማኅሌት** — 181 orders with month paging, feast search, seasonal access, source editions and alternatives.
- **Commentary** — optional online Catena viewer, separate from bundled Scripture.

## Installation

Signed APKs are published on the [Releases page](https://github.com/natinael96/sinq/releases) — every `v*` tag builds one in CI.

1. Download `Sinq-v*.apk` from the [Latest Release](https://github.com/natinael96/sinq/releases/latest).
2. Allow **Install unknown apps** for your browser or file manager (Android Settings → Apps).
3. Open the APK and install. Requires **Android 6.0 (API 23)** or newer.
   `java.time` and `java.util.Base64` reach that floor through core library
   desugaring, so the older minimum costs no source changes.

**To get updates automatically**, install [Obtainium](https://github.com/ImranR98/Obtainium) and add
`https://github.com/natinael96/sinq` — it watches the releases and prompts you when a new version
appears. Android verifies each update carries the same signing key, so a tampered APK cannot install
over a genuine one.

## Building from Source

### Prerequisites

| Requirement | Version |
|---|---|
| JDK | 17 (used in CI) |
| Android Studio (optional) | A version compatible with AGP 8.10.1 |
| Android SDK | compileSdk 36 |
| Python (content pipeline only) | 3.10+ |

### Build

```bash
git clone https://github.com/natinael96/sinq.git
cd sinq
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Content Pipeline

The prayer text is **generated, never hand-edited**. The built-in prayer-hour assets under `app/src/main/assets/content/` are produced by:

```bash
python tools/extract_content.py
```

| Input | Role |
|---|---|
| `../80-weahadu/data/am` | Legacy Scripture input used by `extract_content.py` for the hours |
| `../80-weahadu/data/{am-1980,gez-1980}` | Amharic Bible and Ge'ez Psalms inputs for `extract_bible_editions.py` |
| `sources/hours/hour_mapping.json` | Which psalms, stanzas, and gospel passages compose each hour |

`extract_content.py` assembles the prayer hours. `extract_bible_editions.py` bundles the full Amharic 1980 edition and Ge'ez 1980 Psalms and writes their catalog. Section IDs and legacy reader routes remain permanent compatibility contracts for bookmarks and highlights.

The separately licensed Gitsawe transcription is preserved under
`sources/gitsawe/`. `tools/split_gitsawe_months.py` and
`tools/split_gitsawe_parts.py` produce auditable source splits; the four
`tools/import_gitsawe_*.py` importers normalize the fixed, movable weekday,
Sunday and Bahre Hasab collections. See
[sources/gitsawe/README.md](sources/gitsawe/README.md) for provenance and regeneration commands.

## Architecture

Single-module Compose app with bundled offline reading. Content loads from assets and is cached; bounded user records live in Preferences DataStore, and the journal uses Room/SQLite. The manifest declares internet access for update checks and the external commentary viewer.

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM target 11) |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Persistence | DataStore Preferences + kotlinx.serialization; Room for the journal |
| Reminders | AlarmManager (`reminders/`) |

```text
app/src/main/java/com/agpeya/app/
├── MainActivity.kt      # NavHost and app entry
├── data/                # Repositories: content, settings, bookmarks, highlights, layouts
├── model/               # Serializable content and user-data models
├── reminders/           # Alarm scheduling for prayer notifications
├── search/              # Homophone-folding Amharic search
├── widget/              # Gitsawe and memento mori widgets
└── ui/                  # Home, Journey, Library, Settings and pushed readers,
                         # search, marks, journal, Mahlet, records and setup
```

### Versioning

Semantic-style releases use PATCH for fixes and MINOR for features; `versionCode` increments on every update. See [project status](docs/PROJECT_STATUS.md) for current work and [PLAN.md](PLAN.md) for the historical roadmap.

## Verification

```bash
python3 tools/validate_content.py
./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon
```

Normal builds use the checked-in assets. Other corpora have separate generators; see [sources](sources/README.md). Ordinary CI is currently manual-only. The `v*` release workflow runs content validation, unit tests and release-vital lint before building a signed APK with `-PupdateNotice` and a separate Play-upload AAB without that flag. An AAB cannot be installed by tapping it on a phone.

## Contributing

We welcome contributions of all kinds — bug reports, UI/UX improvements, translations, and liturgical corrections.

Please see our **[Contribution Guidelines](CONTRIBUTING.md)** for details on development setup, architecture invariants, testing requirements, and the PR process. All contributors are expected to uphold our **[Code of Conduct](CODE_OF_CONDUCT.md)**.

> **Note on text changes:** For liturgical and prayer text edits, edit the source mappings (e.g. `sources/hours/hour_mapping.json`) or generator scripts (`tools/`) and rebuild; never hand-edit the generated JSON in `app/src/main/assets/content/`.

## License & Content

The code and the bundled prayer text are under **different licenses**. If you fork this repo, that distinction matters — the content does not inherit the code's license.

- **Code:** [Apache License 2.0](LICENSE).
- **Prayer and Scripture text:** the 80-weahadu Amharic Bible by [EOTCOpenSource](https://github.com/EOTCOpenSource/80-weahadu), used under [**CC BY-NC-ND 4.0**](https://creativecommons.org/licenses/by-nc-nd/4.0/). Passages are selected and arranged into the hours of prayer; verse text is reproduced unchanged, except that the acrostic letters of Psalm 118 are rendered as stanza headings. This material may not be used commercially or redistributed in modified form. Sinq is and will remain non-commercial: no ads, no in-app purchases, no subscriptions.
- **ግጻዌ, Synaxarium, Mahlet and church books:** [NOTICE](NOTICE) records these transcriptions as CC BY-NC-ND 4.0. Source-specific provenance and unresolved review questions remain in [the rights record](docs/CONTENT_RIGHTS.md); content does not inherit Apache-2.0.
- **ውዳሴ ማርያም (Wudase Maryam):** the Ge'ez and Amharic text is a centuries-old, public-domain Ethiopian Orthodox liturgical prayer. Scanned and digitized directly from publicly available printed editions and PDF scans by the Sinq maintainer, and reshaped into stanzas by `tools/build_wudase.py`.
- **Font:** [Abyssinica SIL](https://software.sil.org/abyssinica/) and Noto Sans Ethiopic, under the [SIL Open Font License 1.1](docs/AbyssinicaSIL-OFL.txt).
- **Reader fonts:** the selectable faces — Ethiopic Abay Light (abass alamnehe), Bela Bereka (Abel Daniel), Zemenay (Abel Yeshewalem), and Abba Garima (Jérémie Hornus, Gaëtan Baehr, Daniel Yacob) — are distributed by [Font.et](https://www.font.et/) under the SIL Open Font License; per-font notices, including the additionally bundled Waldba face, are in [docs/fonts/](docs/fonts/). OFL permits bundling and redistribution with software provided the fonts are not sold on their own. *Note: Zemenay's embedded metadata names an "ETHL" license (t.me/ethelglyphs) while Font.et distributes it as OFL; we follow the distributor's stated terms.*

> The Apache-2.0 grant covers the **source code only**. Nothing under `app/src/main/assets/content/` inherits it — see [NOTICE](NOTICE) for the per-source terms, and [docs/CONTENT_RIGHTS.md](docs/CONTENT_RIGHTS.md) for the full rights record.

### Prayer clock widget

Long-press the Android home screen, open **Widgets → Sinq → Prayer clock**, and place or resize it. The green-and-gold 24-hour dial matches the Sinq website, with seven canonical prayer hours, Ge'ez numerals and the current hour highlighted. The digital time is live; tapping opens the current prayer. The dial follows local time and canonical hours, independently of custom reminder schedules. Android power restrictions can delay dial redraws; no foreground service or wakeup alarm is used.
