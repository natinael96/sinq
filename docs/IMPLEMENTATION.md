# Sinq (ስንቅ) — Implementation Reference

**What is actually built, as of `versionName 0.2.6` / `versionCode 11` (commit `b390dbb`).**

This document is a deep, code-level description of the shipped application: its
architecture, its data model, the algorithms it implements, every component and
screen, the libraries it depends on, and the reasoning behind the non-obvious
decisions. It is descriptive, not aspirational — where the README or `PLAN.md`
promises something that is not in the code, this document says so (see §14).

Companion documents: `docs/SRS.md` (requirements), `docs/SDS.md` (design spec),
`docs/CONTENT_STRUCTURE.md` (content JSON schema), `docs/CONTENT_RIGHTS.md`
(licensing of the text), `PLAN.md` (the running decision log).

---

## Table of contents

1. [What the app is](#1-what-the-app-is)
2. [Technology stack and libraries](#2-technology-stack-and-libraries)
3. [Architecture](#3-architecture)
4. [Source map](#4-source-map)
5. [Domain model](#5-domain-model)
6. [Content pipeline and bundled assets](#6-content-pipeline-and-bundled-assets)
7. [Persistence layer — the seven DataStores](#7-persistence-layer--the-seven-datastores)
8. [Navigation and the single-Activity shell](#8-navigation-and-the-single-activity-shell)
9. [The reminders subsystem](#9-the-reminders-subsystem)
10. [Algorithms and pure logic](#10-algorithms-and-pure-logic)
11. [UI layer — screens and components](#11-ui-layer--screens-and-components)
12. [Design system: theme, typography, identity](#12-design-system-theme-typography-identity)
13. [Build, signing, shrinking, release](#13-build-signing-shrinking-release)
14. [Testing](#14-testing)
15. [Known gaps, dead code, and drift](#15-known-gaps-dead-code-and-drift)
16. [Concept glossary](#16-concept-glossary)

---

## 1. What the app is

Sinq is an **Amharic-first, fully offline Android app** for the Ethiopian
Orthodox Tewahedo Book of Hours (ሰዓታት / the Agpeya). It ships every word of
prayer text inside the APK, has **no network permission at all**, no account, no
analytics, and no server.

Four functional pillars:

| Pillar | What it means in code |
|---|---|
| **Read** | 8 prayer hours + the complete 150-psalm Psalter, rendered from bundled JSON with Ge'ez verse numerals, two reading modes, five font steps, bookmarks and verse highlights. |
| **Remind** | Real alarm-clock–grade prayer reminders: exact alarms, a ringing foreground service, a full-screen lock-screen alarm, snooze, and a "did you pray?" follow-up. |
| **Track** | Daily habit check-offs (per prayer hour + custom habits), a prayer-day count per Ethiopian month or running fast (never a streak), an Ethiopian-calendar contribution heatmap with a fasting-season wash, and today's candle. |
| **Personalize** | Reorder/hide sections in an hour, add any psalm to any hour, create/rename/hide/reorder whole hours, multiple named reminder "modes", theme + language choice, a local profile name. |

Hard constraints the code enforces:

- **No network.** `AndroidManifest.xml` declares no `INTERNET` permission. All
  content is in `assets/`, all user data is in local DataStore files.
- **Content is immutable data.** Prayer text is generated offline by a Python
  pipeline and never mutated at runtime. Personalization is an *overlay* keyed by
  stable section IDs.
- **IDs are permanent contracts.** Bookmarks, highlights, reminders and prayer-day
  records all reference content IDs (`morning_ps1`, `ps_118`, `hour_morning`).
  Changing an ID in the content pipeline would orphan user data.
- **Amharic is the default, not a translation.** UI chrome is bilingual
  (Amharic/English) via a Kotlin interface; prayer content is never translated.

Runtime floor: **Android 8.0 (API 26)**. Compiled and targeted at **API 36**.

---

## 2. Technology stack and libraries

### 2.1 Version catalog (`gradle/libs.versions.toml`)

| Component | Version | Role |
|---|---|---|
| Android Gradle Plugin | 8.10.1 | Build |
| Kotlin | 2.1.21 | Language (+ `kotlin.plugin.compose`, `kotlin.plugin.serialization`) |
| Compose BOM | 2025.05.01 | Aligns all Compose artifact versions |
| `androidx.core:core-ktx` | 1.16.0 | Kotlin extensions, `NotificationCompat`, `ServiceCompat`, `FileProvider` |
| `androidx.lifecycle:lifecycle-runtime-ktx` / `-viewmodel-compose` | 2.9.0 | Lifecycle-aware coroutine scopes |
| `androidx.activity:activity-compose` | 1.10.1 | `ComponentActivity.setContent`, `rememberLauncherForActivityResult` |
| `androidx.compose.material3` | via BOM | Material 3 design system |
| `androidx.compose.material:material-icons-extended` | via BOM | Icon set |
| `androidx.navigation:navigation-compose` | 2.9.0 | `NavHost`, typed route args, back stack |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.8.1 | All model (de)serialization |
| `androidx.datastore:datastore-preferences` | 1.1.6 | All persistence |
| `junit:junit` | 4.13.2 | Unit tests (`testImplementation` only) |

Debug-only: `androidx.compose.ui:ui-tooling`.

**Notably absent**, and deliberately so: Room, Hilt/Dagger, Retrofit/OkHttp,
WorkManager, Coil/Glide, Firebase, any analytics SDK. The app has no
dependency-injection framework, no ORM, no HTTP client, and no background job
scheduler beyond `AlarmManager`.

### 2.2 Bundled assets

- `app/src/main/res/font/abyssinica_sil.ttf` — scripture-grade Ethiopic serif,
  used for **prayer text** (SIL OFL 1.1).
- `app/src/main/res/font/noto_sans_ethiopic.ttf` — used for **UI chrome**
  (OFL 1.1).
- `app/src/main/assets/content/*.json` — the prayer corpus (§6).

Fonts are bundled rather than relying on device fonts, because Ethiopic
coverage on Android OEM builds is inconsistent.

---

## 3. Architecture

### 3.1 Style

**Single module. Single Activity. Compose-only UI. Repository-backed
unidirectional state. No ViewModel layer.**

```
┌──────────────────────────────────────────────────────────────────┐
│  UI  (Jetpack Compose, Material 3)                               │
│  HomeScreen · ReadingScreen · PsalterScreen · SearchScreen       │
│  JourneyScreen · SettingsScreen · ModesScreen · ModeEditorScreen  │
│  ManageHours/ManageHabits/CustomizeHour · Intro/Tutorial · About │
│                                                                  │
│  reads:  Flow<T> ──collectAsState()──▶ State<T>                  │
│          suspend ──produceState{}────▶ State<T>   (one-shot)     │
│  writes: rememberCoroutineScope().launch { Repo.mutate(...) }    │
└───────────────▲──────────────────────────────┬───────────────────┘
                │ Flows                        │ suspend calls
┌───────────────┴──────────────────────────────▼───────────────────┐
│  Repositories  —  stateless Kotlin `object` singletons            │
│  ContentRepository · SettingsRepository · UserDataRepository      │
│  HighlightRepository · LayoutRepository(+PrayerLayout)            │
│  HoursRepository · ModesRepository · HabitsRepository             │
│                                                                   │
│  Pure functions live here too (prayer-day math, layout merges,        │
│  nextOccurrence, fold) so they are unit-testable without Android. │
└───────────────▲──────────────────────────────┬───────────────────┘
                │                              │
┌───────────────┴────────────┐  ┌──────────────▼───────────────────┐
│  assets/content/*.json     │  │  DataStore<Preferences> × 7      │
│  read-only, cached in RAM  │  │  each holds a JSON blob or keys  │
└────────────────────────────┘  └──────────────────────────────────┘

Reminders subsystem (Android framework, outside the Compose tree):
  AlarmManager.setAlarmClock
        └▶ AlarmReceiver ──▶ AlarmService (foreground, rings)
                          └▶ AlarmActivity (full-screen, over lock screen)
                          └▶ MarkDoneReceiver ("Done? → Yes" writes the habit record)
  StreakReminderScheduler ──▶ StreakReminderReceiver (nightly nudge, user-set time)
  SystemEventsReceiver (BOOT / MY_PACKAGE_REPLACED / TIME_SET / TZ_CHANGED)
        └▶ ReminderScheduler.rescheduleAll
```

### 3.2 Why no ViewModel

Every screen's durable state already lives in a DataStore-backed `Flow`.
`collectAsState()` gives configuration-change survival for free (DataStore
re-emits), and `rememberSaveable` covers the handful of ephemeral values that
matter (e.g. the search query). Adding ViewModels would introduce a layer whose
only job would be to forward the same flows. The trade-off is documented in
`docs/SDS.md` §1.3 as "small, boring architecture".

Consequences worth knowing:

- **One-shot loads use `produceState`**, which re-runs on key change but does
  *not* survive process death — acceptable because the source is a cheap
  in-memory cache.
- **Mutations are fire-and-forget** `scope.launch { ... }` calls. There is no
  loading/error state machine; DataStore writes are effectively infallible for
  this workload.
- **No screen-scoped caching.** `ContentRepository` caches globally instead.

### 3.3 The `object` singleton pattern

Every repository is a Kotlin `object` that takes `Context` as a *parameter* to
each function rather than holding one:

```kotlin
object SettingsRepository {
    fun theme(context: Context): Flow<ThemeChoice> = ...
    suspend fun setTheme(context: Context, choice: ThemeChoice) { ... }
}
```

This works because `preferencesDataStore(name = ...)` is a property delegate on
`Context` — the DataStore instance is owned by the application context, and the
`object` is a pure namespace. Benefits: no DI graph, no lifecycle, trivially
callable from a `BroadcastReceiver` or `Service` as well as from Compose.

### 3.4 State-reading idioms, precisely

| Idiom | Used for | Example |
|---|---|---|
| `Flow.collectAsState(initial = …)` | Anything persisted and reactive | `SettingsRepository.fontStep(context).collectAsState(initial = 1)` |
| `produceState(initialValue) { value = suspendCall() }` | One-shot async loads from assets | `produceState<Hour?>(null, hourId) { value = HoursRepository.hourById(context, hourId) }` |
| `remember(keys) { pureComputation() }` | Derived, cheap, synchronous | `remember(builtIn, config) { HoursRepository.merge(...) }` |
| `derivedStateOf` | Derived values read inside lambdas that must see the *current* value | `PsalterScreen`'s `shown` list (see §11.4 for why) |
| `rememberSaveable` | Ephemeral state that must survive rotation | `SearchScreen`'s query |
| `LaunchedEffect(keys)` | Side effects tied to state (scroll to anchor, debounce, deep-link consumption) | throughout |
| `DisposableEffect` | Window flags, persisting scroll position on exit | `ReadingScreen` keep-screen-on |

---

## 4. Source map

```
app/src/main/java/com/agpeya/app/
├── MainActivity.kt                      281  Activity, NavHost, deep links, locale
├── model/
│   ├── Models.kt                        114  Content + personalization models
│   └── Reminders.kt                      44  ReminderEntry, PrayerMode, ModesState
├── data/
│   ├── ContentRepository.kt              77  Asset loading + in-memory cache
│   ├── SettingsRepository.kt            142  Preferences (12 keys)
│   ├── UserDataRepository.kt            127  Bookmarks, recents, searches, scroll
│   ├── HighlightRepository.kt            43  verseKey → colour
│   ├── LayoutRepository.kt               86  Per-hour layout + PrayerLayout helpers
│   ├── HoursRepository.kt                97  Hour list config + merge()
│   ├── ModesRepository.kt               149  Reminder modes, scheduled-id bookkeeping
│   └── HabitsRepository.kt                   Habit state + prayer-day math (PrayerJourney.kt holds the period metric)
├── search/AmharicSearch.kt              122  Homophone folding + snippet search
├── reminders/
│   ├── ReminderScheduler.kt             144  nextOccurrence, rescheduleAll, snooze
│   ├── AlarmReceiver.kt                  66  Fire → chain next → start service
│   ├── AlarmService.kt                  264  Ringing FGS + notifications
│   ├── AlarmActivity.kt                 150  Full-screen lock-screen alarm
│   ├── MarkDoneReceiver.kt               43  "Done? → Yes" writes the habit record
│   ├── StreakReminderScheduler.kt        52  Nightly nudge scheduling (user-set time)
│   ├── StreakReminderReceiver.kt         81  Nudge firing + re-arm
│   └── SystemEventsReceiver.kt           44  Boot/update/time-change reschedule
└── ui/
    ├── theme/{Theme,Type}.kt          79+52  Colour schemes, typography
    ├── strings/Strings.kt               585  Strings interface + AmharicStrings + EnglishStrings
    ├── common/{AgpeyaBottomBar,EthiopianDate}.kt  93+47
    ├── home/HomeScreen.kt               438
    ├── reading/{ReadingScreen,SectionUi,GeezNumerals}.kt  477+244+10
    ├── psalter/PsalterScreen.kt         392
    ├── search/SearchScreen.kt           247
    ├── habits/{JourneyScreen,EthiopianYearHeatmap,HabitHeatmap,
    │            ManageHabitsScreen}.kt   332+225+118+189
    ├── hours/ManageHoursScreen.kt       243
    ├── customize/CustomizeScreens.kt    339
    ├── modes/{ModesScreen,ModeEditorScreen}.kt  244+380
    ├── intro/IntroScreen.kt             340  Intro + replayable TutorialScreen
    ├── bookmarks/BookmarksScreen.kt     151
    └── settings/{SettingsScreen,AboutScreen,BatteryHelpScreen}.kt  359+113+94
```

~9,000 lines of Kotlin total (main + test).

---

## 5. Domain model

All models are `@Serializable` data classes in `model/`. Two families: **content
models** (deserialized from assets, read-only) and **user models** (serialized
into DataStore, mutable).

### 5.1 Content models — `model/Models.kt`

```kotlin
@Serializable
data class Hour(
    val id: String,             // "morning", "midnight", "veil", or "custom_<uuid>"
    val orderIndex: Int,        // canonical liturgical order 0..7
    val name: String,           // Amharic display name, e.g. "ጸሎተ ነግህ"
    val transliteration: String,
    val timeHint: String,       // "ጠዋት", "እኩለ ሌሊት", …
    val sections: List<Section>,
)

@Serializable
data class Section(
    val id: String,             // globally unique; the bookmark/highlight key
    val orderIndex: Int = 0,
    val type: String,           // "psalm" | "gospel"
    val number: Int? = null,    // psalm number, for psalter entries only
    val title: String,          // "መዝሙር ፩", "ወንጌል"
    val subtitle: String? = null,   // psalm superscription, or gospel reference
    val reference: String? = null,  // machine reference, e.g. "Ps 118:1-8"
    val part: String? = null,       // Midnight watch label: "ክፍል ፩/፪/፫"
    val firstVerse: Int = 1,        // gospel passages start mid-chapter
    val verseHeaders: Map<Int, String> = emptyMap(), // Psalm 118 acrostic letters
    val verses: List<String>,
)

@Serializable data class Psalter(val psalms: List<Section>)
@Serializable data class Manifest(val contentVersion: Int, val hours: List<ManifestHour>)
@Serializable data class ManifestHour(val id: String, val file: String)
```

Design notes:

- **`Section` is the universal unit.** A psalm inside an hour, a psalm in the
  standalone Psalter, a gospel pericope, and a Psalm 118 stanza are all
  `Section`s. This is why `SectionView` can be shared verbatim between
  `ReadingScreen` and `PsalterScreen`.
- **`firstVerse` exists because gospel readings start mid-chapter** (e.g. John
  14:26). Verse numbering renders as `firstVerse + index`.
- **`verseHeaders` is keyed by absolute verse number**, not index — so a Psalm
  118 stanza starting at verse 81 still finds its acrostic letter.
- **`part`** drives the Midnight hour's three-watch section grouping; the reader
  emits a `PartHeader` whenever `part` changes from the previous section.

### 5.2 Personalization models

```kotlin
/** One user's customization of one hour. Empty = defaults. */
@Serializable
data class HourLayout(
    val order: List<String> = emptyList(),   // section ids, user order
    val hidden: Set<String> = emptySet(),    // section ids hidden
    val added: List<Int> = emptyList(),      // psalm numbers grafted into this hour
)

/** A user-created hour; its content is whatever psalms were added. */
@Serializable data class CustomHour(val id: String, val name: String)

/** Configuration of the hour LIST (as opposed to one hour's contents). */
@Serializable
data class HoursConfig(
    val custom: List<CustomHour> = emptyList(),
    val order: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val names: Map<String, String> = emptyMap(),  // overrides built-ins too
)

@Serializable data class Habit(val id: String, val name: String, val isBuiltIn: Boolean = false)

@Serializable
data class HabitsState(
    val custom: List<Habit> = emptyList(),
    val order: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val names: Map<String, String> = emptyMap(),
    val records: Map<String, Set<String>> = emptyMap(),  // "yyyy-MM-dd" → completed ids
)

@Serializable
data class Bookmark(                    // a snapshot, not a pointer
    val hourId: String, val hourName: String,
    val sectionId: String, val sectionIndex: Int,
    val title: String, val subtitle: String? = null,
)
```

**Why `Bookmark` denormalizes.** It stores `hourName`, `title` and `subtitle`
rather than resolving them at render time. The bookmarks list therefore renders
without touching `ContentRepository` at all — no async load, no flicker, no
scan. The cost is staleness if an hour is renamed after bookmarking.

**The `HoursConfig` / `HabitsState` symmetry** is intentional: both are
"list-of-things with order + hidden + name overrides + user-created extras", and
both expose the same five mutators (`add`, `rename`, `delete`, `setHidden`,
`setOrder`) plus one pure merge function.

### 5.3 Reminder models — `model/Reminders.kt`

```kotlin
@Serializable
data class ReminderEntry(
    val id: String,
    val hourId: String,
    val hour: Int, val minute: Int,
    val days: Set<Int> = ALL_DAYS,      // ISO: Monday=1 … Sunday=7
    val enabled: Boolean = false,       // OFF by default — nothing rings unasked
) { companion object { val ALL_DAYS: Set<Int> = (1..7).toSet() } }

@Serializable
data class PrayerMode(
    val id: String, val name: String,
    val isBuiltIn: Boolean = false,
    val entries: List<ReminderEntry> = emptyList(),
)

@Serializable
data class ModesState(val activeModeId: String, val modes: List<PrayerMode>) {
    val activeMode: PrayerMode? get() = modes.find { it.id == activeModeId }
}
```

**Exactly one mode is active** (`PLAN.md` decision D12). Only the active mode's
`enabled` entries produce alarms. Switching modes is therefore a single-value
change that the scheduler translates into a full cancel-and-rebuild.

The built-in mode (`agpeya_classic`, display name **ሰዓታት**) is generated in
code, not persisted as content:

```kotlin
private val DEFAULT_TIMES = listOf(
    "morning" to 6, "terce" to 9, "sext" to 12, "none" to 15,
    "vespers" to 18, "compline" to 21, "midnight" to 0, "veil" to 22,
)
```

Its entries have stable ids `builtin_<hourId>`, cannot be added to or deleted,
but can be re-timed and toggled — and reset via *Reset times*.

---

## 6. Content pipeline and bundled assets

### 6.1 The generator — `tools/extract_content.py`

Content is **never hand-edited**. It is generated from the
[80-weahadu](https://github.com/…) Amharic Bible dataset (expected as a sibling
checkout at `../80-weahadu/data/am/`) plus a hand-curated mapping file.

```
content/hour_mapping.json  +  ../80-weahadu/data/am/{28-psalms,55-matthew,57-luke,58-john}.json
                            │
                            ▼   python tools/extract_content.py
                app/src/main/assets/content/
                  ├── manifest.json      (contentVersion + hour → file)
                  ├── morning.json … veil.json   (8 hours)
                  └── psalms.json        (all 150 psalms)
```

`content/hour_mapping.json` declares, per hour, the psalm numbers (LXX/Ge'ez
numbering, which matches the source chapter numbers directly), the gospel
pericopes as `{book, chapter, fromVerse, toVerse}`, and — for Midnight — three
named `watches`. The Veil hour and Midnight watch 1 additionally declare
`psalm118Stanzas: {from, to}`.

### 6.2 The Psalm 118 acrostic problem

Psalm 118 (LXX numbering; 119 in Hebrew numbering) is a 176-verse acrostic in
22 stanzas of 8 verses, each headed by a Hebrew letter name. In the source data
the first letter is the *section title*, and every subsequent letter is **glued
onto the end of the previous stanza's last verse**. `psalm118_clean()` repairs
this:

```python
first = (ch["sections"][0].get("title") or "").strip().rstrip("።").strip()
letters = [first]
for st in range(1, 22):
    i = st * 8 - 1                       # last verse of stanza `st`, 0-based
    text, _, letter = verses[i].rpartition(" ")
    assert text and 1 <= len(letter) <= 5
    verses[i] = text                     # strip the letter off the verse
    letters.append(letter)
return verses, letters                   # letters[k] heads verses 8k+1 .. 8k+8
```

The letters are emitted as `verseHeaders` keyed by the stanza's first verse
number, so the reader renders them as centred stanza headings. The script is
liberally `assert`-guarded (176 verses expected, letter length 1–5 chars) so a
change in the upstream dataset fails loudly at generation time rather than
silently shipping corrupted scripture.

### 6.3 Generated ID scheme

| Kind | ID pattern | Example |
|---|---|---|
| Psalm within an hour | `<hourId>_ps<n>` | `morning_ps1` |
| Psalm within a Midnight watch | `<hourId>_<watchKey>_ps<n>` | `midnight_watch2_ps119` |
| Psalm 118 stanza | `<prefix>_ps118_s<st>` | `veil_ps118_s20` |
| Gospel | `<prefix>_gospel_<i>` | `terce_gospel_1` |
| Standalone psalm (Psalter) | `ps_<n>` | `ps_118` |

These IDs are the join keys for bookmarks, highlights, and layout overrides.
**They must never change** without a data migration.

### 6.4 What actually ships

| File | Sections | Verses | Text chars | Bytes |
|---|---:|---:|---:|---:|
| `morning.json` | 21 | 223 | 10,114 | 31 KB |
| `terce.json` | 15 | 166 | 7,555 | 24 KB |
| `sext.json` | 14 | 154 | 6,679 | 21 KB |
| `none.json` | 14 | 154 | 6,974 | 22 KB |
| `vespers.json` | 14 | 120 | 5,152 | 17 KB |
| `compline.json` | 14 | 126 | 5,670 | 18 KB |
| `midnight.json` | 56 | 481 | 19,648 | 65 KB |
| `veil.json` | 33 | 328 | 13,965 | 44 KB |
| **hours total** | **181** | **1,752** | **75,757** | ~242 KB |
| `psalms.json` | 150 | 2,444 | 104,001 | 307 KB |
| `manifest.json` | — | — | — | <1 KB |

Total content payload ≈ **560 KB** of UTF-8 JSON (written with
`separators=(",", ":")`, i.e. minified). Small enough to hold entirely in memory,
which is exactly what `ContentRepository` does.

### 6.5 Loading — `ContentRepository`

```kotlin
object ContentRepository {
    private val json = Json { ignoreUnknownKeys = true }
    @Volatile private var cache: List<Hour>? = null
    @Volatile private var psalterCache: List<Section>? = null

    suspend fun hours(context: Context): List<Hour> =
        cache ?: withContext(Dispatchers.IO) { load(context.applicationContext).also { cache = it } }
```

Behaviours worth noting:

- **Lazy, cached, `@Volatile`.** Load happens once per process, off the main
  thread. The double-check is benign-racy: two concurrent first calls may both
  parse, but both produce equivalent immutable lists.
- **`ignoreUnknownKeys = true`** so adding a field to the generator does not
  break older installs mid-rollout.
- **Per-file fault isolation.** A corrupt hour file is logged and skipped
  (`mapNotNull` over `runCatching`); a corrupt manifest degrades to an empty
  list. The app never crashes on content parse failure.
- **`applicationContext`** is used so a cached `AssetManager` can't leak an
  Activity.
- Hours are sorted by `orderIndex` after load.

`suggestedHourId(hourOfDay)` maps clock hour → hour id and drives the Home
"now" card:

```
4–7 morning · 8–10 terce · 11–13 sext · 14–16 none
17–19 vespers · 20–22 compline · else midnight
```

---

## 7. Persistence layer — the seven DataStores

Every repository owns exactly one `DataStore<Preferences>` file, declared as a
`Context` extension delegate:

| DataStore name | Repository | Contents |
|---|---|---|
| `settings` | `SettingsRepository` | 12 scalar preference keys |
| `prayer_modes` | `ModesRepository` | `modes_state_json` + `scheduled_entry_ids` (string set) |
| `hours` | `HoursRepository` | `hours_config_json` |
| `layouts` | `LayoutRepository` | `layouts_json` — `Map<hourId, HourLayout>` |
| `habits` | `HabitsRepository` | `habits_json` — the whole `HabitsState` |
| `user_data` | `UserDataRepository` | `bookmarks_json`, `recents_json`, `progress_json`, `recent_searches_json` |
| `highlights` | `HighlightRepository` | `highlights_json` — `Map<"sectionId:verse", colorKey>` |

### 7.1 The "JSON blob in a Preferences key" pattern

Only `SettingsRepository` uses Preferences as intended (typed scalar keys).
Every other repository stores **one JSON string** under one key, with a uniform
shape:

```kotlin
private fun decode(raw: String?): T =
    raw?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() } ?: DEFAULT

fun state(context: Context): Flow<T> = context.someDataStore.data.map { decode(it[KEY]) }

private suspend fun update(context: Context, transform: (T) -> T) {
    context.someDataStore.edit { prefs ->
        prefs[KEY] = json.encodeToString(T.serializer(), transform(decode(prefs[KEY])))
    }
}
```

Properties this gives you:

- **Atomic read-modify-write.** `DataStore.edit {}` is transactional, so
  concurrent toggles cannot interleave and lose an update.
- **Corruption tolerance.** `runCatching { … }.getOrNull() ?: DEFAULT` means a
  malformed blob resets that feature to defaults instead of crashing.
- **Schema evolution for free.** Every model field has a default value and
  `ignoreUnknownKeys = true`, so old blobs deserialize into new model versions
  and vice versa.

Trade-off accepted: **whole-document rewrite on every mutation.** Toggling one
habit re-serializes the entire `HabitsState` including all history. At realistic
sizes (a few years of daily records ≈ tens of KB) this is irrelevant; at 20 years
it would be worth revisiting.

### 7.2 `SettingsRepository` keys

| Key | Type | Default | Notes |
|---|---|---|---|
| `reading_mode` | enum name | `VERTICAL` | shared by hour reader and Psalter |
| `font_step` | Int | `1` | index into `[17,19,22,25,29]` sp |
| `theme` | enum name | `SYSTEM` | SYSTEM/LIGHT/DARK |
| `keep_screen_on` | Bool | `true` | window flag while reading |
| `language` | enum name | `SYSTEM` | SYSTEM resolves via `Locale.getDefault().language == "am"` |
| `alarm_alert` | enum name | `SOUND_VIBRATE` | SOUND_VIBRATE / SOUND_ONLY / VIBRATE_ONLY / SILENT |
| `alarm_sound` | enum name | `ALARM` | ALARM / RINGTONE / NOTIFICATION stream default |
| `onboarded` | Bool | `false` | gates the intro flow and the start destination |
| `profile_name` | String | `""` | local only; trimmed on write |
| `profile_christian_name` | String | `""` | baptismal name; preferred in greeting |
| `streak_reminder` | Bool | `true` | nightly nudge (key name is historical) |
| `streak_reminder_min` | Int | `1290` (21:30) | when the nudge fires, minutes into the day |

All enum reads are defensive: `runCatching { Enum.valueOf(raw ?: "") }.getOrDefault(…)`,
so a value written by a future version can't crash an older one.

Two **blocking** accessors exist specifically for the alarm service, which must
read preferences from a plain `Thread` inside `Service.onStartCommand`:

```kotlin
fun alarmAlertBlocking(context: Context): AlarmAlert =
    runCatching { runBlocking { alarmAlert(context).first() } }.getOrDefault(AlarmAlert.SOUND_VIBRATE)
```

### 7.3 `UserDataRepository` specifics

- **Bookmarks** are a `List<Bookmark>`; `toggleBookmark` keys on `sectionId`
  (globally unique), so the same psalm bookmarked from the Psalter and from an
  hour are distinct entries with different section ids.
- **Recents** cap at **4** hours, most-recent-first, deduped.
- **Recent searches** cap at **8**, case-insensitively deduped, trimmed.
- **Scroll memory** is `Map<hourId, sectionIndex>` — an *index*, not a pixel
  offset, so it survives font-size changes.

### 7.4 `LayoutRepository` + `PrayerLayout`

`LayoutRepository` is the I/O half; `PrayerLayout` is the pure half:

```kotlin
object PrayerLayout {
    fun ordered(sections: List<Section>, addedPsalms: List<Section>, layout: HourLayout): List<Section> {
        val all = sections + addedPsalms
        if (layout.order.isEmpty()) return all
        val byId = all.associateBy { it.id }
        return layout.order.mapNotNull { byId[it] } + all.filter { it.id !in layout.order }
    }
    fun visible(...) = ordered(...).filter { it.id !in layout.hidden }
}
```

The **"ordered ids first, then everything unmentioned"** merge is used
identically in `HoursRepository.merge` and `HabitsRepository.orderedHabitIds`.
It is the single most reused idea in the codebase, and it means a partial or
stale order list degrades gracefully instead of dropping items.

`removePsalm` cleans up all three places a psalm could be referenced
(`added`, `hidden`, `order`) so removing and re-adding starts clean.

---

## 8. Navigation and the single-Activity shell

### 8.1 `MainActivity`

`MainActivity` is `singleTask` with `windowSoftInputMode="adjustResize"`. It:

1. Calls `enableEdgeToEdge()`.
2. Consumes deep-link extras from the launching intent.
3. Sets content: theme (from `SettingsRepository.theme`) wrapping a
   `CompositionLocalProvider(LocalStrings provides stringsFor(language))`
   wrapping the `NavHost`.

Deep links are held in `mutableStateOf` **fields on the Activity**, not local
Compose state, so `onNewIntent` (which fires on a warm launch, because
`singleTask` reuses the instance) reaches the already-composed `NavHost`:

```kotlin
private val pendingDeepLinkHourId = mutableStateOf<String?>(null)
private val pendingOpenJourney = mutableStateOf(false)

private fun consumeDeepLink(intent: Intent?) {
    intent ?: return
    intent.getStringExtra(ReminderScheduler.EXTRA_HOUR_ID)?.let {
        pendingDeepLinkHourId.value = it
        intent.removeExtra(ReminderScheduler.EXTRA_HOUR_ID)   // strip so rotation can't replay it
    }
    …
}
```

The `removeExtra` calls matter: without them, a configuration change would
re-deliver the same intent and re-navigate.

### 8.2 Start-destination gating

```kotlin
val onboarded by SettingsRepository.onboarded(context).collectAsState(initial = null as Boolean?)
val ready = onboarded ?: return     // render nothing until we know
```

The initial value is `null`, deliberately tri-state. Returning early means the
`NavHost` is not composed at all for one or two frames, which avoids a visible
flash of Home before the intro appears. Every `LaunchedEffect` that navigates is
keyed on `ready`, because navigating before the graph exists crashes
Navigation-Compose.

### 8.3 Route table

| Route | Screen | Arguments |
|---|---|---|
| `intro` | `IntroScreen` | — |
| `home` | `HomeScreen` | — (bottom-nav tab) |
| `journey` | `JourneyScreen` | — (bottom-nav tab) |
| `settings` | `SettingsScreen` | — (bottom-nav tab) |
| `reading/{hourId}?section={section}` | `ReadingScreen` | `hourId: String`, `section: Int = -1` |
| `psalter?section={section}` | `PsalterScreen` | `section: Int = -1` |
| `search` | `SearchScreen` | — |
| `bookmarks` | `BookmarksScreen` | — |
| `habits` | `ManageHabitsScreen` | — |
| `customize` | `ManageHoursScreen` | — |
| `customize/{hourId}` | `CustomizeHourScreen` | `hourId: String` |
| `modes` | `ModesScreen` | — |
| `mode/{modeId}` | `ModeEditorScreen` | `modeId: String` |
| `battery` | `BatteryHelpScreen` | — |
| `gitsawePassage?psalm=&book=&chapter=&start=&end=&role=` | `GitsawePassageScreen` | the cited passage a ግጻዌ section opens on; `role` is the lectionary label |
| `seatat?sec={sec}` | `SeatatScreen` | ሰዓታት reader — the night-and-dawn office as one continuous scroll with a contents sheet; `sec` lands on a section (search results use it) |
| `intention/{habit}` | `SpecialHabitScreen` | `habit: "alms" \| "repentance"` |
| `tutorial` | `TutorialScreen` | — |
| `about` | `AboutScreen` | — |

### 8.4 Tab switching preserves per-tab state

```kotlin
private fun NavController.switchTab(tab: Tab) = navigate(tab.route) {
    popUpTo(Tab.HOME.route) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

Standard Compose bottom-nav semantics: one instance per tab, scroll and local
state saved and restored, no stack growth.

### 8.5 The hour-switch replace

From inside the reader, switching hours via the title dropdown does a *replace*,
not a push:

```kotlin
navController.navigate("reading/$id") {
    popUpTo("reading/{hourId}?section={section}") { inclusive = true }
}
```

`popUpTo` targets the **route pattern**, not the resolved route — so back from
the third hour you switched to still returns to Home, rather than walking back
through every hour you visited.

### 8.6 Psalter bookmarks and the pseudo-hour id

Psalter bookmarks are stored with `hourId = "psalter"` (`PSALTER_BOOKMARK_ID`),
which is never a real hour id. `BookmarksScreen` routes on it:

```kotlin
if (hourId == PSALTER_BOOKMARK_ID) navController.navigate("psalter?section=$index")
else navController.navigate("reading/$hourId?section=$index")
```

The stored `sectionIndex` for a psalter bookmark is `psalmNumber - 1` — an index
into the *whole* 150-psalm list, so it resolves correctly regardless of whether
the Psalter is currently showing today's daily division.

---

## 9. The reminders subsystem

This is the most Android-specific and most defensively written part of the app.
The goal: a prayer reminder must behave like a real alarm clock — exact, doze-proof,
audible, and visible over the lock screen — without asking for scary permissions.

### 9.1 Permissions declared

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED" />
```

`USE_EXACT_ALARM` (rather than `SCHEDULE_EXACT_ALARM`) is auto-granted for apps
whose core function is alarm-clock behaviour, so there is **no runtime
permission prompt** for exact alarms. The FGS type is `systemExempted`: apps
holding `USE_EXACT_ALARM` are the documented eligible case. An earlier build used
`mediaPlayback`, which was a policy mismatch — the manifest comment records this.

### 9.2 The chain pattern

`AlarmManager` has no native "repeat on these weekdays" concept that survives
doze accurately. Sinq therefore uses a **chain**: each alarm is a one-shot; when
it fires, the receiver immediately schedules that entry's *next* occurrence.

```
setAlarmClock(next)  ──fires──▶  AlarmReceiver
                                    ├─ is the entry still enabled in the active mode?
                                    │     yes → scheduleNext(entry)  ──▶ setAlarmClock(next+1)
                                    │           startForegroundService(AlarmService)
                                    └─    no  → drop it from scheduled ids, ring nothing
```

`setAlarmClock` is chosen over `setExactAndAllowWhileIdle` because it is the
only API that is genuinely doze-exempt, shows in the system's next-alarm slot,
and needs no special permission. Every call is wrapped:

```kotlin
try {
    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showPi), pi)
} catch (_: SecurityException) {
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
}
```

so an OEM or policy that refuses exact alarms degrades to an inexact one rather
than crashing.

### 9.3 `rescheduleAll` — derived-state rebuild

The alarm set is **derived state**. Any mutation to modes, entries, or the
visible-hours list requires a full rebuild:

```kotlin
suspend fun rescheduleAll(context: Context, hourNames: Map<String, String>) {
    // 1. cancel everything we previously scheduled (bookkeeping in DataStore)
    for (id in ModesRepository.scheduledIds(app)) {
        pendingIntent(app, id, null, null, create = false)?.let { alarmManager.cancel(it); it.cancel() }
    }
    // 2. schedule the active mode's enabled entries whose hour is visible
    val active = ModesRepository.current(app).activeMode ?: return setScheduledIds(app, emptySet())
    val scheduled = mutableSetOf<String>()
    for (entry in active.entries) {
        if (!entry.enabled) continue
        if (entry.hourId !in hourNames) continue        // hidden hours don't ring
        val at = nextOccurrence(entry, LocalDateTime.now()) ?: continue
        scheduleAt(app, alarmManager, entry, hourNames[entry.hourId]!!, at)
        scheduled += entry.id
    }
    ModesRepository.setScheduledIds(app, scheduled)
}
```

Two subtleties:

- **`scheduled_entry_ids` bookkeeping.** `AlarmManager` offers no way to
  enumerate your pending alarms, so the app persists the set of entry ids it has
  scheduled. Cancellation uses `FLAG_NO_CREATE` to look up the *existing*
  `PendingIntent` — creating one just to cancel it would be a no-op.
- **Hidden hours are silently excluded.** `hourNames` is built from
  `HoursRepository.visibleHours`, so hiding an hour in *Manage Hours* also mutes
  its reminder, without editing the mode.

### 9.4 `PendingIntent` identity

```kotlin
val intent = Intent(context, AlarmReceiver::class.java).apply {
    action = "com.agpeya.app.REMINDER"
    data = Uri.parse("agpeya://reminder/$entryId")     // distinguishes the intents
    putExtra(EXTRA_ENTRY_ID, entryId); …
}
PendingIntent.getBroadcast(context, entryId.hashCode(), intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
```

`PendingIntent` equality **ignores extras** — it compares action, data, type,
class, and categories. Encoding the entry id into the `data` URI (and into the
request code) is what keeps eight simultaneous prayer alarms distinct.
`FLAG_IMMUTABLE` is mandatory from API 31+.

### 9.5 `AlarmReceiver`

A `BroadcastReceiver` gets ~10 seconds on the main thread and must not do disk
I/O there. The receiver therefore uses `goAsync()` + a plain `Thread`:

```kotlin
val pending = goAsync()
Thread {
    try {
        val stillActive = runBlocking {
            val entry = ModesRepository.current(context).activeMode?.entries
                ?.find { it.id == entryId && it.enabled }
            if (entry != null) { ReminderScheduler.scheduleNext(context, entry, hourName); true }
            else { ModesRepository.setScheduledIds(context, scheduledIds(context) - entryId); false }
        }
        if (stillActive) ContextCompat.startForegroundService(context, alarmServiceIntent)
    } finally { pending.finish() }
}.start()
```

The **active-entry re-check** is what makes a stale alarm harmless: if the entry
was disabled, deleted, or its mode deactivated between scheduling and firing,
nothing rings and the bookkeeping self-heals.

Snoozed alarms bypass all of this — they carry `EXTRA_SNOOZE = true`, fire once
unconditionally, and do not chain.

### 9.6 `AlarmService` — the ringing foreground service

`AlarmService` is the component that makes a notification feel like an alarm.

**Startup sequence** (`onStartCommand`):

1. Handle `ACTION_DISMISS` / `ACTION_SNOOZE` early and return `START_NOT_STICKY`.
2. `ensureChannel()` — creates the `prayer_alarms` channel at
   `IMPORTANCE_HIGH` with **sound and vibration disabled on the channel**,
   because the service drives both itself on the alarm audio stream.
3. `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)`
   with a *provisional* notification built from **system-locale strings** — this
   must happen within a few seconds and must not block on a DataStore read.
4. `handler.postDelayed(autoStop, 60_000)` — a hard 60-second timeout.
5. A background `Thread` reads the alarm preferences and the user's language
   override, starts ringing, and (only if the user overrode the language)
   re-posts the notification with correct strings.

```kotlin
return START_NOT_STICKY   // a system restart would replay a null intent
                          // and spuriously ring "morning"
```

**Ringing**:

```kotlin
val uri = RingtoneManager.getActualDefaultRingtoneUri(this, type)
    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
player = MediaPlayer().apply {
    setAudioAttributes(AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)          // routes to the alarm stream,
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)  // ignores DND/ringer-silent
        .build())
    setDataSource(this@AlarmService, uri); isLooping = true; prepare(); start()
}
vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 800), repeat = 0))
```

`USAGE_ALARM` is the key detail — it is why the prayer alarm sounds even when the
phone is on silent, exactly like the clock app.

**The notification**:

- `CATEGORY_ALARM`, `PRIORITY_MAX`, `setOngoing(true)`, `setAutoCancel(false)`.
- `setFullScreenIntent(→ AlarmActivity, highPriority = true)` — **guarded**:
  ```kotlin
  val canFullScreen = Build.VERSION.SDK_INT < 34 ||
      getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
  ```
  Android 14 can revoke FSI permission; the code degrades to a heads-up
  notification whose `contentIntent` still opens the alarm screen.
- Two actions: **Snooze** and **Dismiss**, both `getService` PendingIntents back
  into this same service.

**The "Done?" follow-up.** When the alarm is dismissed *or* times out,
`postDonePrompt()` posts a quiet `PRIORITY_DEFAULT` notification asking whether
the prayer happened, with a **Yes** action wired to `MarkDoneReceiver`. Its id is
`7100 + hourId.hashCode() % 1000`, so different hours don't collide. Snoozing
does **not** post it (`activeHourId = null` first) — the alarm will ring again.

The prompt's wording is deliberately discreet, with no explicit prayer language,
matching the alarm itself.

**Notification/ID map**

| Purpose | Channel | Notification id |
|---|---|---|
| Ringing alarm | `prayer_alarms` (HIGH) | `7001` |
| "Done?" follow-up | `prayer_alarms` | `7100 + hash%1000` |
| Nightly nudge | `streak_reminders` (DEFAULT; channel id is historical) | `7200` |
| Nightly-nudge alarm request code | — | `9100` |

### 9.7 `AlarmActivity`

A `singleInstance`, `excludeFromRecents`, `taskAffinity=""` activity that shows
over the lock screen:

```kotlin
if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
else window.addFlags(FLAG_SHOW_WHEN_LOCKED or FLAG_TURN_SCREEN_ON or FLAG_KEEP_SCREEN_ON)
```

Three actions — **Open** (dismisses the service and deep-links `MainActivity` to
the hour), **Snooze** (+10 min), **Dismiss**. It reads the language as a
`collectAsState` flow rather than blocking, so the screen never stalls coming up.
Its palette is hard-coded (deep green ground, gold primary action) rather than
theme-derived, because it must look right regardless of the user's theme choice.

### 9.8 `MarkDoneReceiver`

Tapping **Yes** on the follow-up cancels the notification and idempotently marks
today's record:

```kotlin
HabitsRepository.markDone(context, LocalDate.now().toString(), HabitsRepository.hourHabitId(hourId))
// hourHabitId("morning") == "hour_morning"
```

This is the bridge between the reminders subsystem and the habits subsystem: an
answered alarm becomes a recorded prayer day without opening the app.

### 9.9 `SystemEventsReceiver`

Registered for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`,
`TIMEZONE_CHANGED` — the four events that invalidate every pending alarm. It
rebuilds the whole schedule and re-arms the nightly nudge, again on a `goAsync()`
background thread with `runCatching` around the content load.

### 9.10 The nightly nudge

Separate from prayer alarms and deliberately gentler:

- Fires at the **user-chosen time** (Settings row under the toggle; default
  21:30), via `setExactAndAllowWhileIdle` — inexact scheduling gets throttled to
  Doze maintenance windows once the app falls into a rare App-Standby bucket,
  and the nudge silently missed days at a time.
- `StreakReminderReceiver` **re-arms tomorrow first**, then checks the setting
  and posts the nudge. It fires every night while enabled — like the ግጻዌ nudge,
  and unlike its first design, which skipped any day that already had a log and
  so read as broken on exactly the attentive days. The wording never depends on
  history: a feast is named, a fasting day colours the invitation, and the
  default is "ሰርክ ደርሷል — ዕለቱን በጸሎት ዝጉ". BigTextStyle like the ግጻዌ one.
- Tapping it sets `EXTRA_OPEN_STREAK` (key name is historical) and lands you on
  the Journey tab.
- Uses its own `streak_reminders` channel at `IMPORTANCE_DEFAULT` so the user can
  silence it independently of prayer alarms.

`StreakReminderScheduler.sync(context, enabled)` is idempotent and called from
three places: app start (after onboarding), the Settings toggle, and
`SystemEventsReceiver`.

### 9.11 Notification-permission guidance

Three layers of help, because a reminder app that silently doesn't remind is
worthless:

1. **Just-in-time request.** `ModeEditorScreen.ensureNotificationPermission()`
   launches the `POST_NOTIFICATIONS` request at the moment the user enables a
   reminder (API 33+ only). If denied, an `AlertDialog` explains and offers a
   deep link to app notification settings.
2. **Persistent banner.** `ModesScreen` calls
   `NotificationManagerCompat.from(context).areNotificationsEnabled()` on every
   recomposition and shows an `errorContainer`-coloured banner when off.
3. **`BatteryHelpScreen`.** A short guide to un-restricting battery usage and
   OEM auto-start allow-lists, with a button to
   `ACTION_APPLICATION_DETAILS_SETTINGS`, reached from *Reminders not firing?* on
   the Modes screen.

Note that alarms still fire the full-screen intent even without
`POST_NOTIFICATIONS` — only the visible notification needs the grant.

---

## 10. Algorithms and pure logic

Everything in this section is `Context`-free and unit-tested.

### 10.1 Amharic homophone folding — `AmharicSearch`

Ge'ez script writes several letter families that sound identical in modern
Amharic, and users spell the same word different ways: ጸሎት/ፀሎት, ሰላም/ሠላም,
ሀ/ሐ/ኀ/ኸ, አ/ዐ. Ethiopic is a **syllabary**: each family occupies a contiguous
8-codepoint block, one per vowel *order* (ə, u, i, a, e, ɨ, o …).

The fold maps each family's block onto a canonical block **preserving the offset
within the block**, i.e. preserving the vowel:

```kotlin
fun foldChar(c: Char): Char = when (val cp = c.code) {
    in 0x1220..0x1227 -> (0x1230 + (cp - 0x1220)).toChar()  // ሠ-series → ሰ-series
    in 0x1210..0x1217 -> (0x1200 + (cp - 0x1210)).toChar()  // ሐ-series → ሀ-series
    in 0x1280..0x1287 -> (0x1200 + (cp - 0x1280)).toChar()  // ኀ-series → ሀ-series
    in 0x12B8..0x12BF -> (0x1200 + (cp - 0x12B8)).toChar()  // ኸ-series → ሀ-series
    in 0x12D0..0x12D7 -> (0x12A0 + (cp - 0x12D0)).toChar()  // ዐ-series → አ-series
    in 0x1340..0x1347 -> (0x1338 + (cp - 0x1340)).toChar()  // ፀ-series → ጸ-series
    else -> c
}
```

The critical property: **fold is 1:1 per character**, never inserting or
deleting. Therefore an index found in the folded haystack is *the same index* in
the original string, and the highlight lands on real characters:

```kotlin
val hit = fold(haystack).indexOf(needle)      // needle = fold(query)
return if (hit >= 0) snippet(haystack, hit, matchLen) else null
```

Search scope and behaviour:

- Runs over **all 8 hours' sections and all 150 psalms**, on `Dispatchers.Default`.
- Haystack per section = `title + subtitle + all verses`, space-joined.
- Minimum query length **2**; the UI debounces **180 ms**.
- Snippet radius **28 characters** either side, with `…` ellipses; offsets are
  computed *without trimming* so the highlight span stays exact.
- Results carry a `Source` (`HOUR` / `PSALTER`) that both labels the pill in the
  result row and decides the navigation target.

Complexity is `O(corpus)` per keystroke-after-debounce — roughly 180 K characters
of `indexOf` scanning, which is comfortably sub-frame on modern hardware. There
is **no inverted index**; adding one (or Room FTS) is the documented next step if
the corpus grows.

### 10.2 Ethiopian calendar conversion — `EthiopianDate`

The Ethiopian (Amete Mihret) calendar has 12 months of 30 days plus ጳጉሜን
(Pagumen) of 5 or 6. Conversion uses the standard **Beyene–Kudlek** Julian-day
algorithm, with epoch JDN **1723856** and a 1461-day four-year cycle:

```kotlin
fun from(date: LocalDate): EthiopianDate {
    val jdn = date.toEpochDay() + 2440588L                 // epoch day → JDN
    val r = ((jdn - ETHIOPIC_EPOCH) % 1461).toInt()
    val n = r % 365 + 365 * (r / 1460)                     // handles the 366th day
    val year  = 4 * ((jdn - ETHIOPIC_EPOCH) / 1461) + r / 365 - r / 1460
    return EthiopianDate(year.toInt(), n / 30 + 1, n % 30 + 1)
}

fun toGregorian(): LocalDate {
    val jdn = ETHIOPIC_EPOCH + 365L * year + year / 4 + 30L * (month - 1) + (day - 1)
    return LocalDate.ofEpochDay(jdn - 2440588L)
}
```

The `r / 1460` terms are what make Pagumen's 6th day fall in the right year.
Round-tripping is verified across 2019–2027 at a 17-day stride in
`EthiopianDateTest`, plus explicit anchors (the Ethiopian Millennium =
12 Sep 2007; Pagumen 6 = 11 Sep 2019).

Formatting:

```
formatEthiopian(date, s)      → "ረቡዕ፣ ሐምሌ 8 2018 ዓ.ም"
formatEthiopianShort(date, s) → "ሐምሌ 8"
```

Weekday and month names come from the `Strings` table, so the date renders in
the user's chosen UI language.

### 10.3 Ge'ez numerals

```kotlin
private val ONES = listOf("", "፩","፪","፫","፬","፭","፮","፯","፰","፱")
private val TENS = listOf("", "፲","፳","፴","፵","፶","፷","፸","፹","፺")

fun geezNumeral(n: Int): String = when {
    n >= 100 -> "፻" + if (n > 100) geezNumeral(n - 100) else ""
    else -> TENS[n / 10] + ONES[n % 10]
}
```

Covers 1–199 — enough for every verse number in the corpus (Psalm 118 has 176)
and every psalm number. Used for verse markers, psalm titles, and the pager's
"page X / Y" counter. The identical function exists in the Python generator so
titles baked into the JSON match what the app renders.

### 10.4 Next-occurrence scheduling

```kotlin
fun nextOccurrence(entry: ReminderEntry, now: LocalDateTime): LocalDateTime? {
    if (entry.days.isEmpty()) return null
    for (offset in 0..7L) {
        val date = now.toLocalDate().plusDays(offset)
        if (date.dayOfWeek.value !in entry.days) continue
        val candidate = date.atTime(entry.hour, entry.minute)
        if (candidate.isAfter(now)) return candidate
    }
    return null
}
```

Seven-day scan (0..7 inclusive, so the same weekday next week is reachable),
strictly-after semantics (an alarm at exactly *now* rolls to the next
occurrence), `null` for an empty day set. `java.time` handles DST transitions
implicitly via `atZone(ZoneId.systemDefault())` at the call site.

### 10.5 Prayer-day mathematics — `HabitsRepository` + `PrayerJourney`

Records are `Map<"yyyy-MM-dd", Set<habitId>>`. Habit ids are:
`hour_<hourId>` for prayer hours, `church` / `prostrate` / `bible` for built-in
habits, `custom_<uuid>` for user-created ones.

**There are no streaks.** The metric everywhere is *distinct days with prayer
activity inside the current period* — the running fast when one is underway
(`FastingCalendar.fastOn(today)`), the Ethiopian month otherwise. Because the
count is a set size and not a run length, a missed day changes nothing but that
day: nothing resets, nothing breaks, and returning after a gap is just today's
candle waiting.

`PrayerJourney.summarize(records, today)` is the single source of truth for the
hero line on both Home and Journey: `prayedToday` (the candle), `daysPrayed`,
the fast + `fastDay` when in one, and `returning` (history exists but nothing
today or yesterday → the quiet "ተመልሰዋል — ዛሬ ይጀምሩ" line, never a loss notice).
`HabitsRepository.habitDaysBetween` / `prayerDaysBetween` give the per-habit
month counts on the Journey screen. All of it derives from the same records the
streak-era app wrote — no migration, and old backups restore unchanged.

**Heatmap intensity** is *proportional*, not absolute:

```kotlin
fun level(count: Int, maxPossible: Int): Int {
    if (count <= 0) return 0
    val fraction = count.toFloat() / maxPossible.coerceAtLeast(1)
    return when { fraction >= 0.75f -> 4; fraction >= 0.50f -> 3; fraction >= 0.25f -> 2; else -> 1 }
}
```

`maxPossible = visibleHours.size + visibleHabits.size`. This is why the heatmap
stays meaningful whether the user tracks 4 things or 12: a fixed 1-2-3-4 scale
would show permanent dark-gold for someone tracking four items and permanent
pale for someone tracking twelve. `coerceAtLeast(1)` guards the divide-by-zero;
any activity always registers at least level 1.

---

## 11. UI layer — screens and components

### 11.1 `HomeScreen`

A single `LazyColumn` with, top to bottom:

1. **Header** — the ስንቅ wordmark, today's Ethiopian date, and a greeting using
   the baptismal name if set (`christianName.ifBlank { profileName }`); plus
   Search and Bookmarks icon buttons (these moved out of the bottom bar in
   commit `f5dfb0f`).
2. **`NowCard`** — the time-of-day suggested hour, rendered on
   `colorScheme.primary` with a radial gold glow clipped to the card shape.
3. **`TodayCard`** — `n/m` completed today, today's candle (lit when anything
   is recorded), the same `PrayerJourney` prayer-day line as the Journey hero,
   and a compact `HabitHeatmap`. Tapping it switches to the Journey tab.
4. **Continue reading** — a `LazyRow` of up to four recent-hour chips.
5. **Library row** — Psalter (active) plus ዘወትር ጸሎት and ውዳሴ ማርያም (disabled,
   "coming soon" captions).
6. **The hour list** — visible hours with their `timeHint`, divider-separated.

The Home card collapses all prayer hours into **one aggregate dot** (synthetic
id `"prayer"`, lit if any `hour_*` was done today); the per-hour breakdown lives
on the Journey screen. `maxPossible` passed to the heatmap deliberately excludes
that synthetic dot: `hours.size + (habitIds.size - 1)`.

### 11.2 `ReadingScreen`

The most intricate screen. It composes: an hour loaded by id, that hour's
`HourLayout`, the added psalms resolved from the Psalter, the global bookmark and
highlight maps, and four settings (font step, reading mode, keep-screen-on).

```kotlin
val sections by produceState(emptyList<Section>(), hour, layout) {
    val extras = layout.added.mapNotNull { ContentRepository.psalm(context, it) }
    value = PrayerLayout.visible(h.sections, extras, layout)
}
```

**Two readers over one anchor.** `VerticalReader` (a `LazyColumn`) and
`PagedReader` (a `HorizontalPager`) share a single `anchor: Int`:

```kotlin
LaunchedEffect(readingMode, anchor, sections.size) {
    val target = anchor.coerceIn(0, sections.size - 1)
    if (readingMode == ReadingMode.VERTICAL) listState.scrollToItem(target)
    else pagerState.scrollToPage(target)
}
```

Keying on `readingMode` is essential: calling `scrollToPage` on a pager that
hasn't been composed yet suspends forever. Capturing the anchor *before* the mode
flips, and scrolling *after* the new reader composes, is what makes the toggle
feel like the page stayed put.

**Index translation.** A search hit or bookmark carries an index into the hour's
*full, unmodified* section list. Since the user may have reordered or hidden
sections, the effect resolves it by id:

```kotlin
anchor = if (initialSectionIndex >= 0) {
    val targetId = h.sections.getOrNull(initialSectionIndex)?.id
    sections.indexOfFirst { it.id == targetId }.takeIf { it >= 0 } ?: 0
} else UserDataRepository.savedPosition(context, h.id).coerceIn(0, sections.size - 1)
```

**Other behaviours**

- Collapsing app bar (`enterAlwaysScrollBehavior`) is attached **only in vertical
  mode** — in paged mode the nested-scroll connection swallows the pager's swipes.
- The title is a dropdown listing all visible hours, so you can switch prayer
  without going back (§8.5).
- `A−` / `A+` step the shared font size; `FONT_STEPS_SP = [17, 19, 22, 25, 29]`.
- A `ModalBottomSheet` table of contents, with `PartHeader` grouping for
  Midnight's watches.
- `DisposableEffect(keepScreenOn)` adds/clears `FLAG_KEEP_SCREEN_ON` on the
  window found by walking up the `ContextWrapper` chain; it always clears on
  dispose.
- `DisposableEffect(hourId, readingMode)` persists the first-visible index on
  exit.
- Paged mode shows `፩ / ፳፩`-style Ge'ez pagination.

### 11.3 `SectionUi` — the shared renderer

`SectionView`, `VerseText`, `HighlightBar`, and `highlightColor` live here and
are used by **both** the hour reader and the Psalter.

```kotlin
section.verses.forEachIndexed { i, verse ->
    val verseNumber = section.firstVerse + i
    section.verseHeaders[verseNumber]?.let { header -> /* centred stanza heading */ }
    val verseKey = HighlightRepository.verseKey(section.id, verseNumber)   // "id:n"
    val annotated = remember(verse, verseNumber, markerColor, bodyFontSp) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = markerColor,
                                fontSize = (bodyFontSp * 0.58f).sp,
                                baselineShift = BaselineShift.Superscript)) {
                append(geezNumeral(verseNumber))
            }
            append(" "); append(verse)
        }
    }
    Text(annotated, style, modifier = Modifier
        .clip(RoundedCornerShape(6.dp)).background(highlightColor(highlights[verseKey]))
        .pointerInput(verseKey) { detectTapGestures { onVerseTap(verseKey) } }
        .padding(horizontal = 6.dp, vertical = 3.dp))
}
```

Deliberate choices:

- **One `Text` per verse**, not one per section — each verse needs its own
  highlight background and its own tap target.
- **No `SelectionContainer`** — it would swallow the verse taps. Text selection
  is traded away for tap-to-highlight.
- Line height is `1.85 ×` font size (Ethiopic needs generous leading).
- Highlight tints are `0x55`-alpha so they read correctly on both the ivory and
  the deep-green ground; the palette swatches use the same hues at full alpha.
- Tapping a verse raises `HighlightBar`, a bottom sheet-ish `Surface` with four
  colour circles, a "clear" action, and a close action.

### 11.4 `PsalterScreen`

The standalone 150-psalm reader. Shares `SectionView`, the font steps, and the
reading-mode toggle with `ReadingScreen`, but adds:

**Daily divisions.** The traditional weekday division of the Psalter:

```
Mon 1–30 · Tue 31–60 · Wed 61–80 · Thu 81–110 · Fri 111–130 · Sat 131–150 · Sun — (undefined)
```

Sunday shows a "coming soon" placeholder. A toggle switches between *today's*
psalms and the whole Psalter; opening from a bookmark or search hit forces whole-
Psalter mode so the target psalm exists.

**Why `derivedStateOf` and not `remember`:**

```kotlin
val shown by remember(range) {
    derivedStateOf {
        if (!daily) psalms else range?.let { r -> psalms.filter { it.number in r } } ?: emptyList()
    }
}
```

`pageCount` and `key` lambdas passed to `HorizontalPager` are captured once and
read later. With a plain recomputed local, the pager could hold one generation of
the list while the key lambda closed over another — mismatched generations while
the psalms load asynchronously. `derivedStateOf` guarantees both read the current
value at call time. The pager body additionally uses `shown.getOrNull(page)`
because a composed page can outlive the list for one frame when the daily toggle
shrinks it.

**Contents sheet with search** — filters by psalm *number* if the query contains
digits, otherwise by folded title/subtitle text via `AmharicSearch.fold`.

### 11.5 `SearchScreen`

Debounced (180 ms), min 2 characters. Three states: recent searches (idle),
"no results", and the result list. Each result row shows a `SourceTag` pill
(hour name or "Psalter") and a snippet with the matched span rendered bold in
the gold secondary colour via `buildAnnotatedString` + the exact offsets returned
by the search. Opening a result records the query in recent searches.

### 11.6 `JourneyScreen` (ጉዞ)

- Hero card: today's candle plus the period's prayer-day line
  (`journeyLine(PrayerJourney.summarize(...))`); the kicker is the lit/waiting
  state, or "ተመልሰዋል — ዛሬ ይጀምሩ" after one or more whole missed days.
- **Collapsible ጸሎት group** — a header row showing `donePrayers/totalHours`,
  expanding into one `CheckRow` per visible hour. Other habits are flat rows.
  Tapping a row toggles today's record.
- `EthiopianYearHeatmap` — the main historical view — with a tapped-day readout
  (`"ረቡዕ፣ ሐምሌ 8 2018 ዓ.ም · 3 ልማዶች · ዐቢይ ጾም"`, the fast named when there is one).
- Per-item rows: distinct days kept this Ethiopian month (`በዚህ ወር n ቀን`),
  including a prayer aggregate row. No current/best runs anywhere.
- Link to `ManageHabitsScreen`.

### 11.7 `EthiopianYearHeatmap`

A GitHub-contributions-style grid, but laid out on the **Ethiopian year**:

- Columns are Monday-aligned weeks spanning መስከረም 1 → end of ጳጉሜን of the
  selected EC year, computed by converting both ends through `EthiopianDate`.
- Month labels above are computed as **spans**: consecutive columns are grouped
  by the EC month of their first in-range day, and each label is given a width of
  `COL × columns`.
- Weekday labels (Mon–Sun) sit fixed to the left of the horizontally scrolling
  grid.
- Cell colour: transparent outside the year, 35 %-alpha placeholder for future
  days (so the year's full shape is always visible), otherwise the gold ramp
  `0.30 / 0.50 / 0.75 / 1.0` by `HabitsRepository.level`.
- On first composition of the current year it scrolls proportionally so today's
  column is in view.
- A year switcher below clamps to `[APP_EPOCH_EC .. currentEc]`, where
  `APP_EPOCH_EC = EthiopianDate(2018, 11, 1)` — Hamle 1, 2018 EC, the app's first
  possible day of data.

`HabitHeatmap` is the simpler Gregorian-week variant used on Home (14 weeks, no
legend, 9 dp cells).

### 11.8 Customization screens

| Screen | What it edits | Repository |
|---|---|---|
| `ManageHoursScreen` | The hour *list*: hide/show, rename (built-ins too), reorder, create custom, delete custom | `HoursRepository` |
| `CustomizeHourScreen` | One hour's *contents*: hide/show sections, reorder, add/remove psalms, reset | `LayoutRepository` |
| `ManageHabitsScreen` | The habit list: hide/show, rename, reorder, create, delete custom | `HabitsRepository` |

All three use the same row grammar — a visibility eye, the name, up/down arrows,
optional rename and delete — and the same `AlertDialog` name-entry pattern.
Reordering is explicit arrow buttons rather than drag-and-drop: simpler,
accessible, and it works inside a `LazyColumn` without a reorder library.

`CustomizeHourScreen`'s psalm picker is a `ModalBottomSheet` listing all 150
psalms filtered by a numeric text field.

### 11.9 `IntroScreen` / `TutorialScreen`

A three-stage state machine:

```
PAGES (2 intro pages + a name form)  ──▶  ASK ("want a quick tour?")  ──▶  TUTORIAL (3 feature pages)
     └── skip ─────────────────────────────────── onDone ──────────────────────┘
```

- Intro pages: the ስንቅ wordmark hero, and an "everything is offline" page.
- The name form collects `profile_name` and `christian_name`, saved on advance
  or skip.
- Tutorial pages: **Reminders**, **Journey**, **Psalter** — the three features
  that are not self-evident.
- `TutorialScreen` re-uses `tutorialPages()` and the same `TourScaffold` so the
  tour is **replayable from Settings** without the name form.
- `onDone` is guaranteed to fire exactly once, on completion or any skip; it
  writes `onboarded = true` and pops the intro off the back stack.

### 11.10 `SettingsScreen`

Theme (3-way segmented), language (3-way segmented), keep-screen-on switch,
nightly-reminder switch (which calls `StreakReminderScheduler.sync` inline), alarm
alert mode + alarm sound dropdowns (the sound row disables itself when the alert
mode is vibrate-only or silent), the local profile rows, and links to Manage
Hours, Reminder Modes, Tutorial, and About. ምጽዋት and ንስሐ are doors, not rows:
each opens its own `SpecialHabitScreen` (`intention/{habit}`) holding the
toggle, cadence, time, and the next due day — Settings stays a list, the
editing lives on the page.

ግጻዌ sections open the same way: a reading row navigates to
`GitsawePassageScreen`, which shows only the cited verses (an open-ended
citation — start with no end — reads through to the chapter's last verse) in
the reading face, with two `NavRow` doors out: the book, and the chapter that
holds the passage (`GitsaweLinks.bookRoute` / `chapterRoute`, derived from the
same `ReadingTarget` so section, book and chapter can never disagree).

### 11.11 `AgpeyaBottomBar`

A floating pill (`RoundedCornerShape(24.dp)`, surface colour, 1 dp variant
border) with four tabs — Home, Journey, Library, Settings — each taking `weight(1f)` so
labels never wrap on narrow screens (`maxLines = 1, softWrap = false,
overflow = Ellipsis`). It applies `navigationBarsPadding()` itself. Selected
state is gold; unselected is muted.

---

## 12. Design system: theme, typography, identity

### 12.1 Colour

A hand-authored **green & gold** identity — no Material You dynamic colour, on
purpose, because the liturgical palette is part of the app's character.

| Role | Light | Dark |
|---|---|---|
| primary | `#0E3B31` GreenDeep | `#124136` GreenCard |
| secondary (gold) | `#A67F2E` GoldLight | `#E4BC5A` GoldDark |
| background | `#EFEDE2` IvoryGround | `#0B3129` GreenGroundDark |
| surface | `#F7F5EB` | `#10382F` |
| surfaceVariant | `#E3E0D1` | `#1B4A3E` |
| onBackground | `#1D2B24` InkLight | `#F2EDDE` Ivory |
| onSurfaceVariant | `#5D6B60` | `#9DBBAD` |

The semantic rule in the codebase: **gold (`colorScheme.secondary`) is the voice
of what matters** — verse numbers, day counts, section titles, selected tabs,
search-match highlighting. Deep green carries the ground; ivory is the ink.

`AgpeyaTheme(themeChoice)` resolves `SYSTEM` via `isSystemInDarkTheme()`. The
Android-level theme (`values/themes.xml`) hard-codes
`android:windowBackground = #0B3129` so the pre-Compose launch window matches the
dark ground.

### 12.2 Typography

```kotlin
val Ethiopic   = FontFamily(Font(R.font.noto_sans_ethiopic))   // UI chrome
val Abyssinica = FontFamily(Font(R.font.abyssinica_sil))       // prayer text
```

The Material 3 `Typography` overrides six styles with Ethiopic and ~1.7×
line-height ratios (`bodyLarge` 18 sp / 31 sp). Prayer text explicitly overrides
`fontFamily = Abyssinica` at the call site and computes line height as
`1.85 × fontSize`.

### 12.3 Localization approach

Localization is **not** `res/values-*/strings.xml`. It is a Kotlin `interface`
with two implementing `object`s, provided through a `staticCompositionLocalOf`:

```kotlin
interface Strings { val back: String; …; fun daysUnit(n: Int): String; … }
object AmharicStrings : Strings { … }
object EnglishStrings : Strings { … }
val LocalStrings = staticCompositionLocalOf<Strings> { AmharicStrings }
```

Why this instead of resources:

- **The compiler enforces completeness.** Adding a string to the interface breaks
  the build until both languages define it. XML resources fail silently at
  runtime.
- **Parameterized strings are functions**, so plurals and formatting are ordinary
  Kotlin (`fun daysUnit(n: Int) = "$n ቀናት"`) rather than `%d` placeholders.
- **In-app language override works instantly** — changing `language` re-emits a
  flow and recomposes the whole tree, with no Activity recreation and no
  `AppCompatDelegate` locale plumbing.

Non-Compose components (`AlarmService`, `StreakReminderReceiver`) call the
top-level `stringsFor(language)` directly. `res/values/strings.xml` retains
exactly one entry: `app_name = ስንቅ`.

Prayer content is **never** in `Strings` — it stays Amharic, as data.

---

## 13. Build, signing, shrinking, release

### 13.1 Build configuration

```kotlin
namespace  = "com.agpeya.app"      // applicationId unchanged despite the Sinq rename
compileSdk = 36 ; minSdk = 26 ; targetSdk = 36
versionCode = 11 ; versionName = "0.2.6"
sourceCompatibility/targetCompatibility = JavaVersion.VERSION_11 ; jvmTarget = "11"
buildFeatures { compose = true }
```

Versioning policy, from the build file's own comment:
`0.MINOR.PATCH` — PATCH for fixes, MINOR for features, `1.0.0` reserved for the
first public Play release, and **`versionCode` increments by 1 on every update,
no exceptions**.

### 13.2 Signing

Two sources, one resolution function, and a safe fallback:

```kotlin
fun signingValue(property: String, envVar: String): String? =
    keystoreProperties.getProperty(property) ?: System.getenv(envVar)
…
signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
```

- Locally: `keystore.properties` at the repo root (gitignored) pointing at
  `sinq-upload.jks` (also gitignored).
- On CI: `SINQ_KEYSTORE_FILE`, `SINQ_KEYSTORE_PASSWORD`, `SINQ_KEY_ALIAS`,
  `SINQ_KEY_PASSWORD`.
- With neither present, release builds are produced **unsigned** rather than
  failing the build — so a contributor without the keystore can still run
  `assembleRelease`.

### 13.3 R8 / ProGuard

`isMinifyEnabled = true` and `isShrinkResources = true` for release, with
`proguard-android-optimize.txt` plus `app/proguard-rules.pro`. Two keep-rule
groups, both learned from real release crashes:

**kotlinx.serialization** — the entire content pipeline is `@Serializable`
models. The library ships consumer rules, but the app keeps its serializers
explicitly, because losing them means a crash on first launch while parsing
assets:

```proguard
-keepclassmembers @kotlinx.serialization.Serializable class com.agpeya.app.** { *** Companion; }
-if @kotlinx.serialization.Serializable class com.agpeya.app.**
-keepclasseswithmembers class com.agpeya.app.<1>$Companion { kotlinx.serialization.KSerializer serializer(...); }
-keepclassmembers class com.agpeya.app.**$$serializer { *** INSTANCE; }
```

**DataStore's bundled protobuf-lite** (fixed in commit `e7bda6a`) — DataStore
stores string sets through a repackaged protobuf whose generated messages resolve
fields **reflectively by their original names** (e.g. `StringSet.strings_`). R8
field renaming makes that lookup fail at runtime with
`Field strings_ … not found`:

```proguard
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { <fields>; }
```

### 13.4 Release workflow

`.github/workflows/release.yml`, triggered by pushing a `v*` tag:

1. `actions/checkout@v4`, `actions/setup-java@v4` (Temurin 17, Gradle cache).
2. Decode `secrets.KEYSTORE_BASE64` into `sinq-upload.jks`.
3. `./gradlew assembleRelease bundleRelease --no-daemon` with the four `SINQ_*`
   env vars set from secrets.
4. Rename outputs to `Sinq-<tag>.apk` / `.aab`.
5. `softprops/action-gh-release@v2` with `generate_release_notes: true`.

---

## 14. Testing

Four JVM unit-test classes, JUnit 4, no Android instrumentation, no Robolectric —
all of them exercise pure functions that were deliberately kept `Context`-free.

| Class | Tests | Covers |
|---|---:|---|
| `ReminderSchedulerTest` | 8 | `nextOccurrence`: today vs tomorrow, exact-now roll-over, weekday subsets (Wed/Fri fasting), week wrap Sat→Mon, same-weekday-after-time waits a full week, empty day set → null, midnight (hour 0) |
| `PrayerJourneyTest` | 20 | Distinct-day counting (days not events, gap survival, inclusive range, malformed keys), Ethiopian-month and fast periods with their boundaries, the candle/return states, per-habit and prayer-aggregate day counts, proportional `level()` including the degenerate `maxPossible = 0`, `dayCount` |
| `EthiopianDateTest` | 5 | Ethiopian Millennium anchor, post-leap new year, mid-year conversion, `toGregorian` inverting `from` over 2019–2027 at a 17-day stride, Pagumen 5/6-day handling |
| `AmharicSearchTest` | 6 | ሰ/ሠ, ጸ/ፀ, ሀ/ሐ/ኀ/ኸ, አ/ዐ families fold together; vowel order is preserved within a family; non-homophone letters are unchanged |

Run with:

```bash
./gradlew test
```

**Not covered by automated tests:** Compose UI, DataStore round-trips, the alarm
subsystem end-to-end, asset parsing, and the content generator. These are
verified manually; the reminders path in particular depends on device- and
OEM-specific behaviour that a JVM test cannot model.

---

## 15. Known gaps, dead code, and drift

Recorded honestly so the next change starts from the truth.

**Declared but not implemented**

- **`CounterState`** (`Models.kt`) — the prayer-rope (መቁጠሪያ) counter model
  exists with `total` and `target = 41`, but there is **no repository, no UI, and
  no reference to it anywhere else in the codebase**. The README lists Mequteria
  as a feature; it is not shipped.
- **ዘወትር ጸሎት** and **ውዳሴ ማርያም** appear on Home as disabled "coming soon" cards.
- **Sunday's Psalter division** is undefined (`dailyRange(SUNDAY) == null`); the
  daily view shows a "coming soon" placeholder that day.

**Dead code**

- `CustomizeScreens.kt` defines **`CustomizeHoursScreen`**, which nothing calls —
  it was superseded by `ManageHoursScreen`. Only `CustomizeHourScreen` (singular)
  from that file is routed.
- Unused `Strings` members left over from earlier layouts, with no call sites:
  `tabBookmarks`, `customizePrayers`, `remindersNotFiring`, `getStarted`,
  `alarmPrayerTime`, `openPrayer`, `reminderReached`, `batteryHelp`, and
  `highlight`. (`tabSearch` *is* still used, as an icon content description.)
- One hard-coded Amharic string survives in `BookmarksScreen`
  (`contentDescription = "አስወግድ"`), bypassing the `Strings` table.

**Documentation drift**

- The README badge says `version-0.2.1`; the build is `0.2.6`.
- `docs/SDS.md` predates the unified search, the streak nudge, and the tutorial
  replay; this document supersedes it where they disagree.
- `docs/SRS.md`/`SDS.md` refer to the app as "ጸሎት (Tselot)"; it is now **Sinq
  (ስንቅ)**, though `applicationId` remains `com.agpeya.app`.

**Content provenance**

- `content/hour_mapping.json` carries a comment stating the psalm/gospel lists
  were fetched from agpeya.org on 2026-06-12 and **must be confirmed by a fluent
  reviewer before release**. See `docs/CONTENT_RIGHTS.md` for the licensing
  position on the 80-weahadu text.

**Scaling considerations (not yet problems)**

- Search is a linear scan over ~180 K characters per query; a Room FTS index is
  the documented escape hatch.
- Every habit toggle rewrites the full history blob.
- `HabitsState.records` grows unbounded; there is no archival or pruning.

---

## 16. Concept glossary

| Term | Meaning in this codebase |
|---|---|
| **Agpeya / ሰዓታት** | The Coptic–Ethiopian Book of Hours; the seven (here eight) canonical prayer hours. |
| **Hour** | One canonical prayer time and its content. Ids: `morning`, `terce`, `sext`, `none`, `vespers`, `compline`, `midnight`, `veil`. |
| **Veil (ሌሊት 9 ሰዓት)** | The eighth hour; ships **hidden by default** (`HoursRepository.DEFAULT`), opt-in via Manage Hours. |
| **Watch (ክፍል)** | The Midnight hour's three divisions, carried on `Section.part` and rendered as `PartHeader`s. |
| **Section** | The atomic content unit — one psalm, one gospel pericope, or one Psalm 118 stanza. |
| **Layout** | A user's per-hour overlay: order + hidden + added psalms. Never touches the text. |
| **Mode (PrayerMode)** | A named reminder schedule. Exactly one is active; only its enabled entries ring. |
| **Chain pattern** | One-shot alarms that reschedule themselves on fire (§9.2). |
| **Folding** | Mapping Ethiopic homophone letter families to a canonical family for search (§10.1). |
| **EC / ዓ.ም** | Ethiopian Calendar (Amete Mihret); 13 months, ~7–8 years behind the Gregorian year. |
| **Pagumen (ጳጉሜን)** | The 13th Ethiopian month, 5 days (6 in a leap year). |
| **Ge'ez numerals** | ፩ ፪ ፫ … ፻ — used for verse numbers, psalm titles, and pagination. |
| **Habit id** | `hour_<hourId>` for prayer hours, `church`/`prostrate`/`bible` built-in, `custom_<uuid>` user-created. |
| **`maxPossible`** | Total trackable items today; the denominator of the heatmap intensity scale. |
| **Pseudo hour id** | `"psalter"` — the `hourId` under which Psalter bookmarks are stored so they route to the Psalter screen. |

---

*Generated from the source at commit `b390dbb`, version 0.2.6 (versionCode 11).*
