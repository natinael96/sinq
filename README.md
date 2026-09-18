# Sinq (ስንቅ)

> **Current baseline:** 2.5.0 (versionCode 76), reviewed 2026-09-18. See [detailed project status](docs/PROJECT_STATUS.md) for implemented features, content counts, verification results and outstanding work.

**The Ethiopian Orthodox Tewahedo Book of Hours (ሰዓታት) for Android — Amharic-first, with prayer and Scripture available offline.**

![Version](https://img.shields.io/github/v/tag/natinael96/sinq?label=version&color=0E3B31)
![Platform](https://img.shields.io/badge/platform-Android%206.0%2B-E4BC5A)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-0E3B31)

*Sinq (ስንቅ) — "provisions for the journey."*

Sinq brings the Agpeya's seven canonical prayer hours and the complete Psalter (መዝሙረ ዳዊት) to your phone in a focused, distraction-free reading experience — deep liturgical green, gold accents, and Ge'ez verse numerals. No account or analytics SDK: the prayer and Scripture texts ship in the APK, and personal records are stored locally. GitHub APK builds check for updates online; optional Catena commentary opens a third-party website. User-selected backups and shares export files ([privacy policy](https://natinael96.github.io/sinq/privacy-policy.html)).

The [UI/UX audit](docs/UI_UX_PRODUCT_AUDIT.md) and [fix tracker](docs/UI_UX_FIX_PROGRESS.md) document the current remediation work and remaining device checks.

## Features

### Prayer
- **The prayer hours** — ጸሎተ ነግህ (Morning), ሠለስት (Terce), ቀትር (Sext), ተሰዓት (None), ሰርክ (Vespers), ንዋም (Compline), መንፈቀ ሌሊት (Midnight, with its three watches), and the Veil prayer — with a time-of-day suggestion on the home screen.
- **Unified Scripture library** — the Old and New Testaments use the Amharic 1980 edition; all 150 Psalms use Amharic 1980 by default with a reader-local Ge'ez 1980 switch.
- **ውዳሴ ማርያም and ዘወትር ጸሎት** — a portion for each weekday plus ይወድስዋ መላእክት and አንቀጸ ብርሃን, in Amharic with a Ge'ez toggle.

### Calendar and lectionary
- **ግጻዌ** — the complete source-backed lectionary: all 366 fixed dates,
  movable weekday seasons, and the Sunday/mezmur cycle, resolved through the
  Bahre Hasab. Each valid citation opens in the unified Scripture system.
- **ስንክሳር** — Amharic and Ge'ez editions, each covering 366 dates, with commemorations, አርኬ hymns and closing prayer.
- **አጽዋማት** — the fasting calendar: what is in effect today, and every fast of the Ethiopian year.
- **Bahre Hasab reference** — the printed 2001–2015 EC annual table, available from the Library.

### Reading
- **Bible** — the bundled Amharic 1980 Ethiopian Orthodox canon, organized into Old and New Testaments without a network connection.
- Two reading modes: vertical scroll or page-by-page swiping, remembered per preference.
- Six font-size steps (16–28sp), four selectable Ethiopic faces, three line-spacing
  choices, and four text alignments, optically matched across reading surfaces.
- Keep-screen-on across supported text readers; prayer positions retain their section identity and offset.
- Light and dark themes; Amharic and English interface languages.

### Personal
- **Bookmarks** — prayer sections, psalms, scripture chapters and ስንክሳር passages, in one list.
- **Highlights** — tap any verse to colour it (four colours), shared across every screen where the verse appears.
- **Search** — homophone-tolerant Amharic search (ሀ/ሐ/ኀ, ሰ/ሠ, ጸ/ፀ … treated as equal) across the prayers, Psalter, Scripture, ስንክሳር, ውዳሴ ማርያም and church books.
- **Copy, share and save** — export a verse, focused reading, selected ግጻዌ office or scripture chapter as text or paginated image cards; save images to the gallery on Android 10+.
- **Backup and restore** — selected Journey/reading history, marks, prayer lists, setup and offering records to a local JSON file. Journal export is opt-in, plaintext and excludes confession drafts.
- **Journey & habits** — track daily prayer and personal practices without punitive streaks or broken-run language.
- **Reminders** — prayer-time notifications with per-mode configuration, plus a nightly streak nudge and a morning ግጻዌ reading.
- **Home-screen widgets** — today's ምስባክ and ወንጌል, plus a memento mori widget.
- **Reading plans** — day-by-day readings, progress, a book map and completion flow. Automatic reminders at 06:30, 14:00 and 20:00 local time follow up while today’s passages remain unfinished; quiet hours and the off switch still apply.
- **Journal and personal records** — reflections, prayer intentions, tithe/vow records, penance and private confession notes. The optional journal passphrase gates access; it does not encrypt the database.

### Expanded library
- **Church books** — 37 curated books with chapter navigation.
- **ሥርዓተ ማኅሌት** — 190 orders with month paging, feast search, seasonal access, source editions and alternatives.
- **Commentary** — optional online Catena viewer, separate from bundled Scripture.

## Installation

Signed APKs are published on the [Releases page](https://github.com/natinael96/sinq/releases) — every `v*` tag builds one in CI.

1. Download `Sinq-vX.Y.Z.apk` from the latest release.
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

Issues and pull requests are welcome — especially corrections to prayer text mapping, Amharic/English translations, and testing across devices. For text changes, edit `sources/hours/hour_mapping.json` or `tools/extract_content.py` and regenerate; never edit the bundled JSON directly.

## License & Content

The code and the bundled prayer text are under **different licenses**. If you fork this repo, that distinction matters — the content does not inherit the code's license.

- **Code:** [Apache License 2.0](LICENSE).
- **Prayer and Scripture text:** the 80-weahadu Amharic Bible by [EOTCOpenSource](https://github.com/EOTCOpenSource/80-weahadu), used under [**CC BY-NC-ND 4.0**](https://creativecommons.org/licenses/by-nc-nd/4.0/). Passages are selected and arranged into the hours of prayer; verse text is reproduced unchanged, except that the acrostic letters of Psalm 118 are rendered as stanza headings. This material may not be used commercially or redistributed in modified form. Sinq is and will remain non-commercial: no ads, no in-app purchases, no subscriptions.
- **ግጻዌ, Synaxarium, Mahlet and church books:** [NOTICE](NOTICE) records these transcriptions as CC BY-NC-ND 4.0. Source-specific provenance and unresolved review questions remain in [the rights record](docs/CONTENT_RIGHTS.md); content does not inherit Apache-2.0.
- **ውዳሴ ማርያም (Wudase Maryam):** the Ge'ez and Amharic text is a centuries-old, public-domain Ethiopian Orthodox liturgical prayer. This particular digitization is from [tecleet/wudase-mariam](https://github.com/tecleet/wudase-mariam) (no license stated), reshaped into stanzas by `tools/build_wudase.py`. If you hold rights to that transcription and want it removed or re-credited, please open an issue.
- **Font:** [Abyssinica SIL](https://software.sil.org/abyssinica/) and Noto Sans Ethiopic, under the [SIL Open Font License 1.1](docs/AbyssinicaSIL-OFL.txt).
- **Reader fonts:** the selectable faces — Ethiopic Abay Light (abass alamnehe), Bela Bereka (Abel Daniel), and Zemenay (Abel Yeshewalem) — are distributed by [Font.et](https://www.font.et/) under the SIL Open Font License; per-font notices, including the additionally bundled Waldba face, are in [docs/fonts/](docs/fonts/). OFL permits bundling and redistribution with software provided the fonts are not sold on their own. *Note: Zemenay's embedded metadata names an "ETHL" license (t.me/ethelglyphs) while Font.et distributes it as OFL; we follow the distributor's stated terms.*

> The Apache-2.0 grant covers the **source code only**. Nothing under `app/src/main/assets/content/` inherits it — see [NOTICE](NOTICE) for the per-source terms, and [docs/CONTENT_RIGHTS.md](docs/CONTENT_RIGHTS.md) for the full rights record.

### Prayer clock widget

Long-press the Android home screen, open **Widgets → Sinq → Prayer clock**, and place or resize it. The green-and-gold 24-hour dial matches the Sinq website, with seven canonical prayer hours, Ge'ez numerals and the current hour highlighted. The digital time is live; tapping opens the current prayer. The dial follows local time and canonical hours, independently of custom reminder schedules. Android power restrictions can delay dial redraws; no foreground service or wakeup alarm is used.
