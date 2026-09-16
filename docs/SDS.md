# Sinq software design specification

Reviewed **2026-09-16** for **2.3.2 / 72**. Companion to [SRS](SRS.md), [implementation reference](IMPLEMENTATION.md) and [project status](PROJECT_STATUS.md).

## Architecture

A single Android app module hosts a single-Activity Compose navigation shell. Screens read repository flows and use suspend operations for mutations. UI state is primarily local Compose state; there is no dedicated application ViewModel layer.

```text
MainActivity / Compose screens
  ├── Content repositories ── bundled JSON assets + decoded caches
  ├── User repositories ───── Preferences DataStore
  ├── JournalRepository ───── Room / SQLite (journal, schema v1)
  ├── Reminder schedulers ─── AlarmManager / receivers / notifications
  ├── UpdateRepository ────── GitHub metadata (UPDATE_NOTICE builds)
  └── CatenaScreen ────────── external commentary WebView
```

The application ID is `com.sinq.app`; source remains in `com.agpeya.app`. Minimum SDK 23, compile/target SDK 36, desugaring and JVM target 11 are configured in the app build. Dependency versions are recorded in [implementation](IMPLEMENTATION.md).

## Domain and storage

| Domain | Design |
|---|---|
| Hours/Psalter | Serializable sections and verses; permanent IDs; generated from source mappings |
| Scripture | Source-edition catalog and book/chapter metadata; references resolved into reader routes |
| Gitsawe/calendar | Fixed-date records combined with computus and fixed-anchored Sunday selection |
| Synaxarium/Wudase | Edition/day-specific content repositories and readers |
| Books/Mahlet | Lazy loaded book/order data; source editions and optional alternatives retained |
| User overlays | Bookmarks, highlights, custom hours/layouts, schedules, habits and settings in DataStore |
| Reading plan | Bundled reading definitions plus persisted user progress |
| Journal | Room rows browsed by date/month and passage anchor; no full-text index |
| Backup | Versioned JSON selection with preview and repository-specific restore/merge behavior |

The journal uses Room because entries grow over time and need partial reads/writes. Bounded preferences and user overlays retain the JSON-in-DataStore pattern. The journal passphrase gates the UI; neither the database nor the current backup writer gains encryption from it. Confession drafts are excluded from backup and removed by the confession-completion operation.

## Navigation and interface

Top-level tabs: Home, Journey, Library, Settings. Search, marks, journal, individual readers, reading-plan tools and settings details are pushed destinations in `MainActivity.kt`. Existing reader routes remain compatible with saved bookmarks and deep links.

Shared rendering, verse selection, references and sharing live under `ui/reading` and `ui/common`. Reader preferences are shared across surfaces. `Tokens.kt`, `Theme.kt`, `Type.kt` and `ReadingTypography.kt` define spacing, color, typography and motion; [design system](DESIGN_SYSTEM.md) is the implementation guide.

## Calendar and reminder design

`EthiopianDate`, `BahreHasab`, `SundayCycleCalendar` and `FastingCalendar` separate date conversion, movable anchors, fixed-anchored windows and fast presentation. `GitsaweRepository` selects and combines the applicable collections. Source-derived rubrics still require [liturgical review](LITURGICAL_REVIEW.md).

Prayer modes own their entries and active-mode selection. Schedulers derive alarms from saved state, and receivers handle delivery and relevant system changes. Additional nudges have their own scheduling paths. All notification IDs are assigned in `NotificationIds.kt`; fixed notifications and hashed families occupy disjoint ranges after the 2.3.2 fix.

Two widget providers use the same content/calendar infrastructure: Gitsawe changes with the day; memento mori does not need a daily content refresh.

## Content pipeline

`source inputs → tools/*.py → app/src/main/assets/content/ → repositories → readers`

Keep source snapshots auditable and generated assets reproducible. Some generators depend on external sibling repositories, while a normal Android build uses checked-in outputs. Psalm acrostic handling and scan corrections belong in explicit transformations, not silent edits to generated sacred text. Stable IDs are checked against `tools/section-ids.json`.

## Failure handling and security boundaries

- Empty or malformed content should use shared empty/error states; missing source text must not be invented.
- Import rejects unsupported or oversized backups and previews the effect of restoring selected records.
- Optional online failures must not block offline prayer reading.
- OS notification restrictions require device-level verification and user-facing guidance.
- No ad/analytics SDK or app account is configured. The `INTERNET` permission supports update metadata and external commentary; Android automatic backup is disabled.
- Content licenses are distinct from the source-code license. [NOTICE](../NOTICE) and [rights](CONTENT_RIGHTS.md) preserve the record.

## Verification and delivery

Content validation and JVM tests cover structural and algorithmic contracts; lint and assembly verify Android build integration. Manual CI is separate from tag-triggered release gates. Release APK and AAB are built separately so only the hand-installed APK enables GitHub update notices. The website is maintained on `gh-pages` by a separate workflow.

Actual checks performed for this audit and remaining device/editorial gates are recorded in [project status](PROJECT_STATUS.md).
