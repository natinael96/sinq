# Sinq (ስንቅ)

**The Ethiopian Orthodox Tewahedo Book of Hours (ሰዓታት) for Android — Amharic-first, beautiful, and fully offline.**

![Version](https://img.shields.io/badge/version-0.4.0-0E3B31)
![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-E4BC5A)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-0E3B31)

*Sinq (ስንቅ) — "provisions for the journey."*

Sinq brings the Agpeya's seven canonical prayer hours and the complete Psalter (መዝሙረ ዳዊት) to your phone in a focused, distraction-free reading experience — deep liturgical green, gold accents, and Ge'ez verse numerals. No account, no network, no analytics: everything ships in the APK and stays on your device.

## Features

### Prayer
- **The prayer hours** — ጸሎተ ነግህ (Morning), ሠለስት (Terce), ቀትር (Sext), ተሰዓት (None), ሰርክ (Vespers), ንዋም (Compline), መንፈቀ ሌሊት (Midnight, with its three watches), and the Veil prayer — with a time-of-day suggestion on the home screen.
- **Full Psalter** — all 150 psalms, with the traditional weekday reading divisions and the whole book a toggle away. Psalm 118's acrostic renders with its 22 Hebrew-letter stanza headings (አሌፍ … ታው).
- **Mequteria (መቁጠሪያ)** — a prayer-rope counter with configurable loop length.

### Reading
- Two reading modes: vertical scroll or page-by-page swiping, remembered per preference.
- Five font-size steps, Ge'ez verse numerals, and Abyssinica SIL typography.
- Keep-screen-on while praying; scroll position remembered per hour.
- Light and dark themes; Amharic and English interface languages.

### Personal
- **Bookmarks** — mark any prayer section or psalm; jump back from one list.
- **Highlights** — tap any verse to color it (four colors), shared across every screen where the verse appears.
- **Search** — homophone-tolerant Amharic search (ሀ/ሐ/ኀ, ሰ/ሠ, ጸ/ፀ … treated as equal) across all prayers.
- **Customization** — reorder or hide sections within an hour, add any psalm to any hour, create custom hours, rename or hide hours.
- **Habits & streaks** — track daily practices, keep a streak, and share it as an image card.
- **Reminders** — schedule prayer-time notifications with per-mode configuration.

## Installation

APK releases on GitHub are planned. Until then, build from source (below) or sideload a build shared with you:

1. Allow **Install unknown apps** for your browser or file manager (Android Settings → Apps).
2. Open the APK and install. Requires **Android 8.0 (API 26)** or newer.

## Building from Source

### Prerequisites

| Requirement | Version |
|---|---|
| Android Studio (or standalone JDK) | Ladybug+ / JDK 17 |
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

The prayer text is **generated, never hand-edited**. Bundled JSON under `app/src/main/assets/content/` is produced by:

```bash
python tools/extract_content.py
```

| Input | Role |
|---|---|
| `../80-weahadu/data/am/*.json` | Source text — the 80-weahadu Amharic Bible (sibling repository) |
| `content/hour_mapping.json` | Which psalms, stanzas, and gospel passages compose each hour |

The script assembles each hour's sections, splits Psalm 118's acrostic letters into stanza headings, builds the full Psalter, and writes a manifest. Section IDs are permanent contracts — bookmarks and highlights reference them across app updates.

## Architecture

Single-module Compose app, offline-only, no runtime database — content loads from assets and is cached in memory; user data lives in Preferences DataStore as serialized JSON.

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM target 11) |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Persistence | DataStore Preferences + kotlinx.serialization |
| Reminders | AlarmManager (`reminders/`) |

```text
app/src/main/java/com/agpeya/app/
├── MainActivity.kt      # NavHost and app entry
├── data/                # Repositories: content, settings, bookmarks, highlights, layouts
├── model/               # Serializable content and user-data models
├── reminders/           # Alarm scheduling for prayer notifications
├── search/              # Homophone-folding Amharic search
└── ui/                  # Screens: home, reading, psalter, bookmarks, search,
                         # habits, settings, customize, intro, theme, strings
```

### Versioning

`0.MINOR.PATCH` pre-release scheme — PATCH for fixes, MINOR for features; `versionCode` increments on every update. `1.0.0` is reserved for the first public release. See [PLAN.md](PLAN.md) for the roadmap.

## Contributing

Issues and pull requests are welcome — especially corrections to prayer text mapping, Amharic/English translations, and testing across devices. For text changes, edit `content/hour_mapping.json` or `tools/extract_content.py` and regenerate; never edit the bundled JSON directly.

## License & Content

The code and the bundled prayer text are under **different licenses**. If you fork this repo, that distinction matters — the content does not inherit the code's license.

- **Code:** license not yet finalized. *(Pick one before 1.0 — see the note below.)*
- **Prayer text:** the 80-weahadu Amharic Bible by [EOTCOpenSource](https://github.com/EOTCOpenSource/80-weahadu), used under [**CC BY-NC-ND 4.0**](https://creativecommons.org/licenses/by-nc-nd/4.0/). Passages are selected and arranged into the hours of prayer; verse text is reproduced unchanged, except that the acrostic letters of Psalm 118 are rendered as stanza headings. This means the bundled content under `app/src/main/assets/content/` **may not be used commercially, and may not be redistributed in modified form** — by this project or by anyone forking it. Sinq is and will remain non-commercial: no ads, no in-app purchases, no subscriptions.
- **Font:** [Abyssinica SIL](https://software.sil.org/abyssinica/) and Noto Sans Ethiopic, under the [SIL Open Font License 1.1](docs/AbyssinicaSIL-OFL.txt).

> **Note on choosing a code license:** a permissive code license (MIT/Apache-2.0) is fine and does not conflict with the content terms, as long as this section keeps the split explicit. Full rights record: [docs/CONTENT_RIGHTS.md](docs/CONTENT_RIGHTS.md).
